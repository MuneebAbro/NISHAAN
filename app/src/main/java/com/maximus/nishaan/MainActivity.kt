package com.maximus.nishaan

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.maximus.nishaan.core.util.Constants
import com.maximus.nishaan.core.util.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Single Activity host for the entire app.
 * All screen transitions handled via Navigation Component.
 */
class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val app = newBase.applicationContext as? NishaanApplication
        val language = if (app != null) {
            runBlocking {
                app.appContainer.dataStore.data
                    .map { it[stringPreferencesKey(Constants.PREF_LANGUAGE)] ?: "en" }
                    .first()
            }
        } else {
            "en"
        }
        super.attachBaseContext(LocaleHelper.wrap(newBase, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load and apply theme mode from DataStore
        val app = application as NishaanApplication
        val themeMode = runBlocking {
            app.appContainer.dataStore.data
                .map { it[stringPreferencesKey("app_theme")] ?: "system" }
                .first()
        }
        applyTheme(themeMode)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Dynamically adjust status bar icons to be dark in Light Mode and light in Dark Mode
        val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isNightMode

        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.splashFragment,
                R.id.onboardingFragment,
                R.id.languageSelectFragment,
                R.id.permissionsFragment,
                R.id.settingsFragment,
                R.id.authFragment -> {
                    bottomNav.visibility = View.GONE
                }
                else -> {
                    bottomNav.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun applyTheme(themeMode: String) {
        val mode = when (themeMode) {
            "light" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
    }
}