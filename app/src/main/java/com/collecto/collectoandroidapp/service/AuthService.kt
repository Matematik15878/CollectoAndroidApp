package com.collecto.collectoandroidapp.service

import android.net.Uri
import android.util.Patterns
import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import com.collecto.collectoandroidapp.R
import com.collecto.collectoandroidapp.model.AppUser
import com.collecto.collectoandroidapp.repository.AuthRepository

class AuthService (private val context: Context) {
    // Local auth repo
    private val authRepository = AuthRepository(context.applicationContext)

    // Password validation method
    fun validatePassword(password: String): Pair<Boolean, String> {
        val specialCharsRegex = ".*[\\^\\$\\*\\.\\[\\]{}()\\?\\-\"!@#%&/\\\\,><':;|_~`+=].*".toRegex()
        if (password.length < 8) {
            return Pair(false, context.getString(R.string.password_min_length_error))
        }
        else if (password.length > 16) {
            return Pair(false, context.getString(R.string.password_max_length_error))
        }
        else if (!password.matches(".*[a-z].*".toRegex())) {
            return Pair(false, context.getString(R.string.password_lowercase_error))
        }
        else if (!password.matches(".*[A-Z].*".toRegex())) {
            return Pair(false, context.getString(R.string.password_uppercase_error))
        }
        else if (!password.matches(".*\\d.*".toRegex())) {
            return Pair(false, context.getString(R.string.password_digit_error))
        }
        else if (!password.matches(specialCharsRegex)) {
            return Pair(false, context.getString(R.string.password_special_char_error))
        }
        else {
            return Pair(true, "")
        }
    }

    // Username validation method
    fun validateUsername(username: String): Pair<Boolean, String> {
        return if (username.length < 5 || username.length > 25) {
            Pair(false, context.getString(R.string.username_length_error))
        } else if (!username.matches("^[a-zA-Z0-9]*$".toRegex())) {
            Pair(false, context.getString(R.string.username_special_char_error))
        } else {
            Pair(true, "")
        }
    }

    // Email validation method
    fun validateEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Email confirmation code validation method
    fun validateCode(code: String): Boolean {
        return if (code.length != 6) {
            false
        } else if (!code.matches("^[0-9]*$".toRegex())) {
            false
        } else {
            true
        }
    }

    // Register user method
    suspend fun registerUser(user: AppUser): Pair<Boolean, String> {
        val result = authRepository.registerUser(user)
        return when (result) {
            "Registered" -> Pair(true, "")
            "User already exists" -> Pair(false, "User already exists")
            else -> Pair(false, "")
        }
    }

    // Email verification method
    suspend fun verifyWithOTP(email: String, code: String): Boolean {
        return authRepository.verifyWithOTP(email, code)
    }

    // Method for resending mail verification code
    suspend fun resendCode(email: String) {
        authRepository.resendCode(email)
    }

    // Method for sign in
    suspend fun logInAccount(user: AppUser): Boolean {
        return authRepository.logInAccount(user)
    }

    // Send confirmation code
    suspend fun sendEmailForResetPassword(email: String) {
        authRepository.sendEmailForResetPassword(email)
    }

    // Method to change password
    suspend fun resetPassword(email: String, code: String, password: String): Pair<Boolean, String> {
        if (authRepository.verifyResetPasswordOTP(email, code)) {
            val (marker, error) = authRepository.resetPassword(password)
            if (!marker && error == "Old password") {
                authRepository.logOut()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(65000)
                    authRepository.sendEmailForResetPassword(email)
                }
                return Pair(false, "Old password")
            } else if (marker) {
                return Pair(true, "Changed")
            } else {
                return Pair(false, "Not changed")
            }
        }
        else {
            return Pair(false, "Wrong code")
        }
    }

    // Method checks whether the user is authorized
    suspend fun checkAuthStatus(): Boolean {
        return authRepository.checkAuthStatus()
    }

    // Get user's email if authorized
    suspend fun getUserEmail(): String? {
        authRepository.checkAuthStatus()
        return authRepository.getUserEmail()
    }

    // Get user's username if authorized
    suspend fun getUserUsername(): String? {
        authRepository.checkAuthStatus()
        val userMetadata = authRepository.getUserMetadata()

        if (userMetadata != null) {
            return userMetadata["display_name"] as? String
        }
        return null
    }

    // Method for logging out of your account
    suspend fun logOut() {
        authRepository.logOut()
    }

    // Refresh session
    suspend fun refreshSession() {
        authRepository.refreshSession()
    }

    // Method for updating username
    suspend fun updateUsername(username: String): Boolean {
        return authRepository.updateUsername(username)
    }

    // Returns UID
    fun getUserId(): String? {
        return authRepository.getUserId()
    }

    // Saves the user's avatar
    suspend fun saveAvatar(uri: Uri): Boolean {
        val result = authRepository.saveAvatar(uri)
        return result
    }

    // Returns the user's avatar
    suspend fun loadAvatar(): Bitmap? {
        return authRepository.loadAvatar()
    }

    // Deletes the user's avatar
    suspend fun deleteAvatar() {
        authRepository.deleteAvatar()
    }

}