package com.collecto.collectoandroidapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Collection (
    val id: Long,
    val name: String,
    val description: String? = null,
    val userId: String,
    val customFields: List<CustomCollectionField>? = emptyList(),
    val imagePath: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val maxFieldId: String? = null
) : Parcelable