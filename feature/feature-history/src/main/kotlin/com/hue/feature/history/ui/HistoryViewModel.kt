package com.hue.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hue.core.color.model.Season
import com.hue.domain.model.FabricAnalysis
import com.hue.domain.usecase.DeleteScanUseCase
import com.hue.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val scans: List<FabricAnalysis> = emptyList(),
    val isLoading: Boolean = true,
    val filterSeason: Season? = null,
    val searchQuery: String = ""
) {
    val filteredScans: List<FabricAnalysis>
        get() = scans
            .filter { filterSeason == null || it.season.primarySeason == filterSeason }
            .filter { searchQuery.isBlank() ||
                      it.season.primarySeason.displayName.contains(searchQuery, ignoreCase = true) ||
                      it.topMatches.firstOrNull()?.color?.name?.contains(searchQuery, ignoreCase = true) == true }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistory: GetHistoryUseCase,
    private val deleteScan: DeleteScanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getHistory()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { /* log */ }
                .collect { scans ->
                    _uiState.update { it.copy(scans = scans, isLoading = false) }
                }
        }
    }

    fun setFilter(season: Season?) {
        _uiState.update { it.copy(filterSeason = season) }
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { deleteScan(id) }
    }
}
