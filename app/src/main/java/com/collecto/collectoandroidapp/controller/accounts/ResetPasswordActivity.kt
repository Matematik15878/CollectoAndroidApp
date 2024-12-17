package com.collecto.collectoandroidapp.controller.accounts

import android.os.Bundle
import android.widget.Button
import android.content.Intent
import android.widget.TextView
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

class ResetPasswordActivity : BaseActivity() {

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
        setContentView(R.layout.activity_reset_password)

        // Retrieve the email from the intent
        val emailTextView: TextView = findViewById(R.id.email_address_input)
        emailTextView.text = intent.getStringExtra("email")

        // Password reset button functionality
        val registerButton: Button = findViewById(R.id.reset_button)
        registerButton.setOnClickListener {
            view -> view.startAnimation(animScale)
            handlePasswordReset()
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
                val intent = Intent(this@ResetPasswordActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    // Starting the password reset process
    private fun handlePasswordReset() {
        loadingOverlay = LoadingOverlay(this)
        val emailTextView: TextView = findViewById(R.id.email_address_input)
        val email = emailTextView.text.toString()

        lifecycleScope.launch {
            loadingOverlay.show()
            delay(300)
            authService.sendEmailForResetPassword(email)
            val intent = Intent(this@ResetPasswordActivity, NewPasswordActivity::class.java)
            intent.putExtra("email", email)
            startActivity(intent)
            loadingOverlay.hide()
        }
    }

}