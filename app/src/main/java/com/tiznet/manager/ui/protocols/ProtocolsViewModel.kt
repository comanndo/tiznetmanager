package com.tiznet.manager.ui.protocols

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.ProtocolSwitch
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.ui.common.legacyUnsupported
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProtocolsViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session

    private val _state = MutableStateFlow<UiState<List<ProtocolSwitch>>>(UiState.Idle)
    val state: StateFlow<UiState<List<ProtocolSwitch>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (session.isLegacyApi) {
            _state.value = UiState.Error(legacyUnsupported())
            return
        }
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { apiCall { session.api().protocols().items } }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(ApiException.from(it, "خطا").message, it) }
        }
    }

    fun toggle(item: ProtocolSwitch, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val body = buildJsonObject { put(item.key, JsonPrimitive(!item.enabled)) }
            runCatching { apiCall { session.api().updateProtocols(body).items } }
                .onSuccess { _state.value = UiState.Success(it); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }
}
