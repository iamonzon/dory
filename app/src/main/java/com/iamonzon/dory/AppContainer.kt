package com.iamonzon.dory

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.iamonzon.dory.data.db.DoryDatabase
import com.iamonzon.dory.data.repository.CategoryRepository
import com.iamonzon.dory.data.repository.ItemRepository
import com.iamonzon.dory.data.repository.ReviewRepository
import com.iamonzon.dory.data.repository.SettingsRepository
import com.iamonzon.dory.data.repository.dataStore
import com.iamonzon.dory.ui.creation.CreationViewModel
import com.iamonzon.dory.ui.dashboard.DashboardViewModel
import com.iamonzon.dory.ui.dashboard.ItemEditViewModel
import com.iamonzon.dory.ui.profile.AdvancedSettingsViewModel
import com.iamonzon.dory.ui.profile.ArchivedItemsViewModel
import com.iamonzon.dory.ui.profile.CategoryManagementViewModel
import com.iamonzon.dory.ui.profile.ProfileViewModel
import com.iamonzon.dory.ui.review.ReviewSessionViewModel
import com.iamonzon.dory.ui.review.ReviewViewModel

class AppContainer(context: Context) {

    private val database = DoryDatabase.create(context)

    private val itemDao = database.itemDao()
    private val reviewDao = database.reviewDao()
    private val categoryDao = database.categoryDao()

    val settingsRepository = SettingsRepository(context.dataStore)
    val categoryRepository = CategoryRepository(categoryDao, itemDao)
    val reviewRepository = ReviewRepository(reviewDao, itemDao, categoryDao, settingsRepository)
    val itemRepository = ItemRepository(itemDao, reviewDao, categoryDao, settingsRepository)

    // ViewModel factories
    val dashboardViewModelFactory get() = DashboardViewModel.factory(itemRepository)

    val reviewViewModelFactory: (Long) -> ViewModelProvider.Factory
        get() = { itemId -> ReviewViewModel.factory(itemId, itemRepository, reviewRepository, categoryRepository) }

    val reviewSessionViewModelFactory get() = ReviewSessionViewModel.factory(itemRepository)

    val creationViewModelFactory get() = CreationViewModel.factory(itemRepository, categoryRepository)

    val profileViewModelFactory get() = ProfileViewModel.factory(itemRepository, categoryRepository, settingsRepository)

    val categoryManagementViewModelFactory get() = CategoryManagementViewModel.factory(categoryRepository, itemRepository)

    val archivedItemsViewModelFactory get() = ArchivedItemsViewModel.factory(itemRepository)

    val advancedSettingsViewModelFactory get() = AdvancedSettingsViewModel.factory(settingsRepository)

    val itemEditViewModelFactory: (Long) -> ViewModelProvider.Factory
        get() = { itemId -> ItemEditViewModel.factory(itemId, itemRepository, reviewRepository) }
}
