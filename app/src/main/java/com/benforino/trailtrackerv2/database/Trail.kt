package com.benforino.trailtrackerv2.database

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

data class Trail(
    var id:String,
    var distance: Float?,
    var img: String?,
    var name:String
)
