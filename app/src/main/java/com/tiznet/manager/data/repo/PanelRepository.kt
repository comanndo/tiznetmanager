package com.tiznet.manager.data.repo

import com.tiznet.manager.data.db.PanelDao
import com.tiznet.manager.data.db.PanelEntity
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.data.remote.HiddifyApi
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
     * Verifies credentials against a panel. When [proxyPath] is empty the root bootstrap is
     * tried first; on 404/401 a couple of common discovery paths are attempted.
     * Returns the resolved bootstrap (proxy path + panel version).
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
        val candidates = mutableListOf(proxyPath.trim().trim('/'))
        if (proxyPath.isBlank()) candidates.add("")
        var lastError: ApiException? = null
        for (path in candidates.distinct()) {
            val (api, _) = RetrofitFactory.apiForTemporary(normalized, path, username, password)
            try {
                val bootstrap = apiCall {
                    api.bootstrap(RetrofitFactory.bootstrapUrl(normalized, path))
                }
                // Confirm the admin API itself accepts these credentials.
                runCatching { apiCall { api.me() } }
                return bootstrap
            } catch (e: ApiException) {
                lastError = e
                // Wrong credentials must not be masked by trying the next candidate.
                if (e.isUnauthorized || e.isForbidden) throw e
            }
        }
        throw lastError ?: ApiException(-1, "اتصال به پنل ممکن نشد")
    }

    fun apiFor(panel: PanelEntity): HiddifyApi = RetrofitFactory.apiFor(panel, crypto)

    fun decryptPassword(panel: PanelEntity): String =
        runCatching { crypto.decrypt(panel.encryptedPassword) }.getOrDefault("")
}
