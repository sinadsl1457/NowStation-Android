package com.nowstation.app.ride.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowstation.app.ride.domain.ActiveRide
import com.nowstation.app.ride.domain.RideTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 탑승 화면. 상태는 RideTracker(앱 전체 공유)를 그대로 본다. */
@HiltViewModel
class RideViewModel @Inject constructor(private val tracker: RideTracker) : ViewModel() {
    val ride: StateFlow<ActiveRide?> = tracker.state

    fun end() {
        viewModelScope.launch { tracker.end() }
    }
}
