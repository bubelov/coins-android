package com.bubelov.coins.repository.place

import android.util.Log
import com.bubelov.coins.api.coins.CoinsApi
import com.bubelov.coins.api.coins.CreatePlaceArgs
import com.bubelov.coins.api.coins.UpdatePlaceArgs
import com.bubelov.coins.data.Place
import com.bubelov.coins.data.PlaceQueries
import com.bubelov.coins.repository.synclogs.LogsRepository
import com.bubelov.coins.repository.user.UserRepository
import com.bubelov.coins.util.TableSyncResult
import com.squareup.sqldelight.runtime.coroutines.asFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.lang.Exception
import java.time.LocalDateTime
import kotlin.time.measureTime

class PlacesRepository(
    private val api: CoinsApi,
    private val db: PlaceQueries,
    private val builtInCache: BuiltInPlacesCache,
    private val userRepository: UserRepository,
    private val log: LogsRepository
) {

    suspend fun find(id: String): Place? {
        return withContext(Dispatchers.IO) {
            db.selectById(id).executeAsOneOrNull()
        }
    }

    suspend fun findBySearchQuery(searchQuery: String): List<Place> {
        return withContext(Dispatchers.IO) {
            db.selectBySearchQuery(searchQuery).executeAsList()
        }
    }

    suspend fun findRandom() = withContext(Dispatchers.IO) {
        db.selectRandom().executeAsOneOrNull()
    }

    fun getAll(): Flow<List<Place>> {
        return db.selectAll().asFlow().map { it.executeAsList() }
    }

    fun get(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): Flow<List<Place>> {
        return db.selectByBoundingBox(
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon
        ).asFlow().map { it.executeAsList() }
    }

    suspend fun sync() = withContext(Dispatchers.IO) {
        initBuiltInCache()

        val syncStartDate = LocalDateTime.now()

        try {
            val maxUpdatedAt = db.selectMaxUpdatedAt().executeAsOneOrNull()?.MAX
                ?: LocalDateTime.now().minusYears(50).toString()

            val response = api.getPlaces(
                createdOrUpdatedAfter = LocalDateTime.parse(maxUpdatedAt)
            )

            val newPlaces = response.filter {
                db.selectById(it.id).executeAsOneOrNull() == null
            }

            db.transaction {
                response.forEach {
                    db.insertOrReplace(it)
                }
            }

            val tableSyncResult = TableSyncResult(
                startDate = syncStartDate,
                endDate = LocalDateTime.now(),
                success = true,
                affectedRecords = response.size
            )

            PlacesSyncResult(tableSyncResult, newPlaces)
        } catch (e: Exception) {
            Log.e("PlacesRepository", "Sync failed", e)

            val tableSyncResult = TableSyncResult(
                startDate = syncStartDate,
                endDate = LocalDateTime.now(),
                success = false,
                affectedRecords = 0
            )

            PlacesSyncResult(tableSyncResult, emptyList())
        }
    }

    suspend fun addPlace(place: Place): Place {
        return withContext(Dispatchers.IO) {
            val response = api.addPlace(
                authorization = "Bearer ${userRepository.getToken()}",
                args = CreatePlaceArgs(place)
            )

            db.insertOrReplace(response)
            response
        }
    }

    suspend fun updatePlace(place: Place): Place {
        return withContext(Dispatchers.IO) {
            val response = api.updatePlace(
                id = place.id,
                authorization = "Bearer ${userRepository.getToken()}",
                args = UpdatePlaceArgs(place)
            )

            db.insertOrReplace(response)
            response
        }
    }

    suspend fun initBuiltInCache() {
        withContext(Dispatchers.IO) {
            val empty = db.selectCount().executeAsOne() == 0L

            if (!empty) {
                return@withContext
            }

            log += "Initializing built-in places cache"

            val places = builtInCache.loadPlaces()

            val insertDuration = measureTime {
                db.transaction {
                    places.forEach {
                        db.insertOrReplace(it)
                    }
                }
            }

            log += "Inserted ${places.size} places in ${insertDuration.inMilliseconds.toInt()} ms"
        }
    }

    data class PlacesSyncResult(
        val tableSyncResult: TableSyncResult,
        val newPlaces: List<Place>
    )
}