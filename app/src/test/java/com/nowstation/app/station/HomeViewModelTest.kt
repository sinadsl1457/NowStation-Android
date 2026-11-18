package com.nowstation.app.station

import com.nowstation.app.core.result.AppError
import com.nowstation.app.core.result.AppResult
import com.nowstation.app.ride.domain.RideTracker
import com.nowstation.app.station.domain.Station
import com.nowstation.app.station.ui.HomeViewModel
import com.nowstation.app.station.ui.PickTarget
import com.nowstation.app.station.domain.RecentRoute
import com.nowstation.app.testutil.FakeLauncher
import com.nowstation.app.testutil.FakeRecentRouteStore
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
    private val recent = FakeRecentRouteStore()
    private val vm by lazy { HomeViewModel(stations, RideTracker(rides, Clock.systemUTC()), launcher, recent) }

    @Test
    fun `입력이 멈추면 한 번만 찾는다`() = runTest(main.dispatcher) {
        vm.onQueryChange("태")
        vm.onQueryChange("태평")
        advanceTimeBy(HomeViewModel.SEARCH_DELAY_MILLIS + 1)

        assertEquals(listOf("", "태평"), stations.queries)
        assertEquals(listOf("태평"), vm.state.value.results.map { it.name })
    }

    @Test
    fun `칸을 누르면 검색이 열리고, 역을 고르면 닫혀 홈으로 돌아간다`() = runTest(main.dispatcher) {
        advanceUntilIdle()
        vm.edit(PickTarget.ORIGIN)
        assertTrue(vm.state.value.searching)

        vm.pick(stations.stations[0])

        assertEquals("태평", vm.state.value.origin?.name)
        assertEquals(false, vm.state.value.searching)
        assertTrue(vm.state.value.canStart)
    }

    @Test
    fun `지원 노선을 받아 온다`() = runTest(main.dispatcher) {
        vm // 만들어질 때 받아 온다
        advanceUntilIdle()
        assertEquals(listOf("2호선", "수인분당선", "신분당선"), vm.state.value.lines)
    }

    @Test
    fun `지원 노선을 못 받아도 오류를 띄우지 않는다`() = runTest(main.dispatcher) {
        stations.linesResult = AppResult.Error(AppError.Network)
        vm
        advanceUntilIdle()
        assertEquals(emptyList<String>(), vm.state.value.lines)
        assertNull(vm.state.value.message)
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

    @Test
    fun `시작에 성공한 경로만 최근 경로 맨 앞에 저장한다`() = runTest(main.dispatcher) {
        recent.routes = listOf(RecentRoute(stations.stations[1], null))
        advanceUntilIdle()
        vm.pick(stations.stations[0])
        vm.start()
        advanceUntilIdle()

        assertEquals(listOf("태평", "강남"), recent.routes.map { it.origin.name })
        assertEquals(listOf("태평", "강남"), vm.state.value.recent.map { it.origin.name })

        rides.startResult = AppResult.Error(AppError.Network)
        vm.pick(stations.stations[1])
        vm.start()
        advanceUntilIdle()
        assertEquals(listOf("태평", "강남"), recent.routes.map { it.origin.name })
    }

    @Test
    fun `최근 경로를 누르면 그 역들로 바로 시작한다`() = runTest(main.dispatcher) {
        val route = RecentRoute(stations.stations[0], stations.stations[1])
        recent.routes = listOf(route)
        advanceUntilIdle()

        vm.startRecent(vm.state.value.recent[0])
        advanceUntilIdle()

        assertEquals(1, launcher.started)
        assertEquals("태평" to "강남", rides.lastStart)
    }
}
