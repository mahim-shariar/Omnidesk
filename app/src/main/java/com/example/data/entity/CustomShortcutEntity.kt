package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_shortcuts")
data class CustomShortcutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val label: String,
    val keys: String,
    val description: String = "",
    val colorHex: Long = 0xFF2ECC71
)
