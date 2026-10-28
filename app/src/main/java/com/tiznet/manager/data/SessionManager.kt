package com.tiznet.manager.data

import com.tiznet.manager.data.db.PanelEntity
import com.tiznet.manager.data.model.AdminInfo
import com.tiznet.manager.data.model.BootstrapResponse
import com.tiznet.manager.data.model.PanelCapabilities
import com.tiznet.manager.data.prefs.PrefsManager
import com.tiznet.manager.data.remote.HiddifyApi
import com.tiznet.manager.data.repo.PanelRepository
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds the currently connected panel and its authenticated API client at runtime. */
class SessionManager(
    private val repository: PanelRepository,
    private val prefs: PrefsManager
) {
    private val _activePanel = MutableStateFlow<PanelEntity?>(null)
    val activePanel: StateFlow<PanelEntity?> = _activePanel.asStateFlow()

    private val _bootstrap = MutableStateFlow<BootstrapResponse?>(null)
    val bootstrap: StateFlow<BootstrapResponse?> = _bootstrap.asStateFlow()

    private val _me = MutableStateFlow<AdminInfo?>(null)
    val me: StateFlow<AdminInfo?> = _me.asStateFlow()

    val isConnected: Boolean get() = _activePanel.value != null

    /** v11/v12 panels expose only the limited commercial REST API v2. */
    val isLegacyApi: Boolean
        get() = PanelCapabilities.isLegacy(
            _bootstrap.value?.panelVersion ?: _activePanel.value?.panelVersion
        )

    fun requirePanel(): PanelEntity =
        _activePanel.value ?: throw IllegalStateException("No active panel")

    fun api(): HiddifyApi = repository.apiFor(requirePanel())

    /** Loads the panel saved as active before the user opened the app. */
    suspend fun restoreLastSession(): Boolean {
        if (!prefs.hasActivePanel) return false
        val panel = repository.byId(prefs.activePanelId) ?: return false
        return runCatching { activate(panel, silent = true) }.getOrDefault(false)
    }

    suspend fun activate(panel: PanelEntity, silent: Boolean = false): Boolean {
        val api = repository.apiFor(panel)
        // me/ exists on every supported generation (v11-v14) and proves the credentials.
        val info = apiCall { api.me() }
        val version = runCatching {
            apiCall {
                api.panelInfo(
                    com.tiznet.manager.data.remote.RetrofitFactory.panelInfoUrl(panel)
                )
            }.version
        }.getOrNull()
        // Bootstrap is v13+ only: tolerate 404 on legacy panels.
        val boot = runCatching {
            apiCall {
                api.bootstrap(
                    com.tiznet.manager.data.remote.RetrofitFactory
                        .bootstrapUrl(panel.baseUrl, panel.proxyPath)
                )
            }
        }.getOrNull()
        val effectiveBoot = boot ?: BootstrapResponse(
            proxyPath = panel.proxyPath,
            panelVersion = version
        )
        _activePanel.value = panel
        _bootstrap.value = effectiveBoot
        _me.value = info
        prefs.activePanelId = panel.id
        repository.touch(panel)
        val resolvedPath = boot?.proxyPath?.takeIf { it.isNotBlank() }
        if (resolvedPath != null && resolvedPath != panel.proxyPath) {
            repository.update(panel.copy(proxyPath = resolvedPath))
        }
        repository.storeVersion(panel.id, version ?: boot?.panelVersion)
        if (!silent) _activePanel.value = repository.byId(panel.id) ?: panel
        return true
    }

    suspend fun switchTo(panel: PanelEntity) = activate(panel)

    fun logout() {
        _activePanel.value = null
        _bootstrap.value = null
        _me.value = null
        prefs.clearActivePanel()
    }

    fun refreshBootstrap(boot: BootstrapResponse) {
        _bootstrap.value = boot
    }
}
