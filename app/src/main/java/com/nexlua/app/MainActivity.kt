package com.nexlua.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.nexlua.app.databinding.ActivityMainBinding
import com.nexlua.app.util.WrapDrawable

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val motivationalQuotes = arrayOf(
        "今天过的怎样？",
        "你知道吗？编程是艺术与技术的结合",
        "代码改变世界",
        "每一次调试都是成长的机会",
        "编程不是工作，是一种生活方式",
        "你知道吗？群主没唧唧"
    )
    private var currentQuoteIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private val updateQuoteRunnable = object : Runnable {
        override fun run() {
            currentQuoteIndex = (currentQuoteIndex + 1) % motivationalQuotes.size
            val slideOutUp = AnimationUtils.loadAnimation(this@MainActivity, R.anim.slide_out_up)
            slideOutUp.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    binding.motivationalText.text = motivationalQuotes[currentQuoteIndex]
                    binding.motivationalText.startAnimation(
                        AnimationUtils.loadAnimation(
                            this@MainActivity,
                            R.anim.slide_in_up
                        )
                    )
                }
            })
            binding.motivationalText.startAnimation(slideOutUp)
            handler.postDelayed(this, 5000) // 每5秒更新一次
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        val navView: BottomNavigationView = binding.navView
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_project,
                R.id.navigation_plugin,
                R.id.navigation_docs,
                R.id.navigation_home
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        handler.post(updateQuoteRunnable)
        binding.searchContainer.setOnClickListener {
            Toast.makeText(this, "搜索框被点击", Toast.LENGTH_SHORT).show()
        }
        with(binding.menu) {
            overflowIcon = WrapDrawable(
                R.drawable.ic_dots_vertical,
                com.google.android.material.R.attr.colorOnSurface
            )
            val colorAttr = com.google.android.material.R.attr.colorOnSurfaceVariant
            menu.add(R.string.new_project)
                .setIcon(WrapDrawable(R.drawable.ic_folder_plus_outline, colorAttr))
                .setOnMenuItemClickListener {
                    true
                }
            menu.add(R.string.import_project)
                .setIcon(WrapDrawable(R.drawable.ic_file_import_outline, colorAttr))
                .setOnMenuItemClickListener {
                    true
                }
            menu.add(R.string.install_plugin)
                .setIcon(WrapDrawable(R.drawable.ic_plugin_outline, colorAttr))
                .setOnMenuItemClickListener {
                    true
                }
            menu.add(R.string.settings)
                .setIcon(WrapDrawable(R.drawable.ic_cog_outline, colorAttr))
                .setOnMenuItemClickListener {
                    true
                }
            try {
                val method = menu.javaClass.getDeclaredMethod(
                    "setOptionalIconsVisible",
                    java.lang.Boolean.TYPE
                )
                method.isAccessible = true
                method.invoke(menu, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        enableEdgeToEdge()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateQuoteRunnable)
    }
}