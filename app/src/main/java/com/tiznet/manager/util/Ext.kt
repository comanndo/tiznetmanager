package com.tiznet.manager.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.tiznet.manager.data.remote.ApiException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Runs an API call and maps every failure to an [ApiException] with a Persian message. */
suspend fun <T> apiCall(block: suspend () -> T): T = try {
    block()
} catch (t: Throwable) {
    throw ApiException.from(t, "ارتباط با پنل برقرار نشد")
}

fun View.visible(show: Boolean) {
    visibility = if (show) View.VISIBLE else View.GONE
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun Fragment.toast(message: String, long: Boolean = false) {
    Toast.makeText(requireContext(), message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun View.snack(message: String, actionText: String? = null, action: (() -> Unit)? = null) {
    val snack = Snackbar.make(this, message, Snackbar.LENGTH_LONG)
    if (actionText != null && action != null) snack.setAction(actionText) { action() }
    snack.show()
}

fun Context.copyToClipboard(label: String, text: String) {
    val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
}

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
private val displayDate = SimpleDateFormat("yyyy/MM/dd", Locale.US)
private val displayDateTime = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)

/** Parses ISO/date strings the panel sends and renders them in Persian-friendly numeric format. */
fun formatDate(input: String?): String {
    if (input.isNullOrBlank()) return "—"
    return runCatching {
        val parsed = when {
            input.contains("T") -> SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(input.substring(0, minOf(19, input.length)))
            input.length >= 10 -> dateFormat.parse(input.substring(0, 10))
            else -> null
        }
        if (parsed != null) displayDate.format(parsed) else input.substring(0, minOf(10, input.length))
    }.getOrDefault(input)
}

fun formatDateTime(input: String?): String {
    if (input.isNullOrBlank()) return "—"
    return runCatching {
        val clean = input.substring(0, minOf(19, input.length)).replace("T", " ")
        val parsed = dateTimeFormat.parse(clean)
        if (parsed != null) displayDateTime.format(parsed) else clean
    }.getOrDefault(input)
}

fun formatEpoch(epochSeconds: Long?): String {
    if (epochSeconds == null || epochSeconds <= 0) return "—"
    return displayDateTime.format(Date(epochSeconds * 1000))
}

/** Human readable bytes: 1536 -> "1.5 GB". */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return String.format(Locale.US, "%.2f %s", value, units[index])
}

fun formatGb(gb: Double): String = String.format(Locale.US, "%.2f GB", gb)

fun formatPercent(value: Double): String = String.format(Locale.US, "%.0f%%", value)

fun relativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return runCatching {
        val clean = iso.substring(0, minOf(19, iso.length)).replace("T", " ")
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(clean) ?: return@runCatching iso
        val diff = (System.currentTimeMillis() - time.time) / 1000
        when {
            diff < 60 -> "همین حالا"
            diff < 3600 -> "${diff / 60} دقیقه پیش"
            diff < 86400 -> "${diff / 3600} ساعت پیش"
            diff < 86400 * 30 -> "${diff / 86400} روز پیش"
            else -> formatDate(iso)
        }
    }.getOrDefault(iso)
}

fun uptimeText(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return buildString {
        if (days > 0) append("${days}d ")
        append("${hours}h ${minutes}m")
    }
}
