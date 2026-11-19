package com.nowstation.app.ride.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowstation.app.core.result.AppResult
import com.nowstation.app.ride.domain.RideTracker
import com.nowstation.app.ride.domain.TrainChoice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrainPickUiState(
    val trains: List<TrainChoice> = emptyList(),
    val loaded: Boolean = false,
    val failed: Boolean = false,
    /** 고르는 중인 열차(누른 줄에 진행 표시, 다른 버튼 잠금). */
    val picking: TrainChoice? = null,
    val pickFailed: Boolean = false,
)

/**
 * 탑승 시작 직후 "지금 탄 열차" 고르기. 목록은 이 화면만 쓰고 화면과 함께 사라져서 RideTracker가 아니라 여기 둔다.
 * 공공 데이터가 늦어 15초마다 다시 받는다. iOS TrainPickScreen과 같은 흐름.
 */
@HiltViewModel
class TrainPickViewModel @Inject constructor(private val tracker: RideTracker) : ViewModel() {
    private val _state = MutableStateFlow(TrainPickUiState())
    val state: StateFlow<TrainPickUiState> = _state.asStateFlow()

    /** 이 화면이 고르는 탑승. 그 탑승을 고르는 동안만 받는다(고른 뒤·끝난 뒤에도 남아 서버를 계속 부르고, 빈 목록이 다음 탑승에 보였다). */
    private val rideId = tracker.state.value?.view?.rideId

    init {
        viewModelScope.launch {
            while (isActive && stillChoosing()) {
                refresh()
                delay(REFRESH_MS)
            }
        }
    }

    private fun stillChoosing() = tracker.state.value?.let { it.choosing && it.view.rideId == rideId } == true

    private suspend fun refresh() {
        when (val r = tracker.trains()) {
            is AppResult.Success -> _state.update { it.copy(trains = r.data, loaded = true, failed = false) }
            is AppResult.Error -> _state.update { it.copy(loaded = true, failed = true) }
        }
    }

    fun pick(train: TrainChoice) {
        if (_state.value.picking != null) return
        _state.update { it.copy(picking = train) }
        viewModelScope.launch {
            val r = tracker.choose(train)
            _state.update { it.copy(picking = null, pickFailed = r is AppResult.Error) }
            if (r is AppResult.Error) refresh()
        }
    }

    fun dismissPickFailed() = _state.update { it.copy(pickFailed = false) }

    fun unsure() = tracker.skipChoosing()

    /** 아직 안 탔으면 탑승을 없던 일로 한다(경로는 홈이 다시 채운다). */
    fun notYet(onDone: () -> Unit) {
        viewModelScope.launch {
            tracker.end()
            onDone()
        }
    }

    private companion object {
        const val REFRESH_MS = 15_000L
    }
}
