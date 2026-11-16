package com.nowstation.app.station.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowstation.app.core.result.AppError
import com.nowstation.app.core.result.AppResult
import com.nowstation.app.ride.domain.RideTracker
import com.nowstation.app.ride.service.RideServiceLauncher
import com.nowstation.app.station.domain.Station
import com.nowstation.app.station.domain.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 지금 고르는 칸: 출발역(필수) 또는 내릴 역(선택). */
enum class PickTarget { ORIGIN, DESTINATION }

data class HomeUiState(
    val target: PickTarget = PickTarget.ORIGIN,
    val query: String = "",
    val results: List<Station> = emptyList(),
    val origin: Station? = null,
    val destination: Station? = null,
    val isStarting: Boolean = false,
    val message: String? = null,
) {
    val canStart: Boolean get() = origin != null && !isStarting
}

/** 홈: 역을 검색해 출발역(과 내릴 역)을 고르고 탑승을 시작한다. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stations: StationRepository,
    private val tracker: RideTracker,
    private val launcher: RideServiceLauncher,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        search("")
    }

    /** 입력이 잠깐 멈추면 찾는다(글자마다 서버를 부르지 않게). */
    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DELAY_MILLIS)
            search(query)
        }
    }

    fun pick(station: Station) {
        _state.update {
            when (it.target) {
                // 출발역을 고르면 바로 내릴 역(선택) 고르기로 넘어간다
                PickTarget.ORIGIN -> it.copy(origin = station, target = PickTarget.DESTINATION, query = "")
                PickTarget.DESTINATION -> it.copy(destination = station, query = "")
            }
        }
        search("")
    }

    fun edit(target: PickTarget) {
        _state.update { it.copy(target = target, query = "") }
        search("")
    }

    fun clearDestination() = _state.update { it.copy(destination = null) }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun start() {
        val current = _state.value
        val origin = current.origin ?: return
        if (current.isStarting) return
        _state.update { it.copy(isStarting = true) }
        viewModelScope.launch {
            when (val result = tracker.start(origin.name, current.destination?.name)) {
                is AppResult.Success -> {
                    launcher.start()
                    // 탑승을 끝내고 홈으로 돌아왔을 때 빈 목록이 보이지 않게 다시 채운다
                    _state.update { HomeUiState() }
                    search("")
                }
                is AppResult.Error -> _state.update { it.copy(isStarting = false, message = messageOf(result.error)) }
            }
        }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            when (val result = stations.search(query.trim())) {
                is AppResult.Success -> _state.update { if (it.query.trim() == query.trim()) it.copy(results = result.data) else it }
                is AppResult.Error -> _state.update { it.copy(message = messageOf(result.error)) }
            }
        }
    }

    private fun messageOf(error: AppError) = when (error) {
        AppError.Network -> "서버에 연결할 수 없어요"
        is AppError.BadRequest -> error.message
        else -> "잠시 후 다시 시도해 주세요"
    }

    companion object {
        const val SEARCH_DELAY_MILLIS = 250L
    }
}
