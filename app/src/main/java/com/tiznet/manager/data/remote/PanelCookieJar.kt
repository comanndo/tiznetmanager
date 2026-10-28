package com.tiznet.manager.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory [CookieJar] scoped to one panel connection.
 *
 * Hiddify's modern admin API authenticates with a server-side session cookie
 * obtained from the sign-in form (not HTTP Basic auth), so every client keeps
 * its own jar for the login handshake and all following API calls.
 */
class PanelCookieJar : CookieJar {

    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val key = url.host
        val merged = (store[key] ?: mutableListOf()).filterNot { existing ->
            cookies.any { it.name == existing.name && it.path == existing.path }
        }.toMutableList()
        merged.addAll(cookies)
        store[key] = merged
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return store[url.host]
            ?.filter { it.expiresAt >= now && it.matches(url) }
            ?: emptyList()
    }

    fun hasSessionCookie(): Boolean =
        store.values.flatten().any { it.name == SESSION_COOKIE && it.value.isNotBlank() }

    fun clear() = store.clear()

    companion object {
        // Flask's default signed session cookie name; Hiddify keeps the default.
        const val SESSION_COOKIE = "session"
    }
}
