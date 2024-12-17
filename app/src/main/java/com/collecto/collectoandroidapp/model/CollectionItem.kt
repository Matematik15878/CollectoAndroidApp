package com.collecto.collectoandroidapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class CollectionItem(
    val id: Long,
    val name: String,
    val field_contents: List<CollectionItemFields>? = emptyList(),
    val user_id: String,
    val collection_id: Long,
    val created_at: String? = null,
    val updated_at: String? = null,
    val image_path: String? = null,
    val description: String? = null
) : Parcelable