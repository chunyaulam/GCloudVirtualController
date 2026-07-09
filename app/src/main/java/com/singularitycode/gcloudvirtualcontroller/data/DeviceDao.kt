package com.singularitycode.gcloudvirtualcontroller.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY name ASC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM devices WHERE ip = :ip AND port = :port LIMIT 1)")
    suspend fun exists(ip: String, port: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM devices WHERE ip = :ip AND port = :port AND id != :id LIMIT 1)")
    suspend fun existsExcludingId(ip: String, port: Int, id: Int): Boolean

    @Delete
    suspend fun delete(device: DeviceEntity)
}
