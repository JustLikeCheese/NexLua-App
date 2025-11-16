package com.nexlua.app.activity

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.apksig.ApkSigner
import com.android.identity.util.UUID
import com.androlua.LuaEditor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nexlua.app.R
import com.nexlua.app.data.Project
import com.nexlua.app.data.ProjectManager
import com.nexlua.app.databinding.ActivityEditorBinding
import com.nexlua.app.util.BinUtil
import com.nexlua.app.util.DialogUtil
import com.nexlua.app.util.ZipUtil
import com.nexlua.app.util.installApk
import com.reandroid.apk.ApkModule
import com.reandroid.archive.FileInputSource
import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import es.dmoral.toasty.Toasty
import org.jf.smali.Smali
import org.jf.smali.SmaliOptions
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec


class EditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditorBinding
    private lateinit var project: Project
    private lateinit var editor: LuaEditor
    private var currentFile: File? = null
    // 定义一个 TAG 用于 Logcat 调试
    private val TAG = "EditorActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        editor = LuaEditor(this)
        project = ProjectManager.parseProject(File(intent.getStringExtra("path")))!!
        binding.root.addView(editor)
        editFile(File(File(project.projectDirectory, "src"), "main.lua"))
        supportActionBar?.title = project.name
        enableEdgeToEdge()
    }

    fun jsonArrayToStringArray(jsonArray: JSONArray?): Array<String> {
        if (jsonArray == null) return emptyArray()
        return Array(jsonArray.length()) { index -> jsonArray.getString(index) }
    }

    fun compileSmaliToDex(smaliDir: File, outputDexFile: File): Boolean {
        return try {
            val options = SmaliOptions()
            options.apiLevel = 21
            options.outputDexFile = outputDexFile.absolutePath
            Smali.assemble(options, smaliDir.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Smali.assemble failed", e)
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add("运行").setIcon(R.drawable.play_outline).setOnMenuItemClickListener {
            buildProject()
            true
        }.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add("撤销").setIcon(R.drawable.undo_variant).setOnMenuItemClickListener {
            editor.undo()
            true
        }.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add("重做").setIcon(R.drawable.redo_variant).setOnMenuItemClickListener {
            editor.redo()
            true
        }.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        // 菜单
        menu.add("打开").setOnMenuItemClickListener {
            chooseFile {
                saveFile()
                editFile(it)
            }
            true
        }
        menu.add("保存文件").setOnMenuItemClickListener {
            saveFile()
            Toasty.success(this, "保存文件 ${currentFile?.name ?: ""} 成功").show()
            true
        }
        menu.add("打包项目").setOnMenuItemClickListener {
            buildProject()
            true
        }
        menu.add("格式化代码").setOnMenuItemClickListener {
            if (currentFile != null) {
                // 判断当前文件是否为 Lua 文件
                val fileName = currentFile!!.name
                if (fileName.endsWith(".lua")) {
                    editor.format()
                } else if (fileName.endsWith(".json")) {
                    try {
                        val jsonObject = JSONObject(editor.text.toString())
                        val formattedJson = jsonObject.toString(4)
                        editor.setText(formattedJson)
                    } catch (e: JSONException) {
                        Toasty.error(this, "格式化 JSON 失败").show()
                    }
                }
            }
            true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPause() {
        super.onPause()
        saveFile()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveFile()
    }

    fun saveFile(): Boolean {
        if (currentFile != null) {
            currentFile!!.writeText(editor.text.toString())
            return true
        }
        return false
    }

    fun editFile(file: File) {
        if (file.isFile) {
            currentFile = file
            editor.setText(file.readText())
            supportActionBar?.subtitle =
                file.absolutePath.substring(project.projectDirectory.absolutePath.length + 1)
        }
    }

    fun buildProject() {
        saveFile()
        val listView = ListView(this)
        val listData = mutableListOf<String>()
        val listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listData)
        listView.adapter = listAdapter
        val log = { text: String ->
            runOnUiThread {
                listData.add(text)
                listAdapter.notifyDataSetChanged()
                listView.setSelection(listAdapter.count - 1)
            }
        }
        var apkfile: File? = null
        MaterialAlertDialogBuilder(this)
            .setTitle("打包项目")
            .setView(listView)
            .setCancelable(false)
            .setPositiveButton("安装") { dialog, which ->
                dialog.dismiss()
                if (apkfile != null && apkfile!!.isFile) {
                    installApk(apkfile!!)
                }
            }
            .setNegativeButton("取消") { dialog, which ->
                dialog.dismiss()
            }
            .show()

        Thread {
            // 初始化项目源码目录
            // MyApp1 (projectRootDir)
            // ├─src (projectDir)
            // │  └─main.lua
            // ├─build (buildRootDir)
            // │  ├─generated (generatedDir)
            // │  │  ├─assets
            // │  │  │  └─main.lua
            // │  │  ├─smali
            // │  │  │  └─com
            // │  │  │     └─nexlua
            // │  │  │         └─ LuaConfig.smali
            // │  ├─intermediates (intermediateDir)
            // │  │  └─ smali2dex
            // │  ├─outputs (outputDir)
            // │  │  ├─app-signed.apk
            // │  │  └─app-unsigned.apk
            // │  ├─tmp (tempDir)
            // │  │  └─template
            // └─manifest.json (projectConfigFile)
            val projectRootDir = project.projectDirectory
            val projectDir = File(projectRootDir, "src")
            val projectConfigFile = File(projectRootDir, "manifest.json")
            // 初始化构建项目文件夹
            val buildRootDir = File(projectRootDir, "build")
            val generatedDir = File(buildRootDir, "generated")
            val intermediateDir = File(buildRootDir, "intermediates")
            val outputDir = File(buildRootDir, "outputs")
            val tempDir = File(buildRootDir, "tmp")

            try {
                // 清除旧的缓存目录
                log("清除旧的缓存目录..")
                if (buildRootDir.exists()) buildRootDir.deleteRecursively()
                // 创建新的构建目录
                generatedDir.mkdirs()
                intermediateDir.mkdirs()
                outputDir.mkdirs()
                tempDir.mkdirs()

                // 读取项目配置 (manifest.json)
                log("载入项目 manifest.json 配置文件..")
                val projectConfig = JSONObject(projectConfigFile.readText())
                // 项目基本配置
                val appName = projectConfig.getString("name")
                val packageName = projectConfig.getString("packageName")
                val versionName = projectConfig.optString("versionName", "1.0.0")
                val versionCode = projectConfig.optInt("versionCode", 1)
                val minSdkVersion = projectConfig.optInt("minSdkVersion", 21)
                val targetSdkVersion = projectConfig.optInt("targetSdkVersion", 33)
                val projectIconName = projectConfig.optString("icon")
                val userPermissions =
                    jsonArrayToStringArray(projectConfig.optJSONArray("user_permissions"))
                // LuaConfig 类配置
                val luaEntry = projectConfig.optString("lua_entry", "main.lua")
                val requiredPermissionsInWelcome =
                    jsonArrayToStringArray(projectConfig.optJSONArray("required_permissions_in_welcome"))
                val requiredPermissions =
                    jsonArrayToStringArray(projectConfig.optJSONArray("required_permissions"))
                val onlyDecompress =
                    jsonArrayToStringArray(projectConfig.optJSONArray("only_decompress"))
                val skipDecompress =
                    jsonArrayToStringArray(projectConfig.optJSONArray("skip_decompress"))
                val protectedFile =
                    jsonArrayToStringArray(projectConfig.optJSONArray("protected_files"))

                // 生成 AndroidManifest.xml
                log("正在构建 AndroidManifest.xml..")
                val axmlModule = ApkModule()
                val axmlTableBlock = TableBlock()
                val axmlManifestBlock = AndroidManifestBlock()
                axmlModule.setManifest(axmlManifestBlock)
                axmlModule.tableBlock = axmlTableBlock
                // 设置基本属性
                axmlManifestBlock.packageName = packageName
                axmlManifestBlock.versionCode = versionCode
                axmlManifestBlock.versionName = versionName
                axmlManifestBlock.minSdkVersion = minSdkVersion
                axmlManifestBlock.targetSdkVersion = targetSdkVersion
                // 插入权限
                for (permission in userPermissions) {
                    axmlManifestBlock.addUsesPermission("android.permission.$permission")
                }
                // 设置 Application 标签的属性
                axmlManifestBlock.getOrCreateApplicationElement().apply {
                    getOrCreateAndroidAttribute("name", AndroidManifestBlock.ID_name)
                        .setValueAsString("com.nexlua.LuaApplication")
                    getOrCreateAndroidAttribute("largeHeap", 0x0101025a)
                        .setValueAsBoolean(true)
                    getOrCreateAndroidAttribute("extractNativeLibs", 0x010104ea)
                        .setValueAsBoolean(true)
                }
                // 添加 Welcome 界面
                axmlManifestBlock.getOrCreateMainActivity("com.nexlua.Welcome").apply {
                    getOrCreateAndroidAttribute("exported", 0x01010010)
                        .setValueAsBoolean(true)
                }
                // 添加 Main 界面
                axmlManifestBlock.getOrCreateActivity("com.nexlua.Main", false).apply {
                    getOrCreateAndroidAttribute("exported", 0x01010010)
                        .setValueAsBoolean(false)
                }
                // 添加 LuaActivity 界面
                axmlManifestBlock.getOrCreateActivity("com.nexlua.LuaActivity", false).apply {
                    getOrCreateAndroidAttribute("exported", 0x01010010)
                        .setValueAsBoolean(false)
                }
                // 生成 resources.arsc 并链接到 Manifest
                log("正在生成 resources.arsc 资源文件..")
                val packageBlock = axmlTableBlock.newPackage(0x7f, packageName)
                // 应用名称 string/name
                val appNameEntry = packageBlock.getOrCreate("", "string", "app_name")
                appNameEntry.setValueAsString(appName)
                axmlManifestBlock.setApplicationLabel(appNameEntry.resourceId)
                // 应用图标 drawable/icon
                if (!projectIconName.isNullOrEmpty()) {
                    val iconFile = File(projectDir, projectIconName)
                    if (iconFile.exists() && iconFile.isFile) {
                        log("添加项目图标: $projectIconName")
                        val iconEntry = packageBlock.getOrCreate("", "drawable", "app_icon")
                        val iconPathInApk = "res/drawable/app_icon.png"
                        axmlModule.add(FileInputSource(iconFile, iconPathInApk))
                        iconEntry.setValueAsString(iconPathInApk)
                        axmlManifestBlock.setIconResourceId(iconEntry.resourceId)
                    } else {
                        log("警告: 项目图标文件 '$projectIconName' 未找到。")
                    }
                }
                // 创建 SMALI 文件夹
                val smaliRootDir = File(generatedDir, "smali")
                val smaliSrcDir = File(File(smaliRootDir, "com"), "nexlua")
                smaliSrcDir.mkdirs()
                val protectedFileMappings = mutableMapOf<String, String>()
                // 添加 Lua 文件到 assets, 文件在 protected_files 中, 则抽离到 Smali
                log("正在添加 assets 文件..")
                projectDir.walk().forEach { file ->
                    if (file.isFile) {
                        var isProtected = false
                        for (fileName in protectedFile) {
                            if (file.name == fileName) {
                                val randomClassName = "ProtectedLoader_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                                val content = BinUtil.binLuaModule(randomClassName, file.readText())
                                File(smaliSrcDir, "$randomClassName.smali").writeText(content)
                                protectedFileMappings[fileName] = randomClassName
                                log("$fileName smali 已生成")
                                isProtected = true
                                break
                            }
                        }
                        if (!isProtected) {
                            val entryName = "assets/" + file.relativeTo(projectDir).path
                            axmlModule.add(FileInputSource(file, entryName))
                        }
                    }
                }
                // 编译 LuaConfig.smali
                log("正在编译 manifest.json 配置..")
                val luaConfigInitCode = BinUtil.generateClinitSmali(luaEntry, requiredPermissionsInWelcome, requiredPermissions, onlyDecompress, skipDecompress, protectedFileMappings)
                val configCode = String.format(BinUtil.LUA_CONFIG_TEMPLATE, luaConfigInitCode)
                File(smaliSrcDir, "LuaConfig.smali").writeText(configCode)

                // Smali2Dex
                val classes2dex = File(intermediateDir, "classes2.dex")
                log("准备编译 Smali 到 DEX..")
                // 【最终修复】确保在编译前，所有文件写入操作都已完成
                System.gc() // 建议性的垃圾回收，有时有助于 I/O 刷新
                Thread.sleep(100) // 短暂等待，确保文件系统同步

                if (!compileSmaliToDex(smaliRootDir, classes2dex) || !classes2dex.exists() || classes2dex.length() == 0L) {
                    log("!!!!!! Smali.assemble 执行失败 !!!!!!")
                    throw IllegalStateException("Smali compilation failed. Check Smali syntax or runtime logs.")
                }
                log("Smali2Dex 成功！classes2.dex 大小: ${classes2dex.length()} 字节")
                axmlModule.add(FileInputSource(classes2dex, "classes2.dex"))

                // 添加模版文件
                log("正在添加模版文件..")
                val templateDir = File(tempDir, "template")
                templateDir.mkdir()
                ZipUtil.unzipFromAssets(this, "template.zip", templateDir.absolutePath)
                templateDir.walk().forEach { file ->
                    if (file.isFile) {
                        val entryName = file.relativeTo(templateDir).path
                        if (!entryName.equals("AndroidManifest.xml", ignoreCase = true) &&
                            !entryName.equals("resources.arsc", ignoreCase = true)
                        ) {
                            axmlModule.add(FileInputSource(file, entryName))
                        }
                    }
                }
                // 构建最终 APK
                log("正在构建 APK 安装包..")
                val unsignedApkFile = File(outputDir, "app-unsigned.apk")
                axmlModule.writeApk(unsignedApkFile)
                // 签名 APK
                log("正在签名 APK 安装包..")
                apkfile = File(outputDir, "app-signed.apk")
                if (signApk(unsignedApkFile, apkfile, minSdkVersion)) {
                    runOnUiThread {
                        Toasty.success(this, "项目打包完成").show()
                    }
                } else {
                    runOnUiThread {
                        Toasty.error(this, "项目签名失败!").show()
                    }
                }
            } catch (e: Exception) {
                // 使用带有完整堆栈追踪的日志
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val stackTrace = sw.toString()

                Log.e(TAG, "项目打包失败!", e)
                runOnUiThread {
                    Toasty.error(this, "项目打包失败!").show()
                }
                log("======= 构建失败 =======")
                log(stackTrace)
            }
        }.start()
    }

    fun signApk(unsignedApk: File, signedApk: File, minSdkVersion: Int): Boolean {
        var privateKeyStream: InputStream? = null
        var certificateStream: InputStream? = null

        try {
            val assets = applicationContext.assets
            privateKeyStream = assets.open("testkey.pk8")
            certificateStream = assets.open("testkey.x509.pem")

            val privateKeyBytes = privateKeyStream.readBytes()
            val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKey: PrivateKey = keyFactory.generatePrivate(keySpec)

            val certFactory = CertificateFactory.getInstance("X.509")
            val certificate: X509Certificate =
                certFactory.generateCertificate(certificateStream) as X509Certificate
            val signerConfig =
                ApkSigner.SignerConfig.Builder("CERT", privateKey, listOf(certificate)).build()
            val apkSignerBuilder = ApkSigner.Builder(listOf(signerConfig)).apply {
                setInputApk(unsignedApk)
                setOutputApk(signedApk)
                setV1SigningEnabled(true)
                setV2SigningEnabled(true)
                setV3SigningEnabled(true)
                // 传递 minSdkVersion 对 v2+ 签名很重要
                setMinSdkVersion(minSdkVersion)
            }
            val apkSigner = apkSignerBuilder.build()
            apkSigner.sign()
            return true

        } catch (e: Exception) {
            Log.e(TAG, "签名失败", e)
            return false
        } finally {
            privateKeyStream?.close()
            certificateStream?.close()
        }
    }

    fun chooseFile(callback: (File) -> Unit) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val message = TextView(this)
        val listView = ListView(this)
        layout.addView(message)
        layout.addView(listView)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        listView.adapter = adapter
        var currentDirectory = currentFile?.parentFile ?: project.projectDirectory
        var listFiles = listOf<File>()
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("选择文件")
            .setPositiveButton("确定", null)
            .setView(layout)
            .show()

        // 更新列表显示
        fun updateAdapter() {
            adapter.clear()
            adapter.add("..")
            listFiles = currentDirectory.listFiles()?.sortedWith(
                compareBy({ !it.isDirectory }, { it.name.lowercase() })
            ) ?: emptyList()
            listFiles.forEach { adapter.add(if (it.isDirectory) "${it.name}/" else it.name) }
            message.text = "当前目录: ${currentDirectory.absolutePath}"
        }

        // 文件操作
        fun fileOperation(file: File? = null) {
            val options = if (file == null) arrayOf("新建文件", "新建目录")
            else arrayOf("新建文件", "新建目录", "重命名", "删除")
            MaterialAlertDialogBuilder(this)
                .setTitle(if (file == null) "文件操作" else "文件 ${file.name}")
                .setItems(options) { _, which ->
                    when (options[which]) {
                        "新建文件" -> DialogUtil.showInputDialog(
                            this,
                            "新建文件",
                            "",
                            "请输入文件名"
                        ) { name ->
                            val newFile = File(currentDirectory, name)
                            when {
                                name.isEmpty() -> {
                                    Toasty.error(this, "文件名不能为空").show(); false
                                }
                                newFile.exists() -> {
                                    Toasty.error(this, "文件已存在").show(); false
                                }
                                !newFile.createNewFile() -> {
                                    Toasty.error(this, "创建文件失败").show(); false
                                }
                                else -> {
                                    saveFile()
                                    editFile(newFile)
                                    Toasty.success(this, "文件 ${newFile.name} 创建成功").show()
                                    true
                                }
                            }
                        }
                        "新建目录" -> DialogUtil.showInputDialog(
                            this,
                            "新建目录",
                            "",
                            "请输入目录名称"
                        ) { name ->
                            val newDir = File(currentDirectory, name)
                            when {
                                name.isEmpty() -> {
                                    Toasty.error(this, "目录名称不能为空").show(); false
                                }
                                newDir.exists() -> {
                                    Toasty.error(this, "目录已存在").show(); false
                                }
                                !newDir.mkdir() -> {
                                    Toasty.error(this, "创建目录失败").show(); false
                                }
                                else -> {
                                    Toasty.success(this, "创建目录 $name 成功")
                                        .show(); updateAdapter(); true
                                }
                            }
                        }
                        "重命名" -> file?.let { f ->
                            DialogUtil.showInputDialog(
                                this,
                                "重命名文件",
                                f.name,
                                "原文件: ${f.name}"
                            ) { newName ->
                                val newFile = File(currentDirectory, newName)
                                when {
                                    newName.isEmpty() -> {
                                        Toasty.error(this, "新名称不能为空").show(); false
                                    }
                                    newFile.exists() -> {
                                        Toasty.error(this, "同名文件已存在").show(); false
                                    }
                                    !f.renameTo(newFile) -> {
                                        Toasty.error(this, "重命名失败").show(); false
                                    }
                                    else -> {
                                        if (currentFile?.absolutePath == f.absolutePath) currentFile =
                                            newFile; Toasty.success(this, "文件已重命名为 $newName")
                                            .show(); updateAdapter(); true
                                    }
                                }
                            }
                        }
                        "删除" -> file?.let { f ->
                            DialogUtil.showDialog(this, "删除文件", "确定要删除 ${f.name} 吗?") {
                                val success = if (f.isFile) f.delete() else f.deleteRecursively()
                                if (success) Toasty.success(this, "删除成功")
                                    .show() else Toasty.error(this, "删除失败").show()
                                if (currentFile?.absolutePath == f.absolutePath) currentFile = null
                                updateAdapter()
                                true
                            }
                        }
                    }
                }.show()
        }
        updateAdapter()
        listView.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                currentDirectory.parentFile?.let { currentDirectory = it; updateAdapter() }
            } else {
                val file = listFiles[position - 1]
                if (file.isDirectory) {
                    currentDirectory = file; updateAdapter()
                } else {
                    dialog.dismiss(); callback(file)
                }
            }
        }
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val file = if (position > 0) listFiles[position - 1] else null
            fileOperation(file)
            true
        }
    }
}