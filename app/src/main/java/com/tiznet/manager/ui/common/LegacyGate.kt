package com.tiznet.manager.ui.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tiznet.manager.R

/**
 * Message shown on screens whose Admin V2 endpoints do not exist on
 * Hiddify v11/v12 (the feature arrived in v13).
 */
fun AndroidViewModel.legacyUnsupported(): String =
    getApplication<Application>().getString(R.string.unsupported_legacy)
