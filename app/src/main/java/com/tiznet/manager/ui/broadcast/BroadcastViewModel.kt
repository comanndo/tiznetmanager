package com.tiznet.manager.ui.broadcast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.BroadcastState
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BroadcastViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session
    private val api get() = session.api()

    private val _state = MutableStateFlow<UiState<BroadcastState>>(UiState.Idle)
    val state: StateFlow<UiState<BroadcastState>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { apiCall { api.broadcastState() } }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(ApiException.from(it, "خطا").message, it) }
        }
    }

    fun send(text: String, format: String, audience: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val body = buildJsonObject {
                put("text", text)
                put("format", format)
                put("audience", audience)
                put("ids", JsonArray(emptyList<JsonPrimitive>()))
                put("addUsage", false)
            }
            runCatching { apiCall { api.sendBroadcast(body) } }
                .onSuccess { onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }
}
