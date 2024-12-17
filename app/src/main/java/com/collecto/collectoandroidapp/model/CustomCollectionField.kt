package com.collecto.collectoandroidapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class CustomCollectionField (
    val id: Int = 0,
    var order: Int = 0,
    var name: String = ""
) : Parcelable