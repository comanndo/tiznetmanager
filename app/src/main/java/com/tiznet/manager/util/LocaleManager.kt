package com.tiznet.manager.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * In-app language switching.
 *
 * Persian is the default regardless of the device locale (the app ships fa as
 * its default resources). The chosen tag is applied through AppCompat's
 * per-app locale API, which recreates the active activity and works back to
 * API 24 via the appcompat 1.6+ backport.
 */
object LocaleManager {

    const val PERSIAN = "fa"
    const val ENGLISH = "en"
    const val SYSTEM = ""

    val supported = listOf(PERSIAN, ENGLISH)

    fun apply(languageTag: String) {
        val locales = if (languageTag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList() // follow system settings
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
