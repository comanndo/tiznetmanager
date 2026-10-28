package com.tiznet.manager.ui.tags

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.Tag
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.ui.common.legacyUnsupported
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TagsViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session
    private val api get() = session.api()

    private val _state = MutableStateFlow<UiState<List<Tag>>>(UiState.Idle)
    val state: StateFlow<UiState<List<Tag>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (session.isLegacyApi) {
            _state.value = UiState.Error(legacyUnsupported())
            return
        }
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { apiCall { api.tags().tags } }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(ApiException.from(it, "خطا").message, it) }
        }
    }

    fun create(name: String, color: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { apiCall { api.createTag(buildJsonObject { put("name", name); put("color", color) }) } }
                .onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun update(tag: Tag, name: String, color: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                apiCall {
                    api.updateTag(tag.id, buildJsonObject { put("name", name); put("color", color) })
                }
            }.onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun delete(tag: Tag, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { apiCall { api.deleteTag(tag.id) } }
                .onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }
}
