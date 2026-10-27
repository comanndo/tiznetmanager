package com.tiznet.manager.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved Hiddify panel. The password is stored AES/GCM encrypted (see CryptoManager);
 * [proxyPath] may be empty when auto-detection is used on every connect.
 */
@Entity(tableName = "panels")
data class PanelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val proxyPath: String,
    val username: String,
    val encryptedPassword: String,
    val panelVersion: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
)
