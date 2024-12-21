package com.collecto.collectoandroidapp.controller.collections

import android.view.View
import android.os.Bundle
import android.text.Editable
import android.widget.Spinner
import kotlinx.coroutines.Job
import android.content.Intent
import android.widget.TextView
import android.widget.EditText
import kotlinx.coroutines.delay
import android.widget.ImageView
import com.bumptech.glide.Glide
import java.time.OffsetDateTime
import android.text.TextWatcher
import kotlinx.coroutines.launch
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.view.GravityCompat
import android.view.animation.Animation
import com.collecto.collectoandroidapp.R
import androidx.lifecycle.lifecycleScope
import android.view.animation.AnimationUtils
import androidx.drawerlayout.widget.DrawerLayout
import com.collecto.collectoandroidapp.BaseActivity
import com.collecto.collectoandroidapp.model.Collection
import com.collecto.collectoandroidapp.service.AuthService
import com.google.android.material.navigation.NavigationView
import com.collecto.collectoandroidapp.service.CollectionsService
import com.collecto.collectoandroidapp.controller.items.AddItemActivity
import com.collecto.collectoandroidapp.controller.settings.SettingsActivity

class ShowCollectionsActivity : BaseActivity() {

    // Local objects
    private lateinit var authService: AuthService
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var collectionService: CollectionsService
    private var collectionItemCounts: Map<Long, Int> = emptyMap()
    private var allCollectionsList: List<Collection> = emptyList()

    // Basic logic of the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionService = CollectionsService(applicationContext)
        setContentView(R.layout.activity_all_collections)

        // Local objects initialization
        authService = AuthService(applicationContext)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        // Animations
        val animLargerScale : Animation = AnimationUtils.loadAnimation(this, R.anim.imageview_click_anim)

        // Menu functionality
        val menuButton : ImageButton = findViewById(R.id.menu_button)
        lifecycleScope.launch {
            navigationView.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.settings -> {
                        val intent = Intent(this@ShowCollectionsActivity, SettingsActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.add_collection -> {
                        val intent = Intent(this@ShowCollectionsActivity, AddCollectionActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.add_item -> {
                        val intent = Intent(this@ShowCollectionsActivity, AddItemActivity::class.java)
                        startActivity(intent)
                    }
                }
                drawerLayout.closeDrawers()
                true
            }
        }
        menuButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                authService.refreshSession()
                val userNameTextView = navigationView.getHeaderView(0).findViewById<TextView>(R.id.userName)
                userNameTextView.text = authService.getUserUsername()
                drawerLayout.openDrawer(GravityCompat.START)
            }
            lifecycleScope.launch {
                val avatar: ImageView = findViewById(R.id.avatar)
                val avatarImage = authService.loadAvatar()
                if (avatarImage != null) {
                    Glide.with(this@ShowCollectionsActivity)
                        .load(avatarImage)
                        .circleCrop()
                        .into(avatar)
                } else {
                    avatar.setImageResource(R.drawable.ic_logo_user)
                }
            }
        }

        // Add item button functionality
        val addItemButton : ImageButton = findViewById(R.id.add_item_button)
        addItemButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                delay(300)
                val intent = Intent(this@ShowCollectionsActivity, AddCollectionActivity::class.java)
                startActivity(intent)
            }
        }

        // Search by name functionality
        val searchBar: LinearLayout = findViewById(R.id.search_bar)
        val searchInput: EditText = findViewById(R.id.search_input)
        val searchButton: ImageButton = findViewById(R.id.search_button)
        val closeButton: ImageButton = findViewById(R.id.close_button)

        // Close search window
        closeButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                delay(200)
                closeButton.visibility = View.GONE
                searchInput.visibility = View.GONE
                searchBar.visibility = View.GONE
                showCollections(allCollectionsList)
            }
        }

        // Open search window
        searchButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                delay(100)
                closeButton.visibility = View.VISIBLE
                searchInput.visibility = View.VISIBLE
                searchBar.visibility = View.VISIBLE
            }
        }

        // Start search
        var searchJob: Job? = null
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    val query = s.toString().trim()
                    searchCollections(query)
                    val emptyTextView = findViewById<TextView>(R.id.empty_text)
                    val catImageView = findViewById<ImageView>(R.id.cat)
                    catImageView.visibility = View.GONE
                    emptyTextView.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Spinner variables and settings
        val spinner: Spinner = findViewById(R.id.language_spinner)
        val sortButton: ImageButton = findViewById(R.id.order_button_up_down)
        var sortState = 0
        var sortType = "name"
        val adapter = ArrayAdapter(this, R.layout.layout_spinner_item, listOf(getString(R.string.order_by_name), getString(R.string.order_by_amount_of_elements)))
        spinner.adapter = adapter

        // Spinner functionality
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sortType = if (position == 0) "name" else "count"
                allCollectionsList = sortCollections(sortType, sortState)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Sort functionality
        sortButton.setOnClickListener {
            view -> view.startAnimation(animLargerScale)
            lifecycleScope.launch {
                sortState = (sortState + 1) % 3
                updateSortIcon(sortButton, sortState)
                allCollectionsList = sortCollections(sortType, sortState)
                showCollections(allCollectionsList)
            }
        }

        // Show all collections
        lifecycleScope.launch {
            loadCollections()
            showCollections(allCollectionsList)
        }

    }

    // Loads all user's collections
    private suspend fun loadCollections() {
        val collections = collectionService.getAllCollections()
        allCollectionsList = collections.sortedByDescending { collection ->
            collection.updatedAt?.let { OffsetDateTime.parse(it) }
        }
        collectionItemCounts = collectionService.countElements(allCollectionsList)
    }

    // Method for searching collections
    private fun searchCollections(query: String) {
        val filteredCollections = allCollectionsList.filter {
            it.name.contains(query, ignoreCase = true)
        }
        lifecycleScope.launch {
            showCollections(filteredCollections)
        }
    }

    // Updates the sort icon
    private fun updateSortIcon(sortButton: ImageButton, sortState: Int) {
        when (sortState) {
            0 -> sortButton.setImageResource(R.drawable.ic_button_sort)
            1 -> sortButton.setImageResource(R.drawable.ic_button_sort_desc)
            2 -> sortButton.setImageResource(R.drawable.ic_button_sort_asc)
        }
    }

    // Shows the popup menu
    private fun showPopupMenu(view: View, collection: Collection) {
        val popupMenu = androidx.appcompat.widget.PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.collection_actions_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    lifecycleScope.launch {
                        updateCollection(collection)
                    }
                    true
                }
                R.id.action_delete -> {
                    lifecycleScope.launch {
                        deleteCollection(collection)
                        loadCollections()
                        showCollections(allCollectionsList)
                    }
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    // Shows the full collections list
    private suspend fun showCollections(collections: List<Collection>) {
        val collectionsContainer = findViewById<LinearLayout>(R.id.collections_container)
        val emptyTextView = findViewById<TextView>(R.id.empty_text)
        val catImageView = findViewById<ImageView>(R.id.cat)

        val animScale : Animation = AnimationUtils.loadAnimation(this, R.anim.button_click_anim)

        collectionsContainer.removeAllViews()

        if (collections.isEmpty()) {
            catImageView.visibility = View.VISIBLE
            emptyTextView.visibility = View.VISIBLE
        } else {
            catImageView.visibility = View.GONE
            emptyTextView.visibility = View.GONE
        }

        collections.forEach { collection ->
            val collectionView = layoutInflater.inflate(R.layout.layout_collections, collectionsContainer, false)
            val icon = collectionView.findViewById<ImageView>(R.id.collection_icon)
            val titleTextView = collectionView.findViewById<TextView>(R.id.record_input_text)
            val amountTextView = collectionView.findViewById<TextView>(R.id.amount_of_elements)
            val actionsButton = collectionView.findViewById<ImageView>(R.id.actions_button)

            titleTextView.text = collection.name
            val itemCount = collectionItemCounts[collection.id] ?: 0
            amountTextView.text = "$itemCount items"

            val collectionIcon = collectionService.loadIcon(collection)
            if (collectionIcon != null) {
                Glide.with(this@ShowCollectionsActivity)
                    .load(collectionIcon)
                    .circleCrop()
                    .into(icon)
            } else {
                icon.setImageResource(R.drawable.ic_logo_photo)
            }

            collectionView.setOnClickListener {
                view -> view.startAnimation(animScale)
                lifecycleScope.launch {
                    openCollection(collection)
                }
            }

            actionsButton.setOnClickListener { view ->
                showPopupMenu(view, collection)
            }

            collectionsContainer.addView(collectionView)
        }
    }

    // Sorts collections by selected criteria
    fun sortCollections(sortType: String, sortState: Int): List<Collection> {
        val newList = when (sortType) {
            "name" -> when (sortState) {
                1 -> allCollectionsList.sortedBy { it.name.lowercase() }
                2 -> allCollectionsList.sortedByDescending { it.name.lowercase() }
                else -> allCollectionsList.sortedBy { collection -> collection.updatedAt?.let { OffsetDateTime.parse(it) } }
            }
            "count" -> when (sortState) {
                1 -> allCollectionsList.sortedBy { collectionItemCounts[it.id] ?: 0 }
                2 -> allCollectionsList.sortedByDescending { collectionItemCounts[it.id] ?: 0 }
                else -> allCollectionsList.sortedBy { collection -> collection.updatedAt?.let { OffsetDateTime.parse(it) } }
            }
            else -> allCollectionsList
        }

        return newList
    }

    // Method for deleting the selected collection
    private fun deleteCollection(collection: Collection) {
        val builder = android.app.AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.delete_text))
        builder.setMessage(getString(R.string.delete_question))

        builder.setPositiveButton(getString(R.string.delete)) { dialog, which ->
            lifecycleScope.launch {
                delay(300)
                collectionService.deleteCollection(collection)
                loadCollections()
                showCollections(allCollectionsList)
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

    // Method for updating the selected collection
    private fun updateCollection(collection: Collection) {
        val intent = Intent(this, UpdateCollectionActivity::class.java)
        intent.putExtra("collection", collection)
        startActivity(intent)
    }

    // Method for opening the selected collection
    private fun openCollection(collection: Collection) {
        val intent = Intent(this, OpenCollectionActivity::class.java)
        intent.putExtra("collection", collection)
        startActivity(intent)
    }

}