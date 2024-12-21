package com.collecto.collectoandroidapp.controller.collections

import android.os.Bundle
import android.view.View
import android.text.Editable
import android.content.Intent
import android.graphics.Typeface
import android.widget.Spinner
import kotlinx.coroutines.Job
import android.widget.TextView
import com.bumptech.glide.Glide
import kotlinx.coroutines.delay
import android.widget.ImageView
import java.time.OffsetDateTime
import android.text.TextWatcher
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
import com.collecto.collectoandroidapp.view.LoadingOverlay
import com.collecto.collectoandroidapp.service.ItemsService
import com.collecto.collectoandroidapp.model.CollectionItem
import com.collecto.collectoandroidapp.service.CollectionsService
import com.collecto.collectoandroidapp.controller.items.AddItemActivity
import com.collecto.collectoandroidapp.controller.items.OpenItemActivity
import com.collecto.collectoandroidapp.controller.items.UpdateItemActivity

class OpenCollectionActivity : BaseActivity() {

    data class SpinnerItem(
        val id: Int,
        val name: String
    ) {
        override fun toString(): String {
            return name
        }
    }

    private var sortState: Int = 0

    // Local services
    private lateinit var itemsService: ItemsService
    private lateinit var collectionService: CollectionsService

    // Collection and items
    private lateinit var collection: Collection
    private lateinit var collectionItems: List<CollectionItem>

    // Local functional elements
    private lateinit var collectionImage: ImageView
    private lateinit var backButton: ImageView
    private lateinit var addItemButton: ImageView
    private lateinit var itemsContainer: LinearLayout
    private lateinit var updateButton: ImageView
    private lateinit var collectionName: TextView
    private lateinit var amountOfItems: TextView
    private lateinit var collectionDescription: TextView
    private lateinit var searchEditText: TextView

    // Overlay
    private lateinit var loadingOverlay: LoadingOverlay

    // Basic logic of the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemsService = ItemsService(applicationContext)
        collectionService = CollectionsService(applicationContext)
        setContentView(R.layout.activity_show_collection)

        // Collection variable
        collection = intent.getParcelableExtra("collection")!!

        // Local functional elements
        addItemButton = findViewById(R.id.add_new_item_button)
        collectionImage = findViewById(R.id.collection_image)
        backButton = findViewById(R.id.back_button)
        amountOfItems = findViewById(R.id.amount_of_items)
        itemsContainer = findViewById(R.id.items_container)
        updateButton = findViewById(R.id.update)
        collectionDescription = findViewById(R.id.collection_description)
        collectionName = findViewById(R.id.collection_name)

        // Animations
        val animLargerScale : Animation = AnimationUtils.loadAnimation(this, R.anim.imageview_click_anim)

        // Filling fields with collection data
        collection.let {
            lifecycleScope.launch {
                collectionItems = itemsService.getItems(it.id)
                delay(200)
                fillSpinnerWithCustomFields()
                collectionName.text = it.name
                if(it.description != "") {
                    collectionDescription.text = it.description
                    collectionDescription.setTypeface(null, Typeface.NORMAL)
                }
                val image = collectionService.loadImage(collection)
                if (image != null) {
                    collectionImage.setPadding(0, 0, 0, 0)
                    Glide.with(this@OpenCollectionActivity)
                        .load(image)
                        .circleCrop()
                        .into(collectionImage)
                } else {
                    collectionImage.setImageResource(R.drawable.ic_logo_photo)
                }
                showItems(collectionItems)
                amountOfItems.text = collectionItems.size.toString()
            }

        }

        // Back button functionality
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                delay(300)
                val intent = Intent(this@OpenCollectionActivity, ShowCollectionsActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        // Edit collection functionality
        updateButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            val intent = Intent(this, UpdateCollectionActivity::class.java)
            intent.putExtra("collection", collection)
            startActivity(intent)
            finish()
        }

        // Add item functionality
        addItemButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                delay(300)
                val intent = Intent(this@OpenCollectionActivity, AddItemActivity::class.java)
                intent.putExtra("collection", collection)
                startActivity(intent)
                finish()
            }
        }

        // Element search functionality
        searchEditText = findViewById(R.id.search_input)
        searchEditText.addTextChangedListener(object : TextWatcher {
            private var searchJob: Job? = null

            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(100)
                    searchItemsByField(s.toString())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Element sorting functionality
        val sortButton: ImageButton = findViewById(R.id.order_button_up_down)
        sortButton.setOnClickListener {
            sortState = when (sortState) {
                0 -> 2
                2 -> 1
                else -> 0
            }

            updateSortIcon(sortButton, sortState)
            lifecycleScope.launch {
                sortItemsByField()
            }
        }



    }

    // Return to the main page
    override fun onBackPressed() {
        lifecycleScope.launch {
            delay(300)
            val intent = Intent(this@OpenCollectionActivity, ShowCollectionsActivity::class.java)
            startActivity(intent)
            finish()
        }

        super.onBackPressed()
    }

    // Shows the full collections list
    private suspend fun showItems(items: List<CollectionItem>) {
        val itemsContainer = findViewById<LinearLayout>(R.id.items_container)

        itemsContainer.removeAllViews()

        items.forEach { item ->
            val itemView = layoutInflater.inflate(R.layout.layout_items, itemsContainer, false)
            val icon = itemView.findViewById<ImageView>(R.id.item_icon)
            val titleTextView = itemView.findViewById<TextView>(R.id.record_input_text)
            val actionsButton = itemView.findViewById<ImageView>(R.id.actions_button)

            titleTextView.text = item.name

            val collectionIcon = itemsService.loadIcon(item)
            if (collectionIcon != null) {
                Glide.with(this@OpenCollectionActivity)
                    .load(collectionIcon)
                    .circleCrop()
                    .into(icon)
            } else {
                icon.setImageResource(R.drawable.ic_logo_photo)
            }

            itemView.setOnClickListener {
                val intent = Intent(this@OpenCollectionActivity, OpenItemActivity::class.java)
                intent.putExtra("collection", collection)
                intent.putExtra("item", item)
                startActivity(intent)
            }

            actionsButton.setOnClickListener { view ->
                showPopupMenu(view, item)
            }

            itemsContainer.addView(itemView)
        }
    }

    // Shows the popup menu
    private fun showPopupMenu(view: View, item: CollectionItem) {
        val popupMenu = androidx.appcompat.widget.PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.item_actions_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            loadingOverlay = LoadingOverlay(this)
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    lifecycleScope.launch {
                        loadingOverlay.show()
                        updateItem(item)
                        loadingOverlay.hide()
                    }
                    true
                }
                R.id.action_delete -> {
                    lifecycleScope.launch {
                        loadingOverlay.show()
                        deleteItem(item)
                        loadingOverlay.hide()
                    }
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    // Method for deleting the selected collection
    private fun deleteItem(item: CollectionItem) {
        val builder = android.app.AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.delete_text))
        builder.setMessage(getString(R.string.delete_question))

        builder.setPositiveButton(getString(R.string.delete)) { dialog, which ->
            lifecycleScope.launch {
                itemsService.deleteItem(item)
                recreate()
            }
        }

        builder.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
        }

        builder.show()
    }

    // Method for deleting the selected collection
    private fun updateItem(item: CollectionItem) {
        val intent = Intent(this, UpdateItemActivity::class.java)
        intent.putExtra("collection", collection)
        intent.putExtra("item", item)
        startActivity(intent)
    }

    // Spinner Filling Method
    private fun fillSpinnerWithCustomFields() {
        val customFields = collection.customFields
        val spinnerItems = customFields?.map { SpinnerItem(it.id, it.name) }?.toMutableList() ?: mutableListOf()

        spinnerItems.add(0, SpinnerItem(-1, "name"))

        val spinnerAdapter = ArrayAdapter(
            this,
            R.layout.layout_spinner_item,
            spinnerItems
        )

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val fieldSpinner: Spinner = findViewById(R.id.field_spinner)
        fieldSpinner.adapter = spinnerAdapter
    }

    // Search for elements by selected field
    private suspend fun searchItemsByField(query: String) {
        val fieldSpinner: Spinner = findViewById(R.id.field_spinner)
        val selectedItem = fieldSpinner.selectedItem as? SpinnerItem
        collectionItems = if (selectedItem == null || selectedItem.id == -1) {
            itemsService.getItems(collection.id)
                .filter { it.name.contains(query, ignoreCase = true) }
        } else {
            itemsService.getItems(collection.id)
                .filter { item ->
                    item.fieldContents?.any {
                        it.fieldId == selectedItem.id && it.fieldContent.contains(query, ignoreCase = true)
                    } == true
                }
        }

        showItems(collectionItems)
    }

    // Sort items by selected field
    private suspend fun sortItemsByField() {
        val fieldSpinner: Spinner = findViewById(R.id.field_spinner)
        val selectedFieldIndex = fieldSpinner.selectedItemPosition
        val selectedFieldId = collection.customFields?.getOrNull(selectedFieldIndex - 1)?.id

        if (selectedFieldId == null) {
            collectionItems = when (sortState) {
                1 -> collectionItems.sortedByDescending { it.name }
                2 -> collectionItems.sortedBy { it.name }
                else -> collectionItems.sortedBy { parseDate(it.updatedAt) }
            }
        } else {
            val fieldContents = collectionItems.mapNotNull { item ->
                item.fieldContents?.firstOrNull { it.fieldId == selectedFieldId }?.fieldContent
            }

            val isNumeric = fieldContents.all { it.matches("\\d+".toRegex()) }

            collectionItems = if (isNumeric) {
                when (sortState) {
                    1 -> collectionItems.sortedByDescending { item -> item.fieldContents?.firstOrNull { it.fieldId == selectedFieldId }?.fieldContent?.toIntOrNull() ?: Int.MAX_VALUE }
                    2 -> collectionItems.sortedBy { item -> item.fieldContents?.firstOrNull { it.fieldId == selectedFieldId }?.fieldContent?.toIntOrNull() ?: Int.MAX_VALUE }
                    else -> collectionItems.sortedBy { parseDate(it.updatedAt) }
                }
            } else {
                when (sortState) {
                    1 -> collectionItems.sortedByDescending { item -> item.fieldContents?.firstOrNull { it.fieldId == selectedFieldId }?.fieldContent ?: "" }
                    2 -> collectionItems.sortedBy { item -> item.fieldContents?.firstOrNull { it.fieldId == selectedFieldId }?.fieldContent ?: "" }
                    else -> collectionItems.sortedBy { parseDate(it.updatedAt) }
                }
            }
        }

        showItems(collectionItems)
    }

    // Date parsing method
    private fun parseDate(dateString: String?): Long {
        return try {
            OffsetDateTime.parse(dateString).toEpochSecond()
        } catch (e: Exception) {
            Long.MIN_VALUE
        }
    }

    // Update icon when sorting
    private fun updateSortIcon(sortButton: ImageButton, sortState: Int) {
        when (sortState) {
            0 -> sortButton.setImageResource(R.drawable.ic_button_sort)
            1 -> sortButton.setImageResource(R.drawable.ic_button_sort_desc)
            2 -> sortButton.setImageResource(R.drawable.ic_button_sort_asc)
        }
    }

}