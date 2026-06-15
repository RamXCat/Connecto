package com.example.data.database

import androidx.room.*
import com.example.data.model.Device
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY id DESC")
    fun getAllDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE isActive = 1 LIMIT 1")
    fun getActiveDevice(): Flow<Device?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device): Long

    @Update
    suspend fun updateDevice(device: Device)

    @Delete
    suspend fun deleteDevice(device: Device)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Int)

    @Query("UPDATE devices SET isActive = 0")
    suspend fun clearActiveFlag()

    @Transaction
    suspend fun setActiveDevice(id: Int) {
        clearActiveFlag()
        setActiveFlag(id)
    }

    @Query("UPDATE devices SET isActive = 1 WHERE id = :id")
    suspend fun setActiveFlag(id: Int)
}
