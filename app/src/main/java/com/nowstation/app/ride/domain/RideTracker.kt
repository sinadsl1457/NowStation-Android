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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    /** 아직 서버에 못 보낸 움직임 수(지하에서 끊김). */
    val unsentMotions: Int = 0,
    /** 탑승을 시작한 시각(화면에 "08:42 탑승"). */
    val startedAt: Instant? = null,
) {
    companion object {
        /** 내릴 역 알림이 막 켜졌나. 환승 뒤 다시 켜지면 또 울린다. */
        fun alightAlertStarted(before: ActiveRide?, after: ActiveRide?): Boolean =
            after?.view?.prepareToAlight == true &&
                (before?.view?.prepareToAlight != true || before.view.rideId != after.view.rideId)
    }
}

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

    /** 보낼 움직임. 지하에서 끊기면 쌓였다가 다음에 한꺼번에 간다(서버는 늦게 온 움직임도 시각대로 판단한다). */
    private val pending = MutableStateFlow<List<Motion>>(emptyList())

    /** 갱신 반복과 센서가 동시에 보내도 같은 움직임을 두 번 보내지 않게 한 번에 하나씩. */
    private val syncLock = Mutex()

    suspend fun start(origin: String, destination: String?): AppResult<RideView> {
        val result = repository.start(origin, destination)
        if (result is AppResult.Success) {
            pending.value = emptyList()
            val now = Instant.now(clock)
            _state.value = ActiveRide(origin, destination, result.data, online = true, updatedAt = now, startedAt = now)
        }
        return result
    }

    /** 들어오는 열차 목록(세로 노선도). 탑승 화면이 5초마다 부른다. */
    suspend fun strip(): AppResult<TrainStrip?> {
        val ride = _state.value ?: return AppResult.Success(null)
        return repository.strip(ride.view.rideId)
    }

    /** 노선도에서 다른 열차를 고른다(골라 둔 열차가 틀렸을 때). */
    suspend fun choose(train: StripTrain, line: String): AppResult<RideView> {
        val ride = _state.value ?: return AppResult.Error(AppError.NotFound)
        val result = repository.choose(ride.view.rideId, line, train.trainNo)
        if (result is AppResult.Success) {
            _state.update {
                if (it?.view?.rideId != ride.view.rideId) it
                else it.copy(view = result.data, online = true, updatedAt = Instant.now(clock))
            }
        }
        return result
    }

    /** 포그라운드 서비스가 몇 초마다 부른다. 못 보낸 움직임이 있으면 그것부터 보낸다. */
    suspend fun refresh() = sync()

    /** 폰이 느낀 출발·정차. 바로 보내고, 못 보내면 쌓아 둔다. */
    suspend fun reportMotion(motion: Motion) {
        if (_state.value == null) return
        pending.update { (it + motion).takeLast(MAX_PENDING) }
        _state.update { it?.copy(unsentMotions = pending.value.size) }
        sync()
    }

    /** 끝내기. 서버에 못 알려도 앱에서는 끝낸다(서버는 받아 오기 대상에서 빠지지 않을 뿐). */
    suspend fun end() {
        val ride = _state.value ?: return
        _state.value = null
        pending.value = emptyList()
        repository.end(ride.view.rideId)
    }

    private suspend fun sync() = syncLock.withLock {
        val ride = _state.value ?: return@withLock
        val batch = pending.value
        val result = if (batch.isEmpty()) {
            repository.get(ride.view.rideId)
        } else {
            // 보낸 시각을 같이 보내 서버가 폰 시계 차이를 맞춘다
            repository.sendMotion(ride.view.rideId, batch, Instant.now(clock))
        }
        when (result) {
            is AppResult.Success -> {
                pending.update { it.drop(batch.size) }
                apply(result.data)
            }
            is AppResult.Error -> {
                // 서버가 거절한 움직임은 다시 보내도 같으니 버린다(계속 막히지 않게)
                if (result.error is AppError.BadRequest) pending.update { it.drop(batch.size) }
                handleError(result.error)
            }
        }
    }

    private fun apply(view: RideView) {
        _state.update {
            it?.copy(view = view, online = true, updatedAt = Instant.now(clock), unsentMotions = pending.value.size)
        }
    }

    private fun handleError(error: AppError) {
        if (error == AppError.NotFound) {
            _state.value = null
            pending.value = emptyList()
            _notices.trySend(RideNotice.Lost)
        } else {
            _state.update { it?.copy(online = false, unsentMotions = pending.value.size) }
        }
    }

    private companion object {
        /** 오래 끊겨도 메모리가 늘지 않게. 판단에 쓰이는 건 최근 몇 개뿐이다. */
        const val MAX_PENDING = 50
    }
}
