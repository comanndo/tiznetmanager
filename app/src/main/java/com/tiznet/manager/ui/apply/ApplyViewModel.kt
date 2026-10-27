package com.tiznet.manager.ui.apply

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.ApplyState
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ApplyViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session
    private val api get() = session.api()

    private val _state = MutableStateFlow<ApplyState?>(null)
    val state: StateFlow<ApplyState?> = _state.asStateFlow()

    private val _log = MutableStateFlow("")
    val log: StateFlow<String> = _log.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var pollJob: Job? = null

    fun loadState() {
        viewModelScope.launch {
            runCatching { apiCall { api.applyState() } }
                .onSuccess { _state.value = it }
                .onFailure { _error.value = ApiException.from(it, "خطا").message }
        }
    }

    fun run(action: String) {
        pollJob?.cancel()
        viewModelScope.launch {
            _running.value = true
            _error.value = null
            runCatching { apiCall { api.runApply(action) } }
                .onSuccess { started ->
                    _state.value = ApplyState(running = started.running, files = started.files)
                    _log.value = started.log
                    startPolling()
                }
                .onFailure {
                    _running.value = false
                    _error.value = ApiException.from(it, "خطا").message
                }
        }
    }

    private fun startPolling() {
        pollJob = viewModelScope.launch {
            var offset = _log.value.length.toLong()
            repeat(120) {
                delay(2000)
                if (!isActive) return@launch
                runCatching {
                    val chunk = apiCall { api.applyLog(offset = offset) }
                    if (chunk.text.isNotEmpty()) {
                        _log.value += chunk.text
                        offset = chunk.offset.takeIf { it > 0 } ?: (offset + chunk.text.length)
                    }
                    val applyState = runCatching { apiCall { api.applyState() } }.getOrNull()
                    applyState?.let { _state.value = it }
                    if (applyState?.running.isNullOrEmpty()) {
                        _running.value = false
                        return@launch
                    }
                }
            }
            _running.value = false
        }
    }

    fun clearLog() {
        viewModelScope.launch {
            runCatching { apiCall { api.clearApplyLogs() } }
                .onSuccess { _log.value = "" }
                .onFailure { _error.value = ApiException.from(it, "خطا").message }
        }
    }

    fun consumeError() { _error.value = null }
}
