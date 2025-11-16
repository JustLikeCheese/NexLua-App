package com.nexlua.app.data

import java.io.File

data class Project(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val projectDirectory: File,
    val projectFile: File,
    val projectIcon: String,
)