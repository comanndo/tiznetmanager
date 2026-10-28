package com.tiznet.manager.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Hiddify authentication handshake.
 *
 * Modern Hiddify (v11+) does NOT accept HTTP Basic auth together with a
 * password: the Basic branch only matches the legacy `username` column, which
 * is empty for current admins (super admins never had one). The web panel signs
 * in through an HTML form — GET the login page to obtain a CSRF token, POST the
 * credentials, and reuse the returned session cookie for the Admin V2 API.
 *
 * Older panels used `secret_textbox`/`password_textbox` field names, so the
 * POST carries both naming schemes. When the form flow is unavailable we still
 * fall back to Basic auth / the Hiddify-API-Key header in [PanelAuthInterceptor].
 */
object PanelAuth {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Matches the hidden Flask-WTF CSRF input regardless of attribute order.
    private val CSRF_REGEX = Regex(
        "<input[^>]*name=[\"']csrf_token[\"'][^>]*>",
        RegexOption.IGNORE_CASE
    )
    private val VALUE_REGEX = Regex("value=[\"']([^\"']+)[\"']")

    sealed interface Result {
        data object Success : Result
        data class Failure(val status: Int, val technical: String) : Result
    }

    private fun bareClient(jar: PanelCookieJar, followRedirects: Boolean = true): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(followRedirects)
            .followSslRedirects(followRedirects)
            .cookieJar(jar)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "text/html,application/json;q=0.9,*/*;q=0.8")
                        .build()
                )
            }
            .build()

    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/126.0.0.0 Mobile Safari/537.36 TizNetManager/1.0"

    /**
     * Resolves the proxy path when the user left the field blank.
     * The bootstrap endpoint answers with JSON, a login redirect (whose
     * Location starts with the proxy path), or a 401.
     */
    fun discoverProxyPath(baseUrl: String): String? {
        val client = bareClient(PanelCookieJar(), followRedirects = false)
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/__admin_v2_bootstrap")
            .header("Accept", "application/json")
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    runCatching {
                        json.parseToJsonElement(body).jsonObject["proxy_path"]
                            ?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                    }.getOrNull()?.let { return it.trim('/') }
                }
                val location = response.header("Location").orEmpty()
                return parseProxyFromLocation(location)
            }
        }
        return null
    }

    private fun parseProxyFromLocation(location: String): String? {
        if (location.isBlank()) return null
        val absolute = location.toHttpUrlOrNull()
        val segments = absolute?.pathSegments
            ?: location.substringAfter("://", location).trimStart('/').split('?', '/')
        return segments.firstOrNull { it.isNotBlank() && it != "admin" }
    }

    /**
     * Performs the browser-style form login and leaves the session cookie in [jar].
     */
    fun formLogin(
        baseUrl: String,
        proxyPath: String,
        username: String,
        password: String,
        jar: PanelCookieJar
    ): Result {
        val client = bareClient(jar)
        val loginUrl = buildLoginUrl(baseUrl, proxyPath)

        val loginPage = runCatching {
            client.newCall(Request.Builder().url(loginUrl).get().build()).execute()
        }.getOrElse { return Result.Failure(-1, it.message ?: "login page unreachable") }

        loginPage.use { response ->
            if (!response.isSuccessful) {
                return Result.Failure(response.code, "login page HTTP ${response.code}")
            }
            val html = response.body?.string().orEmpty()
            val token = extractCsrf(html)
                ?: return Result.Failure(-1, "CSRF token not found in login page")

            val form = FormBody.Builder()
                .add("csrf_token", token)
                .add("next", "")
                // Current Admin V2 login form
                .add("username", username)
                .add("password", password)
                // Legacy (Hiddify <= 10 era) login form
                .add("secret_textbox", username)
                .add("password_textbox", password)
                .build()

            val postResponse = runCatching {
                client.newCall(
                    Request.Builder().url(loginUrl).post(form).build()
                ).execute()
            }.getOrElse { return Result.Failure(-1, it.message ?: "login POST failed") }

            postResponse.use { resp ->
                // 401 = invalid credentials, 429 = throttle, 400 = expired CSRF
                if (resp.code in 200..399 && jar.hasSessionCookie()) return Result.Success
                return Result.Failure(
                    resp.code,
                    "form login HTTP ${resp.code} (session cookie " +
                        if (jar.hasSessionCookie()) "present)" else "missing)"
                )
            }
        }
    }

    private fun buildLoginUrl(baseUrl: String, proxyPath: String): String {
        val base = baseUrl.trimEnd('/')
        val path = proxyPath.trim('/')
        return if (path.isBlank()) "$base/" else "$base/$path/"
    }

    private fun extractCsrf(html: String): String? =
        CSRF_REGEX.find(html)?.value?.let { tag ->
            VALUE_REGEX.find(tag)?.groupValues?.get(1)
        }
}
