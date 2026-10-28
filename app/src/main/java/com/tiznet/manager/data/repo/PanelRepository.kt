package com.tiznet.manager.data.repo

import com.tiznet.manager.data.db.PanelDao
import com.tiznet.manager.data.db.PanelEntity
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.data.remote.HiddifyApi
import com.tiznet.manager.data.remote.PanelAuth
import com.tiznet.manager.data.remote.RetrofitFactory
import com.tiznet.manager.data.model.BootstrapResponse
import com.tiznet.manager.util.CryptoManager
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.Flow

class PanelRepository(
    private val dao: PanelDao,
    private val crypto: CryptoManager
) {
    val panels: Flow<List<PanelEntity>> = dao.observeAll()

    suspend fun all(): List<PanelEntity> = dao.getAll()

    suspend fun byId(id: Long): PanelEntity? = dao.getById(id)

    suspend fun save(panel: PanelEntity): Long = dao.insert(panel)

    suspend fun update(panel: PanelEntity) = dao.update(panel)

    suspend fun delete(panel: PanelEntity) {
        RetrofitFactory.evict(panel.id)
        dao.delete(panel)
    }

    suspend fun touch(panel: PanelEntity) = dao.touch(panel.id)

    suspend fun storeVersion(id: Long, version: String?) = dao.updateVersion(id, version)

    /**
     * Verifies credentials against a panel. When [proxyPath] is empty it is
     * auto-discovered from the unauthenticated bootstrap endpoint/redirect.
     * The session handshake (form login + cookie) happens inside the API
     * client's interceptor; here we additionally confirm that the authenticated
     * admin endpoint accepts the session.
     */
    suspend fun testConnection(
        baseUrl: String,
        proxyPath: String,
        username: String,
        password: String
    ): BootstrapResponse {
        val normalized = RetrofitFactory.normalizeBaseUrl(baseUrl)
        if (!RetrofitFactory.isValidUrl(normalized)) {
            throw ApiException(-1, "آدرس پنل معتبر نیست")
        }
        val resolvedPath = proxyPath.trim().trim('/').ifBlank {
            runCatching { PanelAuth.discoverProxyPath(normalized) }.getOrNull().orEmpty()
        }
        val (api, _) = RetrofitFactory.apiForTemporary(normalized, resolvedPath, username, password)
        // A 401 here means the form login (and its fallbacks) rejected the credentials.
        val bootstrap = apiCall {
            api.bootstrap(RetrofitFactory.bootstrapUrl(normalized, resolvedPath))
        }
        runCatching { apiCall { api.me() } }.onFailure { e ->
            if (e is ApiException && (e.isUnauthorized || e.isForbidden)) throw e
        }
        return bootstrap.copy(
            proxyPath = bootstrap.proxyPath.ifBlank { resolvedPath }
        )
    }

    fun apiFor(panel: PanelEntity): HiddifyApi = RetrofitFactory.apiFor(panel, crypto)

    fun decryptPassword(panel: PanelEntity): String =
        runCatching { crypto.decrypt(panel.encryptedPassword) }.getOrDefault("")
}
