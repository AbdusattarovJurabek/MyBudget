package uz.mybudget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uz.mybudget.app.presentation.dashboard.DashboardScreen
import uz.mybudget.app.presentation.pin.PinScreen
import uz.mybudget.app.presentation.settings.SettingsScreen
import uz.mybudget.app.presentation.stats.StatsScreen
import uz.mybudget.app.security.PinManager
import uz.mybudget.app.ui.theme.MyBudgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MyBudgetTheme {
                val pinManager = remember { PinManager(applicationContext) }
                var unlocked by rememberSaveable { mutableStateOf(false) }

                if (unlocked) {
                    MainScaffold(onLock = { unlocked = false })
                } else {
                    PinScreen(
                        isSetup = !pinManager.hasPin(),
                        onCreatePin = { pin ->
                            runCatching { pinManager.setPin(pin) }.isSuccess
                        },
                        onVerifyPin = pinManager::verify,
                        onUnlocked = { unlocked = true }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScaffold(onLock: () -> Unit) {
    val navController = rememberNavController()
    val items = listOf(Screen.Dashboard, Screen.Stats, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = {
                            Text(
                                screen.title,
                                fontWeight = if (currentRoute == screen.route) FontWeight.Bold
                                else FontWeight.Normal
                            )
                        },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(onLock = onLock)
            }
            composable(Screen.Stats.route) {
                StatsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard_view", "Asosiy", Icons.Default.Dashboard)
    data object Stats : Screen("stats_view", "Statistika", Icons.Default.BarChart)
    data object Settings : Screen("settings_view", "Sozlamalar", Icons.Default.Settings)
}
