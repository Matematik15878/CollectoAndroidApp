package com.collecto.collectoandroidapp.controller.items

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.text.Editable
import android.content.Intent
import android.widget.Spinner
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
import android.widget.ArrayAdapter
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
import com.collecto.collectoandroidapp.model.CollectionItem
import com.collecto.collectoandroidapp.model.CollectionItemFields
import com.collecto.collectoandroidapp.service.CollectionsService
import com.collecto.collectoandroidapp.model.CustomCollectionField
import com.collecto.collectoandroidapp.controller.collections.OpenCollectionActivity

class UpdateItemActivity : BaseActivity() {

    // Local services
    private lateinit var itemsService: ItemsService
    private lateinit var collectionService: CollectionsService

    // User selection from where to upload photos
    private val requestCodeGallery = 100
    private val requestCodeFileManager = 101

    // Collection Item Fields
    private lateinit var recordsContainer: LinearLayout

    // Functional elements of the page
    private lateinit var titleLengthText: TextView
    private lateinit var itemImage: ImageView
    private lateinit var itemNameInput: EditText
    private lateinit var descriptionLengthText: TextView
    private lateinit var itemDescriptionInput: EditText

    // Overlay
    private lateinit var loadingOverlay: LoadingOverlay

    // Image uri
    private var selectedImageUri: Uri? = null

    // Local collection object
    private var item: CollectionItem? = null
    private var collection: Collection? = null

    // Basic logic of the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemsService = ItemsService(applicationContext)
        collectionService = CollectionsService(applicationContext)
        setContentView(R.layout.activity_update_item)

        // Get the collection to be modified
        item = intent.getParcelableExtra("item")
        collection = intent.getParcelableExtra("collection")

        // Animations
        val animLargerScale : Animation = AnimationUtils.loadAnimation(this, R.anim.imageview_click_anim)
        loadingOverlay = LoadingOverlay(this)

        recordsContainer = findViewById(R.id.fields_container)
        collection?.let {
            val collectionNames = listOf(it.name)
            val adapter = ArrayAdapter(this, R.layout.layout_spinner_item, collectionNames)
            adapter.setDropDownViewResource(R.layout.layout_spinner_item)
            val collectionSpinner: Spinner = findViewById(R.id.collection_spinner)
            collectionSpinner.adapter = adapter
        }

        // Collection title input
        titleLengthText = findViewById(R.id.title_length)
        itemNameInput = findViewById(R.id.item_name_input)
        itemNameInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                titleLengthText.text = "$length / 50"

                if (length == 0 || length > 50) {
                    titleLengthText.setTextColor(resources.getColor(R.color.red))
                } else {
                    titleLengthText.setTextColor(TypedValue().apply {
                        this@UpdateItemActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
                    }.data)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Collection description input
        descriptionLengthText = findViewById(R.id.description_length)
        itemDescriptionInput = findViewById(R.id.item_description_input)
        itemDescriptionInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                descriptionLengthText.text = "$length / 500"

                if (length > 500) {
                    descriptionLengthText.setTextColor(resources.getColor(R.color.red))
                } else {
                    descriptionLengthText.setTextColor(TypedValue().apply {
                        this@UpdateItemActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
                    }.data)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        //
        item?.let {
            lifecycleScope.launch {
                fillFieldsFromItem(it)
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
                item?.let { it1 -> updateCollectionItem(it1) }
                loadingOverlay.hide()
            }
        }

        // Collection image adding process
        itemImage.setOnClickListener {
            lifecycleScope.launch {
                loadingOverlay.show()
                showImageSourceDialog()
                loadingOverlay.hide()
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

            val itemImage: ImageView = findViewById(R.id.item_image)
            itemImage.setImageURI(selectedImageUri)
            itemImage.setPadding(0, 0, 0, 0)

            Glide.with(this)
                .load(selectedImageUri)
                .circleCrop()
                .into(itemImage)
        }
    }

    // Filling in the fields of a collection element
    private suspend fun fillFieldsFromItem(item: CollectionItem) {
        itemImage = findViewById(R.id.item_image)
        lifecycleScope.launch {
            val image = itemsService.loadIcon(item)
            if (image != null) {
                itemImage.setPadding(0, 0, 0, 0)
                Glide.with(this@UpdateItemActivity)
                    .load(image)
                    .circleCrop()
                    .into(itemImage)
            } else {
                itemImage.setImageResource(R.drawable.ic_logo_photo)
            }
        }

        itemNameInput.setText(item.name)

        itemDescriptionInput.setText(item.description)

        collection?.let { coll ->
            loadCustomFields(coll.customFields, item)
        }
    }

    // Loading field contents
    private suspend fun loadCustomFields(customFields: List<CustomCollectionField>?, item: CollectionItem) {
        recordsContainer.removeAllViews()
        delay(100)

        customFields?.sortedBy { it.order }?.forEach { field ->
            val fieldView = layoutInflater.inflate(R.layout.layout_fields, null)

            val fieldNameTextView: TextView = fieldView.findViewById(R.id.name_of_field)
            val fieldInputEditText: EditText = fieldView.findViewById(R.id.record_input_text)
            val fieldInputLength: TextView = fieldView.findViewById(R.id.field_length)

            fieldInputEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val length = s?.length ?: 0
                    fieldInputLength.text = "$length / 100"

                    if (length > 100) {
                        fieldInputLength.setTextColor(resources.getColor(R.color.red))
                    } else {
                        fieldInputLength.setTextColor(TypedValue().apply {
                            this@UpdateItemActivity.theme.resolveAttribute(android.R.attr.textColorHint, this, true)
                        }.data)
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            fieldNameTextView.text = field.name
            fieldInputEditText.tag = field.id

            val fieldValue = item.fieldContents?.find { it.fieldId == field.id }?.fieldContent
            fieldInputEditText.setText(fieldValue)

            recordsContainer.addView(fieldView)
        }
    }

    // Collection Saving Method
    private suspend fun updateCollectionItem(localItem: CollectionItem) {
        val collectionId = collection?.id ?: return
        val userId = collection!!.userId
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
            id = localItem.id,
            name = itemName,
            fieldContents = fieldContents,
            userId = userId,
            collectionId = collectionId,
            createdAt = localItem.createdAt,
            updatedAt = "",
            imagePath = localItem.imagePath,
            description = itemDescription
        )

        val (result, error) = itemsService.modifyItem(collectionItem, selectedImageUri)
        if (result) {
            Toast.makeText(this@UpdateItemActivity, getString(R.string.successful_add_item), Toast.LENGTH_LONG).show()
            val intent = Intent(this@UpdateItemActivity, OpenCollectionActivity::class.java)
            intent.putExtra("collection", collection)
            delay(500)
            startActivity(intent)
            finish()
        } else if (error == "Wrong length") {
            Toast.makeText(this@UpdateItemActivity, getString(R.string.wrong_name_length), Toast.LENGTH_LONG).show()
        } else if (error == "Wrong amount of fields"){
            Toast.makeText(this@UpdateItemActivity, getString(R.string.wrong_amount_of_fields), Toast.LENGTH_LONG).show()
        } else if (error == "Identical fields"){
            Toast.makeText(this@UpdateItemActivity, getString(R.string.identical_fields), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this@UpdateItemActivity, getString(R.string.unsuccessful_add_item), Toast.LENGTH_LONG).show()
        }

    }

}