package com.nexlua.app.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DialogUtil {
    fun showInputDialog(
        context: Context,
        title: String,
        text: String = "",
        hint: String = "",
        callback: (String) -> Boolean
    ) {
        val editText = AppCompatEditText(context)
        editText.setText(text)
        editText.hint = hint
        lateinit var dialog: AlertDialog
        dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(editText)
            .setPositiveButton("确定") { _, _ -> if (callback(editText.text.toString())) dialog.dismiss() }
            .setNegativeButton("取消", null)
            .show()
    }
    fun showDialog(
        context: Context,
        title: String,
        message: String,
        callback: () -> Boolean
    ) {
        lateinit var dialog: AlertDialog
        dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定") { _, _ -> if (callback()) dialog.dismiss() }
            .setNegativeButton("取消", null)
            .show()
    }
}