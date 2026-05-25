package com.hue.domain.usecase

import com.hue.domain.model.FabricAnalysis
import com.hue.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    operator fun invoke(): Flow<List<FabricAnalysis>> = historyRepository.getAllScans()
}

class SaveScanUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(analysis: FabricAnalysis): Long =
        historyRepository.saveScan(analysis)
}

class DeleteScanUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(id: Long) = historyRepository.deleteScan(id)
}

class GetScanByIdUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(id: Long): FabricAnalysis? = historyRepository.getScanById(id)
}
