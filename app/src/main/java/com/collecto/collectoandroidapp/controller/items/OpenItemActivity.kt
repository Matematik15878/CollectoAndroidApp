package com.collecto.collectoandroidapp.controller.items

import android.os.Bundle
import android.content.Intent
import android.widget.Spinner
import android.widget.TextView
import com.bumptech.glide.Glide
import kotlinx.coroutines.delay
import android.widget.ImageView
import android.graphics.Typeface
import kotlinx.coroutines.launch
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ArrayAdapter
import android.view.animation.Animation
import androidx.lifecycle.lifecycleScope
import com.collecto.collectoandroidapp.R
import android.view.animation.AnimationUtils
import com.collecto.collectoandroidapp.BaseActivity
import com.collecto.collectoandroidapp.model.Collection
import com.collecto.collectoandroidapp.service.ItemsService
import com.collecto.collectoandroidapp.model.CollectionItem
import com.collecto.collectoandroidapp.service.CollectionsService

class OpenItemActivity : BaseActivity() {

    // Local services
    private lateinit var itemsService: ItemsService
    private lateinit var collectionService: CollectionsService

    // Collection and items
    private lateinit var item: CollectionItem
    private lateinit var collection: Collection

    // Local functional elements
    private lateinit var itemImage: ImageView
    private lateinit var backButton: ImageView
    private lateinit var fieldsContainer: LinearLayout
    private lateinit var updateButton: ImageView
    private lateinit var itemName: TextView
    private lateinit var itemDescription: TextView

    // Basic logic of the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemsService = ItemsService(applicationContext)
        collectionService = CollectionsService(applicationContext)
        setContentView(R.layout.activity_show_item)

        // Collection and item variables
        item = intent.getParcelableExtra("item")!!
        collection = intent.getParcelableExtra("collection")!!

        // Local functional elements
        itemImage = findViewById(R.id.item_image)
        backButton = findViewById(R.id.back_button)
        fieldsContainer = findViewById(R.id.fields_container)
        updateButton = findViewById(R.id.update_button)
        itemDescription = findViewById(R.id.item_description)
        itemName = findViewById(R.id.item_name)

        // Animations
        val animLargerScale : Animation = AnimationUtils.loadAnimation(this, R.anim.imageview_click_anim)

        // Filling fields with item data
        lifecycleScope.launch {
            delay(500)
            itemName.text = item.name
            if(item.description != "") {
                itemDescription.text = item.description
                itemDescription.setTypeface(null, Typeface.NORMAL)
            }
            showFields(item)
            val image = itemsService.loadImage(item)
            if (image != null) {
                itemImage.setPadding(0, 0, 0, 0)
                Glide.with(this@OpenItemActivity)
                    .load(image)
                    .circleCrop()
                    .into(itemImage)
            } else {
                itemImage.setImageResource(R.drawable.ic_logo_photo)
            }
        }

        // Display collection name
        val collectionSpinner: Spinner = findViewById(R.id.collection_spinner)
        val adapter = ArrayAdapter(this@OpenItemActivity, R.layout.layout_spinner_item, listOf(collection.name))
        collectionSpinner.adapter = adapter

        // Back button functionality
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
                view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                delay(300)
                onBackPressed()
            }
        }

        // Edit item functionality
        updateButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            val intent = Intent(this, UpdateItemActivity::class.java)
            intent.putExtra("collection", collection)
            intent.putExtra("item", item)
            startActivity(intent)
            finish()
        }

    }

    // Shows the full collections list
    private fun showFields(item: CollectionItem) {
        fieldsContainer.removeAllViews()

        val sortedCustomFields = collection.customFields?.sortedBy { it.order } ?: listOf()

        sortedCustomFields.forEach { field ->
            val content = item.fieldContents?.firstOrNull { it.fieldId == field.id }
            val fieldName = field.name
            val itemView = layoutInflater.inflate(R.layout.layout_item_fields, fieldsContainer, false)

            val titleTextView = itemView.findViewById<TextView>(R.id.name_of_field)
            val fieldContentTextView = itemView.findViewById<TextView>(R.id.record_text)

            titleTextView.text = fieldName
            fieldContentTextView.text = content?.fieldContent ?: ""

            fieldsContainer.addView(itemView)
        }
    }

}