package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val ipAddress: String,
    val encryptedToken: String,
    val iv: String, // Initialization vector for encryption
    val type: String, // "Laptop", "Desktop", "Server"
    val isActive: Boolean = false
)
