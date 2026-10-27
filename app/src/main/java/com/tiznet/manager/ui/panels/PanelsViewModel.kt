package com.tiznet.manager.ui.panels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.db.PanelEntity
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.data.remote.RetrofitFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PanelsViewModel(app: Application) : AndroidViewModel(app) {

    private val application = app as TizNetApp
    private val repo = application.repository
    private val crypto = application.crypto
    val session = application.session

    val panels: StateFlow<List<PanelEntity>> = repo.panels
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun consumeError() { _error.value = null }

    fun switchTo(panel: PanelEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { session.switchTo(panel) }
                .onSuccess { _busy.value = false; onDone() }
                .onFailure {
                    _busy.value = false
                    _error.value = ApiException.from(it, "خطا").message
                }
        }
    }

    fun save(
        existing: PanelEntity?,
        name: String,
        url: String,
        proxyPath: String,
        username: String,
        password: String,
        onDone: (String?) -> Unit
    ) {
        if (!RetrofitFactory.isValidUrl(RetrofitFactory.normalizeBaseUrl(url))) {
            onDone(application.getString(com.tiznet.manager.R.string.login_invalid_url))
            return
        }
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                val boot = repo.testConnection(url, proxyPath, username, password)
                val entity = PanelEntity(
                    id = existing?.id ?: 0,
                    name = name.ifBlank {
                        RetrofitFactory.normalizeBaseUrl(url)
                            .removePrefix("https://").removePrefix("http://")
                            .substringBefore('/').substringBefore(':')
                    },
                    baseUrl = RetrofitFactory.normalizeBaseUrl(url),
                    proxyPath = boot.proxyPath.ifBlank { proxyPath.trim().trim('/') },
                    username = username.trim(),
                    encryptedPassword = crypto.encrypt(password),
                    panelVersion = boot.panelVersion ?: existing?.panelVersion,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                val id = repo.save(entity)
                session.activate(entity.copy(id = id))
            }.onSuccess {
                _busy.value = false
                onDone(null)
            }.onFailure {
                _busy.value = false
                onDone(ApiException.from(it, "خطا").message)
            }
        }
    }

    fun delete(panel: PanelEntity) {
        viewModelScope.launch {
            repo.delete(panel)
            if (session.activePanel.value?.id == panel.id) session.logout()
        }
    }
}
