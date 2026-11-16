package com.nowstation.app.ride.domain

import com.nowstation.app.core.result.AppError
import com.nowstation.app.core.result.AppResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** 진행 중인 탑승. 서버에 닿지 못하면 마지막 상태를 두고 [online]만 내린다(지하에서 잠깐 끊겨도 화면이 비지 않게). */
data class ActiveRide(
    val origin: String,
    val destination: String?,
    val view: RideView,
    val online: Boolean,
    val updatedAt: Instant,
)

sealed interface RideNotice {
    /** 서버에서 탑승이 사라짐(서버 재시작 등). 앱도 끝낸다. */
    data object Lost : RideNotice
}

/**
 * 진행 중인 탑승 하나를 앱 전체가 같이 본다(화면, 포그라운드 서비스, 잠금화면 알림).
 * 화면이 사라져도 탑승은 계속돼야 해서 화면 ViewModel이 아니라 여기(@Singleton)가 갖는다.
 */
@Singleton
class RideTracker @Inject constructor(
    private val repository: RideRepository,
    private val clock: Clock,
) {
    private val _state = MutableStateFlow<ActiveRide?>(null)
    val state: StateFlow<ActiveRide?> = _state.asStateFlow()

    private val _notices = Channel<RideNotice>(Channel.BUFFERED)
    val notices: Flow<RideNotice> = _notices.receiveAsFlow()

    suspend fun start(origin: String, destination: String?): AppResult<RideView> {
        val result = repository.start(origin, destination)
        if (result is AppResult.Success) {
            _state.value = ActiveRide(origin, destination, result.data, online = true, updatedAt = Instant.now(clock))
        }
        return result
    }

    /** 포그라운드 서비스가 몇 초마다 부른다. */
    suspend fun refresh() {
        val ride = _state.value ?: return
        when (val result = repository.get(ride.view.rideId)) {
            is AppResult.Success -> apply(result.data)
            is AppResult.Error -> handleError(result.error)
        }
    }

    /** 폰이 느낀 출발·정차(4일차). 보낸 시각을 같이 보내 서버가 폰 시계 차이를 맞춘다. */
    suspend fun sendMotion(motions: List<Motion>) {
        val ride = _state.value ?: return
        when (val result = repository.sendMotion(ride.view.rideId, motions, Instant.now(clock))) {
            is AppResult.Success -> apply(result.data)
            is AppResult.Error -> handleError(result.error)
        }
    }

    /** 끝내기. 서버에 못 알려도 앱에서는 끝낸다(서버는 받아 오기 대상에서 빠지지 않을 뿐). */
    suspend fun end() {
        val ride = _state.value ?: return
        _state.value = null
        repository.end(ride.view.rideId)
    }

    private fun apply(view: RideView) {
        _state.update { it?.copy(view = view, online = true, updatedAt = Instant.now(clock)) }
    }

    private fun handleError(error: AppError) {
        if (error == AppError.NotFound) {
            _state.value = null
            _notices.trySend(RideNotice.Lost)
        } else {
            _state.update { it?.copy(online = false) }
        }
    }
}
