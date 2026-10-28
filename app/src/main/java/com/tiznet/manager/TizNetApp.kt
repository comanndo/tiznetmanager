package com.tiznet.manager

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.tiznet.manager.data.SessionManager
import com.tiznet.manager.data.db.AppDatabase
import com.tiznet.manager.data.prefs.PrefsManager
import com.tiznet.manager.data.repo.PanelRepository
import com.tiznet.manager.util.CrashReporter
import com.tiznet.manager.util.CryptoManager

class TizNetApp : Application() {

    lateinit var prefs: PrefsManager
        private set
    lateinit var crypto: CryptoManager
        private set
    lateinit var repository: PanelRepository
        private set
    lateinit var session: SessionManager
        private set

    override fun onCreate() {
        // Install before anything else so even initialization failures are recorded.
        CrashReporter.install(this)
        super.onCreate()
        prefs = PrefsManager(this)
        AppCompatDelegate.setDefaultNightMode(prefs.themeMode)
        crypto = CryptoManager()
        val dao = AppDatabase.get(this).panelDao()
        repository = PanelRepository(dao, crypto)
        session = SessionManager(repository, prefs)
        instance = this
    }

    companion object {
        lateinit var instance: TizNetApp
            private set
    }
}
