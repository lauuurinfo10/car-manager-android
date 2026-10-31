package com.example.carmanager.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.carmanager.R
import com.example.carmanager.data.ApiClient
import com.example.carmanager.data.Car
import com.example.carmanager.data.ExpiringDocument
import com.example.carmanager.domain.Urgency
import com.example.carmanager.domain.daysLeftLabel
import com.example.carmanager.ui.components.ErrorBox
import com.example.carmanager.ui.components.IconTile
import com.example.carmanager.ui.components.LoadingBox
import com.example.carmanager.ui.components.PlateBadge
import com.example.carmanager.ui.components.SectionTitle
import com.example.carmanager.ui.components.StatusPill
import com.example.carmanager.ui.components.urgencyColor
import com.example.carmanager.ui.displayDate
import com.example.carmanager.ui.km
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val cars: List<Car> = emptyList(),
    val alerts: List<ExpiringDocument> = emptyList(),
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(refreshing = !it.loading, error = null) }
        viewModelScope.launch {
            try {
                // coroutineScope: dacă o cerere eșuează, eroarea ajunge în catch, nu dărâmă aplicația
                val newState = coroutineScope {
                    val cars = async { ApiClient.api.listCars() }
                    val alerts = async { ApiClient.api.expiringDocuments(30) }
                    HomeState(loading = false, cars = cars.await(), alerts = alerts.await())
                }
                _state.value = newState
            } catch (t: Throwable) {
                _state.update { it.copy(loading = false, refreshing = false, error = ApiClient.errorMessage(t)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCarClick: (Int) -> Unit,
    onAddCar: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    // reîncărcăm de fiecare dată când revenim pe ecran (ex. după ce am adăugat o mașină)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.load() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Garajul meu", fontWeight = FontWeight.Bold) },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCar,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Adaugă mașină") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        val s = state
        when {
            s.loading -> Column(Modifier.padding(padding)) { LoadingBox() }
            s.error != null && s.cars.isEmpty() -> Column(Modifier.padding(padding)) { ErrorBox(s.error, vm::load) }
            else -> PullToRefreshBox(
                isRefreshing = s.refreshing,
                onRefresh = vm::load,
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                HomeContent(s, onCarClick)
            }
        }
    }
}

@Composable
private fun HomeContent(state: HomeState, onCarClick: (Int) -> Unit) {
    val carsById = state.cars.associateBy { it.id }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.error != null) {
            item { Text(state.error, color = MaterialTheme.colorScheme.error) }
        }
        if (state.cars.isEmpty()) {
            item { EmptyGarage() }
            return@LazyColumn
        }
        item { SummaryCard(state.cars.size, state.alerts) }
        if (state.alerts.isNotEmpty()) {
            item { SectionTitle("Necesită atenție", Modifier.padding(top = 6.dp)) }
            items(state.alerts, key = { "alert-${it.id}" }) { doc ->
                AlertRow(doc, carsById[doc.carId], onClick = { onCarClick(doc.carId) })
            }
        }
        item { SectionTitle("Mașinile tale", Modifier.padding(top = 6.dp)) }
        items(state.cars, key = { "car-${it.id}" }) { car ->
            val carAlerts = state.alerts.filter { it.carId == car.id }
            CarCard(car, carAlerts, onClick = { onCarClick(car.id) })
        }
    }
}

/** Banner de sus: „Totul e în regulă” sau „N acte necesită atenție”. */
@Composable
private fun SummaryCard(carCount: Int, alerts: List<ExpiringDocument>) {
    val expired = alerts.count { it.daysLeft < 0 }
    val (start, end) = when {
        expired > 0 -> Color(0xFFC62828) to Color(0xFFE53935)
        alerts.isNotEmpty() -> Color(0xFFD97706) to Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary to Color(0xFF3F7FD9)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(start, end)))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        expired > 0 -> if (expired == 1) "Un act a expirat!" else "$expired acte au expirat!"
                        alerts.isNotEmpty() -> if (alerts.size == 1) "Un act expiră curând" else "${alerts.size} acte expiră curând"
                        else -> "Totul e în regulă"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (alerts.isEmpty()) "Niciun act nu expiră în următoarele 30 de zile."
                    else "Verifică lista de mai jos și reînnoiește la timp.",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (carCount == 1) "1 mașină în garaj" else "$carCount mașini în garaj",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Icon(
                if (alerts.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(56.dp),
            )
        }
    }
}

@Composable
private fun AlertRow(doc: ExpiringDocument, car: Car?, onClick: () -> Unit) {
    val urgency = if (doc.daysLeft < 0) Urgency.OVERDUE else Urgency.SOON
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(R.drawable.ic_document, tint = urgencyColor(urgency))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(doc.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(car?.displayName, doc.expiryDate.displayDate()).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(daysLeftLabel(doc.daysLeft.toLong()), urgency)
        }
    }
}

@Composable
private fun CarCard(car: Car, alerts: List<ExpiringDocument>, onClick: () -> Unit) {
    val urgency = when {
        alerts.any { it.daysLeft < 0 } -> Urgency.OVERDUE
        alerts.isNotEmpty() -> Urgency.SOON
        else -> Urgency.OK
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(R.drawable.ic_car, tint = MaterialTheme.colorScheme.primary, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(car.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Fabricată în ${car.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlateBadge(car.licensePlate)
                Spacer(Modifier.width(10.dp))
                Icon(painterResource(R.drawable.ic_speed), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(car.currentMileage.km(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                StatusPill(
                    when (urgency) {
                        Urgency.OK -> "în regulă"
                        Urgency.SOON -> if (alerts.size == 1) "1 alertă" else "${alerts.size} alerte"
                        Urgency.OVERDUE -> "expirat"
                    },
                    urgency,
                )
            }
        }
    }
}

@Composable
private fun EmptyGarage() {
    Column(
        Modifier.fillMaxWidth().padding(top = 64.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconTile(R.drawable.ic_car, tint = MaterialTheme.colorScheme.primary, size = 96.dp)
        Spacer(Modifier.height(20.dp))
        Text("Garajul e gol", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Adaugă prima mașină și ține evidența RCA-ului, ITP-ului, rovinietei și a reviziilor într-un singur loc.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
