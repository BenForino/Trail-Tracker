package com.benforino.trailtrackerv2.database

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trail_table")
data class Trail(
    @PrimaryKey val id:Int,
    val timeStamp: Long?,
    val distance: Float?,
    val timeTaken: Long?,
    val img: Bitmap?
)
