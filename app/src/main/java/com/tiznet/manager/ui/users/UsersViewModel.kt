package com.tiznet.manager.ui.users

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.UNLIMITED_GB
import com.tiznet.manager.data.model.UserRow
import com.tiznet.manager.data.model.UsersState
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class UsersViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session

    private val _state = MutableStateFlow<UiState<UsersState>>(UiState.Idle)
    val state: StateFlow<UiState<UsersState>> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow("all")
    val filter: StateFlow<String> = _filter.asStateFlow()

    private val _selection = MutableStateFlow<Set<String>>(emptySet())
    val selection: StateFlow<Set<String>> = _selection.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserRow>>(emptyList())

    val visibleUsers: StateFlow<List<UserRow>> =
        combine(_allUsers, _query, _filter) { users, query, filter ->
            users.filter { user ->
                (filter == "all" || user.status == filter) &&
                    (query.isBlank() ||
                        user.name.contains(query, true) ||
                        user.uuid.contains(query, true))
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                if (session.isLegacyApi) {
                    val rows = apiCall { session.api().legacyUsers() }
                    com.tiznet.manager.data.remote.LegacyMappers.usersState(
                        rows, session.me.value?.uuid
                    )
                } else {
                    apiCall { session.api().users() }
                }
            }
                .onSuccess {
                    _allUsers.value = it.users
                    _state.value = UiState.Success(it)
                }
                .onFailure { _state.value = UiState.Error(ApiException.from(it, "خطا").message, it) }
        }
    }

    fun setQuery(value: String) { _query.value = value }
    fun setFilter(value: String) { _filter.value = value }

    fun toggleSelection(uuid: String) {
        _selection.update { current ->
            if (uuid in current) current - uuid else current + uuid
        }
    }

    fun selectAll(ids: List<String>) { _selection.value = ids.toSet() }
    fun clearSelection() { _selection.value = emptySet() }

    fun deleteUser(uuid: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                apiCall {
                    if (session.isLegacyApi) session.api().legacyDeleteUser(uuid)
                    else session.api().deleteUser(uuid)
                }
            }
                .onSuccess {
                    _allUsers.update { list -> list.filterNot { it.uuid == uuid } }
                    onDone(null)
                }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun toggleEnabled(user: UserRow, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val body = buildJsonObject { put("enable", !user.enable) }
            runCatching {
                apiCall {
                    if (session.isLegacyApi) session.api().legacyUpdateUser(user.uuid, body)
                    else session.api().updateUser(user.uuid, body)
                }
            }
                .onSuccess {
                    _allUsers.update { list ->
                        list.map { if (it.uuid == user.uuid) it.copy(enable = it.enable.not()) else it }
                    }
                    onDone(null)
                }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun bulkAction(action: String, days: Int? = null, gb: Double? = null, onDone: (String?) -> Unit) {
        val uuids = _selection.value.toList()
        if (uuids.isEmpty()) return
        if (session.isLegacyApi) {
            onDone("عملیات گروهی فقط روی پنل هیدیفای نسخه ۱۳ به بالا پشتیبانی می‌شود")
            return
        }
        viewModelScope.launch {
            val body = buildJsonObject {
                put("action", action)
                put("uuids", JsonArray(uuids.map { JsonPrimitive(it) }))
                days?.let { put("days", it) }
                gb?.let { put("gb", it) }
            }
            runCatching { apiCall { session.api().bulkUserAction(body) } }
                .onSuccess { clearSelection(); load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    companion object {
        fun usagePercent(user: UserRow): Int {
            if (user.usageLimitGb <= 0 || user.usageLimitGb >= UNLIMITED_GB) return 0
            return (user.currentUsageGb / user.usageLimitGb * 100).toInt().coerceIn(0, 100)
        }
    }
}
