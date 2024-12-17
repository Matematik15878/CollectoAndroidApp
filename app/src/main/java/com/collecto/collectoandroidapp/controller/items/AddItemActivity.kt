package com.collecto.collectoandroidapp.controller.items

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.text.Editable
import android.widget.Spinner
import android.content.Intent
import android.util.TypedValue
import android.widget.EditText
import android.app.AlertDialog
import android.widget.TextView
import android.text.TextWatcher
import android.widget.ImageView
import kotlinx.coroutines.delay
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.provider.MediaStore
import android.widget.LinearLayout
import android.view.animation.Animation
import androidx.lifecycle.lifecycleScope
import com.collecto.collectoandroidapp.R
import android.view.animation.AnimationUtils
import com.collecto.collectoandroidapp.BaseActivity
import com.collecto.collectoandroidapp.model.Collection
import com.collecto.collectoandroidapp.service.ItemsService
import com.collecto.collectoandroidapp.model.CollectionItem
import com.collecto.collectoandroidapp.model.CollectionItemFields
import com.collecto.collectoandroidapp.service.CollectionsService
import com.collecto.collectoandroidapp.model.CustomCollectionField
import com.collecto.collectoandroidapp.controller.collections.OpenCollectionActivity
import com.collecto.collectoandroidapp.view.LoadingOverlay

class AddItemActivity : BaseActivity() {

    // Local services
    private lateinit var itemsService: ItemsService
    private lateinit var collectionService: CollectionsService

    // User selection from where to upload photos
    private val REQUEST_CODE_GALLERY = 100
    private val REQUEST_CODE_FILE_MANAGER = 101

    // Collection Item Fields
    private lateinit var recordsContainer: LinearLayout

    // Functional elements of the page
    private lateinit var itemImage: ImageView
    private lateinit var backButton: ImageView
    private lateinit var saveButton: ImageView
    private lateinit var collectionSpinner: Spinner
    private lateinit var itemNameLength: TextView
    private lateinit var itemNameInput: EditText
    private lateinit var descriptionLengthText: TextView
    private lateinit var itemDescriptionInput: EditText

    // User selected collection
    private var selectedCollection: Collection? = null

    // Image uri
    private var selectedImageUri: Uri? = null

    // Overlay
    private lateinit var loadingOverlay: LoadingOverlay

    // Basic logic of the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionService = CollectionsService(applicationContext)
        itemsService = ItemsService(applicationContext)
        setContentView(R.layout.activity_add_item)

        // User selected collection
        selectedCollection = intent.getParcelableExtra("collection")

        // Overlay
        loadingOverlay = LoadingOverlay(this)

        // Initialization of fields
        recordsContainer = findViewById(R.id.fields_container)
        itemNameLength = findViewById(R.id.title_length)
        itemNameInput = findViewById(R.id.item_name_input)
        descriptionLengthText = findViewById(R.id.description_length)
        itemDescriptionInput = findViewById(R.id.item_description_input)
        saveButton = findViewById(R.id.save_button)

        // Spinner collection
        collectionSpinner = findViewById(R.id.language_spinner)
        fillSpinner()

        // Loading collection fields
        selectedCollection?.let {
            lifecycleScope.launch {
                loadCustomFields(it.custom_fields)
            }
        }

        // Title character counter
        itemNameInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                itemNameLength.text = "$length / 50"

                if (length == 0 || length > 50) {
                    itemNameLength.setTextColor(resources.getColor(R.color.red))
                } else {
                    itemNameLength.setTextColor(TypedValue().apply {
                        this@AddItemActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
                    }.data)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Description character counter
        itemDescriptionInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                descriptionLengthText.text = "$length / 500"

                if (length > 500) {
                    descriptionLengthText.setTextColor(resources.getColor(R.color.red))
                } else {
                    descriptionLengthText.setTextColor(TypedValue().apply {
                        this@AddItemActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
                    }.data)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Back button functionality
        val animLargerScale : Animation = AnimationUtils.loadAnimation(this, R.anim.imageview_click_anim)
        backButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                delay(300)
                onBackPressed()
            }
        }

        // Saving a collection
        saveButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                loadingOverlay.show()
                saveCollectionItem()
                loadingOverlay.hide()
            }
        }

        // Select a photo
        itemImage = findViewById(R.id.item_image)
        itemImage.setOnClickListener {
            showImageSourceDialog()
        }
    }

    // Fills the collection spinner
    private fun fillSpinner() {
        lifecycleScope.launch {
            try {
                val allCollections = collectionService.getAllCollections()
                val collectionNames = allCollections.map { it.name }
                val adapter = ArrayAdapter(this@AddItemActivity, R.layout.layout_spinner_item, collectionNames)
                collectionSpinner.adapter = adapter

                selectedCollection?.let {
                    val selectedName = it.name
                    val selectedPosition = collectionNames.indexOf(selectedName)
                    if (selectedPosition != -1) {
                        collectionSpinner.setSelection(selectedPosition)
                    }
                }

                collectionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val selectedName = collectionNames[position]
                        val collection = allCollections.find { it.name == selectedName }
                        collection?.let {
                            selectedCollection = it
                            recordsContainer.removeAllViews()
                            lifecycleScope.launch {
                                loadCustomFields(it.custom_fields)
                            }
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Loads collection fields
    private suspend fun loadCustomFields(customFields: List<CustomCollectionField>?) {
        // Clear the container before adding new fields
        recordsContainer.removeAllViews()
        delay(100)

        customFields?.sortedBy { it.order }?.forEach { field ->
            // Create new view for each custom field
            val fieldView = layoutInflater.inflate(R.layout.layout_fields, null)

            // Set the field name and ID
            val fieldNameTextView: TextView = fieldView.findViewById(R.id.name_of_field)
            val fieldInputEditText: EditText = fieldView.findViewById(R.id.record_input_text)
            val fieldInputLength: TextView = fieldView.findViewById(R.id.field_length)

            fieldNameTextView.text = field.name
            fieldInputEditText.tag = field.id

            fieldInputEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val length = s?.length ?: 0
                    fieldInputLength.text = "$length / 100"

                    if (length > 100) {
                        fieldInputLength.setTextColor(resources.getColor(R.color.red))
                    } else {
                        fieldInputLength.setTextColor(TypedValue().apply {
                            this@AddItemActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
                        }.data)
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            // Add the newly created view to the container
            recordsContainer.addView(fieldView)
        }
    }

    // Collection Saving Method
    private suspend fun saveCollectionItem() {
        val collectionId = selectedCollection?.id ?: return
        val userId = selectedCollection!!.user_id
        val itemName = itemNameInput.text.toString()
        val itemDescription = itemDescriptionInput.text.toString()

        val fieldContents = mutableListOf<CollectionItemFields>()

        for (i in 0 until recordsContainer.childCount) {
            val fieldView = recordsContainer.getChildAt(i)
            val fieldInputEditText: EditText = fieldView.findViewById(R.id.record_input_text)

            val fieldId = fieldInputEditText.tag as? Int ?: continue
            val fieldValue = fieldInputEditText.text.toString()

            fieldContents.add(CollectionItemFields(fieldId, fieldValue))
        }

        val collectionItem = CollectionItem(
            id = 0,
            name = itemName,
            field_contents = fieldContents,
            user_id = userId,
            collection_id = collectionId,
            created_at = "",
            updated_at = "",
            image_path = "",
            description = itemDescription
        )

        val (result, error) = itemsService.saveItem(collectionItem, selectedImageUri)
        if (result) {
            Toast.makeText(this@AddItemActivity, getString(R.string.successful_add_item), Toast.LENGTH_LONG).show()
            val intent = Intent(this@AddItemActivity, OpenCollectionActivity::class.java)
            intent.putExtra("collection", selectedCollection)
            delay(500)
            startActivity(intent)
            finish()
        } else if (error == "Wrong length") {
            Toast.makeText(this@AddItemActivity, getString(R.string.wrong_name_length), Toast.LENGTH_LONG).show()
        } else if (error == "Wrong amount of fields"){
            Toast.makeText(this@AddItemActivity, getString(R.string.wrong_amount_of_fields), Toast.LENGTH_LONG).show()
        } else if (error == "Identical fields"){
            Toast.makeText(this@AddItemActivity, getString(R.string.identical_fields), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this@AddItemActivity, getString(R.string.unsuccessful_add_item), Toast.LENGTH_LONG).show()
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

            val collectionImage: ImageView = findViewById(R.id.item_image)
            collectionImage.setImageURI(selectedImageUri)
            collectionImage.setPadding(0, 0, 0, 0)

            Glide.with(this)
                .load(selectedImageUri)
                .circleCrop()
                .into(collectionImage)
        }
    }

}