package com.benforino.trailtrackerv2.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TrailDao {
    @Query("SELECT * FROM trail_table")
    fun getAll(): List<Trail>

    @Insert
    suspend fun insertTrail(trail: Trail)

    @Delete
    fun delete(trail: Trail)
}