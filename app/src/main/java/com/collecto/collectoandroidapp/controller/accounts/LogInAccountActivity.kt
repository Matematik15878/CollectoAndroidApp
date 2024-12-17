package com.collecto.collectoandroidapp.controller.accounts

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.Button
import android.content.Intent
import android.widget.EditText
import android.widget.TextView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.ImageButton
import android.view.animation.Animation
import androidx.lifecycle.lifecycleScope
import com.collecto.collectoandroidapp.R
import android.view.animation.AnimationUtils
import com.collecto.collectoandroidapp.model.AppUser
import com.collecto.collectoandroidapp.service.AuthService
import com.collecto.collectoandroidapp.BaseActivity
import com.collecto.collectoandroidapp.view.LoadingOverlay
import com.collecto.collectoandroidapp.controller.settings.SettingsActivity
import com.collecto.collectoandroidapp.controller.collections.ShowCollectionsActivity

class LogInAccountActivity : BaseActivity() {

    // Local values
    private lateinit var loadingOverlay: LoadingOverlay
    private lateinit var authService: AuthService

    // Basic logic of the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authService = AuthService(this)

        // Animations
        val animScale : Animation = AnimationUtils.loadAnimation(this, R.anim.button_click_anim)
        val animLargerScale : Animation = AnimationUtils.loadAnimation(this, R.anim.imageview_click_anim)

        // Opening the login page
        setContentView(R.layout.activity_login)

        // Opening the registration page
        val signUpText: TextView = findViewById(R.id.register_text)
        signUpText.setOnClickListener {
            view -> view.startAnimation(animScale)
            lifecycleScope.launch {
                delay(300)
                val intent = Intent(this@LogInAccountActivity, RegisterAccountActivity::class.java)
                startActivity(intent)
            }
        }

        // Login button functionality
        val registerButton: Button = findViewById(R.id.login_button)
        registerButton.setOnClickListener {
            view -> view.startAnimation(animScale)
            handleSignIn()
        }

        // Password recovery page
        val forgotPasswordText: TextView = findViewById(R.id.forgot_password_text)
        forgotPasswordText.setOnClickListener {
            view -> view.startAnimation(animScale)
            handleResetPassword()
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
                val intent = Intent(this@LogInAccountActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    // Password reset process start
    private fun handleResetPassword() {
        lifecycleScope.launch {
            delay(300)
            val emailInput: TextView = findViewById(R.id.email_address_input)
            val email = emailInput.text.toString()

            val intent = Intent(this@LogInAccountActivity, ResetPasswordActivity::class.java)
            intent.putExtra("email", email)

            startActivity(intent)
        }
    }

    // Method of data processing before signing in
    private fun handleSignIn() {
        // Text fields
        val passwordField : EditText = findViewById(R.id.password_input)
        val emailField : TextView = findViewById(R.id.email_address_input)

        // Error fields
        val emailErrorField: TextView = findViewById(R.id.email_error)
        val passwordErrorField: TextView = findViewById(R.id.password_error)
        emailErrorField.visibility = View.INVISIBLE
        passwordErrorField.visibility = View.INVISIBLE

        // Get text field values
        val email = emailField.text.toString()
        val password = passwordField.text.toString()

        // Marker for checking fields before registration
        var isValid = true

        // Check if the password meets the requirements
        val (isPasswordValid, passwordErrors) = authService.validatePassword(password)
        if (!isPasswordValid) {
            isValid = false
            passwordErrorField.text = passwordErrors
            passwordErrorField.visibility = View.VISIBLE
        } else {
            passwordErrorField.visibility = View.INVISIBLE
        }

        // Check if the email is valid
        if (!authService.validateEmail(email)) {
            emailErrorField.text = getString(R.string.invalid_email_error)
            emailErrorField.visibility = View.VISIBLE
            isValid = false
        } else {
            emailErrorField.visibility = View.INVISIBLE
        }

        // Registration process start
        if (isValid) {
            val user = AppUser(username = "", email = email, password = password)
            handleLoginUser(user)
        }
    }

    // Log in account method
    private fun handleLoginUser(user: AppUser) {
        loadingOverlay = LoadingOverlay(this)

        lifecycleScope.launch {
            loadingOverlay.show()
            delay(300)

            if (authService.logInAccount(user)) {
                Toast.makeText(this@LogInAccountActivity, getString(R.string.successful_authentication), Toast.LENGTH_LONG).show()
                val intent = Intent(this@LogInAccountActivity, ShowCollectionsActivity::class.java)
                startActivity(intent)
            } else {
                wrongCredentialsEntered()
            }

            loadingOverlay.hide()
        }
    }

    // User provided wrong credentials
    private fun wrongCredentialsEntered() {
        val emailErrorField: TextView = findViewById(R.id.email_error)
        emailErrorField.text = getString(R.string.wrong_email_or_password)
        emailErrorField.visibility = View.VISIBLE
    }

}