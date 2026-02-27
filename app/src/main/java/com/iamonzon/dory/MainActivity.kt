package com.iamonzon.dory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.iamonzon.dory.navigation.Dashboard
import com.iamonzon.dory.navigation.DoryNavGraph
import com.iamonzon.dory.navigation.ItemCreation
import com.iamonzon.dory.navigation.Onboarding
import com.iamonzon.dory.navigation.Profile
import com.iamonzon.dory.navigation.ReviewSession
import com.iamonzon.dory.ui.actionhub.ActionHubDialog
import com.iamonzon.dory.ui.theme.DoryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoryTheme {
                DoryApp()
            }
        }
    }
}

enum class TopLevelTab(
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Default.Home),
    ACTION_HUB("Action Hub", Icons.Default.AddCircle),
    PROFILE("Profile", Icons.Default.AccountCircle)
}

@Composable
fun DoryApp() {
    val navController = rememberNavController()
    var currentTab by rememberSaveable { mutableStateOf(TopLevelTab.DASHBOARD) }
    var showActionHub by remember { mutableStateOf(false) }

    val app = LocalContext.current.applicationContext as DoryApplication
    val hasCompletedOnboarding by app.container.settingsRepository
        .observeHasCompletedOnboarding()
        .collectAsStateWithLifecycle(initialValue = true)

    val startDestination: Any = if (hasCompletedOnboarding) Dashboard else Onboarding

    if (showActionHub) {
        ActionHubDialog(
            onDismiss = { showActionHub = false },
            onAddNewItem = { navController.navigate(ItemCreation) },
            onStartReviewSession = { navController.navigate(ReviewSession) }
        )
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelTab.entries.forEach { tab ->
                item(
                    icon = {
                        Icon(tab.icon, contentDescription = tab.label)
                    },
                    label = { Text(tab.label) },
                    selected = tab == currentTab,
                    onClick = {
                        when (tab) {
                            TopLevelTab.ACTION_HUB -> showActionHub = true
                            TopLevelTab.DASHBOARD -> {
                                currentTab = tab
                                navController.navigate(Dashboard) {
                                    popUpTo(Dashboard) { inclusive = true }
                                }
                            }
                            TopLevelTab.PROFILE -> {
                                currentTab = tab
                                navController.navigate(Profile) {
                                    popUpTo(Dashboard)
                                }
                            }
                        }
                    }
                )
            }
        }
    ) {
        DoryNavGraph(
            navController = navController,
            startDestination = startDestination
        )
    }
}
