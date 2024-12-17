package com.collecto.collectoandroidapp

import java.util.Locale
import android.os.Bundle
import android.content.Intent
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import com.collecto.collectoandroidapp.service.AuthService
import com.collecto.collectoandroidapp.controller.accounts.RegisterAccountActivity
import com.collecto.collectoandroidapp.controller.collections.ShowCollectionsActivity

class MainActivity : BaseActivity() {

    // Authorization service
    private lateinit var authService: AuthService

    // Basic logic of the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authService = AuthService(this)
        enableEdgeToEdge()
        loadSettings()

        // If user is authenticated, then show all collections, else let him sign up
        lifecycleScope.launch {
           try {
               val intent = if (authService.checkAuthStatus()) {
                   Intent(this@MainActivity, ShowCollectionsActivity::class.java)
               } else {
                   Intent(this@MainActivity, RegisterAccountActivity::class.java)
               }
               startActivity(intent)
               finish()
           } catch (e: Exception) {
               val intent = Intent(this@MainActivity, RegisterAccountActivity::class.java)
               startActivity(intent)
               finish()
           }
        }
    }

    // Method of loading settings
    private fun loadSettings() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val language = prefs.getString("language", "en") ?: "en"
        setAppLocale(language)

        val isNightMode = prefs.getBoolean("night_mode", false)
        setNightMode(isNightMode)
    }

    // Method of setting the application language
    private fun setAppLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    // Method to install application theme
    private fun setNightMode(isNightMode: Boolean) {
        val mode = if (isNightMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

}