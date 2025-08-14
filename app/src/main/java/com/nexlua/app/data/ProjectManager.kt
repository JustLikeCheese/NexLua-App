package com.nexlua.app.data

import android.annotation.SuppressLint
import android.content.Context
import com.nexlua.app.MyApplication

@SuppressLint("StaticFieldLeak")
object ProjectManager {
    private val context: Context by lazy { MyApplication.instance }
    private val projects = mutableListOf<Project>()
    fun getProjects(): List<Project> {
        return projects
    }
}