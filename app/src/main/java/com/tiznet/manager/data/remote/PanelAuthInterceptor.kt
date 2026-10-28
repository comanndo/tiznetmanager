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
 * request is retried with HTTP Basic credentials and — for passwordless
 * admins — the Hiddify-API-Key header, which the backend maps to the UUID.
 *
 * A failed form login is never retried blindly: Hiddify throttles sign-in
 * attempts per IP (10 failures / 15 minutes), so after an explicit failure we
 * go straight to the fallback instead of burning another attempt.
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
        if (response.code != 401) return response

        response.close()
        if (loggedIn) {
            // Cookie existed but was rejected/expired: re-handshake once.
            jar.clear()
            handshakeDone.set(false)
            ensureSession()
            response = chain.proceed(chain.request())
            if (response.code != 401) return response
            response.close()
        }
        // Last-resort compatibility paths for older panel builds.
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
        if (password.isEmpty()) {
            // Passwordless admin: the backend accepts the UUID as an API key.
            builder.header("Hiddify-API-Key", panel.username)
        }
        return builder.build()
    }
}
