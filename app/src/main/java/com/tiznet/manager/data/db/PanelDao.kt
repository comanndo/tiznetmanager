package com.tiznet.manager.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PanelDao {

    @Query("SELECT * FROM panels ORDER BY lastUsedAt DESC")
    fun observeAll(): Flow<List<PanelEntity>>

    @Query("SELECT * FROM panels ORDER BY lastUsedAt DESC")
    suspend fun getAll(): List<PanelEntity>

    @Query("SELECT * FROM panels WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PanelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(panel: PanelEntity): Long

    @Update
    suspend fun update(panel: PanelEntity)

    @Delete
    suspend fun delete(panel: PanelEntity)

    @Query("DELETE FROM panels WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE panels SET lastUsedAt = :time WHERE id = :id")
    suspend fun touch(id: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE panels SET panelVersion = :version WHERE id = :id")
    suspend fun updateVersion(id: Long, version: String?)

    @Query("SELECT COUNT(*) FROM panels")
    suspend fun count(): Int
}
