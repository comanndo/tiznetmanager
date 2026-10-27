package com.tiznet.manager.util

import android.content.res.Resources

val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun dp(value: Int): Int = value.dp
