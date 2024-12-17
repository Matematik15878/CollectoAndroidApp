package com.collecto.collectoandroidapp.repository

import java.io.File
import android.net.Uri
import org.json.JSONObject
import android.widget.Toast
import org.json.JSONException
import android.content.Context
import android.graphics.Bitmap
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import android.graphics.BitmapFactory
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.Auth
import com.collecto.collectoandroidapp.R
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.JsonPrimitive
import com.collecto.collectoandroidapp.model.AppUser
import kotlinx.serialization.json.buildJsonObject
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.providers.builtin.Email

class AuthRepository(private val context: Context) {
    // Local supabase
    private val supabase = createSupabaseClient(
        supabaseUrl = context.getString(R.string.supabase_url),
        supabaseKey = context.getString(R.string.supabase_key)
    ) {
        install(Auth)
        install(Storage)
        install(Postgrest)
    }

    // Registration method
    suspend fun registerUser(user: AppUser): String {
        try {
            val result = supabase.auth.signUpWith(Email) {
                email = user.email
                password = user.password
                data = buildJsonObject {
                    put("display_name", JsonPrimitive(user.username))
                }
            }
            if (result != null) {
                val marker = result.identities?.isEmpty() != true
                return if (marker) {
                    "Registered"
                } else {
                    "User already exists"
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            return "Not registered"
        }
        return "Not registered"
    }

    // Email verification method
    suspend fun verifyWithOTP(email: String, token: String): Boolean {
        try {
            supabase.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = token)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    // Email verification code resend method
    suspend fun resendCode(email: String) {
        try{
            supabase.auth.resendEmail(OtpType.Email.SIGNUP, email)

        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
    }

    // Method for sign in
    suspend fun logInAccount(user: AppUser): Boolean {
        try {
            val result = supabase.auth.signInWith(Email) {
                email = user.email
                password = user.password
            }
        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    // Send confirmation code
    suspend fun sendEmailForResetPassword(email: String) {
        try {
            supabase.auth.resetPasswordForEmail(email = email)
        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
    }

    // Method for email verification when changing password
    suspend fun verifyResetPasswordOTP(email: String, code: String): Boolean {
        try {
            supabase.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    // Method to change password
    suspend fun resetPassword(new_password: String): Pair<Boolean, String> {
        try {
            supabase.auth.updateUser { password = new_password }
        } catch (e: Exception) {
            return if (e.message == "New password should be different from the old password.") {
                Pair(false, "Old password")
            } else {
                Pair(false, "Not changed")
            }
        }
        return Pair(true, "Changed")
    }

    // Method checks whether the user is authorized
    suspend fun checkAuthStatus(): Boolean {
        return supabase.auth.sessionStatus
            .filter { status ->
                status is SessionStatus.Authenticated || status is SessionStatus.NotAuthenticated
            }
            .first()
            .let { status ->
                when (status) {
                    is SessionStatus.Authenticated -> true
                    is SessionStatus.NotAuthenticated -> false
                    else -> false
                }
            }
    }

    // Get user's email if authorized
    fun getUserEmail(): String? {
        val user = supabase.auth.currentUserOrNull()
        if (user != null) {
            return user.email
        }
        return null
    }

    // Get user's username if authorized
    fun getUserMetadata(): Map<String, Any>? {
        val user = supabase.auth.currentUserOrNull()
        if (user != null) {
            return user.userMetadata?.let { parseMetadataToMap(it) }
        }
        return null
    }

    // JsonObject to Map
    private fun parseMetadataToMap(metadata: JsonObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()

        try {
            val jsonObject = JSONObject(metadata.toString())
            val displayName = jsonObject.optString("display_name", null)

            if (displayName != null) {
                map["display_name"] = displayName
            } else {
                map["display_name"] = "Unknown"
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return map
    }

    // Method for logging out of your account
    suspend fun logOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
    }

    // Refresh session
    suspend fun refreshSession() {
        supabase.auth.refreshCurrentSession()
    }

    // Method for updating username
    suspend fun updateUsername(username: String): Boolean {
        try {
            val result = supabase.auth.updateUser {
                data {
                    put("display_name", JsonPrimitive(username))
                }
            }
            if (result != null) {
                return true
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    // Returns UID
    fun getUserId(): String? {
        val user = supabase.auth.currentUserOrNull()
        if (user != null) {
            return user.id
        }
        return null
    }

    // Saves the user's avatar
    suspend fun saveAvatar(fileUri: Uri): Boolean {
        val bucketAvatars = supabase.storage.from("avatars")
        val compressedImage = uriToByteArray(fileUri, 100, context)

        refreshSession()
        val userId = getUserId()
        val cacheDir = File(context.cacheDir, "avatars/$userId")
        val cacheFile = File(cacheDir, "user_avatar.png")
        if (cacheFile.exists()) {
            cacheFile.delete()
        }

        try {
            val imagePath = "$userId/user_avatar"
            bucketAvatars.upload(imagePath, compressedImage) {
                upsert = true
            }
            cacheDir.mkdirs()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Converts photo into byteArray
    private fun uriToByteArray(uri: Uri, maxSizeKB: Int, context: Context): ByteArray {
        val originalBitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))

        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height

        val maxWidth = 1024
        val maxHeight = 1024

        var scaledWidth = originalWidth
        var scaledHeight = originalHeight

        if (originalWidth > maxWidth || originalHeight > maxHeight) {
            val ratio = Math.min(maxWidth.toFloat() / originalWidth, maxHeight.toFloat() / originalHeight)
            scaledWidth = (originalWidth * ratio).toInt()
            scaledHeight = (originalHeight * ratio).toInt()
        }

        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

        var stream = ByteArrayOutputStream()
        var quality = 90
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

        while (stream.size() / 1024 > maxSizeKB && quality > 50) {
            stream.reset()
            quality -= 5
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        }

        return stream.toByteArray()
    }

    // Returns the user's avatar
    suspend fun loadAvatar(): Bitmap? {
        val userId = getUserId()
        val cacheDir = File(context.cacheDir, "avatars/$userId")
        val cacheFile = File(cacheDir, "user_avatar.png")

        if (cacheFile.exists()) {
            return BitmapFactory.decodeFile(cacheFile.absolutePath)
        }

        val bucket = supabase.storage.from("avatars")
        val isAuthenticated = checkAuthStatus()

        if (isAuthenticated) {
            try {
                val imagePath = "$userId/user_avatar"
                val bytes = bucket.downloadAuthenticated(imagePath)
                val bitmap = byteArrayToBitmap(bytes)

                bitmap?.let {
                    cacheDir.mkdirs()
                    FileOutputStream(cacheFile).use { fos ->
                        it.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    }
                }

                return bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }

    // Converts byteArray into bitmap
    private fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    // Deletes the user's avatar
    suspend fun deleteAvatar() {
        val isAuthenticated = checkAuthStatus()
        if (isAuthenticated) {
            val bucket = supabase.storage.from("avatars")
            try {
                val imagePath = getUserId() + "/user_avatar"
                bucket.delete(imagePath)

                val userId = getUserId()
                val cacheDir = File(context.cacheDir, "avatars/$userId")
                val cacheFile = File(cacheDir, "user_avatar.png")

                if (cacheFile.exists()) {
                    cacheFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}