package com.nexlua.app.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.color.MaterialColors

fun Drawable.wrap(context: Context, colorResId: Int): Drawable {
    val iconColor = MaterialColors.getColor(context, colorResId, 0)
    val colorStateList = ColorStateList.valueOf(iconColor)
    DrawableCompat.setTintList(DrawableCompat.wrap(mutate()), colorStateList)
    return this
}

fun Context.WrapDrawable(iconResId: Int, colorResId: Int): Drawable {
    return AppCompatResources.getDrawable(this, iconResId)!!.wrap(this, colorResId)
}
