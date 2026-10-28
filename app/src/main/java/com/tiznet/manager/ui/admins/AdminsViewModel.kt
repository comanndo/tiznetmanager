package com.tiznet.manager.ui.admins

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.AdminRow
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

class AdminsViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session

    private val _state = MutableStateFlow<UiState<List<AdminRow>>>(UiState.Idle)
    val state: StateFlow<UiState<List<AdminRow>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                if (session.isLegacyApi) {
                    val meUuid = session.me.value?.uuid
                    apiCall { session.api().legacyAdmins() }
                        .map { com.tiznet.manager.data.remote.LegacyMappers.run { it.toRow(it.uuid == meUuid) } }
                } else {
                    apiCall { session.api().admins().admins }
                }
            }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(ApiException.from(it, "خطا").message, it) }
        }
    }

    fun create(body: JsonObject, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                if (session.isLegacyApi) {
                    apiCall {
                        session.api().legacyCreateAdmin(
                            com.tiznet.manager.data.remote.LegacyMappers.sanitizeAdminBody(body)
                        )
                    }
                } else {
                    apiCall { session.api().createAdmin(body) }
                }
            }
                .onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun update(uuid: String, body: JsonObject, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                if (session.isLegacyApi) {
                    apiCall {
                        session.api().legacyUpdateAdmin(
                            uuid,
                            com.tiznet.manager.data.remote.LegacyMappers.sanitizeAdminBody(body)
                        )
                    }
                } else {
                    apiCall { session.api().updateAdmin(uuid, body) }
                }
            }
                .onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun delete(uuid: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                apiCall {
                    if (session.isLegacyApi) session.api().legacyDeleteAdmin(uuid)
                    else session.api().deleteAdmin(uuid)
                }
            }
                .onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun buildPayload(
        name: String,
        comment: String,
        alias: String,
        password: String,
        maxUsers: Int?,
        maxActive: Int?,
        caps: Map<String, Boolean>
    ): JsonObject = buildJsonObject {
        put("name", name)
        put("comment", comment)
        put("alias", alias)
        if (password.isNotBlank()) put("password", password)
        if (maxUsers != null) put("max_users", maxUsers) else put("max_users", JsonNull)
        if (maxActive != null) put("max_active_users", maxActive)
        else put("max_active_users", JsonNull)
        put("capabilities", JsonObject(caps.mapValues { (_, v) -> kotlinx.serialization.json.JsonPrimitive(v) }))
    }
}
