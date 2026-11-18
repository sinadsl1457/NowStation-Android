package com.nowstation.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowstation.app.core.ui.NowStationTheme
import com.nowstation.app.ride.domain.RideNotice
import com.nowstation.app.ride.domain.RideTracker
import com.nowstation.app.ride.ui.RideScreen
import com.nowstation.app.ride.ui.RideViewModel
import com.nowstation.app.station.ui.HomeActions
import com.nowstation.app.station.ui.HomeScreen
import com.nowstation.app.station.ui.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** 화면이 둘뿐이라 내비게이션 없이 "탑승 중이면 탑승 화면, 아니면 홈"으로 고른다. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var tracker: RideTracker

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 잠금화면 진행 중 알림에 필요. 거부해도 앱 화면으로는 쓸 수 있다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            NowStationTheme {
                val ride by tracker.state.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) {
                    tracker.notices.collect { notice ->
                        when (notice) {
                            RideNotice.Lost -> Toast.makeText(this@MainActivity, "서버에서 탑승 정보가 사라져 끝냈어요", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                val current = ride
                if (current != null) {
                    val vm: RideViewModel = hiltViewModel()
                    RideScreen(current, onEnd = vm::end)
                } else {
                    val vm: HomeViewModel = hiltViewModel()
                    val state by vm.state.collectAsStateWithLifecycle()
                    LaunchedEffect(state.message) {
                        state.message?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                            vm.dismissMessage()
                        }
                    }
                    val actions = remember(vm) {
                        HomeActions(vm::onQueryChange, vm::pick, vm::edit, vm::clearDestination, vm::start, vm::startRecent, vm::closeSearch)
                    }
                    HomeScreen(state, actions)
                }
            }
        }
    }
}
