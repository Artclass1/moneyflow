package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.Persona
import com.example.api.Personas
import com.example.data.ChatMessage
import com.example.data.ChatSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// Sophisticated Dark Style Palette
private val CustomDeepBlack = Color(0xFF050505)
private val CustomCardCharcoal = Color(0xFF0A0A0A)
private val CustomElevatedGray = Color(0xFF111111)
private val AccentEmerald = Color(0xFF10B981)
private val SupportBlue = Color(0xFF3B82F6)
private val AmberGold = Color(0xFFFBBF24)
private val BorderWhite5 = Color(0x0DFFFFFF)
private val BorderWhite10 = Color(0x1AFFFFFF)

// World Node Data representation for our interactive visualizer
data class MapNode(
    val id: String,
    val name: String,
    val xPercent: Float, // Relative X coordinates on our canvas board
    val yPercent: Float, // Relative Y coordinates on our canvas board
    val category: String, // "COUNTRY" or "ASSET"
    val netFlow: String,
    val detail: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val selectedPersonaId by viewModel.selectedPersonaId.collectAsStateWithLifecycle()
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val apiKeyStatus by viewModel.apiKeyStatus.collectAsStateWithLifecycle()

    // Real-Time live financial feeds
    val cryptoAssets by viewModel.cryptoAssets.collectAsStateWithLifecycle()
    val forexExchangeRates by viewModel.forexExchangeRates.collectAsStateWithLifecycle()
    val isRefreshingFinancialData by viewModel.isRefreshingFinancialData.collectAsStateWithLifecycle()
    val financialDataLastUpdated by viewModel.financialDataLastUpdated.collectAsStateWithLifecycle()

    val currentPersona = remember(selectedPersonaId) { Personas.getById(selectedPersonaId) }
    val currentSession = remember(sessions, currentSessionId) {
        sessions.find { it.id == currentSessionId }
    }

    var showRenameDialog by remember { mutableStateOf<ChatSession?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    // Top view segment navigation tab
    var activeTab by remember { mutableStateOf("FLOWS") } // "CONSOLE" or "FLOWS"

    // Set up interactive Map and Simulation state
    val worldNodes = remember {
        listOf(
            MapNode("US", "USA / WallStreet", 0.22f, 0.40f, "COUNTRY", "+$42.5B", "Institutional Treasury buying & tech sector cash flows", "BULK INFLOW"),
            MapNode("EU", "EU Sovereign Grid", 0.45f, 0.35f, "COUNTRY", "-$12.4B", "Capital outflow into gold backing reserves", "STEADY OUTFLOW"),
            MapNode("CN", "China / Shanghai", 0.72f, 0.42f, "COUNTRY", "+$18.1B", "Domestic stimulus liquidity recirculation", "STABLE"),
            MapNode("JP", "Japan / Yen Vaults", 0.85f, 0.48f, "COUNTRY", "-$28.2B", "Yen carry-trade unwinding global dispersion", "HIGH LIQUIDITY"),
            MapNode("ME", "Middle-East / Crude", 0.58f, 0.58f, "COUNTRY", "+$14.8B", "Sovereign wealth diversification to alternatives", "DIVERSIFYING"),
            MapNode("HK", "HK / Crypto Grid", 0.76f, 0.55f, "ASSET", "+$34.1B", "Cross-border OTC onchain capital injection", "HEAVY INFLOW"),
            MapNode("GLD", "London Spot Gold", 0.42f, 0.58f, "ASSET", "+$21.6B", "Risk hedging sovereign safe haven consolidation", "BULLISH FLOW"),
            MapNode("FX", "Forex Liquidity Hub", 0.32f, 0.65f, "ASSET", "+$108.4B", "Global central banks inter-operability pool", "MASSIVE VOLUME")
        )
    }

    var selectedNode by remember { mutableStateOf<MapNode?>(worldNodes.first()) }

    // Interactive simulator inputs
    var simulatorOrigin by remember { mutableStateOf("US") }
    var simulatorTarget by remember { mutableStateOf("HK") }
    var simulatorVolume by remember { mutableStateOf(5.5f) } // Billions scale
    var simulatorAssetClass by remember { mutableStateOf("CRYPTO") } // "STOCK", "FOREX", "GOLD", "CRYPTO"
    var simulatedResultText by remember { mutableStateOf<String?>(null) }



    // Calculate interactive simulation outcomes dynamically
    LaunchedEffect(simulatorOrigin, simulatorTarget, simulatorVolume, simulatorAssetClass) {
        val originNode = worldNodes.find { it.id == simulatorOrigin }?.name ?: simulatorOrigin
        val targetNode = worldNodes.find { it.id == simulatorTarget }?.name ?: simulatorTarget
        val impact = when {
            simulatorVolume < 2.0f -> "MINIMAL IMPACT"
            simulatorVolume < 8.0f -> "MODERATE SEGMENT SHIFT"
            else -> "CRITICAL SYSTEMIC CONGESTION DETECTED"
        }
        val rateEffect = when (simulatorAssetClass) {
            "STOCK" -> "NASDAQ composite index expected divergence bias +${String.format("%.2f", simulatorVolume * 0.12)}%"
            "FOREX" -> "Regional FX liquidity spread variation: Spot spreads widen by +${String.format("%.1f", simulatorVolume * 1.5)} pips"
            "GOLD" -> "Central vaults transfer confirmation in London Spot Gold: Vault backing expands by +${String.format("%.1f", simulatorVolume * 3.2)} tonnes"
            "CRYPTO" -> "On-chain BTC spot orderbook depth absorption: Liquidity pool utilization ratio +${String.format("%.1f", simulatorVolume * 2.1)}x"
            else -> "Standard SWIFT settlement sequence completed"
        }
        simulatedResultText = "Capital Route: $originNode → $targetNode [$$simulatorVolume Billion via $simulatorAssetClass]\nResult: $impact • $rateEffect."
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                drawerContainerColor = CustomDeepBlack,
                drawerTonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    // Header Area of Drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "MONEYFLOW SYSTEMS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "CORE CONSOLE",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        // SECURE status Pill in Drawer
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(CustomElevatedGray, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderWhite5, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AccentEmerald)
                            )
                            Text(
                                text = "ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentEmerald,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Action: Create New Chat
                    Button(
                        onClick = {
                            viewModel.createNewSession(selectedPersonaId)
                            activeTab = "CONSOLE"
                            scope.launch { drawerState.close() }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentEmerald,
                            contentColor = CustomDeepBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .testTag("new_chat_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Chat")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Session",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    HorizontalDivider(color = BorderWhite5, modifier = Modifier.padding(bottom = 16.dp))

                    Text(
                        text = "CHANNELS / AI INSTANCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Persona Selection Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Personas.ALL.forEach { p ->
                            val isSelected = p.id == selectedPersonaId
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) Color(p.color).copy(alpha = 0.15f)
                                        else CustomElevatedGray
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(p.color) else BorderWhite5,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        viewModel.createNewSession(p.id)
                                        activeTab = "CONSOLE"
                                        scope.launch { drawerState.close() }
                                    }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = getPersonaEmoji(p.id),
                                        fontSize = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = p.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(p.color) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BorderWhite5, modifier = Modifier.padding(bottom = 16.dp))

                    Text(
                        text = "COMMUNICATION ARCHIVES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Chat archives/sessions list
                    Box(modifier = Modifier.weight(1f)) {
                        if (sessions.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No communication records yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8).copy(alpha = 0.5f)
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sessions, key = { it.id }) { session ->
                                    val isSelected = session.id == currentSessionId
                                    val personaObj = remember(session.personaId) { Personas.getById(session.personaId) }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) CustomElevatedGray
                                                else Color.Transparent
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) BorderWhite5 else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                viewModel.selectSession(session.id)
                                                activeTab = "CONSOLE"
                                                scope.launch { drawerState.close() }
                                            }
                                            .padding(10.dp)
                                            .testTag("session_item_${session.id}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(personaObj.color).copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = getPersonaEmoji(session.personaId),
                                                fontSize = 16.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.title,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = personaObj.name,
                                                fontSize = 10.sp,
                                                color = Color(personaObj.color),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                renameInputText = session.title
                                                showRenameDialog = session
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        IconButton(
                                            onClick = { viewModel.deleteSession(session) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalDivider(color = BorderWhite5, modifier = Modifier.padding(bottom = 12.dp))

                    // Wipe memory completely
                    OutlinedButton(
                        onClick = {
                            viewModel.clearAllHistory()
                            activeTab = "CONSOLE"
                            scope.launch { drawerState.close() }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Purge Workspace", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Purge Workspace", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = Color.White
                                )
                            }
                        },
                        title = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "GLOBAL SURVEILLANCE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = if (activeTab == "FLOWS") "MONEYFLOW." else (currentSession?.title ?: "Secure Console"),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Interactive green pulsive beacon
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .background(CustomElevatedGray, RoundedCornerShape(14.dp))
                                        .border(1.dp, BorderWhite5, RoundedCornerShape(14.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
                                    val pulseAlpha by infiniteTransition.animateFloat(
                                        initialValue = 0.3f, targetValue = 1.0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000, easing = EaseInOutCirc),
                                            repeatMode = RepeatMode.Reverse
                                        ), label = "beacon"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(AccentEmerald.copy(alpha = pulseAlpha))
                                    )
                                    Text(
                                        text = "LIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = CustomDeepBlack,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )

                    // Navigation Tabs Header (Sophisticated M3 Style Switcher)
                    TabRow(
                        selectedTabIndex = if (activeTab == "FLOWS") 0 else 1,
                        containerColor = CustomDeepBlack,
                        contentColor = AccentEmerald,
                        divider = { HorizontalDivider(color = BorderWhite5) }
                    ) {
                        Tab(
                            selected = activeTab == "FLOWS",
                            onClick = { activeTab = "FLOWS" },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share, 
                                    contentDescription = "Flow mapping",
                                    tint = if (activeTab == "FLOWS") AccentEmerald else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "WORLD FLOWS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp,
                                    color = if (activeTab == "FLOWS") Color.White else Color(0xFF64748B)
                                )
                            }
                        }
                        Tab(
                            selected = activeTab == "CONSOLE",
                            onClick = { activeTab = "CONSOLE" },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face, 
                                    contentDescription = "AI companion",
                                    tint = if (activeTab == "CONSOLE") AccentEmerald else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "AI COMPANION",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp,
                                    color = if (activeTab == "CONSOLE") Color.White else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            },
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            containerColor = CustomDeepBlack
        ) { scaffoldPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .drawBehind {
                        drawRect(color = CustomDeepBlack)
                        // Background Grid Overlay
                        val gridColor = Color(0x06FFFFFF)
                        val step = 32.dp.toPx()
                        for (x in 0..size.width.toInt() step step.toInt()) {
                            drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1f)
                        }
                        for (y in 0..size.height.toInt() step step.toInt()) {
                            drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
                        }
                    }
            ) {
                if (activeTab == "FLOWS") {
                    // WORLDWIDE MONITORING DASHBOARD (Decentralized Real-Time live-fed MoneyFlow Terminal)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top Sync and Status Banner
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CustomCardCharcoal),
                                border = BorderStroke(1.dp, BorderWhite10),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(AccentEmerald)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "REAL-TIME MONITOR",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (financialDataLastUpdated.isNotBlank()) "Last Sync: $financialDataLastUpdated" else "Syncing live feeds...",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.fetchRealFinancialData() },
                                        enabled = !isRefreshingFinancialData,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CustomElevatedGray,
                                            contentColor = Color.White
                                        ),
                                        border = BorderStroke(1.dp, BorderWhite10),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (isRefreshingFinancialData) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    strokeWidth = 1.5.dp,
                                                    color = AccentEmerald
                                                )
                                                Text("SYNCING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentEmerald)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "refresh",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = Color.White
                                                )
                                                Text("RELOAD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Hero Consolidated Net Capital flow Metric
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CustomCardCharcoal),
                                border = BorderStroke(1.dp, BorderWhite10),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "NET GLOBAL CROSS-BORDER LIQUIDITY FLOWS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 1.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$142,842,901,450",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraLight,
                                        color = Color.White,
                                        fontFamily = FontFamily.Serif
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(AccentEmerald.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "+4.2% FLOW DELTA",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentEmerald,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            text = "Active digital assets & forex settlement matrix",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }

                        // LIVE CRYPTO MARKET FEED (Dynamic data from CoinCap)
                        item {
                            Text(
                                text = "LIVE CRYPTONET FLOWS (COINCAP API)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = Color.White,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                            )
                        }

                        if (cryptoAssets.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = CustomElevatedGray),
                                    border = BorderStroke(1.dp, BorderWhite5),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = AccentEmerald
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Awaiting decentralized data node network...",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(cryptoAssets) { coin ->
                                val price = coin.priceUsd.toDoubleOrNull() ?: 0.0
                                val formattedPrice = if (price >= 1.0) {
                                    "$${String.format("%,.2f", price)}"
                                } else {
                                    "$${String.format("%.4f", price)}"
                                }
                                
                                val change = coin.changePercent24Hr?.toDoubleOrNull() ?: 0.0
                                val formattedChange = "${if (change > 0) "+" else ""}${String.format("%.2f", change)}%"
                                val changeColor = if (change >= 0) AccentEmerald else Color(0xFFEF4444)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = CustomCardCharcoal),
                                    border = BorderStroke(1.dp, BorderWhite5),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(AccentEmerald.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (coin.symbol == "BTC") "₿" else if (coin.symbol == "ETH") "Ξ" else if (coin.symbol == "SOL") "☀️" else "🪙",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentEmerald
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = coin.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = coin.symbol,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = formattedPrice,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(changeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = formattedChange,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = changeColor,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // LIVE FOREX EXCHANGE RATES SPREAD (Dynamic from ER-API)
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "LIVE FOREIGN EXCHANGE SPREADS (ER-API)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = Color.White,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                            )
                        }

                        if (forexExchangeRates.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = CustomElevatedGray),
                                    border = BorderStroke(1.dp, BorderWhite5),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = AmberGold
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Connecting to ER foreign exchange terminal...",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                val popularCurrencies = listOf(
                                    Triple("EUR", "Eurozone", "🇪🇺"),
                                    Triple("GBP", "Great Britain", "🇬🇧"),
                                    Triple("JPY", "Japan", "🇯🇵"),
                                    Triple("CNY", "China", "🇨🇳"),
                                    Triple("HKD", "Hong Kong", "🇭🇰"),
                                    Triple("AUD", "Australia", "🇦🇺")
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    popularCurrencies.forEach { (code, region, flag) ->
                                        val rate = forexExchangeRates[code]
                                        val formattedRate = if (rate != null) {
                                            String.format("%.4f", rate)
                                        } else {
                                            "—"
                                        }

                                        Card(
                                            modifier = Modifier
                                                .width(115.dp)
                                                .border(BorderStroke(1.dp, BorderWhite5), RoundedCornerShape(12.dp)),
                                            colors = CardDefaults.cardColors(containerColor = CustomCardCharcoal),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = code, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                                    Text(text = flag, fontSize = 16.sp)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = region, fontSize = 9.sp, color = Color(0xFF64748B))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = formattedRate,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.White
                                                )
                                                Text(text = "per 1 USD", fontSize = 8.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // HIGH-SPEED TRANSACTION ROUTER & COMPASS
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CustomCardCharcoal),
                                border = BorderStroke(1.dp, BorderWhite10),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CAPITAL TRANS-ROUTE ANALYZER",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B),
                                            letterSpacing = 1.5.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(SupportBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "SANDBOX",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SupportBlue
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Origin and Target Node Selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("ORIGIN CURRENCY", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(CustomElevatedGray)
                                                    .border(1.dp, BorderWhite5, RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        simulatorOrigin = if (simulatorOrigin == "US") "JP" else if (simulatorOrigin == "JP") "EU" else "US"
                                                    }
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = "🌐 USD ($simulatorOrigin)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "dropdown", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("INVESTMENT SHIELD", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(CustomElevatedGray)
                                                    .border(1.dp, BorderWhite5, RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        simulatorTarget = if (simulatorTarget == "HK") "GLD" else if (simulatorTarget == "GLD") "FX" else "HK"
                                                    }
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (simulatorTarget == "HK") "₿ BTC" else if (simulatorTarget == "GLD") "🏆 XAU/GOLD" else "💱 FOREX FX",
                                                    fontSize = 11.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "dropdown", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Slider volume
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("FLOW IMPACT WEIGHT", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                        Text("$$simulatorVolume Billion USD", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Slider(
                                        value = simulatorVolume,
                                        onValueChange = { simulatorVolume = String.format("%.1f", it).toFloat() },
                                        valueRange = 0.5f..20.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = AccentEmerald,
                                            activeTrackColor = AccentEmerald,
                                            inactiveTrackColor = BorderWhite10
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(24.dp)
                                    )

                                    // Real-time computation output layout
                                    simulatedResultText?.let { result ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(CustomElevatedGray)
                                                .border(1.dp, BorderWhite5, RoundedCornerShape(12.dp))
                                                .padding(12.dp)
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Info, contentDescription = "info", tint = SupportBlue, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("PREDICTIVE TRANSIT IMPACT MATRIX", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SupportBlue)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = result,
                                                    fontSize = 11.sp,
                                                    color = Color.White,
                                                    lineHeight = 15.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Connects directly to Gemini Companion
                                        Button(
                                            onClick = {
                                                val explanationPrompt = "Analyze the macroeconomics and liquidity velocity in real-time when transferring $$simulatorVolume Billion from US ($simulatorOrigin) to $simulatorTarget via the smart router system. Use our live fetched prices (e.g., BTC, EUR rates) to explain currency risks."
                                                viewModel.sendMessage(explanationPrompt)
                                                activeTab = "CONSOLE"
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AccentEmerald,
                                                contentColor = CustomDeepBlack
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Send, contentDescription = "send", modifier = Modifier.size(12.dp))
                                                Text("Discuss system security details with Gemini AI", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // CONSTRUCT ORIGINAL GEMINI CONVERTER INTERFACES
                    Column(modifier = Modifier.fillMaxSize()) {
                        // API Warning Bar if key is missing
                        if (apiKeyStatus is ApiKeyStatus.Missing) {
                            ApiKeyWarningBar()
                        }

                        // Message Feed Area
                        Box(modifier = Modifier.weight(1f)) {
                            if (messages.isEmpty()) {
                                WelcomeScreen(
                                    persona = currentPersona,
                                    onStarterClick = { viewModel.sendMessage(it) }
                                )
                            } else {
                                val listState = rememberLazyListState()

                                // Scroll to bottom naturally
                                LaunchedEffect(messages.size, isGenerating) {
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }

                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(messages) { message ->
                                        MessageRow(message, currentPersona)
                                    }

                                    if (isGenerating) {
                                        item {
                                            StreamingIndicatorRow(currentPersona)
                                        }
                                    }

                                    if (errorMessage != null) {
                                        item {
                                            ErrorIndicatorRow(errorMessage!!)
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = BorderWhite5)

                        // Input control bar
                        MessageInputPanel(
                            isGenerating = isGenerating,
                            onSend = { viewModel.sendMessage(it) }
                        )
                    }
                }
            }
        }
    }

    // Rename Session Modal Dialog Box
    showRenameDialog?.let { session ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Session ID", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    label = { Text("Unique Code Identifier") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = CustomElevatedGray,
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInputText.isNotBlank()) {
                            viewModel.renameSession(session, renameInputText.trim())
                        }
                        showRenameDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AccentEmerald)
                ) {
                    Text("Rename", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRenameDialog = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun IndicatorPill(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .background(CustomElevatedGray, RoundedCornerShape(10.dp))
            .border(1.dp, BorderWhite5, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

fun PixelThemeColor(asset: String): Color {
    return when (asset) {
        "STOCK" -> SupportBlue
        "FOREX" -> Color.White
        "GOLD" -> AmberGold
        "CRYPTO" -> AccentEmerald
        else -> Color.White
    }
}

@Composable
fun ApiKeyWarningBar() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x26EF4444)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "API Warning",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Gemini API Key Missing",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Please enter your GEMINI_API_KEY inside the Secrets panel of the AI Studio workspace to communicate with the model.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    persona: Persona,
    onStarterClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High-end aesthetic header text
        Text(
            text = "COSMIC CONTEXT FEED",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 2.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Glowing brand serif display header
        Text(
            text = "${persona.name.uppercase()}.",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                letterSpacing = (-1.5).sp
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(persona.color).copy(alpha = 0.12f))
                .border(1.dp, Color(persona.color).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = persona.title.uppercase(),
                fontSize = 10.sp,
                color = Color(persona.color),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description text block
        Text(
            text = persona.description,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF94A3B8),
            modifier = Modifier.widthIn(max = 290.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Analytics Style Context Board
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CustomElevatedGray),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderWhite10)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ACTIVE CORE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 1.sp)
                    Text(getPersonaEmoji(persona.id) + " " + persona.name.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("LATENCY RATE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 1.sp)
                    Text("24 ms", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentEmerald, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("MODEL RATIO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 1.sp)
                    Text("128k (FLASH)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SupportBlue, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Text(
            text = "SELECT SEED INSTRUCTION",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Starter Prompts Stack styled as sleek, feed cards
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
        ) {
            persona.starterPrompts.forEach { prompt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CustomCardCharcoal)
                        .border(1.dp, BorderWhite5, RoundedCornerShape(14.dp))
                        .clickable { onStarterClick(prompt) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏛️",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = prompt,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Draft content immediately • Fast Stream",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Select",
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageRow(
    message: ChatMessage,
    currentPersona: Persona
) {
    val isUser = message.role == "USER"
    val align = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        val bubbleShape = if (isUser) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
        }

        val bubbleBg = if (isUser) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    Color(0xFF020617)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    CustomCardCharcoal,
                    CustomElevatedGray
                )
            )
        }

        val bubbleBorder = if (isUser) {
            BorderStroke(1.dp, Color(0x1BFFFFFF))
        } else {
            BorderStroke(1.dp, BorderWhite5)
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(bubbleShape)
                .background(bubbleBg)
                .border(bubbleBorder.width, bubbleBorder.brush, bubbleShape)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Column {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = getPersonaEmoji(currentPersona.id),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = currentPersona.name.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(currentPersona.color),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF334155))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SECURE FEED",
                            fontSize = 8.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "👤",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = "OPERATOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                    }
                }

                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
        val timeString = remember(message.timestamp) { formatter.format(Date(message.timestamp)) }

        Text(
            text = "$timeString // UTC",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun StreamingIndicatorRow(persona: Persona) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        val bubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)

        Row(
            modifier = Modifier
                .widthIn(max = 140.dp)
                .clip(bubbleShape)
                .background(CustomCardCharcoal)
                .border(1.dp, BorderWhite5, bubbleShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getPersonaEmoji(persona.id),
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 6.dp)
            )

            val infiniteTransition = rememberInfiniteTransition(label = "dots")
            val animatedDotAlpha1 by infiniteTransition.animateFloat(
                initialValue = 0.3f, targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = 0, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot1"
            )
            val animatedDotAlpha2 by infiniteTransition.animateFloat(
                initialValue = 0.3f, targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = 150, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot2"
            )
            val animatedDotAlpha3 by infiniteTransition.animateFloat(
                initialValue = 0.3f, targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = 300, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot3"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(persona.color).copy(alpha = animatedDotAlpha1)))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(persona.color).copy(alpha = animatedDotAlpha2)))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(persona.color).copy(alpha = animatedDotAlpha3)))
            }
        }
    }
}

@Composable
fun ErrorIndicatorRow(errorText: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x26EF4444)),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Error notification",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = errorText,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MessageInputPanel(
    isGenerating: Boolean,
    onSend: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CustomCardCharcoal,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickPill(text = "📡 Kotlin Sample") {
                    inputText += "```kotlin\nfun main() {\n    println(\"Cosmo feed compiled\")\n}\n```"
                }
                QuickPill(text = "🛰️ Exception Log") {
                    inputText += "```\nFatal Exception: NullPointerException on build target\n```"
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Compile text prompt...", color = Color(0xFF64748B)) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .testTag("chat_input_text"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CustomDeepBlack,
                        unfocusedContainerColor = CustomDeepBlack,
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = BorderWhite5,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { inputText = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear input",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    },
                    maxLines = 4,
                    singleLine = false
                )

                IconButton(
                    onClick = {
                        if (inputText.trim().isNotEmpty() && !isGenerating) {
                            onSend(inputText)
                            inputText = ""
                            keyboardController?.hide()
                        }
                    },
                    enabled = inputText.trim().isNotEmpty() && !isGenerating,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (inputText.trim().isEmpty() || isGenerating) CustomElevatedGray
                            else AccentEmerald
                        )
                        .border(
                            width = 1.dp,
                            color = if (inputText.trim().isEmpty() || isGenerating) Color.Transparent else Color(0x33FFFFFF),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputText.trim().isEmpty() || isGenerating) Color(0xFF475569)
                        else CustomDeepBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickPill(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CustomElevatedGray)
            .border(1.dp, BorderWhite5, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold
        )
    }
}

fun getPersonaEmoji(personaId: String): String {
    return when (personaId) {
        "general" -> "🤖"
        "creative" -> "🎨"
        "coding" -> "💻"
        "coach" -> "🌱"
        else -> "🤖"
    }
}
