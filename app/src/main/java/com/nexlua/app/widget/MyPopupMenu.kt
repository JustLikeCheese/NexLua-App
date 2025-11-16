package com.nexlua.app.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.DrawableCompat

class MyPopupMenu(private val context: Context, anchor: View) : PopupMenu(context, anchor) {
    var colorStateList: ColorStateList? = null

    init {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
        colorStateList = if (typedValue.resourceId != 0) {
            AppCompatResources.getColorStateList(context, typedValue.resourceId)
        } else {
            ColorStateList.valueOf(typedValue.data)
        }

    }

    fun add(title: String, icon: Drawable, listener: MenuItem.OnMenuItemClickListener): MenuItem {
        val wrappedDrawable = DrawableCompat.wrap(icon.mutate())
        DrawableCompat.setTintList(wrappedDrawable, colorStateList)
        return menu.add(title).setIcon(wrappedDrawable).setOnMenuItemClickListener(listener)
    }

    fun add(title: String, icon: Int, listener: MenuItem.OnMenuItemClickListener): MenuItem {
        return add(title, AppCompatResources.getDrawable(context, icon)!!, listener)
    }
}
