package com.tiznet.manager.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.SettingCategory
import com.tiznet.manager.data.model.SettingsSaveOut
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.ui.common.legacyUnsupported
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session

    private val _state = MutableStateFlow<UiState<List<SettingCategory>>>(UiState.Idle)
    val state: StateFlow<UiState<List<SettingCategory>>> = _state.asStateFlow()

    private val _lastSave = MutableStateFlow<SettingsSaveOut?>(null)
    val lastSave: StateFlow<SettingsSaveOut?> = _lastSave.asStateFlow()

    init { load() }

    fun load() {
        if (session.isLegacyApi) {
            _state.value = UiState.Error(legacyUnsupported())
            return
        }
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { apiCall { session.api().settings().categories } }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(ApiException.from(it, "خطا").message, it) }
        }
    }

    fun save(values: Map<String, JsonElement>, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val body = buildJsonObject {
                put("values", JsonObject(values))
            }
            runCatching { apiCall { session.api().saveSettings(body) } }
                .onSuccess {
                    _lastSave.value = it
                    _state.value = UiState.Success(it.categories)
                    onDone(null)
                }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    companion object {
        fun primitive(value: String): JsonElement = JsonPrimitive(value)
        fun primitive(value: Boolean): JsonElement = JsonPrimitive(value)
    }
}
