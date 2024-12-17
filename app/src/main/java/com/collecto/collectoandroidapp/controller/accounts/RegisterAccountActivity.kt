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
import com.collecto.collectoandroidapp.BaseActivity
import com.collecto.collectoandroidapp.view.LoadingOverlay
import com.collecto.collectoandroidapp.service.AuthService
import com.collecto.collectoandroidapp.controller.settings.SettingsActivity

class RegisterAccountActivity : BaseActivity() {

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

        // Opening the registration page
        setContentView(R.layout.activity_registration)

        // Opening the login page
        val signInText: TextView = findViewById(R.id.login_text)
        signInText.setOnClickListener {
            view -> view.startAnimation(animScale)
            lifecycleScope.launch {
                delay(300)
                val intent = Intent(this@RegisterAccountActivity, LogInAccountActivity::class.java)
                startActivity(intent)
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

        // Register button functionality
        val registerButton: Button = findViewById(R.id.register_button)
        registerButton.setOnClickListener {
            view -> view.startAnimation(animScale)
            lifecycleScope.launch {
                handleRegistration()
            }
        }

        // Settings button functionality
        val settingsButton : ImageButton = findViewById(R.id.settings_button)
        settingsButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                delay(300)
                val intent = Intent(this@RegisterAccountActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    // Method of data processing before registration
    private fun handleRegistration() {
        // Text fields
        val usernameField : TextView = findViewById(R.id.username_input)
        val emailField : TextView = findViewById(R.id.email_address_input)
        val passwordField : EditText = findViewById(R.id.password_input)
        val confirmPasswordField : EditText = findViewById(R.id.confirm_password_input)

        // Error fields
        val usernameErrorField : TextView = findViewById(R.id.username_error)
        val emailErrorField : TextView = findViewById(R.id.email_error)
        val passwordErrorField : TextView = findViewById(R.id.password_error)
        val passwordConfirmationErrorField : TextView = findViewById(R.id.password_confirmation_error)

        // Get text field values
        val username = usernameField.text.toString()
        val email = emailField.text.toString()
        val password = passwordField.text.toString()
        val confirmPassword = confirmPasswordField.text.toString()

        // Marker for checking fields before registration
        var isValid = true

        // Check if the username is valid
        val (isUsernameValid, usernameErrors) = authService.validateUsername(username)
        if (!isUsernameValid) {
            isValid = false
            usernameErrorField.text = usernameErrors
            usernameErrorField.visibility = View.VISIBLE
        } else {
            usernameErrorField.visibility = View.INVISIBLE
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

        // Check if the email is valid
        if (!authService.validateEmail(email)) {
            emailErrorField.text = getString(R.string.invalid_email_error)
            emailErrorField.visibility = View.VISIBLE
            isValid = false
        } else {
            emailErrorField.visibility = View.INVISIBLE
        }

        // Check if passwords match
        if (password != confirmPassword || confirmPassword.isEmpty()) {
            passwordConfirmationErrorField.text = getString(R.string.password_mismatch_error)
            passwordConfirmationErrorField.visibility = View.VISIBLE
            isValid = false
        } else {
            passwordConfirmationErrorField.visibility = View.INVISIBLE
        }

        // Registration process start
        if (isValid) {
            val user = AppUser(username = username, email = email, password = password)
            registerUser(user)
        }
    }

    // User registration method
    private fun registerUser(user: AppUser) {
        loadingOverlay = LoadingOverlay(this)
        lifecycleScope.launch {
            loadingOverlay.show()

            val (result, error) = authService.registerUser(user)

            if (error == "User already exists") {
                enteredEmailAlreadyExists()
            } else if (result) {
                confirmEmail(user.email)
            } else {
                Toast.makeText(this@RegisterAccountActivity, getString(R.string.unsuccessful_registration), Toast.LENGTH_LONG).show()
            }

            loadingOverlay.hide()
        }
    }

    // Email confirmation process start
    private fun confirmEmail(email: String) {
        val usernameErrorField: TextView = findViewById(R.id.username_error)
        val emailErrorField: TextView = findViewById(R.id.email_error)

        usernameErrorField.visibility = View.INVISIBLE
        emailErrorField.visibility = View.INVISIBLE

        val intent = Intent(this@RegisterAccountActivity, ConfirmEmailActivity::class.java)
        intent.putExtra("EMAIL", email)
        startActivity(intent)
    }

    // Email already exists
    private fun enteredEmailAlreadyExists() {
        val emailErrorField: TextView = findViewById(R.id.email_error)
        emailErrorField.text = getString(R.string.email_already_exists)
        emailErrorField.visibility = View.VISIBLE
    }

}