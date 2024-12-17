package com.collecto.collectoandroidapp.controller.accounts

import android.os.Looper
import android.os.Bundle
import android.view.View
import android.os.Handler
import android.widget.Toast
import android.widget.Button
import android.content.Intent
import android.widget.TextView
import android.widget.EditText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.ImageButton
import android.view.animation.Animation
import androidx.lifecycle.lifecycleScope
import com.collecto.collectoandroidapp.R
import android.view.animation.AnimationUtils
import com.collecto.collectoandroidapp.BaseActivity
import com.collecto.collectoandroidapp.service.AuthService
import com.collecto.collectoandroidapp.view.LoadingOverlay
import com.collecto.collectoandroidapp.controller.settings.SettingsActivity
import com.collecto.collectoandroidapp.controller.collections.ShowCollectionsActivity

class NewPasswordActivity : BaseActivity() {

    // Local variables
    private lateinit var loadingOverlay: LoadingOverlay
    private lateinit var authService: AuthService

    // Basic logic of the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authService = AuthService(this)

        // Animations
        val animScale : Animation = AnimationUtils.loadAnimation(this, R.anim.button_click_anim)
        val animLargerScale : Animation = AnimationUtils.loadAnimation(this, R.anim.imageview_click_anim)

        // Opening the password reset page
        setContentView(R.layout.activity_new_password)

        // Password reset button functionality
        val registerButton: Button = findViewById(R.id.reset_button)
        registerButton.setOnClickListener {
            view -> view.startAnimation(animScale)
            val email = intent.getStringExtra("email")
            if (email != null) {
                finishPasswordReset(email)
            }
        }

        // Back button functionality
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                delay(300)
                onBackPressed()
            }
        }

        // Settings button functionality
        val settingsButton : ImageButton = findViewById(R.id.settings_button)
        settingsButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                delay(300)
                val intent = Intent(this@NewPasswordActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    // Ending the password reset process
    private fun finishPasswordReset(email: String) {
        loadingOverlay = LoadingOverlay(this)

        val confirmCodeField : TextView = findViewById(R.id.verification_code_input)
        val passwordField : EditText = findViewById(R.id.password_input)
        val confirmPasswordField : EditText = findViewById(R.id.confirm_password_input)

        val confirmationCode = confirmCodeField.text.toString()
        val password = passwordField.text.toString()
        val confirmPassword = confirmPasswordField.text.toString()

        // Error fields
        val codeErrorField : TextView = findViewById(R.id.code_error)
        val passwordErrorField : TextView = findViewById(R.id.new_password_error)
        val passwordConfirmationErrorField : TextView = findViewById(R.id.password_confirmation_error)

        // Marker for checking fields before password reset
        var isValid = true

        // Verification code validation
        val isCodeValid = authService.validateCode(confirmationCode)
        if (!isCodeValid) {
            isValid = false
            codeErrorField.text = getString(R.string.wrong_code_error)
            codeErrorField.visibility = View.VISIBLE
        } else {
            codeErrorField.visibility = View.INVISIBLE
        }

        // Check if the password meets the requirements
        val (isPasswordValid, passwordErrors) = authService.validatePassword(password)
        if (!isPasswordValid) {
            isValid = false
            passwordErrorField.text = passwordErrors
            passwordErrorField.visibility = View.VISIBLE
        } else {
            passwordErrorField.visibility = View.INVISIBLE
        }

        // Check if passwords match
        if (password != confirmPassword || confirmPassword.isEmpty()) {
            passwordConfirmationErrorField.text = getString(R.string.password_mismatch_error)
            passwordConfirmationErrorField.visibility = View.VISIBLE
            isValid = false
        } else {
            passwordConfirmationErrorField.visibility = View.INVISIBLE
        }

        // Resetting process
        if (isValid) {
            loadingOverlay = LoadingOverlay(this)
            lifecycleScope.launch {
                loadingOverlay.show()
                delay(300)

                val (result, error) = authService.resetPassword(email, confirmationCode, password)
                if (result) {
                    Toast.makeText(this@NewPasswordActivity, getString(R.string.successful_reset), Toast.LENGTH_LONG).show()
                    val intent = Intent(this@NewPasswordActivity, ShowCollectionsActivity::class.java)
                    startActivity(intent)
                } else {
                    if (error == "Wrong code") {
                        wrongCredentialsEntered()
                    } else if (error == "Old password") {
                        oldPasswordEntered()
                        delay(2000)
                        val toast = Toast.makeText(this@NewPasswordActivity, getString(R.string.new_code_sent), Toast.LENGTH_LONG)
                        toast.show()
                        Handler(Looper.getMainLooper()).postDelayed({ toast.cancel() }, 60000)
                    } else {
                        Toast.makeText(this@NewPasswordActivity, getString(R.string.unsuccessful_reset), Toast.LENGTH_LONG).show()
                    }
                }

                loadingOverlay.hide()
            }
        }
    }

    // User provided wrong credentials
    private fun wrongCredentialsEntered() {
        val emailErrorField: TextView = findViewById(R.id.code_error)
        emailErrorField.text = getString(R.string.wrong_code_error)
        emailErrorField.visibility = View.VISIBLE
    }

    // User entered old password
    private fun oldPasswordEntered() {
        val passwordErrorField: TextView = findViewById(R.id.password_error)
        passwordErrorField.text = getString(R.string.old_password)
        passwordErrorField.visibility = View.VISIBLE
    }

}