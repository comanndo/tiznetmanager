package com.tiznet.manager.ui.cleanip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.CleanIpNetwork
import com.tiznet.manager.data.model.RealityState
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CleanIpViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session
    private val api get() = session.api()

    private val _reality = MutableStateFlow<UiState<RealityState>>(UiState.Idle)
    val reality: StateFlow<UiState<RealityState>> = _reality.asStateFlow()

    private val _networks = MutableStateFlow<List<CleanIpNetwork>>(emptyList())
    val networks: StateFlow<List<CleanIpNetwork>> = _networks.asStateFlow()

    init {
        loadReality()
        loadNetworks()
    }

    fun loadReality() {
        viewModelScope.launch {
            _reality.value = UiState.Loading
            runCatching { apiCall { api.realityState() } }
                .onSuccess { _reality.value = UiState.Success(it) }
                .onFailure { _reality.value = UiState.Error(ApiException.from(it, "خطا").message, it) }
        }
    }

    fun loadNetworks() {
        viewModelScope.launch {
            runCatching { apiCall { api.cleanIpNetwork() } }
                .onSuccess { _networks.value = it }
        }
    }

    fun saveReality(state: RealityState, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val body = buildJsonObject {
                put("enabled", state.enabled)
                put("sni", state.sni)
                put("dest", state.dest)
                put("pbk", state.pbk)
                put("sid", state.sid)
            }
            runCatching { apiCall { api.saveReality(body) } }
                .onSuccess { onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun runScan(kind: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val body = buildJsonObject { put("kind", kind) }
            runCatching { apiCall { api.runCleanIpScan(body) } }
                .onSuccess {
                    loadNetworks()
                    onDone(null)
                }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }
}
