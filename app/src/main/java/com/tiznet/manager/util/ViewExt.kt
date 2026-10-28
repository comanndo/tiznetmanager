package com.tiznet.manager.util

import android.content.res.Resources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun dp(value: Int): Int = value.dp

/**
 * Keeps a scrollable form visible above the soft keyboard.
 *
 * The activity is edge-to-edge (enableEdgeToEdge), so adjustResize alone does
 * not shrink the content: IME space arrives as WindowInsets. We mirror the
 * larger of the IME and the navigation bar as bottom padding, which lets the
 * NestedScrollView resize and auto-scroll the focused field into view.
 */
fun android.view.View.applyImeInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val bottom = maxOf(ime.bottom, navBars.bottom)
        view.updatePadding(bottom = bottom)
        insets
    }
}
