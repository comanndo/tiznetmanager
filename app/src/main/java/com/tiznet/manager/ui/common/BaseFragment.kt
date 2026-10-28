package com.tiznet.manager.ui.common

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.data.SessionManager
import com.tiznet.manager.data.repo.PanelRepository
import com.tiznet.manager.data.prefs.PrefsManager
import com.tiznet.manager.util.snack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseFragment : Fragment() {

    protected val app: TizNetApp get() = requireActivity().application as TizNetApp
    protected val session: SessionManager get() = app.session
    protected val repository: PanelRepository get() = app.repository
    protected val prefs: PrefsManager get() = app.prefs

    protected fun <T> Flow<T>.collectOnStarted(action: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect { action(it) }
            }
        }
    }

    protected fun runJob(
        loading: (Boolean) -> Unit,
        onError: (String) -> Unit = { msg -> view?.snack(msg) },
        block: suspend () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            loading(true)
            runCatching { block() }
                .onFailure { onError(com.tiznet.manager.data.remote.ApiException.from(it, "خطا").message) }
            loading(false)
        }
    }

    protected fun rootView(): View? = view

    /** Opens the legacy Flask-Admin web UI (used for v11/v12 features without a JSON API). */
    protected fun openWebAdmin(section: String = "") {
        val panel = session.activePanel.value ?: return
        val base = com.tiznet.manager.data.remote.RetrofitFactory.normalizeBaseUrl(panel.baseUrl)
        val path = panel.proxyPath.trim('/')
        val url = buildString {
            append(base)
            if (path.isNotBlank()) append('/').append(path)
            append("/admin/")
            if (section.isNotBlank()) append(section).append('/')
        }
        runCatching {
            startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            )
        }
    }
}
