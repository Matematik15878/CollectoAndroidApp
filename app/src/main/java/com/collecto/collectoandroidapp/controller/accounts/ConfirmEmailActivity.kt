package com.collecto.collectoandroidapp.controller.accounts

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.Button
import android.content.Intent
import kotlinx.coroutines.Job
import android.util.TypedValue
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
import com.collecto.collectoandroidapp.controller.settings.SettingsActivity
import com.collecto.collectoandroidapp.controller.collections.ShowCollectionsActivity

class ConfirmEmailActivity : BaseActivity() {

    // Local values and variables
    private var countdownJob: Job? = null
    private lateinit var authService: AuthService

    // Basic logic of the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authService = AuthService(applicationContext)
        val email = intent.getStringExtra("EMAIL")

        // Animations
        val animScale : Animation = AnimationUtils.loadAnimation(this, R.anim.button_click_anim)
        val animLargerScale : Animation = AnimationUtils.loadAnimation(this, R.anim.imageview_click_anim)

        // Opening the email confirmation page
        setContentView(R.layout.activity_confirm_email)

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
        val registerButton : Button = findViewById(R.id.login_button)
        registerButton.setOnClickListener {
            view -> view.startAnimation(animScale)
            if (email != null) {
                completeConfirmationWithOTP(email)
            }
        }

        // Resend code button functionality
        startTimer()
        val resendCodeButton : TextView = findViewById(R.id.didnt_get_code)
        resendCodeButton.isEnabled = false
        resendCodeButton.setTextColor(TypedValue().apply { this@ConfirmEmailActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true) }.data)
        resendCodeButton.setOnClickListener {
            view -> view.startAnimation(animScale)
            lifecycleScope.launch {
                if (email != null) {
                    resendVerificationCode(email)
                }
            }
        }

        // Settings button functionality
        val settingsButton : ImageButton = findViewById(R.id.settings_button)
        settingsButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                delay(300)
                val intent = Intent(this@ConfirmEmailActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    // Method for sending a verification code
    private fun completeConfirmationWithOTP(email: String) {
        lifecycleScope.launch { delay(300) }

        // Confirmation code
        val confirmCodeField : TextView = findViewById(R.id.verification_code_input)
        val confirmationCode = confirmCodeField.text.toString()

        // Error fields
        val codeErrorField : TextView = findViewById(R.id.code_error)

        // Marker for checking fields
        var marker = true

        // Verification code validation
        val isCodeValid = authService.validateCode(confirmationCode)
        if (!isCodeValid) {
            marker = false
            codeErrorField.text = getString(R.string.wrong_code_error)
            codeErrorField.visibility = View.VISIBLE
        } else {
            codeErrorField.visibility = View.INVISIBLE
        }

        // Marker check and code confirmation
        if (marker) {
            lifecycleScope.launch {
                if (authService.verifyWithOTP(email, confirmationCode)) {
                    Toast.makeText(this@ConfirmEmailActivity, getString(R.string.successful_registration), Toast.LENGTH_LONG).show()
                    val intent = Intent(this@ConfirmEmailActivity, ShowCollectionsActivity::class.java)
                    startActivity(intent)
                } else {
                    codeErrorField.text = getString(R.string.wrong_code_error)
                    codeErrorField.visibility = View.VISIBLE
                }
            }
        }
    }

    // Method for sending a new verification code
    private fun resendVerificationCode(email: String) {
        val resendCodeButton : TextView = findViewById(R.id.didnt_get_code)

        countdownJob?.cancel()
        startTimer()

        resendCodeButton.setTextColor(TypedValue().apply {
            this@ConfirmEmailActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
        }.data)

        lifecycleScope.launch {
            authService.resendCode(email)
        }
    }

    // Method to start timer before sending new code
    private fun startTimer() {
        val countdownTextView: TextView = findViewById(R.id.countdown)
        val resendCodeButton : TextView = findViewById(R.id.didnt_get_code)

        resendCodeButton.isEnabled = false

        countdownJob = lifecycleScope.launch {
            for (i in 60 downTo 0) {
                countdownTextView.text = String.format("%02d", i)
                if (i > 0) {
                    delay(1000)
                }
            }
            resendCodeButton.setTextColor(TypedValue().apply {
                this@ConfirmEmailActivity.theme.resolveAttribute(android.R.attr.textColor, this, true) }.data)
            resendCodeButton.isEnabled = true
        }
    }

}