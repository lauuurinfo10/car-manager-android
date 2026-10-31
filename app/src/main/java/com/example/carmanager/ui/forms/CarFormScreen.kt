package com.example.carmanager.ui.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.toRoute
import com.example.carmanager.data.ApiClient
import com.example.carmanager.data.CarInput
import com.example.carmanager.ui.CarFormRoute
import com.example.carmanager.ui.components.NumberField
import kotlinx.coroutines.launch
import java.time.LocalDate

class CarFormViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val carId = savedStateHandle.toRoute<CarFormRoute>().carId

    var make by mutableStateOf("")
    var model by mutableStateOf("")
    var year by mutableStateOf("")
    var plate by mutableStateOf("")
    var mileage by mutableStateOf("")
    var status by mutableStateOf(FormStatus(loading = carId != null))
        private set

    private val maxYear = LocalDate.now().year + 1
    val yearValid get() = year.toIntOrNull()?.let { it in 1900..maxYear } == true
    val canSave get() = make.isNotBlank() && model.isNotBlank() && plate.isNotBlank() && yearValid && mileage.toIntOrNull() != null

    init { if (carId != null) load() }

    fun load() {
        val id = carId ?: return
        status = FormStatus(loading = true)
        viewModelScope.launch {
            status = try {
                val car = ApiClient.api.getCar(id)
                make = car.make; model = car.model; year = car.year.toString()
                plate = car.licensePlate; mileage = car.currentMileage.toString()
                FormStatus()
            } catch (t: Throwable) {
                FormStatus(loadError = ApiClient.errorMessage(t))
            }
        }
    }

    fun save() {
        if (!canSave) return
        val input = CarInput(make.trim(), model.trim(), year.toInt(), plate.trim().uppercase(), mileage.toInt())
        status = status.copy(saving = true, error = null)
        viewModelScope.launch {
            status = try {
                if (carId == null) ApiClient.api.createCar(input) else ApiClient.api.updateCar(carId, input)
                status.copy(saving = false, done = true)
            } catch (t: Throwable) {
                status.copy(saving = false, error = ApiClient.errorMessage(t))
            }
        }
    }
}

@Composable
fun CarFormScreen(onBack: () -> Unit, vm: CarFormViewModel = viewModel()) {
    FormScaffold(
        title = if (vm.carId == null) "Mașină nouă" else "Editează mașina",
        status = vm.status,
        canSave = vm.canSave,
        onBack = onBack,
        onSave = vm::save,
        onRetryLoad = vm::load,
    ) {
        OutlinedTextField(
            vm.make, { vm.make = it }, label = { Text("Marcă") }, placeholder = { Text("ex. Dacia") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        OutlinedTextField(
            vm.model, { vm.model = it }, label = { Text("Model") }, placeholder = { Text("ex. Logan") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        OutlinedTextField(
            vm.plate, { vm.plate = it }, label = { Text("Număr de înmatriculare") },
            visualTransformation = UppercaseTransformation,
            placeholder = { Text("ex. B 123 ABC") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        )
        FieldRow {
            NumberField(
                "An fabricație", vm.year, { vm.year = it.take(4) }, Modifier.weight(1f),
                isError = vm.year.length == 4 && !vm.yearValid,
            )
            NumberField("Kilometraj", vm.mileage, { vm.mileage = it.take(7) }, Modifier.weight(1f), suffix = "km")
        }
    }
}

/** Afișează textul cu majuscule fără să-l modifice în timp ce utilizatorul tastează. */
private object UppercaseTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val upper = text.text.uppercase()
        // unele litere (ex. „ß”) devin două caractere; atunci lăsăm textul neschimbat
        return TransformedText(if (upper.length == text.length) AnnotatedString(upper) else text, OffsetMapping.Identity)
    }
}
