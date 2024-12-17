package com.collecto.collectoandroidapp.repository

import java.io.File
import android.net.Uri
import android.content.Context
import android.graphics.Bitmap
import java.io.FileOutputStream
import java.time.OffsetDateTime
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
import kotlinx.serialization.json.Json
import io.github.jan.supabase.auth.Auth
import com.collecto.collectoandroidapp.R
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.query.Columns
import com.collecto.collectoandroidapp.model.CollectionItem
import com.collecto.collectoandroidapp.model.CollectionItemFields

class ItemsRepository(private val context: Context) {
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
    private data class FieldsResponse(
        val id: Long,
        val field_contents: String?
    )

    // Collects collection data correctly
    suspend fun getItems(collectionId: Long): List<CollectionItem> {
        try {
            return getItemsWithFields(collectionId)
        } catch (e: Exception) {
            println(e.message)
            return emptyList()
        }
    }

    // Collects collection data correctly
    private suspend fun getItemsWithFields(collectionId: Long): List<CollectionItem> {
        val items = fetchItems(collectionId)

        val result = items.map { item ->
            val itemFieldsResponses = fetchItemFieldsByItemId(item.id)
            val customFieldsMap = itemFieldsResponses.associateBy { it.id }

            val customFieldsJson = customFieldsMap[item.id]?.field_contents
            val fieldContents = customFieldsJson?.let {
                Json.decodeFromString<List<CollectionItemFields>>(it)
            } ?: emptyList()

            CollectionItem(
                id = item.id,
                name = item.name,
                field_contents = fieldContents,
                user_id = item.user_id,
                collection_id = item.collection_id,
                created_at = item.created_at,
                updated_at = item.updated_at,
                image_path = item.image_path,
                description = item.description
            )
        }

        return result
    }

    // Loading all elements of the collection
    private suspend fun fetchItems(collectionId: Long): List<CollectionItem> {
        val columns = "id, name, user_id, image_path, collection_id, created_at, updated_at, description"
        return supabase.from("items").select(columns = Columns.list(columns)) {
            filter {
                eq("collection_id", collectionId)
            }
        }.decodeList<CollectionItem>()
    }

    // Load custom fields for each collection item
    private suspend fun fetchItemFieldsByItemId(itemId: Long): List<FieldsResponse> {
        return supabase.from("items").select(columns = Columns.list("id, field_contents")) {
            filter {
                eq("id", itemId)
            }
        }.decodeList<FieldsResponse>()
    }

    // Downloads a compressed collection icon
    suspend fun loadIcon(item: CollectionItem): Bitmap? {
        val bucket = supabase.storage.from("itemIcons")

        val imagePath = item.image_path ?: return null
        val sanitizedFileName = imagePath.replace("/", "-")

        val cacheDir = File(context.cacheDir, "itemIcons")
        val cacheFile = File(cacheDir, "$sanitizedFileName.png")

        if (cacheFile.exists()) {
            return BitmapFactory.decodeFile(cacheFile.absolutePath)
        }

        try {
            val bytes = bucket.downloadAuthenticated(imagePath)
            val bitmap = bytes?.let { byteArrayToBitmap(it) }

            bitmap?.let {
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
    suspend fun loadImage(item: CollectionItem): Bitmap? {
        val bucket = supabase.storage.from("itemImages")

        val imagePath = item.image_path ?: return null
        val sanitizedFileName = imagePath.replace("/", "-")

        val cacheDir = File(context.cacheDir, "itemImages")
        val cacheFile = File(cacheDir, "$sanitizedFileName.png")

        if (cacheFile.exists()) {
            return BitmapFactory.decodeFile(cacheFile.absolutePath)
        }

        try {
            val bytes = bucket.downloadAuthenticated(imagePath)
            val bitmap = bytes?.let { byteArrayToBitmap(it) }

            bitmap?.let {
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

    // Method of saving a new item of the collection
    suspend fun saveItem(item: CollectionItem, imageName: String?, imagePath: Uri?): Boolean {
        var photoSaveResult = true
        var collectionSaveResult = true
        if (imagePath != null) {
            photoSaveResult = saveImageAndIcon(imageName, imagePath)
        }
        if (photoSaveResult) {
            collectionSaveResult = saveItemData(imageName, item)
        }
        return (photoSaveResult && collectionSaveResult)
    }

    // Method of saving items image and compressed icon
    private suspend fun saveImageAndIcon(fileName: String?, fileUri: Uri): Boolean {
        val bucketImages = supabase.storage.from("itemImages")
        val bucketIcons = supabase.storage.from("itemIcons")

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

        var stream = ByteArrayOutputStream()
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
    private suspend fun saveItemData(imageName: String?, item: CollectionItem): Boolean {
        val fieldContentsJson = Json.encodeToString(item.field_contents)

        val data = mapOf(
            "name" to item.name,
            "field_contents" to fieldContentsJson,
            "user_id" to item.user_id,
            "collection_id" to item.collection_id.toString(),
            "created_at" to OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            "updated_at" to OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            "image_path" to imageName,
            "description" to item.description,
        )

        try {
            supabase.from("items").insert(data)
        } catch (e: Exception) {
            println(e.message)
            return false
        }
        return true
    }

    // Deletes an item
    suspend fun deleteItem(item: CollectionItem) {
        supabase.from("items").delete {
            filter {
                eq("id", item.id)
            }
        }
        val bucketIcons = supabase.storage.from("itemsIcons")
        val bucketImages = supabase.storage.from("itemsImages")
        try {
            item.image_path?.let { bucketIcons.delete(it) }
            item.image_path?.let { bucketImages.delete(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Method of modifying the collection
    suspend fun modifyItem(item: CollectionItem, imageName: String?, imagePath: Uri?): Boolean {
        var photoSaveResult = true
        var collectionSaveResult = false
        if (imagePath != null) {
            photoSaveResult = saveImageAndIcon(imageName, imagePath)
            if (photoSaveResult) {
                collectionSaveResult = modifyItemData(imageName, item)
            }
        } else {
            collectionSaveResult = modifyItemData(item.image_path, item)
        }
        return (photoSaveResult && collectionSaveResult)
    }

    // Modifies data in DB
    private suspend fun modifyItemData(imageName: String?, item: CollectionItem): Boolean {
        val customFieldsJson = Json.encodeToString(item.field_contents)

        try {
            supabase.from("items").update(
                {
                    set("name", item.name)
                    set("description", item.description)
                    set("field_contents", customFieldsJson)
                    set("image_path", imageName)
                    set("updated_at", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                }
            ) {
                filter {
                    eq("id", item.id)
                }
            }
        } catch (e: Exception) {
            println(e.message)
            return false
        }
        return true
    }

}