package com.nowstation.app.ride

import com.nowstation.app.ride.domain.RideTracker
import com.nowstation.app.ride.ui.RideViewModel
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
class RideViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val repo = FakeRideRepository()
    private val tracker = RideTracker(repo, Clock.systemUTC())

    @Test
    fun `탑승 중에는 노선도를 15초마다 받고, 탑승이 끝나면 그만 받는다`() = runTest(main.dispatcher) {
        tracker.start("태평", "강남")
        RideViewModel(tracker)
        runCurrent()
        advanceTimeBy(15_001)
        assertEquals(2, repo.stripCalls)

        tracker.end()
        advanceTimeBy(60_000)
        assertEquals("끝난 탑승의 노선도를 계속 받지 않는다(남은 화면 상태가 서버를 계속 부르던 버그)", 2, repo.stripCalls)
    }
}
