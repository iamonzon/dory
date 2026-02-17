package com.iamonzon.dory.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.iamonzon.dory.DoryApplication
import com.iamonzon.dory.ui.creation.CreationViewModel
import com.iamonzon.dory.ui.creation.ItemCreationScreen
import com.iamonzon.dory.ui.dashboard.DashboardScreen
import com.iamonzon.dory.ui.dashboard.DashboardViewModel
import com.iamonzon.dory.ui.dashboard.ItemEditScreen
import com.iamonzon.dory.ui.dashboard.ItemEditViewModel
import com.iamonzon.dory.ui.onboarding.OnboardingScreen
import com.iamonzon.dory.ui.profile.AdvancedSettingsScreen
import com.iamonzon.dory.ui.profile.AdvancedSettingsViewModel
import com.iamonzon.dory.ui.profile.ArchivedItemsScreen
import com.iamonzon.dory.ui.profile.ArchivedItemsViewModel
import com.iamonzon.dory.ui.profile.CategoryManagementScreen
import com.iamonzon.dory.ui.profile.CategoryManagementViewModel
import com.iamonzon.dory.ui.profile.ProfileScreen
import com.iamonzon.dory.ui.profile.ProfileViewModel
import com.iamonzon.dory.ui.review.ReviewScreen
import com.iamonzon.dory.ui.review.ReviewSessionScreen
import com.iamonzon.dory.ui.review.ReviewSessionViewModel
import com.iamonzon.dory.ui.review.ReviewViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DoryNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = Dashboard
) {
    val app = LocalContext.current.applicationContext as DoryApplication

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Dashboard> {
            val vm: DashboardViewModel = viewModel(factory = app.container.dashboardViewModelFactory)
            DashboardScreen(
                viewModel = vm,
                onItemClick = { itemId -> navController.navigate(Review(itemId)) },
                onEditItem = { itemId -> navController.navigate(ItemEdit(itemId)) }
            )
        }

        composable<Profile> {
            val vm: ProfileViewModel = viewModel(factory = app.container.profileViewModelFactory)
            ProfileScreen(
                viewModel = vm,
                onCategoryManagement = { navController.navigate(CategoryManagement) },
                onArchivedItems = { navController.navigate(ArchivedItems) },
                onAdvancedSettings = { navController.navigate(AdvancedSettings) }
            )
        }

        composable<ItemCreation> {
            val vm: CreationViewModel = viewModel(factory = app.container.creationViewModelFactory)
            ItemCreationScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<Review> { backStackEntry ->
            val route = backStackEntry.toRoute<Review>()
            val vm: ReviewViewModel = viewModel(factory = app.container.reviewViewModelFactory(route.itemId))
            ReviewScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<ReviewSession> {
            val vm: ReviewSessionViewModel = viewModel(factory = app.container.reviewSessionViewModelFactory)
            ReviewSessionScreen(
                viewModel = vm,
                onItemClick = { itemId -> navController.navigate(Review(itemId)) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<ItemEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<ItemEdit>()
            val vm: ItemEditViewModel = viewModel(factory = app.container.itemEditViewModelFactory(route.itemId))
            ItemEditScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<CategoryManagement> {
            val vm: CategoryManagementViewModel = viewModel(factory = app.container.categoryManagementViewModelFactory)
            CategoryManagementScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<ArchivedItems> {
            val vm: ArchivedItemsViewModel = viewModel(factory = app.container.archivedItemsViewModelFactory)
            ArchivedItemsScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<AdvancedSettings> {
            val vm: AdvancedSettingsViewModel = viewModel(factory = app.container.advancedSettingsViewModelFactory)
            AdvancedSettingsScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<Onboarding> {
            OnboardingScreen(
                onComplete = {
                    CoroutineScope(Dispatchers.IO).launch {
                        app.container.settingsRepository.setHasCompletedOnboarding(true)
                    }
                    navController.navigate(Dashboard) {
                        popUpTo(Onboarding) { inclusive = true }
                    }
                }
            )
        }
    }
}
