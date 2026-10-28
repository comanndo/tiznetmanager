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
     * Verifies credentials against a panel.
     *
     * Works on every Hiddify generation:
     *  - v11/v12 have no `__admin_v2_bootstrap`; the proxy path comes from the
     *    pasted link / a root redirect, and `/api/v2/panel/info/` gives the version.
     *  - v13+ additionally answer the bootstrap endpoint, used when available.
     * In all cases `me/` (present on v11-v14) is the proof of authentication.
     */
    suspend fun testConnection(
        baseUrl: String,
        proxyPath: String,
        username: String,
        password: String
    ): BootstrapResponse {
        val (hostBase, embeddedPath) = RetrofitFactory.splitBaseAndPath(baseUrl)
        if (!RetrofitFactory.isValidUrl(hostBase)) {
            throw ApiException(-1, "آدرس پنل معتبر نیست")
        }
        val wantedPath = proxyPath.trim().trim('/').ifBlank { embeddedPath }
        val resolvedPath = wantedPath.ifBlank {
            runCatching { PanelAuth.discoverProxyPath(hostBase) }.getOrNull().orEmpty()
        }
        val (api, _) = RetrofitFactory.apiForTemporary(hostBase, resolvedPath, username, password)

        // Version probe exists on v11-v14.
        val version = runCatching {
            apiCall {
                api.panelInfo(RetrofitFactory.panelInfoUrl(hostBase, resolvedPath))
            }.version
        }.getOrNull()

        // Authentication proof: 401 (v13+) or 403 (v11/v12) means bad credentials,
        // 404 means the proxy path/base URL is wrong.
        runCatching { apiCall { api.me() } }.onFailure { e ->
            if (e is ApiException && (e.isUnauthorized || e.isForbidden || e.isNotFound)) {
                throw if (e.isNotFound && resolvedPath.isBlank()) {
                    ApiException(404, "مسیر پنل (Proxy Path) پیدا نشد؛ لینک کامل پنل را وارد کنید")
                } else {
                    e
                }
            }
        }

        // Bootstrap is modern-only; never let its 404 fail a legacy connection.
        val bootstrap = runCatching {
            apiCall { api.bootstrap(RetrofitFactory.bootstrapUrl(hostBase, resolvedPath)) }
        }.getOrNull()

        return BootstrapResponse(
            proxyPath = bootstrap?.proxyPath?.ifBlank { resolvedPath } ?: resolvedPath,
            panelVersion = version ?: bootstrap?.panelVersion
        )
    }

    fun apiFor(panel: PanelEntity): HiddifyApi = RetrofitFactory.apiFor(panel, crypto)

    fun decryptPassword(panel: PanelEntity): String =
        runCatching { crypto.decrypt(panel.encryptedPassword) }.getOrDefault("")
}
