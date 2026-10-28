package com.tiznet.manager.data.remote

import com.tiznet.manager.data.db.PanelEntity
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Authenticates every request of one panel.
 *
 * Primary path is the browser-compatible form login (session cookie in [jar]).
 * On older panels without that flow, or when the cookie is rejected, the
 * request is retried with HTTP Basic credentials and the Hiddify-API-Key
 * header, which every Hiddify generation maps to the account UUID.
 *
 * Version quirks handled here:
 *  - v11/v12 answer failed API authentication with **403**, v13+ with **401**.
 *  - on v11/v12 the UUID itself is a valid API key even when a password is set.
 *  - Hiddify throttles sign-in attempts (10 failures / 15 min), so a failed
 *    form login is never retried blindly.
 */
class PanelAuthInterceptor(
    private val panel: PanelEntity,
    private val password: String,
    private val jar: PanelCookieJar
) : Interceptor {

    private val handshakeDone = AtomicBoolean(false)

    override fun intercept(chain: Interceptor.Chain): Response {
        val loggedIn = ensureSession()
        var response = chain.proceed(chain.request())
        if (response.code !in AUTH_FAILURE_CODES) return response

        response.close()
        if (loggedIn) {
            // Cookie existed but was rejected/expired: re-handshake once.
            jar.clear()
            handshakeDone.set(false)
            ensureSession()
            response = chain.proceed(chain.request())
            if (response.code !in AUTH_FAILURE_CODES) return response
            response.close()
        }
        // Compatibility paths for older panel builds and passwordless accounts.
        return chain.proceed(fallbackRequest(chain))
    }

    @Synchronized
    private fun ensureSession(): Boolean {
        if (handshakeDone.get()) return jar.hasSessionCookie()
        val result = PanelAuth.formLogin(
            baseUrl = panel.baseUrl,
            proxyPath = panel.proxyPath,
            username = panel.username,
            password = password,
            jar = jar
        )
        handshakeDone.set(true)
        return result is PanelAuth.Result.Success && jar.hasSessionCookie()
    }

    private fun fallbackRequest(chain: Interceptor.Chain): okhttp3.Request {
        val builder = chain.request().newBuilder()
            .header("Authorization", Credentials.basic(panel.username, password))
        // The UUID works as an API key on v11-v14 regardless of the password.
        if (looksLikeUuid(panel.username)) {
            builder.header("Hiddify-API-Key", panel.username.trim())
        } else if (password.isEmpty()) {
            builder.header("Hiddify-API-Key", panel.username)
        }
        return builder.build()
    }

    companion object {
        private val AUTH_FAILURE_CODES = setOf(401, 403)

        /** Canonical dashed UUID, or a bare 32 hex digit UUID. */
        fun looksLikeUuid(value: String): Boolean {
            val v = value.trim()
            return v.matches(
                Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
            ) || v.matches(Regex("^[0-9a-fA-F]{32}$"))
        }
    }
}
