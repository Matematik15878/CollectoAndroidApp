package com.collecto.collectoandroidapp.service

import java.util.Date
import android.net.Uri
import java.util.Locale
import android.content.Context
import android.graphics.Bitmap
import java.time.OffsetDateTime
import java.text.SimpleDateFormat
import com.collecto.collectoandroidapp.model.Collection
import com.collecto.collectoandroidapp.repository.CollectionsRepository

class CollectionsService (context: Context) {
    // Local services and repositories
    private val authService = AuthService(context.applicationContext)
    private val collectionsRepository = CollectionsRepository(context.applicationContext)

    // Method for saving new collection
    suspend fun saveCollection(collection: Collection, imagePath: Uri?): Pair<Boolean, String> {
        val validateName = (validateLength(collection.name, 50) && (collection.name.isNotEmpty()))
        val validateDescription = collection.description?.let { validateLength(it, 500) }

        var validateFieldsNames = true
        for (field in collection.customFields!!) {
            if (!validateLength(field.name, 30)) validateFieldsNames = false
        }
        val validateAmountOfFields = (collection.customFields.size < 21)

        val fieldNames = collection.customFields.map { it.name }
        val uniqueFieldNames = fieldNames.toSet()
        val validateUniqueFields = fieldNames.size == uniqueFieldNames.size

        return if (!validateName || !validateFieldsNames || !validateDescription!!) {
            Pair(false, "Wrong length")
        } else if (!validateAmountOfFields) {
            Pair(false, "Wrong amount of fields")
        } else if (!validateUniqueFields) {
            Pair(false, "Identical fields")
        } else {
            val imageName = generateFileName(collection.userId, imagePath)
            if (collectionsRepository.saveCollection(collection, imageName, imagePath)) {
                Pair(true, "Success")
            } else {
                Pair(false, "Something wrong")
            }
        }

    }

    // Generates a unique file name
    private fun generateFileName(userId: String, imagePath: Uri?): String? {
        if (imagePath == null) return null
        val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        return "${userId}/collection_$currentTime"
    }

    // Method for checking field lengths
    private fun validateLength(string: String, maxLength: Int): Boolean {
        return string.length <= maxLength
    }

    // Returns all collections
    suspend fun getAllCollections(): List<Collection> {
        val collections = collectionsRepository.getAllCollections()
        return collections.sortedBy {
                collection -> collection.updatedAt?.let { OffsetDateTime.parse(it) }
        }
    }

    // Return icon of the collection
    suspend fun loadIcon(collection: Collection): Bitmap? {
        return collectionsRepository.loadIcon(collection)
    }

    // Return image of the collection
    suspend fun loadImage(collection: Collection): Bitmap? {
        return collectionsRepository.loadImage(collection)
    }

    // Deletes the selected collection
    suspend fun deleteCollection(collection: Collection) {
        collectionsRepository.deleteCollection(collection)
    }

    // Counts the number of elements for each collection
    suspend fun countElements(collections: List<Collection>): Map<Long, Int> {
        return collectionsRepository.countElements(collections)
    }

    // Returns UID
    fun getUserID(): String? {
        return authService.getUserId()
    }

    // Method of modifying the collection
    suspend fun modifyCollection(collection: Collection, imagePath: Uri?): Pair<Boolean, String> {
        val validateName = (validateLength(collection.name, 50) && (collection.name.isNotEmpty()))
        val validateDescription = collection.description?.let { validateLength(it, 500) }

        var validateFieldsNames = true
        for (field in collection.customFields!!) {
            if (!validateLength(field.name, 30)) validateFieldsNames = false
        }
        val validateAmountOfFields = (collection.customFields.size < 21)

        val fieldNames = collection.customFields.map { it.name }
        val uniqueFieldNames = fieldNames.toSet()
        val validateUniqueFields = fieldNames.size == uniqueFieldNames.size

        return if (!validateName || !validateFieldsNames || !validateDescription!!) {
            Pair(false, "Wrong length")
        } else if (!validateAmountOfFields) {
            Pair(false, "Wrong amount of fields")
        } else if (!validateUniqueFields) {
            Pair(false, "Identical fields")
        } else {
            val imageName = generateFileName(collection.userId, imagePath)
            if (collectionsRepository.modifyCollection(collection, imageName, imagePath)) {
                Pair(true, "Success")
            } else {
                Pair(false, "Something wrong")
            }
        }

    }

}