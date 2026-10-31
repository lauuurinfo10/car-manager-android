package com.example.carmanager.ui.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.toRoute
import com.example.carmanager.data.ApiClient
import com.example.carmanager.data.DocumentInput
import com.example.carmanager.ui.DocumentFormRoute
import com.example.carmanager.ui.components.DateField
import com.example.carmanager.ui.components.NumberField
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period

private val DOCUMENT_TYPES = listOf("RCA", "ITP", "Rovinietă", "CASCO", "Trusă medicală", "Extinctor")

/** Valabilitate tipică, ca data de expirare să se completeze singură când alegi tipul. */
private val DEFAULT_VALIDITY = mapOf(
    "RCA" to Period.ofYears(1),
    "ITP" to Period.ofYears(1),
    "Rovinietă" to Period.ofYears(1),
    "CASCO" to Period.ofYears(1),
    "Trusă medicală" to Period.ofYears(3),
    "Extinctor" to Period.ofYears(1),
)

private val QUICK_PERIODS = listOf(
    "1 lună" to Period.ofMonths(1),
    "6 luni" to Period.ofMonths(6),
    "1 an" to Period.ofYears(1),
    "2 ani" to Period.ofYears(2),
)

class DocumentFormViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val route = savedStateHandle.toRoute<DocumentFormRoute>()
    val isEdit = route.docId != null

    var title by mutableStateOf("")
    var issued by mutableStateOf(LocalDate.now())
    var expiry by mutableStateOf(LocalDate.now().plusYears(1))
    var price by mutableStateOf("")
    var status by mutableStateOf(FormStatus(loading = isEdit))
        private set

    val datesValid get() = !expiry.isBefore(issued)
    val canSave get() = title.isNotBlank() && datesValid && (price.isEmpty() || price.toDoubleOrNull() != null)

    init { if (isEdit) load() }

    fun load() {
        val docId = route.docId ?: return
        status = FormStatus(loading = true)
        viewModelScope.launch {
            status = try {
                // backend-ul nu are GET /documents/{id}, așa că îl căutăm în lista mașinii
                val doc = ApiClient.api.listDocuments(route.carId).first { it.id == docId }
                title = doc.title; issued = doc.issued; expiry = doc.expiry
                price = doc.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()
                FormStatus()
            } catch (_: NoSuchElementException) {
                FormStatus(loadError = "Actul nu mai există.")
            } catch (t: Throwable) {
                FormStatus(loadError = ApiClient.errorMessage(t))
            }
        }
    }

    fun selectType(type: String) {
        title = type
        if (!isEdit) DEFAULT_VALIDITY[type]?.let { expiry = issued.plus(it).minusDays(1) }
    }

    fun applyPeriod(p: Period) { expiry = issued.plus(p).minusDays(1) }

    fun save() {
        if (!canSave) return
        val input = DocumentInput(title.trim(), issued.toString(), expiry.toString(), price.toDoubleOrNull())
        status = status.copy(saving = true, error = null)
        viewModelScope.launch {
            status = try {
                val id = route.docId
                if (id == null) ApiClient.api.createDocument(route.carId, input) else ApiClient.api.updateDocument(id, input)
                status.copy(saving = false, done = true)
            } catch (t: Throwable) {
                status.copy(saving = false, error = ApiClient.errorMessage(t))
            }
        }
    }

    fun delete() {
        val id = route.docId ?: return
        status = status.copy(saving = true, error = null)
        viewModelScope.launch {
            status = try {
                ApiClient.api.deleteDocument(id)
                status.copy(saving = false, done = true)
            } catch (t: Throwable) {
                status.copy(saving = false, error = ApiClient.errorMessage(t))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentFormScreen(onBack: () -> Unit, vm: DocumentFormViewModel = viewModel()) {
    FormScaffold(
        title = if (vm.isEdit) "Editează actul" else "Act nou",
        status = vm.status,
        canSave = vm.canSave,
        onBack = onBack,
        onSave = vm::save,
        onRetryLoad = vm::load,
        deleteLabel = "Ștergi actul?",
        onDelete = if (vm.isEdit) vm::delete else null,
    ) {
        SuggestionChips(DOCUMENT_TYPES, vm.title, vm::selectType)
        OutlinedTextField(
            vm.title, { vm.title = it }, label = { Text("Denumire") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        DateField("Data emiterii", vm.issued, { vm.issued = it })
        DateField("Data expirării", vm.expiry, { vm.expiry = it })
        if (!vm.datesValid) {
            Text("Data expirării nu poate fi înainte de data emiterii.", color = MaterialTheme.colorScheme.error)
        }
        Text("Valabil:", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_PERIODS.forEach { (label, period) ->
                AssistChip(onClick = { vm.applyPeriod(period) }, label = { Text(label) })
            }
        }
        NumberField("Preț (opțional)", vm.price, { vm.price = it }, decimal = true, suffix = "lei")
    }
}
