package com.awad.addplaces.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationModel(
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var geohash: String = ""
) : Parcelable
