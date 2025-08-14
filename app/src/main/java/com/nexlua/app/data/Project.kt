package com.nexlua.app.data

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import java.io.File

data class Project(
    val name: String?, // 项目名称
    val packageName: String, // 项目包名
    val description: String?, // 项目描述
    val icon: Drawable?, // 项目图标
    val file: File // 项目目录
)