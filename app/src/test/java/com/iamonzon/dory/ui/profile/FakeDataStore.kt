package com.iamonzon.dory.ui.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeDataStore : DataStore<Preferences> {
    private val _data = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = _data
    private val mutex = Mutex()

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        return mutex.withLock {
            val newValue = transform(_data.value)
            _data.value = newValue
            newValue
        }
    }
}
