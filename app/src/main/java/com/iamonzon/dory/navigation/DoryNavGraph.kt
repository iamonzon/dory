package com.iamonzon.dory.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.iamonzon.dory.ui.creation.ItemCreationScreen
import com.iamonzon.dory.ui.dashboard.DashboardScreen
import com.iamonzon.dory.ui.dashboard.ItemEditScreen
import com.iamonzon.dory.ui.onboarding.OnboardingScreen
import com.iamonzon.dory.ui.profile.AdvancedSettingsScreen
import com.iamonzon.dory.ui.profile.ArchivedItemsScreen
import com.iamonzon.dory.ui.profile.CategoryManagementScreen
import com.iamonzon.dory.ui.profile.ProfileScreen
import com.iamonzon.dory.ui.review.ReviewScreen
import com.iamonzon.dory.ui.review.ReviewSessionScreen

@Composable
fun DoryNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = Dashboard
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Dashboard> {
            DashboardScreen(
                onItemClick = { itemId -> navController.navigate(Review(itemId)) },
                onEditItem = { itemId -> navController.navigate(ItemEdit(itemId)) }
            )
        }

        composable<Profile> {
            ProfileScreen(
                onCategoryManagement = { navController.navigate(CategoryManagement) },
                onArchivedItems = { navController.navigate(ArchivedItems) },
                onAdvancedSettings = { navController.navigate(AdvancedSettings) }
            )
        }

        composable<ItemCreation> {
            ItemCreationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<Review> { backStackEntry ->
            val route = backStackEntry.toRoute<Review>()
            ReviewScreen(
                itemId = route.itemId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<ReviewSession> {
            ReviewSessionScreen(
                onItemClick = { itemId -> navController.navigate(Review(itemId)) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<ItemEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<ItemEdit>()
            ItemEditScreen(
                itemId = route.itemId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<CategoryManagement> {
            CategoryManagementScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<ArchivedItems> {
            ArchivedItemsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<AdvancedSettings> {
            AdvancedSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<Onboarding> {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Dashboard) {
                        popUpTo(Onboarding) { inclusive = true }
                    }
                }
            )
        }
    }
}
