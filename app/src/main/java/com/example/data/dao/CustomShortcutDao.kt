package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.CustomShortcutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomShortcutDao {
    @Query("SELECT * FROM custom_shortcuts ORDER BY id ASC")
    fun getAllShortcuts(): Flow<List<CustomShortcutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: CustomShortcutEntity): Long

    @Delete
    suspend fun deleteShortcut(shortcut: CustomShortcutEntity)

    @Query("DELETE FROM custom_shortcuts WHERE id = :id")
    suspend fun deleteById(id: Int)
}
