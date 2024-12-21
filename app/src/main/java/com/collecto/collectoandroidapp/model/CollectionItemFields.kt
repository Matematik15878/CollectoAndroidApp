package com.collecto.collectoandroidapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class CollectionItemFields (
    val fieldId: Int,
    var fieldContent: String = ""
) : Parcelable