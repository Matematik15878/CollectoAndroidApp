package com.collecto.collectoandroidapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class CollectionItem(
    val id: Long,
    val name: String,
    val fieldContents: List<CollectionItemFields>? = emptyList(),
    val userId: String,
    val collectionId: Long,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val imagePath: String? = null,
    val description: String? = null
) : Parcelable