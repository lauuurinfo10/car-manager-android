package com.example.carmanager.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

// Modelele oglindesc schemas.py din backend. Datele vin ca "yyyy-MM-dd".

@Serializable
data class Car(
    val id: Int,
    val make: String,
    val model: String,
    val year: Int,
    @SerialName("license_plate") val licensePlate: String,
    @SerialName("current_mileage") val currentMileage: Int,
) {
    val displayName get() = "$make $model"
}

@Serializable
data class CarInput(
    val make: String,
    val model: String,
    val year: Int,
    @SerialName("license_plate") val licensePlate: String,
    @SerialName("current_mileage") val currentMileage: Int,
)

@Serializable
data class MileageUpdate(
    @SerialName("current_mileage") val currentMileage: Int,
)

@Serializable
data class CarDocument(
    val id: Int,
    @SerialName("car_id") val carId: Int,
    val title: String,
    @SerialName("issued_date") val issuedDate: String,
    @SerialName("expiry_date") val expiryDate: String,
    val price: Double? = null,
) {
    val expiry: LocalDate get() = LocalDate.parse(expiryDate)
    val issued: LocalDate get() = LocalDate.parse(issuedDate)
}

@Serializable
data class DocumentInput(
    val title: String,
    @SerialName("issued_date") val issuedDate: String,
    @SerialName("expiry_date") val expiryDate: String,
    val price: Double? = null,
)

@Serializable
data class ExpiringDocument(
    val id: Int,
    @SerialName("car_id") val carId: Int,
    val title: String,
    @SerialName("issued_date") val issuedDate: String,
    @SerialName("expiry_date") val expiryDate: String,
    val price: Double? = null,
    @SerialName("days_left") val daysLeft: Int,
)

@Serializable
data class Maintenance(
    val id: Int,
    @SerialName("car_id") val carId: Int,
    val title: String,
    val date: String,
    val mileage: Int,
    val price: Double? = null,
) {
    val localDate: LocalDate get() = LocalDate.parse(date)
}

@Serializable
data class MaintenanceInput(
    val title: String,
    val date: String,
    val mileage: Int,
    val price: Double? = null,
)

@Serializable
data class CostSummary(
    @SerialName("car_id") val carId: Int,
    @SerialName("documents_total") val documentsTotal: Double,
    @SerialName("maintenance_total") val maintenanceTotal: Double,
    val total: Double,
)
