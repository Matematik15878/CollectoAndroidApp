package com.collecto.collectoandroidapp.controller.collections

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.text.Editable
import android.content.Intent
import android.app.AlertDialog
import android.widget.TextView
import android.util.TypedValue
import android.widget.EditText
import com.bumptech.glide.Glide
import kotlinx.coroutines.delay
import android.text.TextWatcher
import android.widget.ImageView
import kotlinx.coroutines.launch
import android.widget.ImageButton
import android.widget.LinearLayout
import android.provider.MediaStore
import android.view.animation.Animation
import androidx.lifecycle.lifecycleScope
import com.collecto.collectoandroidapp.R
import android.view.animation.AnimationUtils
import com.collecto.collectoandroidapp.BaseActivity
import com.collecto.collectoandroidapp.model.Collection
import com.collecto.collectoandroidapp.view.LoadingOverlay
import com.collecto.collectoandroidapp.service.CollectionsService
import com.collecto.collectoandroidapp.model.CustomCollectionField

class AddCollectionActivity : BaseActivity() {

    // Local collections service
    private lateinit var collectionService: CollectionsService

    // User selection from where to upload photos
    private val REQUEST_CODE_GALLERY = 100
    private val REQUEST_CODE_FILE_MANAGER = 101

    // Variables for adding collection fields
    private var amountOfFields: Int = 0
    private var fields = mutableListOf<View>()
    private lateinit var addFieldButton: ImageView
    private lateinit var recordsContainer: LinearLayout

    // Functional elements of the page
    private lateinit var saveButton: ImageView
    private lateinit var titleLengthText: TextView
    private lateinit var collectionNameInput: EditText
    private lateinit var descriptionLengthText: TextView
    private lateinit var collectionDescriptionInput: EditText

    // Overlay
    private lateinit var loadingOverlay: LoadingOverlay

    // Image uri
    private var selectedImageUri: Uri? = null

    // Basic logic of the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionService = CollectionsService(applicationContext)
        setContentView(R.layout.activity_add_collection)

        // Collection name input
        titleLengthText = findViewById(R.id.title_length)
        collectionNameInput = findViewById(R.id.collection_name_input)
        collectionNameInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                titleLengthText.text = "$length / 50"

                if (length == 0 || length > 50) {
                    titleLengthText.setTextColor(resources.getColor(R.color.red))
                } else {
                    titleLengthText.setTextColor(TypedValue().apply {
                        this@AddCollectionActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
                    }.data)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Collection description input
        descriptionLengthText = findViewById(R.id.description_length)
        collectionDescriptionInput = findViewById(R.id.collection_description_input)
        collectionDescriptionInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                descriptionLengthText.text = "$length / 500"

                if (length > 500) {
                    descriptionLengthText.setTextColor(resources.getColor(R.color.red))
                } else {
                    descriptionLengthText.setTextColor(TypedValue().apply {
                        this@AddCollectionActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
                    }.data)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Adding new field of the collection
        recordsContainer = findViewById(R.id.fields_container)
        addFieldButton = findViewById(R.id.add_new_field_button)
        addFieldButton.setOnClickListener {
            addNewField()
        }

        // Back button functionality
        val animLargerScale : Animation = AnimationUtils.loadAnimation(this, R.anim.imageview_click_anim)
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                delay(300)
                onBackPressed()
            }
        }

        // Save collection button functionality
        saveButton = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            loadingOverlay = LoadingOverlay(this)
            lifecycleScope.launch {
                loadingOverlay.show()
                if (saveCollection()) {
                    finish()
                } else {
                    loadingOverlay.hide()
                }
            }
        }

        // Collection image adding process
        val collectionImage: ImageView = findViewById(R.id.collection_image)
        collectionImage.setOnClickListener {
            showImageSourceDialog()
        }

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
        startActivityForResult(intent, REQUEST_CODE_GALLERY)
    }

    // Opens the file manager
    private fun openFileManager() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_FILE_MANAGER)
    }

    // Actions after selecting a photo
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data

            val collectionImage: ImageView = findViewById(R.id.collection_image)
            collectionImage.setImageURI(selectedImageUri)
            collectionImage.setPadding(0, 0, 0, 0)

            Glide.with(this)
                .load(selectedImageUri)
                .circleCrop()
                .into(collectionImage)
        }
    }

    // Method for adding a new field in the collection
    private fun addNewField() {
        val fieldView = layoutInflater.inflate(R.layout.layout_item_field, null)
        recordsContainer.addView(fieldView)
        fieldView.requestFocus()
        fields.add(fieldView)
        amountOfFields++

        val moveUpButton = fieldView.findViewById<ImageView>(R.id.move_up)
        val moveDownButton = fieldView.findViewById<ImageView>(R.id.move_down)
        val deleteButton = fieldView.findViewById<ImageView>(R.id.delete_field_button)
        val fieldsAmountText = findViewById<TextView>(R.id.collection_fields_amount)
        updateFieldsAmountText(fieldsAmountText)

        // Moving field up
        moveUpButton.setOnClickListener {
            fieldView.clearFocus()
            val position = fields.indexOf(fieldView)
            if (position > 0) {
                swapItems(position, position - 1)
            }
        }

        // Moving field up
        moveDownButton.setOnClickListener {
            fieldView.clearFocus()
            val position = fields.indexOf(fieldView)
            if (position < fields.size - 1) {
                swapItems(position, position + 1)
            }
        }

        // Delete field
        deleteButton.setOnClickListener {
            fields.removeAt(fields.indexOf(fieldView))
            recordsContainer.removeView(fieldView)
            amountOfFields--
            updateButtonVisibility()
            updateFieldsAmountText(fieldsAmountText)
        }

        // Length of name (max = 30)
        val fieldName: TextView = fieldView.findViewById(R.id.record_input_text)
        val fieldNameLength: TextView = fieldView.findViewById(R.id.field_length)
        fieldName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                fieldNameLength.text = "$length / 30"

                if (length > 30) {
                    fieldNameLength.setTextColor(resources.getColor(R.color.red))
                } else {
                    fieldNameLength.setTextColor(TypedValue().apply {
                        this@AddCollectionActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
                    }.data)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Update the visibility of moveUp and moveDown buttons
        updateButtonVisibility()
        updateFieldsAmountText(fieldsAmountText)
    }

    // Method for swapping 2 fields
    private fun swapItems(position1: Int, position2: Int) {
        val temp = fields[position1]
        fields[position1] = fields[position2]
        fields[position2] = temp

        val fieldsAmountText = findViewById<TextView>(R.id.collection_fields_amount)
        updateFieldsAmountText(fieldsAmountText)

        recordsContainer.removeAllViews()
        for (record in fields) {
            recordsContainer.addView(record)
        }

        updateButtonVisibility()
        updateFieldsAmountText(fieldsAmountText)
    }

    // Method to update button visibility based on field position
    private fun updateButtonVisibility() {
        for (i in fields.indices) {
            val recordView = fields[i]
            val moveUpButton = recordView.findViewById<ImageView>(R.id.move_up)
            val moveDownButton = recordView.findViewById<ImageView>(R.id.move_down)
            val fieldsAmountText = findViewById<TextView>(R.id.collection_fields_amount)
            updateFieldsAmountText(fieldsAmountText)

            if (i == 0) {
                moveUpButton.visibility = View.GONE
            } else {
                moveUpButton.visibility = View.VISIBLE
            }

            if (i == fields.size - 1) {
                moveDownButton.visibility = View.GONE
            } else {
                moveDownButton.visibility = View.VISIBLE
            }
        }
    }

    // Method to update amount of added fields
    private fun updateFieldsAmountText(fieldsAmountText: TextView) {
        fieldsAmountText.text = "($amountOfFields/20)"

        if (amountOfFields > 20) {
            fieldsAmountText.setTextColor(resources.getColor(R.color.red))
        } else {
            val hintColor = obtainStyledAttributes(intArrayOf(android.R.attr.textColorHint))
                .getColor(0, resources.getColor(android.R.color.darker_gray))
            fieldsAmountText.setTextColor(hintColor)
        }
    }

    // Saves the collection
    private suspend fun saveCollection(): Boolean {
        val name = collectionNameInput.text.toString().trim()
        val description = collectionDescriptionInput.text.toString().trim()
        val userID = collectionService.getUserID().toString().trim()
        val fields = getFields()
        val imagePath = selectedImageUri

        val newCollection = Collection(
            0,
            name,
            description,
            userID,
            fields,
            "",
            "",
            "",
            fields.size.toString()
        )

        val (result, error) = collectionService.saveCollection(newCollection, imagePath)
        if (result) {
            Toast.makeText(this@AddCollectionActivity, getString(R.string.successful_add_collection), Toast.LENGTH_LONG).show()
            val intent = Intent(this@AddCollectionActivity, ShowCollectionsActivity::class.java)
            startActivity(intent)
            finish()
        } else if (error == "Wrong length") {
            Toast.makeText(this@AddCollectionActivity, getString(R.string.wrong_name_length), Toast.LENGTH_LONG).show()
        } else if (error == "Wrong amount of fields"){
            Toast.makeText(this@AddCollectionActivity, getString(R.string.wrong_amount_of_fields), Toast.LENGTH_LONG).show()
        } else if (error == "Identical fields"){
            Toast.makeText(this@AddCollectionActivity, getString(R.string.identical_fields), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this@AddCollectionActivity, getString(R.string.unsuccessful_add_collection), Toast.LENGTH_LONG).show()
        }

        return result

    }

    // Returns data list of CollectionFields
    private fun getFields(): List<CustomCollectionField> {
        val customFields = mutableListOf<CustomCollectionField>()
        var amountOfEmptyFields = 0

        for ((index, fieldView) in fields.withIndex()) {
            val fieldNameTextView: TextView = fieldView.findViewById(R.id.record_input_text)
            if (fieldNameTextView.text.toString().trim() != "") {
                val fieldIndex = index - amountOfEmptyFields + 1
                customFields.add(CustomCollectionField(fieldIndex, fieldIndex, fieldNameTextView.text.toString().trim()))
            } else {
                ++amountOfEmptyFields
            }
        }

        return customFields
    }

}