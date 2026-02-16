package com.iamonzon.dory

import android.content.Context
import com.iamonzon.dory.data.db.DoryDatabase
import com.iamonzon.dory.data.repository.CategoryRepository
import com.iamonzon.dory.data.repository.ItemRepository
import com.iamonzon.dory.data.repository.ReviewRepository
import com.iamonzon.dory.data.repository.SettingsRepository
import com.iamonzon.dory.data.repository.dataStore

class AppContainer(context: Context) {

    private val database = DoryDatabase.create(context)

    private val itemDao = database.itemDao()
    private val reviewDao = database.reviewDao()
    private val categoryDao = database.categoryDao()

    val settingsRepository = SettingsRepository(context.dataStore)
    val categoryRepository = CategoryRepository(categoryDao, itemDao)
    val reviewRepository = ReviewRepository(reviewDao, itemDao, categoryDao, settingsRepository)
    val itemRepository = ItemRepository(itemDao, reviewDao, categoryDao, settingsRepository)
}
