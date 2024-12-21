package com.collecto.collectoandroidapp.repository

import java.io.File
import android.net.Uri
import android.content.Context
import android.graphics.Bitmap
import java.time.OffsetDateTime
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
import kotlinx.serialization.json.Json
import io.github.jan.supabase.auth.Auth
import com.collecto.collectoandroidapp.R
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.query.Columns
import com.collecto.collectoandroidapp.model.Collection
import com.collecto.collectoandroidapp.model.CustomCollectionField

class CollectionsRepository(private val context: Context) {
    // Local supabase
    private val supabase = createSupabaseClient(
        supabaseUrl = context.getString(R.string.supabase_url),
        supabaseKey = context.getString(R.string.supabase_key)
    ) {
        install(Auth)
        install(Storage)
        install(Postgrest)
    }

    @Serializable
    private class CustomFieldsResponse(
        val id: Long,
        val customFields: String?
    )

    @Serializable
    data class ItemId(val id: Long)

    // Method of saving a new collection
    suspend fun saveCollection(collection: Collection, imageName: String?, imagePath: Uri?): Boolean {
        var photoSaveResult = true
        var collectionSaveResult = true
        if (imagePath != null) {
            photoSaveResult = saveImageAndIcon(imageName, imagePath)
        }
        if (photoSaveResult) {
            collectionSaveResult = saveCollectionData(imageName, collection)
        }
        return (photoSaveResult && collectionSaveResult)
    }

    // Method of saving collection image and compressed icon
    private suspend fun saveImageAndIcon(fileName: String?, fileUri: Uri): Boolean {
        val bucketImages = supabase.storage.from("collectionsImages")
        val bucketIcons = supabase.storage.from("collectionsIcons")

        try {
            val compressedImage = uriToByteArray(fileUri, 200, context)
            val compressedIcon = uriToByteArray(fileUri, 50, context)
            if (fileName != null) {
                bucketImages.upload(fileName, compressedImage) {
                    upsert = false
                }
                bucketIcons.upload(fileName, compressedIcon) {
                    upsert = false
                }
            }
        } catch (e: Exception) {
           println(e.message)
           return false
        }

        return true
    }

    // Converts photo into byteArray
    private fun uriToByteArray(uri: Uri, maxSizeKB: Int, context: Context): ByteArray {
        val originalBitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))

        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height

        val maxWidth = 1024
        val maxHeight = 1024

        var scaledWidth = originalWidth
        var scaledHeight = originalHeight

        if (originalWidth > maxWidth || originalHeight > maxHeight) {
            val ratio = Math.min(maxWidth.toFloat() / originalWidth, maxHeight.toFloat() / originalHeight)
            scaledWidth = (originalWidth * ratio).toInt()
            scaledHeight = (originalHeight * ratio).toInt()
        }

        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

        val stream = ByteArrayOutputStream()
        var quality = 90
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

        while (stream.size() / 1024 > maxSizeKB && quality > 50) {
            stream.reset()
            quality -= 5
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        }

        return stream.toByteArray()
    }

    // Saves data to DB
    private suspend fun saveCollectionData(imageName: String?, collection: Collection): Boolean {
        val customFieldsJson = Json.encodeToString(collection.customFields)

        val data = mapOf(
            "name" to collection.name,
            "description" to collection.description,
            "user_id" to collection.userId,
            "custom_fields" to customFieldsJson,
            "image_path" to imageName,
            "created_at" to OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            "updated_at" to OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            "max_field_id" to collection.maxFieldId
        )

        try {
            supabase.from("collections").insert(data)
        } catch (e: Exception) {
            println(e.message)
            return false
        }
        return true
    }

    // Return all collections
    suspend fun getAllCollections(): List<Collection> {
        try {
            return getCollectionsWithCustomFields()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    // Collects collection data correctly
    private suspend fun getCollectionsWithCustomFields(): List<Collection> {
        val customFieldsResponses = fetchCustomFields()
        val collections = fetchCollections()

        val customFieldsMap = customFieldsResponses.associateBy { it.id }

        val result = collections.map { collection ->
            val customFieldsJson = customFieldsMap[collection.id]?.customFields
            val customFields = customFieldsJson?.let {
                Json.decodeFromString<List<CustomCollectionField>>(it)
            } ?: emptyList()

            collection.copy(customFields = customFields)
        }

        return result
    }

    // Collects all collection data except JSONB
    private suspend fun fetchCollections(): List<Collection> {
        val columns = "id, name, description, user_id, image_path, max_field_id, created_at, updated_at"
        return supabase
            .from("collections")
            .select(columns = Columns.list(columns))
            .decodeList<Collection>()
    }

    // Collects data from custom collection fields
    private suspend fun fetchCustomFields(): List<CustomFieldsResponse> {
        return supabase.from("collections").select(columns = Columns.list("id, custom_fields")).decodeList<CustomFieldsResponse>()
    }

    // Deletes a collection
    suspend fun deleteCollection(collection: Collection) {
        supabase.from("collections").delete {
            filter {
                eq("id", collection.id)
            }
        }
        val bucketIcons = supabase.storage.from("collectionsIcons")
        val bucketImages = supabase.storage.from("collectionsImages")
        try {
            collection.imagePath?.let { bucketIcons.delete(it) }
            collection.imagePath?.let { bucketImages.delete(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Counts the number of elements for each collection
    suspend fun countElements(collections: List<Collection>): Map<Long, Int> {
        val itemCounts = mutableMapOf<Long, Int>()

        for (collection in collections) {
            try {
                val response = supabase.from("items").select(columns = Columns.list("id")) {
                    filter { eq("collection_id", collection.id) }
                }.decodeList<ItemId>()

                itemCounts[collection.id] = response.size

            } catch (e: Exception) {
                e.printStackTrace()
                itemCounts[collection.id] = 0
            }
        }

        return itemCounts
    }

    // Downloads a compressed collection icon
    suspend fun loadIcon(collection: Collection): Bitmap? {
        val bucket = supabase.storage.from("collectionsIcons")

        val imagePath = collection.imagePath ?: return null
        val sanitizedFileName = imagePath.replace("/", "-")

        val cacheDir = File(context.cacheDir, "collectionIcons")
        val cacheFile = File(cacheDir, "$sanitizedFileName.png")

        if (cacheFile.exists()) {
            return BitmapFactory.decodeFile(cacheFile.absolutePath)
        }

        try {
            val bytes = bucket.downloadAuthenticated(imagePath)
            val bitmap = byteArrayToBitmap(bytes)

            bitmap.let {
                cacheDir.mkdirs()
                FileOutputStream(cacheFile).use { fos ->
                    it.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
            }

            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    // Downloads a collection image
    suspend fun loadImage(collection: Collection): Bitmap? {
        val bucket = supabase.storage.from("collectionsImages")

        val imagePath = collection.imagePath ?: return null
        val sanitizedFileName = imagePath.replace("/", "-")

        val cacheDir = File(context.cacheDir, "collectionsImages")
        val cacheFile = File(cacheDir, "$sanitizedFileName.png")

        if (cacheFile.exists()) {
            return BitmapFactory.decodeFile(cacheFile.absolutePath)
        }

        try {
            val bytes = bucket.downloadAuthenticated(imagePath)
            val bitmap = byteArrayToBitmap(bytes)

            bitmap.let {
                cacheDir.mkdirs()
                FileOutputStream(cacheFile).use { fos ->
                    it.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
            }

            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    // Converts byteArray to Bitmap
    private fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    // Method of modifying the collection
    suspend fun modifyCollection(collection: Collection, imageName: String?, imagePath: Uri?): Boolean {
        var photoSaveResult = true
        var collectionSaveResult = false
        if (imagePath != null) {
            photoSaveResult = saveImageAndIcon(imageName, imagePath)
            if (photoSaveResult) {
                collectionSaveResult = modifyCollectionData(imageName, collection)
            }
        } else {
            collectionSaveResult = modifyCollectionData(collection.imagePath, collection)
        }
        return (photoSaveResult && collectionSaveResult)
    }

    // Modifies data in DB
    private suspend fun modifyCollectionData(imageName: String?, collection: Collection): Boolean {
        val customFieldsJson = Json.encodeToString(collection.customFields)

        try {
            supabase.from("collections").update(
                {
                    set("name", collection.name)
                    set("description", collection.description)
                    set("custom_fields", customFieldsJson)
                    set("image_path", imageName)
                    set("updated_at", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    set("max_field_id", collection.maxFieldId)
                }
            ) {
                filter {
                    eq("id", collection.id)
                }
            }
        } catch (e: Exception) {
            println(e.message)
            return false
        }
        return true
    }

}