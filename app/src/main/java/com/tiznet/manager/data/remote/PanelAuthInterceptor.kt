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
 */
class PanelAuthInterceptor(
    private val panel: PanelEntity,
    private val password: String,
    private val jar: PanelCookieJar
) : Interceptor {

    private val handshakeDone = AtomicBoolean(false)

    override fun intercept(chain: Interceptor.Chain): Response {
        ensureSession()
        var response = chain.proceed(chain.request())
        if (response.code != 401) return response

        // Session expired or rejected: re-handshake once and retry.
        response.close()
        jar.clear()
        handshakeDone.set(false)
        ensureSession()
        response = chain.proceed(chain.request())
        if (response.code != 401) return response

        // Last-resort compatibility paths for older panel builds.
        response.close()
        return chain.proceed(fallbackRequest(chain))
    }

    @Synchronized
    private fun ensureSession() {
        if (handshakeDone.get()) return
        PanelAuth.formLogin(
            baseUrl = panel.baseUrl,
            proxyPath = panel.proxyPath,
            username = panel.username,
            password = password,
            jar = jar
        )
        // Even on failure we mark the handshake done: request proceeds and the
        // 401 branch below takes care of the fallback instead of looping.
        handshakeDone.set(true)
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
