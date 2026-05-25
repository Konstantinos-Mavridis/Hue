package com.hue.data.pantone.repository

import com.google.gson.Gson
import com.hue.data.pantone.db.HueDatabase
import com.hue.data.pantone.db.ScanHistoryEntity
import com.hue.domain.model.FabricAnalysis
import com.hue.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(
    private val db: HueDatabase,
    private val gson: Gson
) : HistoryRepository {

    override fun getAllScans(): Flow<List<FabricAnalysis>> =
        db.scanHistoryDao().getAll().map { list -> list.map { it.toDomain(gson) } }

    override fun getScansBySeason(season: String): Flow<List<FabricAnalysis>> =
        db.scanHistoryDao().getBySeason(season).map { list -> list.map { it.toDomain(gson) } }

    override suspend fun saveScan(analysis: FabricAnalysis): Long =
        db.scanHistoryDao().insert(ScanHistoryEntity.fromDomain(analysis, gson))

    override suspend fun deleteScan(id: Long) = db.scanHistoryDao().deleteById(id)

    override suspend fun getScanById(id: Long): FabricAnalysis? =
        db.scanHistoryDao().getById(id)?.toDomain(gson)

    override suspend fun clearAll() = db.scanHistoryDao().clearAll()
}
