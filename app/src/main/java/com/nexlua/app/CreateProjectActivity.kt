package com.nexlua.app

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nexlua.app.data.Project
import com.nexlua.app.data.ProjectManager
import com.nexlua.app.databinding.ActivityCreateProjectBinding
import com.nexlua.app.util.copyAssetToFile
import es.dmoral.toasty.Toasty
import org.json.JSONArray
import java.io.File
import java.util.Locale.getDefault

class CreateProjectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateProjectBinding

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateProjectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        with(binding) {
            // 设置默认名称
            var index = 1
            val baseName = "MyApp"
            val basePackageName = "com.mycompany"
            var generateName = baseName
            var generatePackageName = "$basePackageName.${baseName.lowercase(getDefault())}"
            while (true) {
                if (index != 0) {
                    generateName = "$baseName$index"
                    generatePackageName = "$basePackageName.${generateName.lowercase(getDefault())}"
                }
                if (!File(ProjectManager.projectDirectory, generateName).exists()) {
                    projectNameInput.setText(generateName)
                    packageNameInput.setText(generatePackageName)
                    break
                }
                index++
            }
            // 监听 InputLayout 的输入
            projectNameInput.doAfterTextChanged { editable ->
                checkProjectExists();
            }
            // 创建项目逻辑
            createProjectFab.setOnClickListener {
                val name = projectNameInput.text.toString()
                val packageName = packageNameInput.text.toString()
                val projectDir = checkProjectExists()
                if (projectDir == null) return@setOnClickListener
                if (name.isEmpty()) {
                    projectNameLayout.error = "项目名称不能为空"
                    projectNameLayout.isErrorEnabled = true
                    projectNameInput.requestFocus()
                    projectNameInput.post {
                        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                            projectNameInput,
                            InputMethodManager.SHOW_IMPLICIT
                        )
                    }
                } else if (packageName.isEmpty()) {
                    packageNameLayout.error = "项目包名不能为空"
                    packageNameLayout.isErrorEnabled = true
                    packageNameInput.requestFocus()
                    packageNameInput.post {
                        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                            packageNameInput,
                            InputMethodManager.SHOW_IMPLICIT
                        )
                    }
                } else {
                    val projectFile = File(projectDir, "manifest.json")
                    projectDir.mkdirs()
                    projectFile.writeText(
                        ProjectManager.toJsonObject(Project(
                            name = name,
                            packageName = packageName,
                            versionName = "1.0.0",
                            versionCode = 1,
                            projectDirectory = projectDir,
                            projectFile = projectFile,
                            projectIcon = "icon.png"
                        )).put("user_permissions",JSONArray())
                            .put("minSdkVersion", 21)
                            .put("targetSdkVersion", 33)
                            .put("required_permissions_in_welcome",JSONArray())
                            .put("required_permissions", JSONArray())
                            .put("only_decompress", JSONArray())
                            .put("skip_decompress", JSONArray())
                            .put("protected_files", JSONArray()).toString(4)
                    )
                    val sourceDir = File(projectDir, "src")
                    val mainFile = File(sourceDir, "main.lua")
                    sourceDir.mkdir()
                    mainFile.writeText(
                        """
                            print("Hello World!")
                        """.trimIndent()
                    )
                    // 复制 Assets 的 icon.png 到项目目录
                    copyAssetToFile("icon.png", File(projectDir, "icon.png"))
                    Toasty.success(this@CreateProjectActivity, "项目创建成功").show()
                    finish()
                }
            }
        }
        enableEdgeToEdge()
    }

    fun checkProjectExists(): File? {
        with(binding) {
            val name = projectNameInput.text.toString()
            val file = File(ProjectManager.projectDirectory, name)
            if (file.exists()) {
                projectNameLayout.error = "项目已存在: ${file.absolutePath}"
                projectNameLayout.isErrorEnabled = true
                projectNameInput.requestFocus()
                projectNameInput.post {
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                        projectNameInput,
                        InputMethodManager.SHOW_IMPLICIT
                    )
                }
                return null
            }
            projectNameLayout.error = null
            projectNameLayout.isErrorEnabled = false
            return file
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.create_project_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_help -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("创建项目帮助")
                    .setMessage(
                        "项目名称:\n" + "即项目在编辑器中的名称、应用打包后的名称\n\n" +
                                "项目包名:\n" + "包名是应用的唯一标识, 用于给区分不同的应用（Tips: 安卓系统是使用包名来区分不同的应用）, 包名必须以字母开头, 只能包含字母(a-z)、数字(0-9)、下划线(_)、和点。不允许包含空格或特殊字符（如 @, #, \$ 等）。"
                    )
                    .setPositiveButton("知道了", null)
                    .show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}