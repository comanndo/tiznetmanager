package com.tiznet.manager.ui.domains

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.DomainRow
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DomainsViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session
    private val api get() = session.api()

    private val _state = MutableStateFlow<UiState<List<DomainRow>>>(UiState.Idle)
    val state: StateFlow<UiState<List<DomainRow>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { apiCall { api.domains().domains } }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(ApiException.from(it, "خطا").message, it) }
        }
    }

    fun create(body: JsonObject, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { apiCall { api.saveDomains(body) } }
                .onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun update(domain: DomainRow, body: JsonObject, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { apiCall { api.patchDomain(domain.id, body) } }
                .onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun delete(domain: DomainRow, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { apiCall { api.deleteDomain(domain.id) } }
                .onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun buildPayload(
        domain: String,
        alias: String,
        mode: String,
        tls: String,
        cdnIp: String,
        servernames: String,
        tlsPort: Int?,
        httpPort: Int?,
        resolve: Boolean,
        ech: Boolean
    ): JsonObject = buildJsonObject {
        put("domain", domain)
        put("alias", alias)
        put("mode", mode)
        put("fake_mode", tls)
        put("cdn_ip", cdnIp)
        put("servernames", servernames)
        if (tlsPort == null) put("tls_port", JsonNull) else put("tls_port", tlsPort)
        if (httpPort == null) put("http_port", JsonNull) else put("http_port", httpPort)
        put("resolve_ip", resolve)
        put("ech", ech)
    }
}
