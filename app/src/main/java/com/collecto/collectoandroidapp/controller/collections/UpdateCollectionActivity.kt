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
import com.collecto.collectoandroidapp.service.ItemsService
import com.collecto.collectoandroidapp.service.CollectionsService
import com.collecto.collectoandroidapp.model.CustomCollectionField

class UpdateCollectionActivity : BaseActivity() {

    // Local services
    private lateinit var itemsService: ItemsService
    private lateinit var collectionService: CollectionsService

    // User selection from where to upload photos
    private val requestCodeGallery = 100
    private val requestCodeFileManager = 101

    // Variables for adding collection fields
    private var fields = mutableListOf<View>()

    // Functional elements of the page
    private lateinit var titleLengthText: TextView
    private lateinit var collectionImage: ImageView
    private lateinit var collectionNameInput: EditText
    private lateinit var descriptionLengthText: TextView
    private lateinit var collectionDescriptionInput: EditText

    // Overlay
    private lateinit var loadingOverlay: LoadingOverlay

    // Image uri
    private var selectedImageUri: Uri? = null

    // Local collection object
    private var collection: Collection? = null

    // Basic logic of the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemsService = ItemsService(applicationContext)
        collectionService = CollectionsService(applicationContext)
        setContentView(R.layout.activity_update_collection)

        // Get the collection to be modified
        collection = intent.getParcelableExtra("collection")

        // Animations
        val animLargerScale : Animation = AnimationUtils.loadAnimation(this, R.anim.imageview_click_anim)
        loadingOverlay = LoadingOverlay(this)

        // Collection title input
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
                        this@UpdateCollectionActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
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
                        this@UpdateCollectionActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
                    }.data)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Fill original data
        collection?.let {
            collectionNameInput.setText(collection!!.name)
            collectionDescriptionInput.setText(collection!!.description)
            fillFieldsContainer(it)
        }

        collectionImage = findViewById(R.id.collection_image)
        lifecycleScope.launch {
            val image = collection?.let { collectionService.loadImage(it) }
            if (image != null) {
                collectionImage.setPadding(0, 0, 0, 0)
                Glide.with(this@UpdateCollectionActivity)
                    .load(image)
                    .circleCrop()
                    .into(collectionImage)
            } else {
                collectionImage.setImageResource(R.drawable.ic_logo_photo)
            }
        }

        // Back button functionality
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener { view ->
            view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                delay(300)
                onBackPressed()
            }
        }

        // Save button functionality
        val saveButton: ImageButton = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            lifecycleScope.launch {
                loadingOverlay.show()
                updateCollection()
                loadingOverlay.hide()
            }
        }

        // Collection image adding process
        val collectionImage: ImageView = findViewById(R.id.collection_image)
        collectionImage.setOnClickListener {
            lifecycleScope.launch {
                loadingOverlay.show()
                showImageSourceDialog()
                loadingOverlay.hide()
            }
        }

        // Add new field functionality
        val addNewFieldButton = findViewById<ImageView>(R.id.add_new_field_button)
        addNewFieldButton.setOnClickListener {
            lifecycleScope.launch {
                addNewField()
            }
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

    // Filling in the collection fields
    private fun fillFieldsContainer(collection: Collection) {
        val fieldsContainer = findViewById<LinearLayout>(R.id.fields_container)
        fieldsContainer.removeAllViews()

        val sortedFields = collection.customFields?.sortedBy { it.order } ?: emptyList()
        fields.clear()

        sortedFields.forEach { field ->
            val fieldView = layoutInflater.inflate(R.layout.layout_item_field, fieldsContainer, false)

            val deleteFieldButton = fieldView.findViewById<ImageView>(R.id.delete_field_button)
            val moveUpButton = fieldView.findViewById<ImageView>(R.id.move_up)
            val moveDownButton = fieldView.findViewById<ImageView>(R.id.move_down)

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
                            this@UpdateCollectionActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
                        }.data)
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            fieldName.text = field.name

            deleteFieldButton.setOnClickListener {
                removeField(field)
            }

            moveUpButton.setOnClickListener {
                moveFieldUp(field)
            }

            moveDownButton.setOnClickListener {
                moveFieldDown(field)
            }

            fields.add(fieldView)
            fieldsContainer.addView(fieldView)
        }

        updateButtonVisibility()
    }

    // Move field up
    private fun moveFieldUp(field: CustomCollectionField) {
        val fieldsList = collection?.customFields?.toMutableList() ?: return
        val index = fieldsList.indexOf(field)

        if (index > 0) {
            saveCurrentFieldData()

            fieldsList[index].order = fieldsList[index - 1].order.also {
                fieldsList[index - 1].order = fieldsList[index].order
            }
            fieldsList.add(index - 1, fieldsList.removeAt(index))
            collection = collection?.copy(customFields = fieldsList)

            fillFieldsContainer(collection!!)
        }
    }

    // Move field down
    private fun moveFieldDown(field: CustomCollectionField) {
        val fieldsList = collection?.customFields?.toMutableList() ?: return
        val index = fieldsList.indexOf(field)

        if (index < fieldsList.size - 1) {
            saveCurrentFieldData()

            fieldsList[index].order = fieldsList[index + 1].order.also {
                fieldsList[index + 1].order = fieldsList[index].order
            }
            fieldsList.add(index + 1, fieldsList.removeAt(index))
            collection = collection?.copy(customFields = fieldsList)

            fillFieldsContainer(collection!!)
        }
    }

    // Save field before change
    private fun saveCurrentFieldData() {
        fields.forEachIndexed { index, fieldView ->
            val fieldNameEditText: EditText = fieldView.findViewById(R.id.record_input_text)
            val updatedName = fieldNameEditText.text.toString().trim()

            if (updatedName.isNotEmpty()) {
                collection?.customFields?.getOrNull(index)?.let {
                    customField -> customField.name = updatedName
                }
            }
        }
    }

    // Add new field functionality
    private fun addNewField() {
        val fields = collection?.customFields?.toMutableList() ?: mutableListOf()
        val newFieldId = (collection?.maxFieldId?.toInt() ?: 0) + 1

        val newField = CustomCollectionField(
            id = newFieldId,
            order = fields.size + 1,
            name = ""
        )

        collection = collection?.copy(
            maxFieldId = newFieldId.toString(),
            customFields = fields.apply { add(newField) }
        )

        collection?.let { fillFieldsContainer(it) }
    }

    // Remove field field functionality
    private fun removeField(field: CustomCollectionField) {
        val fields = collection?.customFields?.toMutableList() ?: return

        fields.remove(field)

        fields.forEachIndexed { index, customField ->
            customField.order = index + 1
        }

        collection = collection!!.copy(customFields = fields)

        fillFieldsContainer(collection!!)
    }

    // The method controls the hiding of unnecessary buttons
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

    // Updates the number of fields
    private fun updateFieldsAmountText(fieldsAmountText: TextView) {
        val currentAmount = fields.size
        val maxFields = 20

        fieldsAmountText.text = "($currentAmount/$maxFields)"

        if (currentAmount > maxFields) {
            fieldsAmountText.setTextColor(resources.getColor(R.color.red))
        } else {
            val hintColor = TypedValue().apply {
                this@UpdateCollectionActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
            }.data
            fieldsAmountText.setTextColor(hintColor)
        }
    }

    // Modifies the collection
    private suspend fun updateCollection(): Boolean {
        val name = collectionNameInput.text.toString().trim()
        val description = collectionDescriptionInput.text.toString().trim()
        val userID = collection?.userId ?: ""
        val customFields = getFields()
        val maxFieldId = collection?.maxFieldId ?: customFields.size.toString()

        val newCollection = Collection(
            id = collection?.id ?: 0,
            name = name,
            description = description,
            userId = userID,
            customFields = customFields,
            imagePath = collection?.imagePath,
            createdAt = collection?.createdAt,
            updatedAt = "",
            maxFieldId = maxFieldId
        )

        val (result, error) = collectionService.modifyCollection(newCollection, selectedImageUri)
        if (result) {
            delay(500)
            Toast.makeText(this@UpdateCollectionActivity, getString(R.string.successful_modify_collection), Toast.LENGTH_LONG).show()
            val intent = Intent(this@UpdateCollectionActivity, ShowCollectionsActivity::class.java)
            startActivity(intent)
            finish()
        } else if (error == "Wrong length") {
            Toast.makeText(this@UpdateCollectionActivity, getString(R.string.wrong_name_length), Toast.LENGTH_LONG).show()
        } else if (error == "Wrong amount of fields"){
            Toast.makeText(this@UpdateCollectionActivity, getString(R.string.wrong_amount_of_fields), Toast.LENGTH_LONG).show()
        } else if (error == "Identical fields"){
            Toast.makeText(this@UpdateCollectionActivity, getString(R.string.identical_fields), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this@UpdateCollectionActivity, getString(R.string.unsuccessful_modify_collection), Toast.LENGTH_LONG).show()
        }

        return result
    }

    // Returns data list of CollectionFields
    private fun getFields(): List<CustomCollectionField> {
        val customFields = mutableListOf<CustomCollectionField>()

        for ((index, fieldView) in fields.withIndex()) {
            val fieldNameEditText: EditText = fieldView.findViewById(R.id.record_input_text)
            val fieldName = fieldNameEditText.text.toString().trim()

            if (fieldName.isNotEmpty()) {
                customFields.add(
                    CustomCollectionField(
                        id = (collection?.customFields?.getOrNull(index)?.id) ?: (index + 1),
                        order = index + 1,
                        name = fieldName
                    )
                )
            }
        }

        return customFields
    }

}