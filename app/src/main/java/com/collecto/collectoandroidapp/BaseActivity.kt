package com.collecto.collectoandroidapp

import java.util.Locale
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    // Current language value
    private var currentLanguage: String? = null

    // Method for updating the app's locale based on the user's preference
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("settings", MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"
        currentLanguage = language
        val context = updateLocale(newBase, language)
        super.attachBaseContext(context)
    }

    // Updates the locale of the app based on the given language code
    private fun updateLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    // Checks if the language has changed and recreates the activity if necessary
    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"

        if (language != currentLanguage) {
            recreate()
        }
    }

}