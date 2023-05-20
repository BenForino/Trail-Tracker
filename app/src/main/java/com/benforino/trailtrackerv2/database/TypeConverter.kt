package com.benforino.trailtrackerv2.database

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import java.io.ByteArrayOutputStream

class TypeConverter {
    @androidx.room.TypeConverter
    fun toBitmap(bytes: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    @androidx.room.TypeConverter
    fun fromBitmap(bmp: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    fun fromListToJson(list: List<List<LatLng>>): String {
        return Gson().toJson(list)
    }
    fun JsonToList(json: String): Trail {
        return Gson().fromJson(json,Trail::class.java)
    }
}