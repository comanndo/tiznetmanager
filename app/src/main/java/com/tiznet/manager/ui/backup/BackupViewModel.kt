package com.tiznet.manager.ui.backup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.model.BackupFile
import com.tiznet.manager.data.model.BackupState
import com.tiznet.manager.data.remote.ApiException
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class BackupViewModel(app: Application) : AndroidViewModel(app) {

    private val session = (app as TizNetApp).session
    private val api get() = session.api()

    private val _state = MutableStateFlow<UiState<BackupState>>(UiState.Idle)
    val state: StateFlow<UiState<BackupState>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { apiCall { api.backupState() } }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(ApiException.from(it, "خطا").message, it) }
        }
    }

    fun createBackup(onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { apiCall { api.createBackup() } }
                .onSuccess { _state.value = UiState.Success(it); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    /** Downloads the backup into the app cache and returns the file for sharing. */
    suspend fun downloadToCache(): File? = runCatching {
        val body = apiCall { api.downloadBackup() }
        val dir = File(getApplication<TizNetApp>().cacheDir, "backups").apply { mkdirs() }
        val file = File(dir, "tiznet-backup-${System.currentTimeMillis()}.json.gz")
        file.outputStream().use { body.byteStream().copyTo(it) }
        file
    }.getOrNull()

    fun sendTelegram(onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { apiCall { api.sendBackupTelegram() } }
                .onSuccess { onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun restore(
        file: java.io.File,
        settings: Boolean,
        users: Boolean,
        domains: Boolean,
        replaceOwner: Boolean,
        onDone: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val mediaType = "application/gzip".toMediaTypeOrNull()
            val part = MultipartBody.Part.createFormData(
                "file", file.name, file.asRequestBody(mediaType)
            )
            fun part(value: Boolean): RequestBody =
                value.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            runCatching {
                apiCall {
                    api.restoreBackup(
                        part, part(settings), part(users), part(domains), part(replaceOwner)
                    )
                }
            }.onSuccess { load(); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }

    fun deleteServerFile(file: BackupFile, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                apiCall { api.deleteBackupFile(buildJsonObject { put("name", file.name) }) }
            }.onSuccess { _state.value = UiState.Success(it); onDone(null) }
                .onFailure { onDone(ApiException.from(it, "خطا").message) }
        }
    }
}
