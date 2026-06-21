package com.emusite.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.emusite.app.data.MainViewModel
import com.emusite.app.navigation.AppNavHost
import com.emusite.app.navigation.Screen
import com.emusite.app.ui.theme.EmusiteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repoIntent = intent?.data?.toString()

        setContent {
            EmusiteTheme {
                val navController = rememberNavController()
                val viewModel: MainViewModel = viewModel()
                val context = LocalContext.current
                val defaultPage = remember {
                    val prefs = context.getSharedPreferences("emusite_settings", android.content.Context.MODE_PRIVATE)
                    val page = prefs.getString("default_page", "home") ?: "home"
                    when (page) {
                        "search" -> Screen.Search.route
                        "plugins" -> Screen.Plugins.route
                        else -> Screen.Home.route
                    }
                }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                LaunchedEffect(repoIntent) {
                    if (repoIntent != null && repoIntent.startsWith("emusiterepo://")) {
                        viewModel.fetchRepository(repoIntent)
                        navController.navigate(Screen.Plugins.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                }

                val bottomNavItems = listOf(
                    Triple(Screen.Home, Icons.Default.Home, "Home"),
                    Triple(Screen.Search, Icons.Default.Search, "Search"),
                    Triple(Screen.Plugins, Icons.Default.Extension, "Plugins"),
                    Triple(Screen.Settings, Icons.Default.Settings, "Settings"),
                )

                val showBottomBar = bottomNavItems.any { (screen, _, _) ->
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                modifier = Modifier.focusGroup()
                            ) {
                                bottomNavItems.forEach { (screen, icon, label) ->
                                    val selected = currentDestination?.hierarchy?.any {
                                        it.route == screen.route
                                    } == true

                                    NavigationBarItem(
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label) },
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(screen.route) {
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
                    AppNavHost(
                        navController = navController,
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                        startDestination = defaultPage
                    )
                }
            }
        }
    }
}
