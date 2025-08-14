package com.nexlua.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Environment
import com.google.android.material.color.DynamicColors
import java.io.File

class MyApplication : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: Context
            private set
        lateinit var appdir: File
            private set
    }
    override fun onCreate() {
        super.onCreate()
        instance = this.applicationContext
        appdir = File(Environment.getExternalStorageDirectory(), "Nexlua")
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}