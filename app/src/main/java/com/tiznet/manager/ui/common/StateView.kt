package com.tiznet.manager.ui.common

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.tiznet.manager.R

/**
 * Binds the reusable loading/error/empty include layout.
 *
 * Note: when view_state.xml is used through an <include> that carries its own
 * android:id, the include overrides the root view's id, so the passed [root]
 * IS the container itself — we must not look up R.id.stateContainer on it
 * (that returns null and crashes on every screen). Child ids are unaffected.
 */
class StateView(private val root: View) {
    private val container: View = root
    private val progress: ProgressBar = root.findViewById(R.id.stateProgress)
    private val loadingText: TextView = root.findViewById(R.id.stateLoadingText)
    private val errorIcon: ImageView = root.findViewById(R.id.stateErrorIcon)
    private val errorText: TextView = root.findViewById(R.id.stateErrorText)
    private val retry: MaterialButton = root.findViewById(R.id.stateRetry)
    private val emptyIcon: ImageView = root.findViewById(R.id.stateEmptyIcon)
    private val emptyText: TextView = root.findViewById(R.id.stateEmptyText)

    fun loading(message: String? = null) {
        container.visibility = View.VISIBLE
        progress.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE
        loadingText.text = message ?: root.context.getString(R.string.state_loading)
        errorIcon.visibility = View.GONE
        errorText.visibility = View.GONE
        retry.visibility = View.GONE
        emptyIcon.visibility = View.GONE
        emptyText.visibility = View.GONE
    }

    fun error(message: String, onRetry: (() -> Unit)? = null) {
        container.visibility = View.VISIBLE
        progress.visibility = View.GONE
        loadingText.visibility = View.GONE
        emptyIcon.visibility = View.GONE
        emptyText.visibility = View.GONE
        errorIcon.visibility = View.VISIBLE
        errorText.visibility = View.VISIBLE
        errorText.text = message
        retry.visibility = if (onRetry != null) View.VISIBLE else View.GONE
        retry.setOnClickListener { onRetry?.invoke() }
    }

    fun empty(message: String? = null) {
        container.visibility = View.VISIBLE
        progress.visibility = View.GONE
        loadingText.visibility = View.GONE
        errorIcon.visibility = View.GONE
        errorText.visibility = View.GONE
        retry.visibility = View.GONE
        emptyIcon.visibility = View.VISIBLE
        emptyText.visibility = View.VISIBLE
        emptyText.text = message ?: root.context.getString(R.string.state_empty)
    }

    fun content() {
        container.visibility = View.GONE
    }
}
