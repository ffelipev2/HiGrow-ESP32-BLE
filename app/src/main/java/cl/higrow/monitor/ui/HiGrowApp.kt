package cl.higrow.monitor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        val tabletHeader = maxWidth >= 900.dp && maxHeight >= 500.dp && LocalDensity.current.fontScale <= 1.3f
        Scaffold(containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                val title = if (detail != null) "Detalle del sensor" else if (selected == 0) "HiGrow Monitor" else tabs[selected]
                if (tabletHeader) TabletHeader(title, selected == 0 && detail == null,
                    onHome = { detail = null; selected = 0 }, onSettings = { detail = null; selected = 3 })
                else TopAppBar(title = { Text(title,
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
                if (!useRail) {
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
                }
            }) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    if (useRail) {
                        TabletNavigation(tabs, icons, selected) { detail = null; selected = it }
                    }
                    Surface(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surface) {
                        BoxWithConstraints(Modifier.fillMaxSize()
                            .padding(horizontal = if (useRail) 16.dp else 14.dp, vertical = 14.dp)) {
                            CompositionLocalProvider(LocalScreenViewport provides ScreenViewport(maxWidth, maxHeight)) {
                                key(selected, detail) {
                                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                                        when {
                                            detailMetric != null -> SensorDetailScreen(state, detailMetric)
                                            selected == 0 -> DashboardScreen(state, openConnection, { detail = it.ordinal }, onDisconnect)
                                            selected == 1 -> GraphsScreen(state, { detail = it.ordinal })
                                            selected == 2 -> HistoryScreen(state)
                                            else -> SettingsScreen(state, openConnection, onDisconnect,
                                                onAutoReconnect, { editThresholds = true })
                                        }
                                        Spacer(Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
    if (bleSheet) {
        ModalBottomSheet(onDismissRequest = { bleSheet = false; onStopScan() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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

@Composable
private fun TabletHeader(title: String, home: Boolean, onHome: () -> Unit, onSettings: () -> Unit) {
    Surface(color = ForestGreen, contentColor = Color.White) {
        Box {
            BotanicalLeaves(Modifier.matchParentSize())
            Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 96.dp)
                .padding(horizontal = 28.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (home) LeafMark(52) else IconButton(onHome) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver al inicio")
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineMedium, fontSize = 30.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (home) "Monitoreo de tus plantas" else "HiGrow Monitor · Bluetooth local",
                        style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD8E8D5))
                }
                Surface(onClick = onSettings, color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Outlined.Settings, "Abrir ajustes", Modifier.padding(14.dp).size(26.dp))
                }
            }
        }
    }
}

@Composable
private fun TabletNavigation(tabs: List<String>, icons: List<ImageVector>, selected: Int, onSelected: (Int) -> Unit) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Box(Modifier.width((104 * fontScale).dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
        .background(MaterialTheme.colorScheme.surface)) {
        BotanicalLeaves(Modifier.align(Alignment.BottomCenter).width(180.dp).height(160.dp), subtle = true)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tabs.forEachIndexed { index, title ->
                val active = selected == index
                Column(Modifier.width((84 * fontScale).dp).heightIn(min = (82 * fontScale).dp).clip(RoundedCornerShape(20.dp))
                    .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .selectable(active, role = Role.Tab, onClick = { onSelected(index) }).padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically)) {
                    val ink = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(icons[index], null, Modifier.size(28.dp), tint = ink)
                    Text(title, style = MaterialTheme.typography.labelMedium, color = ink)
                }
            }
        }
    }
}

@Composable fun LeafMark(size: Int = 36) {
    Box(Modifier.size(size.dp).background(
        Brush.linearGradient(listOf(Color(0xFF76C64D), Color(0xFF17602C))), RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Eco, null, tint = Color.White, modifier = Modifier.size((size * 0.77f).dp))
    }
}
