package com.benforino.trailtrackerv2.viewmodel


import com.benforino.trailtrackerv2.database.Trail
import com.benforino.trailtrackerv2.database.TrailDao
import javax.inject.Inject

class Repository @Inject constructor(
    private val trailDao: TrailDao
) {
    suspend fun insertTrail(trail: Trail) = trailDao.insertTrail(trail)

    fun deleteTrail(trail: Trail) = trailDao.delete(trail)

}