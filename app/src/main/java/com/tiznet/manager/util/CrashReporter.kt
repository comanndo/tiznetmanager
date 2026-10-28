package com.tiznet.manager.util

import android.content.Context
import android.os.Build
import com.tiznet.manager.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

/**
 * Global uncaught-exception recorder.
 *
 * Writes the full stack trace plus device/app metadata to cacheDir/crash_log.txt
 * before handing the exception back to the platform default handler. On the next
 * launch MainActivity detects the file and shows a shareable error dialog, so
 * startup crashes can be diagnosed without logcat access.
 */
object CrashReporter {

    private const val FILE_NAME = "crash_log.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrash(appContext, thread, throwable) }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val body = buildString {
            appendLine("TizNet Manager — Crash Report")
            appendLine("==============================")
            appendLine("Time: ${Date()}")
            appendLine("Thread: ${thread.name}")
            appendLine("App version: ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Product: ${Build.PRODUCT}")
            appendLine()
            appendLine(sw.toString())
        }
        crashFile(context).writeText(body)
    }

    fun hasPendingCrash(context: Context): Boolean = crashFile(context).exists()

    fun readPendingCrash(context: Context): String =
        crashFile(context).takeIf { it.exists() }?.readText().orEmpty()

    fun clear(context: Context) {
        crashFile(context).delete()
    }

    fun crashFile(context: Context): File = File(context.cacheDir, FILE_NAME)
}
