package com.nowstation.app.ride

import com.nowstation.app.core.result.AppResult
import com.nowstation.app.ride.domain.RideTracker
import com.nowstation.app.ride.domain.TrainChoice
import com.nowstation.app.ride.ui.TrainPickViewModel
import com.nowstation.app.testutil.FakeRideRepository
import com.nowstation.app.testutil.MainDispatcherRule
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrainPickViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val repo = FakeRideRepository()
    private val tracker = RideTracker(repo, Clock.systemUTC())
    private val mine = TrainChoice("수인분당선", "6050", "가천대", "ARRIVED", "왕십리", 12)

    @Test
    fun `고르는 동안 15초마다 다시 받고, 고르기가 끝나면 그만 받는다`() = runTest(main.dispatcher) {
        tracker.start("가천대", "선릉")
        repo.trainsResult = AppResult.Success(listOf(mine))
        val vm = TrainPickViewModel(tracker)
        runCurrent()
        assertEquals(listOf(mine), vm.state.value.trains)
        advanceTimeBy(15_001)
        assertEquals(2, repo.trainsCalls)

        tracker.skipChoosing()
        advanceTimeBy(60_000)
        assertEquals("탑승 화면으로 넘어간 뒤에는 목록을 받지 않는다(남은 화면 상태가 서버를 계속 부르던 버그)", 2, repo.trainsCalls)
    }

    @Test
    fun `탑승이 끝난 뒤에는 받지 않아 빈 목록이 다음 탑승에 남지 않는다`() = runTest(main.dispatcher) {
        tracker.start("가천대", "선릉")
        TrainPickViewModel(tracker)
        runCurrent()
        tracker.end()
        advanceTimeBy(60_000)
        assertEquals("끝난 탑승의 목록을 계속 받지 않는다", 1, repo.trainsCalls)
    }
}
