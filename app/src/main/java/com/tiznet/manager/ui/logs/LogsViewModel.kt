package com.tiznet.manager.ui.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.LogFile
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.apiCall
import com.tiznet.manager.util.formatBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogsViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session
    private val api get() = session.api()

    private val _state = MutableStateFlow<UiState<List<LogFile>>>(UiState.Idle)
    val state: StateFlow<UiState<List<LogFile>>> = _state.asStateFlow()

    private val _content = MutableStateFlow<Pair<String, String>?>(null)
    val content: StateFlow<Pair<String, String>?> = _content.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { apiCall { api.applyState().files } }
                .onSuccess { _state.value = if (it.isEmpty()) UiState.Success(it) else UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(ApiException.from(it, "خطا").message, it) }
        }
    }

    fun open(file: LogFile) {
        viewModelScope.launch {
            runCatching {
                val response = apiCall { api.readLogFile(file.name) }
                val html = response.body()?.string().orEmpty()
                val text = android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT)
                    .toString().replace("\n\n+".toRegex(), "\n").trim()
                _content.value = file.name to text.ifBlank { "(empty)" }
            }.onFailure {
                _content.value = file.name to ApiException.from(it, "خطا").message
            }
        }
    }

    fun meta(file: LogFile): String = formatBytes(file.size)
}
