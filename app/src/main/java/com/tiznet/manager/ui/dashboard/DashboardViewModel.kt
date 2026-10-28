package com.tiznet.manager.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.DashboardResponse
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session

    private val _days = MutableStateFlow(30)
    val days: StateFlow<Int> = _days.asStateFlow()

    private val _state = MutableStateFlow<UiState<DashboardResponse>>(UiState.Idle)
    val state: StateFlow<UiState<DashboardResponse>> = _state.asStateFlow()

    init {
        load()
    }

    fun setDays(days: Int) {
        if (_days.value != days) {
            _days.value = days
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                if (session.isLegacyApi) {
                    // v11/v12 only expose server_status; map what it provides.
                    com.tiznet.manager.data.remote.LegacyMappers.dashboard(
                        apiCall { session.api().serverStatus() }
                    )
                } else {
                    apiCall { session.api().dashboard(_days.value) }
                }
            }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure {
                    _state.value = UiState.Error(ApiException.from(it, "خطا").message, it)
                }
        }
    }
}
