package com.benforino.trailtrackerv2.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Trail::class],
    version = 1
)
@TypeConverters(TypeConverter::class)
abstract class TrailDB : RoomDatabase() {
    abstract fun trailDao(): TrailDao
}