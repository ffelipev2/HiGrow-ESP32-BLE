package cl.higrow.monitor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.MonitorUiState
import cl.higrow.monitor.ble.ConnectionStatus
import cl.higrow.monitor.data.LightThresholds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiGrowApp(state: MonitorUiState, onScan: () -> Unit, onStopScan: () -> Unit,
    onConnect: (String) -> Unit, onDisconnect: () -> Unit, onThresholds: (LightThresholds) -> Unit,
    onAutoReconnect: (Boolean) -> Unit, initialTab: Int = 0, initialDetail: SensorMetric? = null) {
    var selected by rememberSaveable { mutableIntStateOf(initialTab) }
    var detail by rememberSaveable { mutableStateOf(initialDetail?.ordinal) }
    var bleSheet by rememberSaveable { mutableStateOf(false) }
    var editThresholds by rememberSaveable { mutableStateOf(false) }
    val tabs = listOf("Inicio", "Gráficos", "Historial", "Ajustes")
    val icons = listOf(Icons.Filled.Home, Icons.Outlined.BarChart, Icons.Outlined.History, Icons.Outlined.Settings)
    val detailMetric = detail?.let { SensorMetric.entries.getOrNull(it) }
    val openConnection = {
        bleSheet = true
        if (state.connection.status in listOf(ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR)) onScan()
    }
    BackHandler(enabled = detail != null || selected != 0) {
        if (detail != null) detail = null else selected = 0
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text(if (detail != null) "Detalle del sensor" else if (selected == 0) "HiGrow Monitor" else tabs[selected],
                maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (detail != null || selected != 0) IconButton(onClick = { detail = null; selected = 0 }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver al inicio")
                    } else Box(Modifier.padding(start = 16.dp, end = 9.dp)) { LeafMark(35) }
                },
                actions = {
                    if (selected != 3) IconButton(onClick = { detail = null; selected = 3 }) {
                        Icon(Icons.Outlined.Settings, "Abrir ajustes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ForestGreen,
                    titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White))
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(selected = selected == index, onClick = { detail = null; selected = index },
                        icon = { Icon(icons[index], null, Modifier.size(24.dp)) },
                        label = { Text(title, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface))
                }
            }
        }) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                key(selected, detail) {
                    Column(Modifier.widthIn(max = 1000.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp)) {
                        when {
                            detailMetric != null -> SensorDetailScreen(state, detailMetric)
                            selected == 0 -> DashboardScreen(state, openConnection, { detail = it.ordinal }, onDisconnect)
                            selected == 1 -> GraphsScreen(state, { detail = it.ordinal })
                            selected == 2 -> HistoryScreen(state)
                            else -> SettingsScreen(state, openConnection, onDisconnect, onAutoReconnect, { editThresholds = true })
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
    if (bleSheet) {
        ModalBottomSheet(onDismissRequest = { bleSheet = false; onStopScan() },
            containerColor = MaterialTheme.colorScheme.surface) {
            ScanScreen(state, onScan, onStopScan,
                onConnect = { address -> onConnect(address); bleSheet = false },
                onDisconnect = onDisconnect, onClose = { bleSheet = false; onStopScan() })
        }
    }
    if (editThresholds) ThresholdDialog(state.lightThresholds, { editThresholds = false }) {
        onThresholds(it); editThresholds = false
    }
}

@Composable fun LeafMark(size: Int = 36) {
    Box(Modifier.size(size.dp).background(
        Brush.linearGradient(listOf(Color(0xFF76C64D), Color(0xFF17602C))), RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Eco, null, tint = Color.White, modifier = Modifier.size((size * 0.77f).dp))
    }
}
