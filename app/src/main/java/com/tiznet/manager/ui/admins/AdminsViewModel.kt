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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
                    // v11 answers 404 "You have no admin" when the list is empty.
                    runCatching { apiCall { session.api().legacyAdmins() } }
                        .getOrElse { e ->
                            val parsed = ApiException.from(e, "خطا")
                            if (parsed.isNotFound) emptyList() else throw parsed
                        }
                        .map {
                            com.tiznet.manager.data.remote.LegacyMappers.run {
                                it.toRow(it.uuid == meUuid)
                            }
                        }
                } else {
                    apiCall { session.api().admins().admins }
                }
            }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure {
                    val parsed = ApiException.from(it, "خطا")
                    _state.value = UiState.Error(
                        if (parsed.isForbidden)
                            getApplication<android.app.Application>()
                                .getString(com.tiznet.manager.R.string.admins_requires_role)
                        else parsed.message,
                        it
                    )
                }
        }
    }

    /** v11 AdminSchema requires mode/can_add_admin/lang which the v13 dialog does not send. */
    private fun legacyPayload(body: JsonObject, isCreate: Boolean): JsonObject = buildJsonObject {
        body["name"]?.let { put("name", it.jsonPrimitive.content) }
        body["comment"]?.let { put("comment", it.jsonPrimitive.contentOrNull ?: "") }
        body["max_users"]?.let { put("max_users", it) }
        body["max_active_users"]?.let { put("max_active_users", it) }
        body["password"]?.let { put("password", it) }
        if (isCreate) {
            put("mode", "agent")
            put(
                "can_add_admin",
                body["capabilities"]?.jsonObject?.get("add_sub_admins")?.jsonPrimitive?.booleanOrNull ?: false
            )
            put("lang", "en")
        }
    }

    fun create(body: JsonObject, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                if (session.isLegacyApi) {
                    apiCall { session.api().legacyCreateAdmin(legacyPayload(body, isCreate = true)) }
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
                        session.api().legacyUpdateAdmin(uuid, legacyPayload(body, isCreate = false))
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
