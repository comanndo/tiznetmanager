package com.tiznet.manager.ui.nodes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.R
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.PanelNode
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class NodesViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session
    private val api get() = session.api()

    private val _state = MutableStateFlow<UiState<List<PanelNode>>>(UiState.Idle)
    val state: StateFlow<UiState<List<PanelNode>>> = _state.asStateFlow()

    val panelVersion = MutableStateFlow("")

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { apiCall { api.nodes() } }
                .onSuccess {
                    panelVersion.value = it.panelVersion
                    _state.value = UiState.Success(it.nodes)
                }
                .onFailure { _state.value = UiState.Error(ApiException.from(it, "خطا").message, it) }
        }
    }

    fun register(name: String, adminLink: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val body = buildJsonObject {
                put("name", name)
                put("admin_link", adminLink)
            }
            runCatching { apiCall { api.addNode(body) } }
                .onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun rename(node: PanelNode, name: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { apiCall { api.node(node.id) } }
                .onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun remove(node: PanelNode, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { apiCall { api.deleteNode(node.id) } }
                .onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun ping(node: PanelNode, onDone: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { apiCall { api.pingNode(node.id) } }
                .onSuccess {
                    val online = (it["online"] as? kotlinx.serialization.json.JsonPrimitive)
                        ?.content?.toBooleanStrictOrNull() ?: false
                    val version = (it["version"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                    onDone(
                        if (online) "online · v${version.orEmpty().ifBlank { "?" }}"
                        else getStringSafe(R.string.nodes_unreachable)
                    )
                }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    private fun getStringSafe(res: Int): String =
        getApplication<TizNetApp>().getString(res)

    fun sync(node: PanelNode, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { apiCall { api.syncNode(node.id) } }
                .onSuccess { onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }
}
