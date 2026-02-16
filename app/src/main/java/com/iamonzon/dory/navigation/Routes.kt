package com.iamonzon.dory.navigation

import kotlinx.serialization.Serializable

@Serializable
object Dashboard

@Serializable
object Profile

@Serializable
object ItemCreation

@Serializable
data class Review(val itemId: Long)

@Serializable
object ReviewSession

@Serializable
data class ItemEdit(val itemId: Long)

@Serializable
object CategoryManagement

@Serializable
object ArchivedItems

@Serializable
object AdvancedSettings

@Serializable
object Onboarding
