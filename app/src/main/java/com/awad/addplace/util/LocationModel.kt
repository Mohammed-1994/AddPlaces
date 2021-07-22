package com.awad.addplace.util

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LocationModel(
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var geohash: String = ""
) : Parcelable
