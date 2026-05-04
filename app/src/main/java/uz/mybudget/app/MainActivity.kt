package uz.mybudget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import uz.mybudget.app.data.repository.AuthRepository
import uz.mybudget.app.presentation.auth.LoginScreen
import uz.mybudget.app.presentation.auth.RegisterScreen
import uz.mybudget.app.presentation.dashboard.DashboardScreen
import uz.mybudget.app.presentation.stats.StatsScreen
import uz.mybudget.app.ui.theme.MyBudgetTheme

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // Firestore offline ishlashi uchun sozlamalar
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        setContent {
            MyBudgetTheme {
                val nav = rememberNavController()
                val auth = remember { AuthRepository() }
                val start = if (auth.currentUser == null) "login" else "main"

                NavHost(navController = nav, startDestination = start) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = { nav.navigate("main") { popUpTo("login") { inclusive = true } } },
                            onRegisterClick = { nav.navigate("register") }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = { nav.navigate("main") { popUpTo("login") { inclusive = true } } },
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("main") {
                        MainScaffold(
                            onLogout = { 
                                auth.logout()
                                nav.navigate("login") { popUpTo("main") { inclusive = true } } 
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScaffold(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Dashboard,
        Screen.Stats
    )

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
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = {
                            Text(
                                screen.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (currentRoute == screen.route) FontWeight.Bold else FontWeight.Normal
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
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                DashboardScreen(onLogout = onLogout)
            }
            composable(Screen.Stats.route) {
                StatsScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard_view", "Asosiy", Icons.Default.Dashboard)
    object Stats : Screen("stats_view", "Statistika", Icons.Default.BarChart)
}
