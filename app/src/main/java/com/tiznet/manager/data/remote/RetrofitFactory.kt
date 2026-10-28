package com.tiznet.manager.data.remote

import android.util.Base64
import com.tiznet.manager.data.db.PanelEntity
import com.tiznet.manager.util.CryptoManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object RetrofitFactory {

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
        encodeDefaults = true
    }

    private val serviceCache = ConcurrentHashMap<Long, HiddifyApi>()
    private val clientCache = ConcurrentHashMap<String, OkHttpClient>()

    /** Normalizes a user entered URL into scheme://host[:port] without a trailing slash. */
    fun normalizeBaseUrl(input: String): String {
        var url = input.trim().removeSuffix("/")
        if (url.isBlank()) return url
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return url.removeSuffix("/")
    }

    fun isValidUrl(input: String): Boolean {
        val normalized = normalizeBaseUrl(input)
        return Regex("^https?://[A-Za-z0-9_\\-.]+(:\\d{1,5})?(/.*)?$").matches(normalized)
    }

    /** Base path of the Admin V2 API for a panel. */
    fun apiBase(panel: PanelEntity): String {
        val base = normalizeBaseUrl(panel.baseUrl)
        val path = panel.proxyPath.trim().trim('/')
        return if (path.isEmpty()) "$base/api/v2/admin/" else "$base/$path/api/v2/admin/"
    }

    /** Bootstrap URL used to resolve the proxy path and read panel version. */
    fun bootstrapUrl(baseUrl: String, proxyPath: String): String {
        val base = normalizeBaseUrl(baseUrl)
        val path = proxyPath.trim().trim('/')
        return if (path.isEmpty()) "$base/__admin_v2_bootstrap" else "$base/$path/__admin_v2_bootstrap"
    }

    private fun httpClient(panel: PanelEntity, password: String): OkHttpClient {
        val key = "${panel.id}|${panel.username}|${panel.baseUrl}|${panel.proxyPath}"
        return clientCache.getOrPut(key) { buildClient(panel, password) }
    }

    private fun buildClient(panel: PanelEntity, password: String): OkHttpClient {
        val jar = PanelCookieJar()
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val headers = okhttp3.Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", PanelAuth.USER_AGENT)
                    .header("Accept", "application/json")
                    .build()
            )
        }
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .cookieJar(jar)
            // Form login + session cookie, with Basic/API-Key fallback for old panels.
            .addInterceptor(PanelAuthInterceptor(panel, password, jar))
            .addInterceptor(headers)
            .addInterceptor(logging)
            .build()
    }

    fun apiFor(panel: PanelEntity, crypto: CryptoManager): HiddifyApi {
        return serviceCache.getOrPut(panel.id) {
            val password = runCatching { crypto.decrypt(panel.encryptedPassword) }.getOrDefault("")
            val contentType = "application/json".toMediaType()
            Retrofit.Builder()
                .baseUrl(apiBase(panel))
                .client(httpClient(panel, password))
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(HiddifyApi::class.java)
        }
    }

    /**
     * A one-off client for the login flow, before the panel is persisted.
     * The session handshake runs lazily on the first request.
     */
    fun apiForTemporary(
        baseUrl: String,
        proxyPath: String,
        username: String,
        password: String
    ): Pair<HiddifyApi, String> {
        val tempPanel = PanelEntity(
            id = -System.currentTimeMillis(),
            name = "temp",
            baseUrl = normalizeBaseUrl(baseUrl),
            proxyPath = proxyPath.trim().trim('/'),
            username = username,
            encryptedPassword = ""
        )
        val client = buildClient(tempPanel, password)
        val base = apiBase(tempPanel)
        val api = Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(HiddifyApi::class.java)
        return api to base
    }

    fun evict(panelId: Long) {
        serviceCache.remove(panelId)
        clientCache.keys.removeAll { it.startsWith("$panelId|") }
    }

    @Suppress("unused")
    private fun basicBase64(username: String, password: String): String =
        Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)
}
