package com.tiznet.manager.ui.outbounds

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.Outbound
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class OutboundsViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session
    private val api get() = session.api()

    private val _state = MutableStateFlow<UiState<List<Outbound>>>(UiState.Idle)
    val state: StateFlow<UiState<List<Outbound>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { apiCall { api.outbounds().outbounds } }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(ApiException.from(it, "خطا").message, it) }
        }
    }

    private fun mutate(block: suspend () -> Unit, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun toggle(outbound: Outbound) = mutate({
        apiCall { api.updateOutbound(outbound.id, buildJsonObject { put("enabled", !outbound.enabled) }) }
    }) {}

    fun move(outbound: Outbound, delta: Int, current: List<Outbound>, onDone: (String?) -> Unit) {
        val index = current.indexOfFirst { it.id == outbound.id }
        val target = index + delta
        if (index < 0 || target < 0 || target >= current.size) return
        val reordered = current.toMutableList().apply {
            add(target, removeAt(index))
        }
        val ids = reordered.map { it.id }
        mutate({
            apiCall {
                api.reorderOutbounds(buildJsonObject {
                    put("ids", JsonArray(ids.map { JsonPrimitive(it) }))
                })
            }
        }, onDone)
    }

    fun makeDefault(outbound: Outbound, onDone: (String?) -> Unit) =
        mutate({ apiCall { api.setDefaultOutbound(outbound.id) } }, onDone)

    fun reset(outbound: Outbound, onDone: (String?) -> Unit) =
        mutate({ apiCall { api.resetOutbound(outbound.id) } }, onDone)

    fun delete(outbound: Outbound, onDone: (String?) -> Unit) =
        mutate({ apiCall { api.deleteOutbound(outbound.id) } }, onDone)

    fun save(existing: Outbound?, input: Input, onDone: (String?) -> Unit) {
        val body = buildJsonObject {
            put("name", input.name)
            put("mode", input.mode)
            put("enabled", input.enabled)
            if (input.host.isNotBlank()) put("host", input.host)
            if (input.port != null) put("port", JsonPrimitive(input.port)) else put("port", JsonNull)
            if (input.username.isNotBlank()) put("username", input.username)
            if (input.password.isNotBlank()) put("password", input.password)
            if (input.config.isNotBlank()) put("config", input.config)
        }
        mutate({
            if (existing == null) apiCall { api.createOutbound(body) }
            else apiCall { api.updateOutbound(existing.id, body) }
        }, onDone)
    }

    data class Input(
        val name: String,
        val mode: String,
        val enabled: Boolean,
        val host: String,
        val port: Int?,
        val username: String,
        val password: String,
        val config: String
    )
}
