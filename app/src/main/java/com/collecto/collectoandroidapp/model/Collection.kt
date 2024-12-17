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
    val user_id: String,
    val custom_fields: List<CustomCollectionField>? = emptyList(),
    val image_path: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val max_field_id: String? = null
) : Parcelable