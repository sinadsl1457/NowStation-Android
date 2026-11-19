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

    private fun pickRoute(origin: Station, destination: Station) {
        vm.edit(PickTarget.ORIGIN)
        vm.pick(origin)
        vm.edit(PickTarget.DESTINATION)
        vm.pick(destination)
    }

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
        assertEquals("내릴 역도 골라야 시작", false, vm.state.value.canStart)

        vm.edit(PickTarget.DESTINATION)
        vm.pick(stations.stations[1])
        assertTrue(vm.state.value.canStart)
    }

    @Test
    fun `바꾸기를 누르면 타는 역과 내릴 역이 서로 바뀐다(퇴근길)`() = runTest(main.dispatcher) {
        advanceUntilIdle()
        assertEquals(false, vm.state.value.canSwap)
        vm.edit(PickTarget.ORIGIN)
        vm.pick(stations.stations[0])
        vm.edit(PickTarget.DESTINATION)
        vm.pick(stations.stations[1])

        vm.swap()

        assertEquals("강남", vm.state.value.origin?.name)
        assertEquals("태평", vm.state.value.destination?.name)
    }

    @Test
    fun `내릴 역을 고르지 않으면 시작할 수 없다`() = runTest(main.dispatcher) {
        advanceUntilIdle()
        vm.edit(PickTarget.ORIGIN)
        vm.pick(stations.stations[0])

        assertEquals(false, vm.state.value.canStart)
        vm.start()
        advanceUntilIdle()
        assertEquals(null, rides.lastStart)
    }

    @Test
    fun `내릴 역 없이 저장된 옛 최근 경로는 타는 역만 채운다`() = runTest(main.dispatcher) {
        recent.routes = listOf(RecentRoute(stations.stations[0], null))
        advanceUntilIdle()

        vm.useRecent(vm.state.value.recent[0])
        advanceUntilIdle()

        assertEquals(null, rides.lastStart)
        assertEquals("태평", vm.state.value.origin?.name)
        assertEquals(null, vm.state.value.destination)
        assertEquals(false, vm.state.value.canStart)
    }

    @Test
    fun `타는 역과 내릴 역이 같으면 시작할 수 없다`() = runTest(main.dispatcher) {
        advanceUntilIdle()
        vm.edit(PickTarget.ORIGIN)
        vm.pick(stations.stations[1])
        vm.edit(PickTarget.DESTINATION)
        vm.pick(stations.stations[1])

        assertTrue(vm.state.value.sameStation)
        assertEquals(false, vm.state.value.canStart)
        vm.start()
        advanceUntilIdle()
        assertEquals(null, rides.lastStart)
    }

    @Test
    fun `내릴 역만 골랐을 때 바꾸면 타는 역이 된다`() = runTest(main.dispatcher) {
        advanceUntilIdle()
        vm.edit(PickTarget.DESTINATION)
        vm.pick(stations.stations[1])
        assertTrue(vm.state.value.canSwap)

        vm.swap()

        assertEquals("강남", vm.state.value.origin?.name)
        assertEquals(null, vm.state.value.destination)
        assertEquals(false, vm.state.value.canStart)
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
        pickRoute(stations.stations[0], stations.stations[1])

        vm.start()
        advanceUntilIdle()

        assertEquals(1, launcher.started)
        assertNull(vm.state.value.origin)
    }

    @Test
    fun `시작에 실패하면 서버가 준 이유를 보여 준다`() = runTest(main.dispatcher) {
        rides.startResult = AppResult.Error(AppError.BadRequest("모르는 역: 태평"))
        advanceUntilIdle()
        pickRoute(stations.stations[0], stations.stations[1])

        vm.start()
        advanceUntilIdle()

        assertEquals(0, launcher.started)
        assertEquals("모르는 역: 태평", vm.state.value.message)
    }

    @Test
    fun `탑승을 시작해 홈을 비워도 역 목록은 다시 보여 준다(탑승을 끝내고 돌아왔을 때)`() = runTest(main.dispatcher) {
        advanceUntilIdle()
        pickRoute(stations.stations[0], stations.stations[1])

        vm.start()
        advanceUntilIdle()

        assertEquals(2, vm.state.value.results.size)
    }

    @Test
    fun `시작에 성공한 경로만 최근 경로 맨 앞에 저장한다`() = runTest(main.dispatcher) {
        recent.routes = listOf(RecentRoute(stations.stations[1], null))
        advanceUntilIdle()
        pickRoute(stations.stations[0], stations.stations[1])
        vm.start()
        advanceUntilIdle()

        assertEquals(listOf("태평", "강남"), recent.routes.map { it.origin.name })
        assertEquals(listOf("태평", "강남"), vm.state.value.recent.map { it.origin.name })

        rides.startResult = AppResult.Error(AppError.Network)
        pickRoute(stations.stations[1], stations.stations[0])
        vm.start()
        advanceUntilIdle()
        assertEquals(listOf("태평", "강남"), recent.routes.map { it.origin.name })
    }

    @Test
    fun `최근 경로를 누르면 칸에만 채우고, 시작은 사용자가 누른다(바꾸기로 귀갓길)`() = runTest(main.dispatcher) {
        recent.routes = listOf(RecentRoute(stations.stations[0], stations.stations[1]))
        advanceUntilIdle()

        vm.useRecent(vm.state.value.recent[0])
        advanceUntilIdle()
        assertEquals(0, launcher.started)
        assertEquals(null, rides.lastStart)
        assertEquals("태평", vm.state.value.origin?.name)
        assertEquals("강남", vm.state.value.destination?.name)

        vm.swap()
        vm.start()
        advanceUntilIdle()
        assertEquals("강남" to "태평", rides.lastStart)
    }
}
