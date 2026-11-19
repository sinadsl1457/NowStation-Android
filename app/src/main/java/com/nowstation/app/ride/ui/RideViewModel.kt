package com.nowstation.app.ride.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowstation.app.core.result.AppResult
import com.nowstation.app.ride.domain.ActiveRide
import com.nowstation.app.ride.domain.RideTracker
import com.nowstation.app.ride.domain.StripTrain
import com.nowstation.app.ride.domain.TrainStrip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 탑승 화면. 탑승 상태는 RideTracker(앱 전체 공유)를 그대로 보고, 가로 노선도는 이 화면만 쓰므로 여기서 15초마다 받는다.
 * 탑승마다 새로 만든다(MainActivity가 rideId를 키로): 끝난 탑승의 노선도를 계속 받지 않게.
 */
@HiltViewModel
class RideViewModel @Inject constructor(private val tracker: RideTracker) : ViewModel() {
    val ride: StateFlow<ActiveRide?> = tracker.state

    private val _strip = MutableStateFlow<TrainStrip?>(null)
    val strip: StateFlow<TrainStrip?> = _strip.asStateFlow()

    private val _picking = MutableStateFlow<String?>(null)
    val picking: StateFlow<String?> = _picking.asStateFlow()

    private val rideId = tracker.state.value?.view?.rideId

    init {
        viewModelScope.launch {
            while (isActive && tracker.state.value?.view?.rideId == rideId && rideId != null) {
                refresh()
                delay(REFRESH_MS)
            }
        }
    }

    private suspend fun refresh() {
        (tracker.strip() as? AppResult.Success)?.let { _strip.value = it.data }
    }

    fun pick(train: StripTrain) {
        val line = _strip.value?.leg?.line ?: return
        if (_picking.value != null || train.selected) return
        _picking.value = train.trainNo
        viewModelScope.launch {
            tracker.choose(train, line)
            refresh()
            _picking.value = null
        }
    }

    fun end() {
        viewModelScope.launch { tracker.end() }
    }

    private companion object {
        const val REFRESH_MS = 15_000L
    }
}
