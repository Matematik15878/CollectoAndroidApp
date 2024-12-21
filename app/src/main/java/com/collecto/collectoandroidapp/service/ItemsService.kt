package com.collecto.collectoandroidapp.service

import java.util.Date
import android.net.Uri
import java.util.Locale
import android.content.Context
import android.graphics.Bitmap
import java.text.SimpleDateFormat
import com.collecto.collectoandroidapp.model.CollectionItem
import com.collecto.collectoandroidapp.repository.ItemsRepository

class ItemsService(context: Context) {
    // Local services and repositories
    private val itemsRepository = ItemsRepository(context.applicationContext)

    // Return all items of the collection
    suspend fun getItems(collectionId: Long): List<CollectionItem> {
        return itemsRepository.getItems(collectionId)
    }

    // Return icon of the item
    suspend fun loadIcon(item: CollectionItem): Bitmap? {
        return itemsRepository.loadIcon(item)
    }

    // Return image of the item
    suspend fun loadImage(item: CollectionItem): Bitmap? {
        return itemsRepository.loadImage(item)
    }

    // Method for saving new item
    suspend fun saveItem(item: CollectionItem, imagePath: Uri?): Pair<Boolean, String> {
        val validateName = (validateLength(item.name, 50) && (item.name.isNotEmpty()))
        val validateDescription = item.description?.let { validateLength(it, 500) }

        var validateFieldsNames = true
        for (field in item.fieldContents!!) {
            if (!validateLength(field.fieldContent, 10)) validateFieldsNames = false
        }

        return if (!validateName || !validateFieldsNames || !validateDescription!!) {
            Pair(false, "Wrong length")
        } else {
            val imageName = generateFileName(item.userId, imagePath)
            if (itemsRepository.saveItem(item, imageName, imagePath)) {
                Pair(true, "Success")
            } else {
                Pair(false, "Something wrong")
            }
        }

    }

    // Method for checking field lengths
    private fun validateLength(string: String, maxLength: Int): Boolean {
        return string.length <= maxLength
    }

    // Generates a unique file name
    private fun generateFileName(userId: String, imagePath: Uri?): String? {
        if (imagePath == null) return null
        val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        return "${userId}/collection_$currentTime"
    }

    // Deletes the selected item
    suspend fun deleteItem(item: CollectionItem) {
        itemsRepository.deleteItem(item)
    }

    // Method of modifying the item
    suspend fun modifyItem(item: CollectionItem, imagePath: Uri?): Pair<Boolean, String> {
        val validateName = (validateLength(item.name, 50) && (item.name.isNotEmpty()))
        val validateDescription = item.description?.let { validateLength(it, 500) }

        var validateFieldsNames = true
        for (field in item.fieldContents!!) {
            if (!validateLength(field.fieldContent, 100)) validateFieldsNames = false
        }

        return if (!validateName || !validateFieldsNames || !validateDescription!!) {
            Pair(false, "Wrong length")
        } else {
            val imageName = generateFileName(item.userId, imagePath)
            if (itemsRepository.modifyItem(item, imageName, imagePath)) {
                Pair(true, "Success")
            } else {
                Pair(false, "Something wrong")
            }
        }

    }

}