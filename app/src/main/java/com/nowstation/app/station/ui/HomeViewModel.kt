package com.nowstation.app.station.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowstation.app.core.result.AppError
import com.nowstation.app.core.result.AppResult
import com.nowstation.app.ride.domain.RideTracker
import com.nowstation.app.ride.service.RideServiceLauncher
import com.nowstation.app.station.domain.RecentRoute
import com.nowstation.app.station.domain.RecentRouteStore
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
    /** 역 검색 화면이 열려 있나(홈 위에 덮음). */
    val searching: Boolean = false,
    val query: String = "",
    val results: List<Station> = emptyList(),
    val origin: Station? = null,
    val destination: Station? = null,
    val recent: List<RecentRoute> = emptyList(),
    /** 지원 노선. 받아 오지 못하면 비워 두고 홈에서 그 칸을 숨긴다. */
    val lines: List<String> = emptyList(),
    val isStarting: Boolean = false,
    val message: String? = null,
) {
    /** 같은 역을 두 칸에 고름(바꾸기·다시 고르다 생김). 시작하지 않고 카드에 알린다. */
    val sameStation: Boolean get() = origin != null && origin.name == destination?.name
    val canStart: Boolean get() = origin != null && !isStarting && !sameStation
    val canSwap: Boolean get() = (origin != null || destination != null) && !isStarting
}

/** 홈: 역을 검색해 출발역(과 내릴 역)을 고르고 탑승을 시작한다. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stations: StationRepository,
    private val tracker: RideTracker,
    private val launcher: RideServiceLauncher,
    private val recentStore: RecentRouteStore,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        _state.update { it.copy(recent = recentStore.load()) }
        search("")
        viewModelScope.launch {
            (stations.lines() as? AppResult.Success)?.let { r -> _state.update { it.copy(lines = r.data) } }
        }
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

    /** 고르면 검색을 닫고 홈으로. 내릴 역은 선택이라 자동으로 열지 않는다. */
    fun pick(station: Station) {
        _state.update {
            when (it.target) {
                PickTarget.ORIGIN -> it.copy(origin = station, query = "", searching = false)
                PickTarget.DESTINATION -> it.copy(destination = station, query = "", searching = false)
            }
        }
        search("")
    }

    fun edit(target: PickTarget) {
        _state.update { it.copy(target = target, query = "", searching = true) }
        search("")
    }

    fun closeSearch() {
        _state.update { it.copy(query = "", searching = false) }
        search("")
    }

    /** 타는 역과 내릴 역을 바꾼다(태평 → 강남으로 출근했으면 퇴근길은 강남 → 태평). */
    fun swap() = _state.update { it.copy(origin = it.destination, destination = it.origin) }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    /** 최근 경로를 눌러 바로 시작. */
    fun startRecent(route: RecentRoute) {
        if (_state.value.isStarting) return
        _state.update { it.copy(origin = route.origin, destination = route.destination) }
        start()
    }

    fun start() {
        val current = _state.value
        val origin = current.origin ?: return
        if (!current.canStart) return
        _state.update { it.copy(isStarting = true) }
        viewModelScope.launch {
            when (val result = tracker.start(origin.name, current.destination?.name)) {
                is AppResult.Success -> {
                    launcher.start()
                    val recent = RecentRoute.push(current.recent, RecentRoute(origin, current.destination))
                    recentStore.save(recent)
                    // 탑승을 끝내고 홈으로 돌아왔을 때 빈 목록이 보이지 않게 다시 채운다
                    _state.update { HomeUiState(recent = recent, lines = it.lines) }
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
