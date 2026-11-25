package com.nmanojlovski.briefing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nmanojlovski.briefing.ui.theme.BriefingTheme
import kotlinx.serialization.Serializable

// The app's screens ("routes"). A route is like an address that Navigation can go to and come back from.
@Serializable data object LatestRoute
@Serializable data object HistoryRoute
@Serializable data class ReaderRoute(val title: String, val url: String)

class MainActivity : ComponentActivity() {
    // `by viewModels()` creates the ViewModel once and gives back the same one after a rotation.
    private val viewModel: BriefingViewModel by viewModels()

    // Shows Android's "Allow notifications?" dialog. We don't need to react to the answer.
    private val askNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Since Android 13 an app must ask before it may show notifications.
        if (savedInstanceState == null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            BriefingTheme {
                BriefingApp(viewModel)
            }
        }
    }

    // Runs every time the app comes to the foreground, so the new morning's briefing
    // shows up even if the app was left open in the background yesterday.
    override fun onStart() {
        super.onStart()
        viewModel.refresh()
        scheduleDailyCheck(this)
    }
}

@Composable
fun BriefingApp(viewModel: BriefingViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = LatestRoute) {
        composable<LatestRoute> {
            LatestScreen(
                state = viewModel.state,
                onRetry = viewModel::refresh,
                onShowAll = { navController.navigate(HistoryRoute) }
            )
        }
        composable<HistoryRoute> {
            HistoryScreen(
                state = viewModel.state,
                isRefreshing = viewModel.isRefreshing,
                onRefresh = viewModel::refresh,
                onOpen = { entry -> navController.navigate(ReaderRoute(entry.title, entry.fullUrl)) },
                onBack = { navController.navigateUp() }
            )
        }
        composable<ReaderRoute> { backStackEntry ->
            val route: ReaderRoute = backStackEntry.toRoute()
            ReaderScreen(title = route.title, url = route.url, onBack = { navController.navigateUp() })
        }
    }
}
