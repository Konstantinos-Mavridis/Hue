package com.hue.data.pantone.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PantoneDao {
    @Query("SELECT * FROM pantone_fhi WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): PantoneFhiEntity?

    @Query("SELECT * FROM pantone_fhi WHERE name LIKE '%' || :query || '%' ORDER BY name")
    fun searchByName(query: String): Flow<List<PantoneFhiEntity>>

    @Query("SELECT COUNT(*) FROM pantone_fhi")
    suspend fun count(): Int

    @Query("SELECT * FROM pantone_fhi")
    suspend fun getAll(): List<PantoneFhiEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(colors: List<PantoneFhiEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(color: PantoneFhiEntity)
}

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE primarySeason = :season ORDER BY timestamp DESC")
    fun getBySeason(season: String): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScanHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: ScanHistoryEntity): Long

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()
}
