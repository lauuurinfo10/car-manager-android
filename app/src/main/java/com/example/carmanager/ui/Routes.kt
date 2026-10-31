package com.example.carmanager.ui

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable data class CarDetailRoute(val carId: Int)
@Serializable data class CarFormRoute(val carId: Int? = null)
@Serializable data class DocumentFormRoute(val carId: Int, val docId: Int? = null)
@Serializable data class MaintenanceFormRoute(val carId: Int, val recordId: Int? = null)
