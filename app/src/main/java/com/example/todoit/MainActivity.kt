package com.example.todoit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.todoit.presentation.common.Screen
import com.example.todoit.presentation.common.TodoItNavHost
import com.example.todoit.ui.theme.TodoItTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodoItTheme {
                TodoItApp()
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
    val route: String,
) {
    HOME("Home", R.drawable.ic_home, Screen.HOME),
    GROUPS("Groups", R.drawable.ic_favorite, Screen.GROUPS),
    SETTINGS("Settings", R.drawable.ic_account_box, Screen.SETTINGS),
}

@Composable
fun TodoItApp() {
    val navController: NavHostController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    // Map current route to a top-level destination for the nav bar selection state
    val selectedDestination = AppDestinations.entries.firstOrNull { dest ->
        currentRoute == dest.route
    } ?: AppDestinations.HOME

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { dest ->
                item(
                    icon = {
                        Icon(
                            painterResource(dest.icon),
                            contentDescription = dest.label,
                        )
                    },
                    label = { Text(dest.label) },
                    selected = dest == selectedDestination,
                    onClick = {
                        navController.navigate(dest.route) {
                            // Pop up to the start destination to avoid growing the back stack
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    ) {
        TodoItNavHost(navController = navController)
    }
}