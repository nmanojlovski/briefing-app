package com.nmanojlovski.briefing

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nmanojlovski.briefing.ui.theme.BriefingTheme
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

// The app's screens ("routes"). A route is like an address that Navigation can go to and come back from.
@Serializable data object HomeRoute
@Serializable data object HistoryRoute
@Serializable data class ReaderRoute(val title: String, val url: String)
@Serializable data object WheelRoute

// Put on the notification's tap action, so the app can open that briefing straight away.
const val EXTRA_TITLE = "briefing_title"
const val EXTRA_URL = "briefing_url"

// Android's default see-through colours behind the navigation bar buttons, in light and dark mode.
private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

class MainActivity : ComponentActivity() {
    // `by viewModels()` creates the ViewModel once and gives back the same one after a rotation.
    private val viewModel: BriefingViewModel by viewModels()

    // Shows Android's "Allow notifications?" dialog. We don't need to react to the answer.
    private val askNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the system bars, with light status bar icons: every screen has a navy top.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        // Since Android 13 an app must ask before it may show notifications.
        if (savedInstanceState == null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Opened by tapping the notification? Then show that briefing on top of the main menu.
        // Only on a fresh start: after a rotation, Navigation already remembers where you were.
        // The URL check makes sure no other app can make us open some random page.
        val title = intent.getStringExtra(EXTRA_TITLE)
        val url = intent.getStringExtra(EXTRA_URL)
        val openFirst = if (savedInstanceState == null && title != null && url != null && url.startsWith(BASE_URL)) {
            ReaderRoute(title, url)
        } else {
            null
        }

        setContent {
            // The sun/moon button on the main menu decides light or dark.
            val dark = when (viewModel.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Switching light/dark: a navy curtain fades in, the colours change behind it,
            // we wait a moment for everything to repaint, and the curtain fades out again.
            // null = not switching; true/false = switching to dark/light.
            var switchingToDark by remember { mutableStateOf<Boolean?>(null) }
            val curtain by animateFloatAsState(
                targetValue = if (switchingToDark != null) 1f else 0f,
                animationSpec = tween(durationMillis = 250),
                label = "curtain"
            )
            LaunchedEffect(switchingToDark) {
                val toDark = switchingToDark ?: return@LaunchedEffect
                delay(250)                    // the curtain is fully closed
                viewModel.setDarkMode(toDark) // the colours change behind it
                delay(300)                    // let the screen settle
                switchingToDark = null        // open the curtain again
            }

            // The navigation bar at the bottom follows light/dark too (the status bar is always on navy).
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark }
                )
            }

            BriefingTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize()) {
                    BriefingApp(
                        viewModel,
                        openFirst,
                        isDark = dark,
                        onToggleTheme = { if (switchingToDark == null) switchingToDark = !dark }
                    )
                    ThemeCurtain(alpha = curtain, moon = switchingToDark ?: dark)
                }
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
fun BriefingApp(
    viewModel: BriefingViewModel,
    openFirst: ReaderRoute?,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    LaunchedEffect(Unit) {
        if (openFirst != null) navController.navigate(openFirst)
    }
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        // New screens slide in from the right; going back slides them out again.
        enterTransition = { slideIntoContainer(SlideDirection.Start) + fadeIn() },
        exitTransition = { slideOutOfContainer(SlideDirection.Start, targetOffset = { it / 4 }) + fadeOut() },
        popEnterTransition = { slideIntoContainer(SlideDirection.End, initialOffset = { it / 4 }) + fadeIn() },
        popExitTransition = { slideOutOfContainer(SlideDirection.End) + fadeOut() }
    ) {
        composable<HomeRoute> {
            HomeScreen(
                state = viewModel.state,
                isDark = isDark,
                onShowAll = { navController.navigate(HistoryRoute) },
                onOpenWheel = { navController.navigate(WheelRoute) },
                onToggleTheme = onToggleTheme
            )
        }
        composable<HistoryRoute> {
            HistoryScreen(
                state = viewModel.state,
                archived = viewModel.archived,
                isRefreshing = viewModel.isRefreshing,
                onRefresh = viewModel::refresh,
                onOpen = { entry -> navController.navigate(ReaderRoute(entry.title, entry.fullUrl)) },
                onBack = { navController.navigateUp() }
            )
        }
        composable<ReaderRoute> { backStackEntry ->
            val route: ReaderRoute = backStackEntry.toRoute()
            ReaderScreen(
                title = route.title,
                url = route.url,
                archived = briefingFileName(route.url) in viewModel.archived,
                onToggleArchive = { viewModel.toggleArchive(route.url) },
                onBack = { navController.navigateUp() }
            )
        }
        composable<WheelRoute> {
            WheelScreen(onBack = { navController.navigateUp() })
        }
    }
}
