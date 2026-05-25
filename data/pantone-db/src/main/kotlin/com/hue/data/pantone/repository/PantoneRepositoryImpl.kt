package com.hue.data.pantone.repository

import com.hue.core.color.algorithm.DeltaE2000
import com.hue.core.color.model.LabColor
import com.hue.data.pantone.db.HueDatabase
import com.hue.domain.model.PantoneFhiColor
import com.hue.domain.model.PantoneMatchResult
import com.hue.domain.repository.PantoneRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PantoneRepositoryImpl @Inject constructor(
    private val db: HueDatabase
) : PantoneRepository {

    override suspend fun findTopMatches(lab: LabColor, topN: Int): List<PantoneMatchResult> {
        val all = db.pantoneDao().getAll()
        return all
            .map { entity ->
                val entryLab = LabColor(entity.labL, entity.labA, entity.labB)
                val dE = DeltaE2000.compute(lab, entryLab)
                entity.toDomain() to dE
            }
            .sortedBy { it.second }
            .take(topN)
            .map { (color, dE) -> PantoneMatchResult(color, dE) }
    }

    override suspend fun getColorByCode(code: String): PantoneFhiColor? =
        db.pantoneDao().getByCode(code)?.toDomain()

    override fun searchByName(query: String): Flow<List<PantoneFhiColor>> =
        db.pantoneDao().searchByName(query).map { list -> list.map { it.toDomain() } }

    override suspend fun getTotalCount(): Int = db.pantoneDao().count()
}
