package com.tiznet.manager.ui.login

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

class LoginViewModel(app: Application) : AndroidViewModel(app) {

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

    fun consumeError() {
        _error.value = null
    }

    /** Validates inputs, verifies the credentials and persists the panel. */
    fun connect(
        name: String,
        url: String,
        proxyPath: String,
        username: String,
        password: String,
        onConnected: () -> Unit
    ) {
        if (url.isBlank() || username.isBlank()) {
            _error.value = "آدرس پنل و نام کاربری الزامی است"
            return
        }
        if (!RetrofitFactory.isValidUrl(RetrofitFactory.normalizeBaseUrl(url))) {
            _error.value = application.getString(com.tiznet.manager.R.string.login_invalid_url)
            return
        }
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            runCatching {
                val boot = repo.testConnection(url, proxyPath, username, password)
                val panel = PanelEntity(
                    name = name.ifBlank { hostOf(url) },
                    baseUrl = RetrofitFactory.normalizeBaseUrl(url),
                    proxyPath = boot.proxyPath.ifBlank { proxyPath.trim().trim('/') },
                    username = username.trim(),
                    encryptedPassword = crypto.encrypt(password),
                    panelVersion = boot.panelVersion
                )
                val id = repo.save(panel)
                session.activate(panel.copy(id = id))
            }.onSuccess {
                _busy.value = false
                onConnected()
            }.onFailure {
                _busy.value = false
                _error.value = ApiException.from(it, "اتصال ناموفق").message
            }
        }
    }

    fun quickConnect(panel: PanelEntity, onConnected: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { session.activate(panel) }
                .onSuccess {
                    _busy.value = false
                    onConnected()
                }
                .onFailure {
                    _busy.value = false
                    _error.value = ApiException.from(it, "اتصال ناموفق").message
                }
        }
    }

    fun delete(panel: PanelEntity) {
        viewModelScope.launch { repo.delete(panel) }
    }

    private fun hostOf(url: String): String =
        RetrofitFactory.normalizeBaseUrl(url)
            .removePrefix("https://").removePrefix("http://")
            .substringBefore('/').substringBefore(':')
}
