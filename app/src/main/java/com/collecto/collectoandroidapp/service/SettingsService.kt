package com.collecto.collectoandroidapp.service

import android.net.Uri
import android.content.Intent
import android.graphics.Bitmap
import android.content.Context
import android.content.SharedPreferences
import com.collecto.collectoandroidapp.controller.accounts.RegisterAccountActivity

class SettingsService(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val authService = AuthService(context)

    // Saving the selected language
    fun saveLanguage(languageCode: String) {
        prefs.edit().putString("language", languageCode).apply()
    }

    // Get current language
    fun getCurrentLanguage(): String {
        return prefs.getString("language", "en") ?: "en"
    }

    // Saving the night mode
    fun saveNightMode(isNightMode: Boolean) {
        prefs.edit().putBoolean("night_mode", isNightMode).apply()
    }

    // Get current night mode
    fun isNightModeEnabled(): Boolean {
        return prefs.getBoolean("night_mode", false)
    }

    // Checking authentication status
    suspend fun isUserAuthenticated(): Boolean {
        return authService.checkAuthStatus()
    }

    // Getting user email
    suspend fun getUserEmail(): String? {
        return authService.getUserEmail()
    }

    // Getting username
    suspend fun getUserUsername(): String? {
        return authService.getUserUsername()
    }

    // Username validation
    fun validateUsername(username: String): Pair<Boolean, String?> {
        return authService.validateUsername(username)
    }

    // Update username
    suspend fun updateUsername(username: String): Boolean {
        return authService.updateUsername(username)
    }

    // Logout from account
    suspend fun logOut() {
        authService.logOut()
        val intent = Intent(context, RegisterAccountActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    // Saves the user's avatar
    suspend fun saveAvatar(uri: Uri): Boolean {
        return authService.saveAvatar(uri)
    }

    // Updates path to the avatar in DB
    suspend fun loadAvatar(): Bitmap? {
        return authService.loadAvatar()
    }

    // Deletes the user's avatar
    suspend fun deleteAvatar() {
        authService.deleteAvatar()
    }

}