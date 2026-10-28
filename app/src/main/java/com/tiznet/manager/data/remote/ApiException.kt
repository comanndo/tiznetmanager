package com.tiznet.manager.data.remote

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Domain-level error with a Persian-friendly message and the HTTP status, when known. */
class ApiException(
    val statusCode: Int,
    override val message: String,
    val serverDetail: String? = null,
    cause: Throwable? = null
) : IOException(message, cause) {

    val isUnauthorized: Boolean get() = statusCode == 401
    val isForbidden: Boolean get() = statusCode == 403
    val isNotFound: Boolean get() = statusCode == 404

    companion object {
        fun from(throwable: Throwable, fallback: String): ApiException = when (throwable) {
            is ApiException -> throwable
            is HttpException -> {
                val detail = parseServerMessage(throwable)
                val code = throwable.code()
                val text = when (code) {
                    400, 422 -> detail ?: "درخواست نامعتبر بود"
                    401 -> "نام کاربری یا رمز عبور اشتباه است"
                    403 -> "به این بخش دسترسی ندارید"
                    404 -> "این بخش روی پنل پیدا نشد؛ نسخه پنل شما قدیمی است و این API فقط در هیدیفای ۱۳ به بالا وجود دارد"
                    429 -> "تعداد درخواست‌ها زیاد است، کمی صبر کنید"
                    in 500..599 -> "خطای داخلی سرور هیدیفای"
                    else -> detail ?: fallback
                }
                // Keep the HTTP status visible so connection problems are diagnosable.
                ApiException(code, "$text (HTTP $code)", detail, throwable)
            }
            is UnknownHostException ->
                ApiException(-1, "آدرس پنل در دسترس نیست؛ اتصال شبکه و آدرس را بررسی کنید", cause = throwable)
            is SocketTimeoutException ->
                ApiException(-1, "پاسخ پنل با تاخیر مواجه شد؛ دوباره تلاش کنید", cause = throwable)
            is IOException ->
                ApiException(-1, "ارتباط با پنل برقرار نشد", cause = throwable)
            else -> ApiException(-1, throwable.message ?: fallback, cause = throwable)
        }

        private fun parseServerMessage(error: HttpException): String? {
            val raw = error.response()?.errorBody()?.string() ?: return null
            // Hiddify answers with {"message": ...} / {"msg": ...} / {"detail": ...}
            val messageMatch = Regex("\"(?:message|msg)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(raw)
            val detailMatch = Regex("\"detail\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(raw)
            return (messageMatch ?: detailMatch)?.groupValues?.get(1)
                ?.replace("\\\"", "\"")?.replace("\\\\", "\\")?.takeIf { it.isNotBlank() }
        }
    }
}
