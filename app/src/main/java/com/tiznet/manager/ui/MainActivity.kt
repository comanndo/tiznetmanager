package com.tiznet.manager.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.tiznet.manager.R
import com.tiznet.manager.TizNetApp
import com.tiznet.manager.databinding.ActivityMainBinding
import com.tiznet.manager.util.CrashReporter
import com.tiznet.manager.util.dp
import com.tiznet.manager.util.visible
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfig: AppBarConfiguration

    private val topLevelDestinations = setOf(
        R.id.dashboardFragment,
        R.id.usersFragment,
        R.id.nodesFragment,
        R.id.protocolsFragment,
        R.id.panelsFragment
    )

    private val fullScreenDestinations = setOf(R.id.loginFragment, R.id.userEditFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as TizNetApp
        if (app.prefs.dynamicColor) DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHost) as NavHostFragment
        navController = navHost.navController

        appBarConfig = AppBarConfiguration(
            topLevelDestinations,
            binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfig)
        binding.bottomNav.setupWithNavController(navController)
        binding.navigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val fullScreen = destination.id in fullScreenDestinations
            binding.bottomNav.visible(!fullScreen)
            binding.appBar.visible(!fullScreen || destination.id != R.id.loginFragment)
            if (fullScreen && destination.id == R.id.loginFragment) {
                binding.appBar.visibility = View.GONE
                lockDrawer(true)
            } else {
                lockDrawer(false)
            }
        }

        binding.navigationView.setNavigationItemSelectedListener(drawerListener())
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_panels) {
                navigateSafely(R.id.panelsFragment)
                true
            } else false
        }

        lifecycleScope.launch {
            app.session.activePanel.collect { panel ->
                val header = binding.navigationView.getHeaderView(0)
                header?.findViewById<android.widget.TextView>(R.id.drawerPanelName)?.text =
                    panel?.let { "${it.name} · ${it.baseUrl.removePrefix("https://").removePrefix("http://")}" }
                        ?: getString(R.string.login_no_panels)
                if (panel == null && navController.currentDestination?.id != R.id.loginFragment) {
                    navigateSafely(R.id.loginFragment)
                }
            }
        }

        lifecycleScope.launch {
            val restored = app.session.restoreLastSession()
            if (restored) navigateSafely(R.id.dashboardFragment)
        }

        showPendingCrashIfAny()
    }

    private fun showPendingCrashIfAny() {
        if (!CrashReporter.hasPendingCrash(this)) return
        val trace = CrashReporter.readPendingCrash(this)
        val textView = TextView(this).apply {
            text = trace
            textSize = 11f
            setTextIsSelectable(true)
            setPadding(dp(20), dp(12), dp(20), 0)
        }
        val scroll = ScrollView(this).apply { addView(textView) }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.crash_dialog_title)
            .setView(scroll)
            .setPositiveButton(R.string.crash_share) { _, _ -> shareCrashLog() }
            .setNegativeButton(R.string.crash_dismiss, null)
            .setOnDismissListener { CrashReporter.clear(this) }
            .show()
    }

    private fun shareCrashLog() {
        val file: File = CrashReporter.crashFile(this)
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "TizNet Manager crash report")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.crash_share)))
        CrashReporter.clear(this)
    }

    private fun drawerListener() = NavigationView.OnNavigationItemSelectedListener { item ->
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        if (item.itemId == R.id.action_disconnect) {
            (application as TizNetApp).session.logout()
            navigateSafely(R.id.loginFragment)
            return@OnNavigationItemSelectedListener true
        }
        navigateSafely(item.itemId)
        true
    }

    private fun navigateSafely(destinationId: Int) {
        runCatching {
            if (navController.currentDestination?.id == destinationId) return@runCatching
            navController.navigate(destinationId)
        }
    }

    private fun lockDrawer(locked: Boolean) {
        binding.drawerLayout.setDrawerLockMode(
            if (locked) DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            else DrawerLayout.LOCK_MODE_UNLOCKED
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfig) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
