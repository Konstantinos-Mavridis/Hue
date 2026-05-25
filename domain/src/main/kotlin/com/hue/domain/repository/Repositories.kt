package com.hue.domain.repository

import com.hue.core.color.model.LabColor
import com.hue.domain.model.FabricAnalysis
import com.hue.domain.model.PantoneFhiColor
import com.hue.domain.model.PantoneMatchResult
import kotlinx.coroutines.flow.Flow

interface PantoneRepository {
    suspend fun findTopMatches(lab: LabColor, topN: Int = 3): List<PantoneMatchResult>
    suspend fun getColorByCode(code: String): PantoneFhiColor?
    fun searchByName(query: String): Flow<List<PantoneFhiColor>>
    suspend fun getTotalCount(): Int
}

interface HistoryRepository {
    fun getAllScans(): Flow<List<FabricAnalysis>>
    fun getScansBySeason(season: String): Flow<List<FabricAnalysis>>
    suspend fun saveScan(analysis: FabricAnalysis): Long
    suspend fun deleteScan(id: Long)
    suspend fun getScanById(id: Long): FabricAnalysis?
    suspend fun clearAll()
}
