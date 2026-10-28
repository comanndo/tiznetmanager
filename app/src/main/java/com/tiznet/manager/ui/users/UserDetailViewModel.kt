package com.tiznet.manager.ui.users

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.UserDetail
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class UserDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session

    private val _state = MutableStateFlow<UiState<UserDetail>>(UiState.Idle)
    val state: StateFlow<UiState<UserDetail>> = _state.asStateFlow()

    fun load(uuid: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                if (session.isLegacyApi) {
                    apiCall { session.api().legacyUser(uuid) }
                        .let { com.tiznet.manager.data.remote.LegacyMappers.run { it.toDetail() } }
                } else {
                    apiCall { session.api().user(uuid).user }
                }
            }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(ApiException.from(it, "خطا").message, it) }
        }
    }

    fun patch(uuid: String, body: kotlinx.serialization.json.JsonObject, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                if (session.isLegacyApi) {
                    val safe = com.tiznet.manager.data.remote.LegacyMappers.sanitizeUserBody(body)
                    apiCall { session.api().legacyUpdateUser(uuid, safe) }
                        .let { com.tiznet.manager.data.remote.LegacyMappers.run { it.toDetail() } }
                } else {
                    apiCall { session.api().updateUser(uuid, body).user }
                }
            }
                .onSuccess { _state.value = UiState.Success(it); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun toggle(user: UserDetail) =
        patch(user.uuid, buildJsonObject { put("enable", !user.enable) }) { }

    fun resetUsage(user: UserDetail, onDone: (String?) -> Unit) {
        if (session.isLegacyApi) {
            onDone(
                getApplication<android.app.Application>()
                    .getString(com.tiznet.manager.R.string.reset_unsupported_legacy)
            )
            return
        }
        patch(user.uuid, buildJsonObject { put("reset_usage", true) }, onDone)
    }

    fun resetDays(user: UserDetail, onDone: (String?) -> Unit) {
        if (session.isLegacyApi) {
            onDone(
                getApplication<android.app.Application>()
                    .getString(com.tiznet.manager.R.string.reset_unsupported_legacy)
            )
            return
        }
        patch(user.uuid, buildJsonObject { put("reset_days", true) }, onDone)
    }

    fun delete(uuid: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                apiCall {
                    if (session.isLegacyApi) session.api().legacyDeleteUser(uuid)
                    else session.api().deleteUser(uuid)
                }
            }
                .onSuccess { onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }
}
