package com.example.data.repository

import com.example.data.dao.PairedDeviceDao
import com.example.data.entity.PairedDeviceEntity
import kotlinx.coroutines.flow.Flow

class DeviceRepository(private val pairedDeviceDao: PairedDeviceDao) {
    val pairedDevices: Flow<List<PairedDeviceEntity>> = pairedDeviceDao.getAllDevices()

    suspend fun getDeviceByIp(ip: String): PairedDeviceEntity? = pairedDeviceDao.getDeviceByIp(ip)

    suspend fun getDeviceById(id: Int): PairedDeviceEntity? = pairedDeviceDao.getDeviceById(id)

    suspend fun saveDevice(device: PairedDeviceEntity): Long {
        val existing = pairedDeviceDao.getDeviceByIp(device.ip)
        return if (existing != null) {
            pairedDeviceDao.updateDevice(
                device.copy(
                    id = existing.id,
                    macOrUid = device.macOrUid.ifEmpty { existing.macOrUid }
                )
            )
            existing.id.toLong()
        } else {
            pairedDeviceDao.insertDevice(device)
        }
    }

    suspend fun updateDevice(device: PairedDeviceEntity) = pairedDeviceDao.updateDevice(device)

    suspend fun updateMacAddress(id: Int, mac: String) = pairedDeviceDao.updateMacAddress(id, mac)

    suspend fun updateMacByIp(ip: String, mac: String) = pairedDeviceDao.updateMacByIp(ip, mac)

    suspend fun removeDevice(device: PairedDeviceEntity) = pairedDeviceDao.deleteDevice(device)

    suspend fun removeDeviceByIp(ip: String) {
        val existing = pairedDeviceDao.getDeviceByIp(ip)
        if (existing != null) {
            pairedDeviceDao.deleteDevice(existing)
        }
    }

    suspend fun removeDeviceById(id: Int) = pairedDeviceDao.deleteById(id)

    suspend fun clearAll() = pairedDeviceDao.clearAll()
}
