package com.hue.feature.matching.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hue.domain.model.*
import com.hue.domain.usecase.AnalyseFabricUseCase
import com.hue.domain.usecase.SaveScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ResultsUiState {
    object Idle : ResultsUiState()
    object Loading : ResultsUiState()
    data class Success(
        val analysis: FabricAnalysis,
        val selectedMatchIndex: Int = 0,
        val isSaved: Boolean = false,
        val showAdvanced: Boolean = false
    ) : ResultsUiState()
    data class Error(val message: String) : ResultsUiState()
}

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val analyseFabric: AnalyseFabricUseCase,
    private val saveScan: SaveScanUseCase,
    @Suppress("UNUSED_PARAMETER") savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultsUiState>(ResultsUiState.Idle)
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    fun analyse(croppedPath: String) {
        _uiState.value = ResultsUiState.Loading
        viewModelScope.launch {
            analyseFabric(AnalysisInput(croppedPath))
                .onSuccess { analysis ->
                    _uiState.value = ResultsUiState.Success(analysis)
                }
                .onFailure { err ->
                    _uiState.value = ResultsUiState.Error(
                        err.message ?: "Analysis failed. Please try again."
                    )
                }
        }
    }

    fun selectMatch(index: Int) {
        val current = _uiState.value as? ResultsUiState.Success ?: return
        _uiState.value = current.copy(selectedMatchIndex = index)
    }

    fun toggleAdvanced() {
        val current = _uiState.value as? ResultsUiState.Success ?: return
        _uiState.value = current.copy(showAdvanced = !current.showAdvanced)
    }

    fun saveToHistory() {
        val current = _uiState.value as? ResultsUiState.Success ?: return
        if (current.isSaved) return
        viewModelScope.launch {
            saveScan(current.analysis)
            _uiState.value = current.copy(isSaved = true)
        }
    }
}
