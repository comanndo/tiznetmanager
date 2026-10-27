package com.tiznet.manager.ui.users

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.OutboundOption
import com.tiznet.manager.data.model.UserDetail
import com.tiznet.manager.data.remote.ApiException
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

class UserEditViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session

    private val _detail = MutableStateFlow<UserDetail?>(null)
    val detail: StateFlow<UserDetail?> = _detail.asStateFlow()

    private val _outbounds = MutableStateFlow<List<OutboundOption>>(emptyList())
    val outbounds: StateFlow<List<OutboundOption>> = _outbounds.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun load(uuid: String?) {
        viewModelScope.launch {
            runCatching { apiCall { session.api().users() } }
                .onSuccess { _outbounds.value = it.outbounds }
            if (uuid != null) {
                runCatching { apiCall { session.api().user(uuid).user } }
                    .onSuccess { _detail.value = it }
            }
        }
    }

    fun submit(input: Input, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val body = buildJsonObject {
                input.name?.takeIf { it.isNotBlank() }?.let { put("name", it) }
                input.comment?.let { put("comment", it) }
                input.uuid?.takeIf { it.isNotBlank() }?.let { put("uuid", it.trim().lowercase()) }
                put("enable", input.enabled)
                input.limit?.let { put("usage_limit_GB", it) }
                input.days?.let { put("package_days", it) }
                put("mode", input.mode)
                if (input.preferredOutbound == null) put("preferred_outbound", JsonNull)
                else put("preferred_outbound", JsonPrimitive(input.preferredOutbound))
                input.telegramId?.takeIf { it.isNotBlank() }?.let {
                    put("telegram_id", JsonPrimitive(it.toLongOrNull()))
                }
                if (input.resetUsage) put("reset_usage", true)
                if (input.resetDays) put("reset_days", true)
            }
            runCatching {
                when {
                    input.bulk -> {
                        val bulkBody = buildJsonObject {
                            // common package fields
                            body.forEach { (k, v) -> put(k, v) }
                            put("count", input.count.coerceIn(1, 100))
                        }
                        apiCall { session.api().bulkCreateUsers(bulkBody) }
                    }
                    input.existingUuid != null ->
                        apiCall { session.api().updateUser(input.existingUuid, body) }
                    else -> apiCall { session.api().createUser(body) }
                }
            }.onSuccess { onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
            _busy.value = false
        }
    }

    data class Input(
        val name: String?,
        val comment: String?,
        val uuid: String?,
        val enabled: Boolean,
        val limit: Double?,
        val days: Int?,
        val mode: String,
        val preferredOutbound: Int?,
        val telegramId: String?,
        val resetUsage: Boolean,
        val resetDays: Boolean,
        val count: Int,
        val bulk: Boolean,
        val existingUuid: String?
    )
}
