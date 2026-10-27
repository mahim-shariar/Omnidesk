package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.PairedDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PairedDeviceDao {
    @Query("SELECT * FROM paired_devices ORDER BY lastConnected DESC")
    fun getAllDevices(): Flow<List<PairedDeviceEntity>>

    @Query("SELECT * FROM paired_devices WHERE ip = :ip LIMIT 1")
    suspend fun getDeviceByIp(ip: String): PairedDeviceEntity?

    @Query("SELECT * FROM paired_devices WHERE id = :id LIMIT 1")
    suspend fun getDeviceById(id: Int): PairedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: PairedDeviceEntity): Long

    @Update
    suspend fun updateDevice(device: PairedDeviceEntity)

    @Query("UPDATE paired_devices SET macOrUid = :mac WHERE id = :id")
    suspend fun updateMacAddress(id: Int, mac: String)

    @Query("UPDATE paired_devices SET macOrUid = :mac WHERE ip = :ip")
    suspend fun updateMacByIp(ip: String, mac: String)

    @Delete
    suspend fun deleteDevice(device: PairedDeviceEntity)

    @Query("DELETE FROM paired_devices WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM paired_devices")
    suspend fun clearAll()
}
