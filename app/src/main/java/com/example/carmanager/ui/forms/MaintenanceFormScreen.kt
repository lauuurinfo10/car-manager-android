package com.example.carmanager.ui.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.toRoute
import com.example.carmanager.data.ApiClient
import com.example.carmanager.data.MaintenanceInput
import com.example.carmanager.domain.DEFAULT_RULES
import com.example.carmanager.ui.MaintenanceFormRoute
import com.example.carmanager.ui.components.DateField
import com.example.carmanager.ui.components.NumberField
import com.example.carmanager.ui.km
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

private val MAINTENANCE_TYPES = DEFAULT_RULES.map { it.name } + listOf("Anvelope", "Baterie")

class MaintenanceFormViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val route = savedStateHandle.toRoute<MaintenanceFormRoute>()
    val isEdit = route.recordId != null

    var title by mutableStateOf("")
    var date by mutableStateOf(LocalDate.now())
    var mileage by mutableStateOf("")
    var price by mutableStateOf("")
    var carMileage by mutableStateOf<Int?>(null)
        private set
    var status by mutableStateOf(FormStatus(loading = true))
        private set

    val canSave get() = title.isNotBlank() && mileage.toIntOrNull() != null &&
        (price.isEmpty() || price.toDoubleOrNull() != null)

    init { load() }

    fun load() {
        status = FormStatus(loading = true)
        viewModelScope.launch {
            status = try {
                val (car, records) = coroutineScope {
                    val car = async { ApiClient.api.getCar(route.carId) }
                    val records = if (isEdit) ApiClient.api.listMaintenance(route.carId) else emptyList()
                    car.await() to records
                }
                carMileage = car.currentMileage
                val id = route.recordId
                if (id == null) {
                    // pentru o intervenție nouă propunem kilometrajul actual al mașinii
                    if (mileage.isEmpty()) mileage = carMileage.toString()
                    FormStatus()
                } else {
                    val r = records.firstOrNull { it.id == id }
                    if (r == null) {
                        FormStatus(loadError = "Intervenția nu mai există.")
                    } else {
                        title = r.title; date = r.localDate; mileage = r.mileage.toString()
                        price = r.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()
                        FormStatus()
                    }
                }
            } catch (t: Throwable) {
                FormStatus(loadError = ApiClient.errorMessage(t))
            }
        }
    }

    fun save() {
        if (!canSave) return
        val input = MaintenanceInput(title.trim(), date.toString(), mileage.toInt(), price.toDoubleOrNull())
        status = status.copy(saving = true, error = null)
        viewModelScope.launch {
            status = try {
                val id = route.recordId
                if (id == null) ApiClient.api.createMaintenance(route.carId, input) else ApiClient.api.updateMaintenance(id, input)
                status.copy(saving = false, done = true)
            } catch (t: Throwable) {
                status.copy(saving = false, error = ApiClient.errorMessage(t))
            }
        }
    }

    fun delete() {
        val id = route.recordId ?: return
        status = status.copy(saving = true, error = null)
        viewModelScope.launch {
            status = try {
                ApiClient.api.deleteMaintenance(id)
                status.copy(saving = false, done = true)
            } catch (t: Throwable) {
                status.copy(saving = false, error = ApiClient.errorMessage(t))
            }
        }
    }
}

@Composable
fun MaintenanceFormScreen(onBack: () -> Unit, vm: MaintenanceFormViewModel = viewModel()) {
    FormScaffold(
        title = if (vm.isEdit) "Editează intervenția" else "Intervenție nouă",
        status = vm.status,
        canSave = vm.canSave,
        onBack = onBack,
        onSave = vm::save,
        onRetryLoad = vm::load,
        deleteLabel = "Ștergi intervenția?",
        onDelete = if (vm.isEdit) vm::delete else null,
    ) {
        SuggestionChips(MAINTENANCE_TYPES, vm.title) { vm.title = it }
        OutlinedTextField(
            vm.title, { vm.title = it }, label = { Text("Ce s-a făcut") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        DateField("Data", vm.date, { vm.date = it })
        NumberField("Kilometraj la intervenție", vm.mileage, { vm.mileage = it.take(7) }, suffix = "km")
        val km = vm.mileage.toIntOrNull()
        val current = vm.carMileage
        if (km != null && current != null && km > current) {
            Text(
                "Kilometrajul mașinii va fi actualizat de la ${current.km()} la ${km.km()}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        NumberField("Preț (opțional)", vm.price, { vm.price = it }, decimal = true, suffix = "lei")
    }
}
