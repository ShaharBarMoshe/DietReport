package com.diet.dietreport

import android.app.AlarmManager
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.diet.dietreport.lock.LockScreen
import com.diet.dietreport.lock.LockViewModelFactory
import com.diet.dietreport.settings.SettingsNavEvent
import com.diet.dietreport.meals.HomeScreen
import com.diet.dietreport.meals.HomeViewModelFactory
import com.diet.dietreport.meals.LogMealScreen
import com.diet.dietreport.meals.LogMealViewModelFactory
import com.diet.dietreport.reports.ReportScreen
import com.diet.dietreport.reports.ReportViewModelFactory
import com.diet.dietreport.settings.SettingsScreen
import com.diet.dietreport.settings.SettingsViewModel
import com.diet.dietreport.settings.SettingsViewModelFactory
import com.diet.dietreport.settings.data.SettingsRepository
import com.diet.dietreport.settings.data.settingsDataStore
import kotlinx.coroutines.flow.first
import com.diet.dietreport.ui.theme.DietReportTheme

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val LOG_MEAL = "log_meal/{slotId}"
    const val REPORT = "report"
    const val LOCK = "lock/{slotId}"

    fun logMeal(slotId: Long) = "log_meal/$slotId"
    fun lock(slotId: Long) = "lock/$slotId"
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
)

private val bottomNavItems = listOf(
    NavItem(Routes.HOME, "Home", Icons.Default.Home, "Home"),
    NavItem(Routes.SETTINGS, "Settings", Icons.Default.Settings, "Settings"),
    NavItem(Routes.REPORT, "Report", Icons.Default.DateRange, "Report"),
)

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory.create(application)
    }

    private val homeViewModel: com.diet.dietreport.meals.HomeViewModel by viewModels {
        HomeViewModelFactory(application)
    }

    private val reportViewModel: com.diet.dietreport.reports.ReportViewModel by viewModels {
        ReportViewModelFactory(application)
    }

    override fun onResume() {
        super.onResume()
        val alarmManager = getSystemService(AlarmManager::class.java)
        val notifManager = getSystemService(NotificationManager::class.java)
        when {
            !alarmManager.canScheduleExactAlarms() ->
                SchedulerErrorBus.post(
                    AppError.SchedulerError("Exact alarm permission revoked — reminders may be delayed.")
                )
            !notifManager.areNotificationsEnabled() ->
                SchedulerErrorBus.post(
                    AppError.SchedulerError("Notifications are disabled — you won't receive meal reminders.")
                )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DietReportTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val isLockScreen = currentDestination?.route == Routes.LOCK

                // Deep-link slot ID from notification "Log meal" action
                val pendingSlotId: Long? = remember {
                    val slotId = intent?.getLongExtra("slot_id", -1L) ?: -1L
                    if (intent?.getStringExtra("destination") == "log_meal" && slotId != -1L) slotId else null
                }

                // On startup: route to deep-link or check onboarding
                LaunchedEffect(Unit) {
                    if (pendingSlotId != null) {
                        navController.navigate(Routes.logMeal(pendingSlotId)) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    } else {
                        val onboarded = SettingsRepository(application.settingsDataStore)
                            .isOnboardingComplete.first()
                        if (!onboarded) {
                            navController.navigate(Routes.SETTINGS) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        }
                    }
                }

                // Handle navigation events from SettingsViewModel (first-save onboarding → Home)
                LaunchedEffect(Unit) {
                    settingsViewModel.navEvent.collect { event ->
                        when (event) {
                            SettingsNavEvent.ToHome -> navController.navigate(Routes.HOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.semantics { testTagsAsResourceId = true },
                    bottomBar = {
                        if (!isLockScreen) {
                            NavigationBar {
                                bottomNavItems.forEach { item ->
                                    NavigationBarItem(
                                        icon = {
                                            Icon(item.icon, contentDescription = item.contentDescription)
                                        },
                                        label = { Text(item.label) },
                                        selected = currentDestination?.hierarchy
                                            ?.any { it.route == item.route } == true,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME,
                        modifier = if (isLockScreen) Modifier else Modifier.padding(innerPadding)
                    ) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onNavigateToLogMeal = { slotId ->
                                    navController.navigate(Routes.logMeal(slotId))
                                },
                            )
                        }
                        composable(Routes.SETTINGS) { SettingsScreen(settingsViewModel) }
                        composable(
                            Routes.LOG_MEAL,
                            arguments = listOf(navArgument("slotId") { type = NavType.LongType }),
                        ) { backStackEntry ->
                            val slotId = backStackEntry.arguments?.getLong("slotId") ?: -1L
                            val logMealViewModel = viewModel<com.diet.dietreport.meals.LogMealViewModel>(
                                factory = LogMealViewModelFactory.create(this@MainActivity, slotId),
                            )
                            LogMealScreen(
                                viewModel = logMealViewModel,
                                onNavigateToLock = { confirmedSlotId ->
                                    navController.navigate(Routes.lock(confirmedSlotId)) {
                                        popUpTo(Routes.LOG_MEAL) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(
                            Routes.LOCK,
                            arguments = listOf(navArgument("slotId") { type = NavType.LongType }),
                        ) { backStackEntry ->
                            val slotId = backStackEntry.arguments?.getLong("slotId") ?: -1L
                            val lockViewModel = viewModel<com.diet.dietreport.lock.LockViewModel>(
                                factory = LockViewModelFactory.create(this@MainActivity, slotId),
                            )
                            LockScreen(
                                viewModel = lockViewModel,
                                onNavigateToHome = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(Routes.REPORT) { ReportScreen(reportViewModel) }
                    }
                }
            }
        }
    }
}
