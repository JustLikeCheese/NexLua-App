package com.nexlua.app.data

import android.util.Log
import com.nexlua.app.MyApplication
import org.json.JSONObject
import java.io.File

object ProjectManager {
    val projectDirectory = File(MyApplication.appdir, "project")
    fun parseProject(projectFile: File): Project? {
        return try {
            val json = JSONObject(projectFile.readText())
            Project(
                name = json.getString("name"),
                packageName = json.getString("packageName"),
                versionName = json.optString("versionName", "1.0.0"),
                versionCode = json.optInt("versionCode", 1),
                projectDirectory = projectFile.parentFile!!,
                projectFile = projectFile,
                projectIcon = json.optString("icon", "icon.png")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun listProjects(): MutableList<Project> {
        return projectDirectory.listFiles()?.filter { it.isDirectory }?.mapNotNull {
            val projectFile = File(it, "manifest.json")
            if (projectFile.exists()) {
                parseProject(projectFile)
            } else {
                null
            }
        }?.toMutableList() ?: mutableListOf()
    }


    fun toJsonObject(project: Project) : JSONObject {
        return JSONObject().apply {
            put("name", project.name)
            put("packageName", project.packageName)
            put("versionName", project.versionName)
            put("versionCode", project.versionCode)
            put("projectIcon", project.projectIcon)
        }
    }
}