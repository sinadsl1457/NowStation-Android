package com.nowstation.app.station

import com.nowstation.app.core.result.AppError
import com.nowstation.app.core.result.AppResult
import com.nowstation.app.ride.domain.RideTracker
import com.nowstation.app.station.domain.Station
import com.nowstation.app.station.ui.HomeViewModel
import com.nowstation.app.station.ui.PickTarget
import com.nowstation.app.testutil.FakeLauncher
import com.nowstation.app.testutil.FakeRideRepository
import com.nowstation.app.testutil.FakeStationRepository
import com.nowstation.app.testutil.MainDispatcherRule
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock

class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val stations = FakeStationRepository(listOf(Station("태평", listOf("수인분당선")), Station("강남", listOf("2호선", "신분당선"))))
    private val rides = FakeRideRepository()
    private val launcher = FakeLauncher()
    private val vm by lazy { HomeViewModel(stations, RideTracker(rides, Clock.systemUTC()), launcher) }

    @Test
    fun `입력이 멈추면 한 번만 찾는다`() = runTest(main.dispatcher) {
        vm.onQueryChange("태")
        vm.onQueryChange("태평")
        advanceTimeBy(HomeViewModel.SEARCH_DELAY_MILLIS + 1)

        assertEquals(listOf("", "태평"), stations.queries)
        assertEquals(listOf("태평"), vm.state.value.results.map { it.name })
    }

    @Test
    fun `타는 역을 고르면 내릴 역 고르기로 넘어간다`() = runTest(main.dispatcher) {
        advanceUntilIdle()
        vm.pick(stations.stations[0])

        assertEquals("태평", vm.state.value.origin?.name)
        assertEquals(PickTarget.DESTINATION, vm.state.value.target)
        assertTrue(vm.state.value.canStart)
    }

    @Test
    fun `시작에 성공하면 서비스를 띄우고 홈을 비운다`() = runTest(main.dispatcher) {
        advanceUntilIdle()
        vm.pick(stations.stations[0])

        vm.start()
        advanceUntilIdle()

        assertEquals(1, launcher.started)
        assertNull(vm.state.value.origin)
    }

    @Test
    fun `시작에 실패하면 서버가 준 이유를 보여 준다`() = runTest(main.dispatcher) {
        rides.startResult = AppResult.Error(AppError.BadRequest("모르는 역: 태평"))
        advanceUntilIdle()
        vm.pick(stations.stations[0])

        vm.start()
        advanceUntilIdle()

        assertEquals(0, launcher.started)
        assertEquals("모르는 역: 태평", vm.state.value.message)
    }

    @Test
    fun `탑승을 시작해 홈을 비워도 역 목록은 다시 보여 준다(탑승을 끝내고 돌아왔을 때)`() = runTest(main.dispatcher) {
        advanceUntilIdle()
        vm.pick(stations.stations[0])

        vm.start()
        advanceUntilIdle()

        assertEquals(2, vm.state.value.results.size)
    }
}
