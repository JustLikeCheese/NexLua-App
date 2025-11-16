package com.nexlua.app.plugin

data class PluginInfo(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val versionName: String,
    val versionCode: Int,
    val entry: String,
    val icon: String,
    val actions: Array<Class<*>>,
)