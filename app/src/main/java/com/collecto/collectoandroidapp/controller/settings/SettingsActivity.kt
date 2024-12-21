package com.collecto.collectoandroidapp.controller.settings

import android.net.Uri
import android.view.View
import android.os.Bundle
import android.widget.Toast
import android.graphics.Color
import android.content.Intent
import android.widget.Spinner
import kotlinx.coroutines.Job
import android.content.Context
import android.app.AlertDialog
import android.widget.TextView
import android.util.TypedValue
import android.widget.EditText
import android.widget.ImageView
import kotlinx.coroutines.delay
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import android.animation.Animator
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.view.View.INVISIBLE
import android.provider.MediaStore
import android.animation.ObjectAnimator
import android.view.animation.Animation
import androidx.lifecycle.lifecycleScope
import com.collecto.collectoandroidapp.R
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.app.AppCompatDelegate
import android.animation.AnimatorListenerAdapter
import android.view.inputmethod.InputMethodManager
import com.collecto.collectoandroidapp.BaseActivity
import com.collecto.collectoandroidapp.view.LoadingOverlay
import com.collecto.collectoandroidapp.service.SettingsService
import com.collecto.collectoandroidapp.controller.accounts.ResetPasswordActivity

class SettingsActivity : BaseActivity() {

    // Local values and variables
    private var checkIfUserHasAvatar = false
    private lateinit var loadingOverlay: LoadingOverlay
    private lateinit var settingsService: SettingsService

    // User selection from where to upload photos
    private val requestCodeGallery = 100
    private val requestCodeFileManager = 101

    // Basic logic of the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settingsService = SettingsService(this)

        // Initial settings related to authorization
        hideInteractions()
        loadUserData()

        // Animations
        val animScale: Animation = AnimationUtils.loadAnimation(this, R.anim.button_click_anim)
        val animLargerScale: Animation = AnimationUtils.loadAnimation(this, R.anim.imageview_click_anim)

        // Languages variables
        val languages = listOf("en", "ru", "uk")
        val spinner = findViewById<Spinner>(R.id.language_spinner)
        val currentLanguage = settingsService.getCurrentLanguage()
        val adapter = ArrayAdapter(this, R.layout.layout_spinner_item, languages)

        // Languages spinner
        spinner.adapter = adapter
        spinner.setSelection(languages.indexOf(currentLanguage))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            var isFirstSelection = true

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isFirstSelection) {
                    isFirstSelection = false
                    return
                }
                val selectedLanguage = languages[position]
                if (selectedLanguage != currentLanguage) {
                    val animator = ObjectAnimator.ofFloat(findViewById(android.R.id.content), "alpha", 1f, 0f)
                    animator.duration = 300
                    animator.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            settingsService.saveLanguage(selectedLanguage)
                            recreate()
                        }
                    })
                    animator.start()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Day / night variables
        val isNightMode = settingsService.isNightModeEnabled()
        val switch = findViewById<SwitchCompat>(R.id.theme_switch)

        // Day / night theme switch
        switch.isChecked = isNightMode
        AppCompatDelegate.setDefaultNightMode(if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        updateThemeIcon(isNightMode)
        switch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                delay(200)
                setNightMode(isChecked)
                settingsService.saveNightMode(isChecked)
            }
        }

        // Password recovery page
        val resetPasswordText: TextView = findViewById(R.id.forgot_password_text)
        resetPasswordText.setOnClickListener { view ->
            view.startAnimation(animScale)
            resetPassword()
        }

        // Log out
        val forgotPasswordText: TextView = findViewById(R.id.log_out_text)
        forgotPasswordText.setOnClickListener { view ->
            view.startAnimation(animScale)
            lifecycleScope.launch {
                settingsService.logOut()
                finish()
            }
        }

        // Back button functionality
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener { view ->
            view.startAnimation(animLargerScale)
            lifecycleScope.launch { delay(300) }
            onBackPressed()
            finish()
        }
    }

    // Method of changing theme
    private fun setNightMode(isNightMode: Boolean) {
        val animator = ObjectAnimator.ofFloat(findViewById(android.R.id.content), "alpha", 1f, 0f)
        animator.duration = 300
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                AppCompatDelegate.setDefaultNightMode(
                    if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
                updateThemeIcon(isNightMode)
                recreate()
            }
        })
        animator.start()
    }

    // Method for changing the icon depending on the day/night theme
    private fun updateThemeIcon(isNightMode: Boolean) {
        val dayNightThemeTextView: TextView = findViewById(R.id.day_night_theme)
        if (isNightMode) {
            dayNightThemeTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_theme_night, 0, 0, 0)
        } else {
            dayNightThemeTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_theme_day, 0, 0, 0)
        }
    }

    // Password reset process start
    private fun resetPassword() {
        lifecycleScope.launch {
            delay(300)
            val email = settingsService.getUserEmail()
            val intent = Intent(this@SettingsActivity, ResetPasswordActivity::class.java)
            intent.putExtra("email", email)

            startActivity(intent)
            finish()
        }
    }

    // Hide account interactions
    private fun hideInteractions() {
        val logOutText: TextView = findViewById(R.id.log_out_text)
        lifecycleScope.launch {
            if (!settingsService.isUserAuthenticated()) {
                logOutText.setTextColor(TypedValue().apply {
                    this@SettingsActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
                }.data)
                logOutText.isEnabled = false
            } else {
                logOutText.setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.red))
                logOutText.isEnabled = true
            }
        }
    }

    // Load user's email, username, avatar
    private fun loadUserData() {
        val email: TextView = findViewById(R.id.userEmail)
        val username: TextView = findViewById(R.id.userName)
        val changeUsernameButton: ImageView = findViewById(R.id.changeUsername)
        val animLargerScale: Animation = AnimationUtils.loadAnimation(this, R.anim.imageview_click_anim)
        changeUsernameButton.visibility = VISIBLE

        lifecycleScope.launch {
            if (settingsService.isUserAuthenticated()) {
                email.text = settingsService.getUserEmail()
                username.text = settingsService.getUserUsername()
                username.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                changeUsernameButton.setOnClickListener { view ->
                    view.startAnimation(animLargerScale)
                    lifecycleScope.launch {
                        delay(300)
                        changeUsername()
                    }
                }
                val avatar: ImageView = findViewById(R.id.avatar)
                val avatarImage = settingsService.loadAvatar()
                if (avatarImage != null) {
                    checkIfUserHasAvatar = true
                    Glide.with(this@SettingsActivity)
                        .load(avatarImage)
                        .circleCrop()
                        .into(avatar)
                } else {
                    checkIfUserHasAvatar = false
                    avatar.setImageResource(R.drawable.ic_logo_user)
                }
                avatar.isClickable = true
                avatar.setOnClickListener {
                    loadingOverlay = LoadingOverlay(this@SettingsActivity)
                    loadingOverlay.show()
                    lifecycleScope.launch {
                        if (checkIfUserHasAvatar) {
                            deleteOrUpload()
                        } else {
                            showImageSourceDialog()
                        }
                    }
                    loadingOverlay.hide()
                }
            } else {
                changeUsernameButton.visibility = INVISIBLE
            }
        }
    }

    // Method allows the user to select the source of the photo
    private fun deleteOrUpload() {
        val options = arrayOf(getString(R.string.upload_avatar), getString(R.string.delete_avatar))
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.choose_action))
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> showImageSourceDialog()
                1 -> deleteAvatar()
            }
        }
        builder.show()
    }

    // Method allows the user to select the source of the photo
    private fun showImageSourceDialog() {
        val options = arrayOf(getString(R.string.choose_from_gallery), getString(R.string.choose_from_storage))
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.choose_image_source))
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openGallery()
                1 -> openFileManager()
            }
        }
        builder.show()
    }

    // Opens the gallery
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, requestCodeGallery)
    }

    // Opens the file manager
    private fun openFileManager() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, requestCodeFileManager)
    }

    // Actions after selecting a photo
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data

            val collectionImage: ImageView = findViewById(R.id.avatar)
            collectionImage.setImageURI(selectedImageUri)
            collectionImage.setPadding(0, 0, 0, 0)

            Glide.with(this@SettingsActivity)
                .load(selectedImageUri)
                .circleCrop()
                .into(collectionImage)

            if (selectedImageUri != null) {
                lifecycleScope.launch {
                    val updateJob = updateAvatar(selectedImageUri)
                    updateJob.join()
                }
                Toast.makeText(this@SettingsActivity, getString(R.string.successful_avatar_change), Toast.LENGTH_LONG).show()
            }
        }
    }

    // Deletes the user's avatar
    private fun deleteAvatar(): Job {
        return lifecycleScope.launch {
            settingsService.deleteAvatar()
            recreate()
        }
    }

    // Method for user to enter new username
    private fun changeUsername() {
        val animLargerScale: Animation = AnimationUtils.loadAnimation(this, R.anim.imageview_click_anim)
        val changeUsernameButton: ImageView = findViewById(R.id.changeUsername)
        val confirmButton: ImageView = findViewById(R.id.checkmark)
        val username: EditText = findViewById(R.id.userName)

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
        val backgroundColor = typedValue.data

        changeUsernameButton.visibility = INVISIBLE
        confirmButton.visibility = VISIBLE

        username.isFocusable = true
        username.isClickable = true
        username.isFocusableInTouchMode = true
        username.backgroundTintList = ColorStateList.valueOf(backgroundColor)

        username.setSelection(username.text.length)
        username.requestFocus()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(username, InputMethodManager.SHOW_IMPLICIT)

        confirmButton.setOnClickListener { view ->
            view.startAnimation(animLargerScale)
            loadingOverlay = LoadingOverlay(this)
            lifecycleScope.launch {
                val (result, error) = settingsService.validateUsername(username.text.toString().trim())
                loadingOverlay.show()
                delay(300)
                if (result) {
                    confirmChangeUsername()
                } else {
                    Toast.makeText(this@SettingsActivity, error, Toast.LENGTH_LONG).show()
                }
                loadingOverlay.hide()
            }
        }
    }

    // Method of updating user's avatar
    private fun updateAvatar(selectedImageUri: Uri): Job {
        return lifecycleScope.launch {
            settingsService.saveAvatar(selectedImageUri)
        }
    }

    // Method for confirming user's decision to change username
    private suspend fun confirmChangeUsername() {
        val changeUsernameButton: ImageView = findViewById(R.id.changeUsername)
        val confirmButton: ImageView = findViewById(R.id.checkmark)
        val username: EditText = findViewById(R.id.userName)

        changeUsernameButton.visibility = VISIBLE
        confirmButton.visibility = INVISIBLE

        username.isFocusable = false
        username.isClickable = false
        username.isFocusableInTouchMode = false
        username.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)

        if (settingsService.updateUsername(username.text.toString().trim())) {
            Toast.makeText(this@SettingsActivity, getString(R.string.successful_user_change), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this@SettingsActivity, getString(R.string.unsuccessful_user_change), Toast.LENGTH_LONG).show()
        }

    }

}