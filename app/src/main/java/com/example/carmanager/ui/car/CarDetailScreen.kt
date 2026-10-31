package com.example.carmanager.ui.car

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.carmanager.R
import com.example.carmanager.data.ApiClient
import com.example.carmanager.data.Car
import com.example.carmanager.data.CarDocument
import com.example.carmanager.data.CostSummary
import com.example.carmanager.data.Maintenance
import com.example.carmanager.data.MileageUpdate
import com.example.carmanager.domain.UpcomingMaintenance
import com.example.carmanager.domain.Urgency
import com.example.carmanager.domain.daysLeftLabel
import com.example.carmanager.domain.expiryStatus
import com.example.carmanager.domain.upcomingMaintenance
import com.example.carmanager.ui.CarDetailRoute
import com.example.carmanager.ui.components.ErrorBox
import com.example.carmanager.ui.components.IconTile
import com.example.carmanager.ui.components.LoadingBox
import com.example.carmanager.ui.components.NumberField
import com.example.carmanager.ui.components.PlateBadge
import com.example.carmanager.ui.components.ProgressLine
import com.example.carmanager.ui.components.SectionTitle
import com.example.carmanager.ui.components.StatTile
import com.example.carmanager.ui.components.StatusPill
import com.example.carmanager.ui.components.urgencyColor
import com.example.carmanager.ui.display
import com.example.carmanager.ui.km
import com.example.carmanager.ui.lei
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class CarDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val car: Car? = null,
    val documents: List<CarDocument> = emptyList(),
    val maintenance: List<Maintenance> = emptyList(),
    val costs: CostSummary? = null,
) {
    val upcoming: List<UpcomingMaintenance>
        get() = car?.let { upcomingMaintenance(maintenance, it.currentMileage) }.orEmpty()
}

class CarDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val carId = savedStateHandle.toRoute<CarDetailRoute>().carId

    private val _state = MutableStateFlow(CarDetailState())
    val state = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted = _deleted.asStateFlow()

    fun load() {
        _state.update { it.copy(error = null) }
        viewModelScope.launch {
            try {
                _state.value = coroutineScope {
                    val car = async { ApiClient.api.getCar(carId) }
                    val docs = async { ApiClient.api.listDocuments(carId) }
                    val maint = async { ApiClient.api.listMaintenance(carId) }
                    val costs = async { ApiClient.api.getCosts(carId) }
                    CarDetailState(
                        loading = false,
                        car = car.await(),
                        documents = docs.await(),
                        maintenance = maint.await(),
                        costs = costs.await(),
                    )
                }
            } catch (t: Throwable) {
                _state.update { it.copy(loading = false, error = ApiClient.errorMessage(t)) }
            }
        }
    }

    fun updateMileage(km: Int) = viewModelScope.launch {
        try {
            val car = ApiClient.api.updateMileage(carId, MileageUpdate(km))
            _state.update { it.copy(car = car) }
            _messages.tryEmit("Kilometraj actualizat")
        } catch (t: Throwable) {
            _messages.tryEmit(ApiClient.errorMessage(t))
        }
    }

    fun deleteCar() = viewModelScope.launch {
        try {
            ApiClient.api.deleteCar(carId)
            _deleted.value = true
        } catch (t: Throwable) {
            _messages.tryEmit(ApiClient.errorMessage(t))
        }
    }
}

private const val TAB_DOCS = 0
private const val TAB_SERVICE = 1
private const val TAB_HISTORY = 2

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CarDetailScreen(
    onBack: () -> Unit,
    onEditCar: (Int) -> Unit,
    onAddDocument: (Int) -> Unit,
    onEditDocument: (Int, Int) -> Unit,
    onAddMaintenance: (Int) -> Unit,
    onEditMaintenance: (Int, Int) -> Unit,
    vm: CarDetailViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(TAB_DOCS) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editMileage by remember { mutableStateOf(false) }
    var chooseAdd by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.load() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(deleted) { if (deleted) onBack() }

    val openDoc: (Int) -> Unit = { onEditDocument(vm.carId, it) }
    val openService: (Int) -> Unit = { onEditMaintenance(vm.carId, it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.car?.displayName ?: "Mașină", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Înapoi") }
                },
                actions = {
                    if (state.car != null) {
                        IconButton(onClick = { onEditCar(vm.carId) }) { Icon(Icons.Default.Edit, "Editează mașina") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Șterge mașina") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (state.car != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        when (tab) {
                            TAB_DOCS -> onAddDocument(vm.carId)
                            TAB_SERVICE -> onAddMaintenance(vm.carId)
                            else -> chooseAdd = true
                        }
                    },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = {
                        Text(
                            when (tab) {
                                TAB_DOCS -> "Adaugă act"
                                TAB_SERVICE -> "Adaugă intervenție"
                                else -> "Adaugă"
                            }
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val s = state
        val car = s.car
        Box(Modifier.padding(padding)) {
            when {
                s.loading -> LoadingBox()
                car == null -> ErrorBox(s.error ?: "Mașina nu a fost găsită.", vm::load)
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 104.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        CarHero(
                            car = car,
                            costs = s.costs,
                            documents = s.documents,
                            onEditMileage = { editMileage = true },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    stickyHeader {
                        PrimaryTabRow(
                            selectedTabIndex = tab,
                            containerColor = MaterialTheme.colorScheme.background,
                        ) {
                            Tab(tab == TAB_DOCS, { tab = TAB_DOCS }, text = { Text("Acte") })
                            Tab(tab == TAB_SERVICE, { tab = TAB_SERVICE }, text = { Text("Service") })
                            Tab(tab == TAB_HISTORY, { tab = TAB_HISTORY }, text = { Text("Istoric") })
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                    when (tab) {
                        TAB_DOCS -> documentsTab(s.documents, openDoc)
                        TAB_SERVICE -> serviceTab(s.upcoming, s.maintenance, car.currentMileage, openService)
                        else -> historyTab(s.documents, s.maintenance, openDoc, openService)
                    }
                }
            }
        }
    }

    if (chooseAdd) {
        ModalBottomSheet(onDismissRequest = { chooseAdd = false }) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                Text(
                    "Ce vrei să adaugi?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                ListItem(
                    headlineContent = { Text("Act") },
                    supportingContent = { Text("RCA, ITP, rovinietă, CASCO…") },
                    leadingContent = { IconTile(R.drawable.ic_document, tint = MaterialTheme.colorScheme.primary) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { chooseAdd = false; onAddDocument(vm.carId) },
                )
                ListItem(
                    headlineContent = { Text("Intervenție service") },
                    supportingContent = { Text("Schimb ulei, revizie, plăcuțe…") },
                    leadingContent = { IconTile(vector = Icons.Default.Build, tint = MaterialTheme.colorScheme.tertiary) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { chooseAdd = false; onAddMaintenance(vm.carId) },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Ștergi mașina?") },
            text = { Text("Se vor șterge și toate actele și intervențiile ei. Acțiunea nu poate fi anulată.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteCar() }) {
                    Text("Șterge", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Anulează") } },
        )
    }

    val carForDialog = state.car
    if (editMileage && carForDialog != null) {
        MileageDialog(
            current = carForDialog.currentMileage,
            onDismiss = { editMileage = false },
            onSave = { editMileage = false; vm.updateMileage(it) },
        )
    }
}

/** Cardul mare de sus: plăcuța, anul și trei statistici (km, cheltuieli, acte valabile). */
@Composable
private fun CarHero(
    car: Car,
    costs: CostSummary?,
    documents: List<CarDocument>,
    onEditMileage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val validDocs = documents.count { expiryStatus(it.expiry).urgency != Urgency.OVERDUE }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E4C91), Color(0xFF3F7FD9))))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                PlateBadge(car.licensePlate, large = true)
                Spacer(Modifier.height(10.dp))
                Text(
                    "${car.make} ${car.model} · ${car.year}",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                painterResource(R.drawable.ic_car), null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                R.drawable.ic_speed, car.currentMileage.km(), "Kilometraj ✎",
                Modifier.weight(1.25f).clickable(onClick = onEditMileage),
            )
            StatTile(
                R.drawable.ic_wallet, Math.round(costs?.total ?: 0.0).toDouble().lei(), "Cheltuieli",
                Modifier.weight(1.1f), tint = MaterialTheme.colorScheme.tertiary,
            )
            StatTile(
                R.drawable.ic_document, "$validDocs/${documents.size}", "Acte valabile",
                Modifier.weight(0.9f),
                tint = if (validDocs < documents.size) MaterialTheme.colorScheme.error else urgencyColor(Urgency.OK),
            )
        }
    }
}

private fun LazyListScope.documentsTab(documents: List<CarDocument>, onClick: (Int) -> Unit) {
    if (documents.isEmpty()) {
        item {
            EmptyHint(
                R.drawable.ic_document,
                "Niciun act încă",
                "Adaugă RCA, ITP sau rovinieta și primești notificare înainte să expire.",
            )
        }
    }
    items(documents, key = { "doc-${it.id}" }) { doc -> DocumentCard(doc) { onClick(doc.id) } }
}

@Composable
private fun DocumentCard(doc: CarDocument, onClick: () -> Unit) {
    val status = expiryStatus(doc.expiry)
    val color = urgencyColor(status.urgency)
    val total = ChronoUnit.DAYS.between(doc.issued, doc.expiry).coerceAtLeast(1)
    ItemCard(onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(R.drawable.ic_document, tint = color)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(doc.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Valabil până la ${doc.expiry.display()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(if (status.urgency == Urgency.OK) "${status.daysLeft} zile" else daysLeftLabel(status.daysLeft), status.urgency)
        }
        Spacer(Modifier.height(12.dp))
        ProgressLine(status.daysLeft.toFloat() / total, color, Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text(
                "Emis ${doc.issued.display()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            doc.price?.let {
                Text(it.lei(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun LazyListScope.serviceTab(
    upcoming: List<UpcomingMaintenance>,
    records: List<Maintenance>,
    currentKm: Int,
    onClick: (Int) -> Unit,
) {
    if (upcoming.isNotEmpty()) {
        item { SectionTitle("Ce urmează", Modifier.padding(horizontal = 20.dp)) }
        items(upcoming, key = { "up-${it.rule.name}" }) { UpcomingCard(it, currentKm) }
    }
    if (records.isEmpty()) {
        item {
            EmptyHint(
                null,
                "Nicio intervenție încă",
                "Adaugă ultimul schimb de ulei sau ultima revizie și îți spunem când urmează următoarea.",
            )
        }
        return
    }
    item { SectionTitle("Intervenții efectuate", Modifier.padding(horizontal = 20.dp).padding(top = 8.dp)) }
    items(records, key = { "m-${it.id}" }) { m ->
        ItemCard(onClick = { onClick(m.id) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(vector = Icons.Default.Build, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${m.localDate.display()} · ${m.mileage.km()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                m.price?.let { Text(it.lei(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun UpcomingCard(u: UpcomingMaintenance, currentKm: Int) {
    val color = urgencyColor(u.urgency)
    // cât din interval s-a „consumat”: luăm maximul dintre km și timp
    val kmFraction = u.rule.everyKm?.let { (currentKm - u.last.mileage).toFloat() / it } ?: 0f
    val dayFraction = u.dueDate?.let {
        val total = ChronoUnit.DAYS.between(u.last.localDate, it).coerceAtLeast(1)
        ChronoUnit.DAYS.between(u.last.localDate, LocalDate.now()).toFloat() / total
    } ?: 0f
    ItemCard(onClick = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(u.rule.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Următoarea la " + listOfNotNull(u.dueKm?.km(), u.dueDate?.display()).joinToString(" sau "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(upcomingLabel(u), u.urgency)
        }
        Spacer(Modifier.height(12.dp))
        ProgressLine(maxOf(kmFraction, dayFraction), color, Modifier.fillMaxWidth())
        Text(
            "Ultima: ${u.last.localDate.display()} la ${u.last.mileage.km()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private fun upcomingLabel(u: UpcomingMaintenance): String {
    val km = u.kmLeft
    val days = u.daysLeft
    return when {
        km != null && km < 0 -> "depășit cu ${(-km).km()}"
        days != null && days < 0 -> "întârziat ${-days} zile"
        // arătăm termenul care se apropie primul: km sau zile
        km != null && km <= 2_000 -> "încă ${km.km()}"
        days != null && days <= 60 -> "$days zile"
        km != null -> "încă ${km.km()}"
        days != null -> "$days zile"
        else -> ""
    }
}

/** Un eveniment din istoric: fie un act, fie o intervenție. */
private data class HistoryEntry(
    val key: String,
    val date: LocalDate,
    val title: String,
    val subtitle: String,
    val price: Double?,
    val isDocument: Boolean,
    val id: Int,
)

private fun LazyListScope.historyTab(
    documents: List<CarDocument>,
    records: List<Maintenance>,
    onDoc: (Int) -> Unit,
    onService: (Int) -> Unit,
) {
    val entries = (
        documents.map {
            HistoryEntry("hd-${it.id}", it.issued, it.title, "Act · valabil până la ${it.expiry.display()}", it.price, true, it.id)
        } + records.map {
            HistoryEntry("hm-${it.id}", it.localDate, it.title, "Service · ${it.mileage.km()}", it.price, false, it.id)
        }
    ).sortedByDescending { it.date }

    if (entries.isEmpty()) {
        item { EmptyHint(null, "Istoricul e gol", "Tot ce adaugi pentru mașina asta va apărea aici, în ordine cronologică.") }
        return
    }
    val total = entries.sumOf { it.price ?: 0.0 }
    item {
        Text(
            "${entries.size} înregistrări · ${total.lei()} în total",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
    items(entries.size, key = { entries[it].key }) { i ->
        val e = entries[i]
        val showYear = i == 0 || entries[i - 1].date.year != e.date.year
        if (showYear) {
            Text(
                e.date.year.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 20.dp, top = if (i == 0) 4.dp else 16.dp, bottom = 4.dp),
            )
        }
        TimelineRow(e, isLast = i == entries.lastIndex) { if (e.isDocument) onDoc(e.id) else onService(e.id) }
    }
}

@Composable
private fun TimelineRow(e: HistoryEntry, isLast: Boolean, onClick: () -> Unit) {
    val color = if (e.isDocument) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp),
    ) {
        // coloana cu punct + linie verticală
        Column(Modifier.width(28.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(22.dp))
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
            if (!isLast) {
                Box(Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            modifier = Modifier.weight(1f).padding(vertical = 5.dp),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        e.date.display(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(e.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(e.subtitle, style = MaterialTheme.typography.bodySmall, color = color)
                }
                e.price?.let { Text(it.lei(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun ItemCard(onClick: (() -> Unit)?, content: @Composable () -> Unit) {
    val modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
    val shape = RoundedCornerShape(18.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun EmptyHint(icon: Int?, title: String, text: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) IconTile(icon, tint = MaterialTheme.colorScheme.primary, size = 64.dp)
        else IconTile(vector = Icons.Default.Build, tint = MaterialTheme.colorScheme.tertiary, size = 64.dp)
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MileageDialog(current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var value by remember { mutableStateOf(current.toString()) }
    val parsed = value.toIntOrNull()
    val valid = parsed != null && parsed >= current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_speed), null) },
        title = { Text("Kilometraj curent") },
        text = {
            Column {
                NumberField("Kilometri", value, { value = it.take(7) }, suffix = "km", isError = !valid)
                if (parsed != null && parsed < current) {
                    Text(
                        "Kilometrajul nu poate scădea (acum: ${current.km()}).",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(parsed!!) }) { Text("Salvează") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } },
    )
}
