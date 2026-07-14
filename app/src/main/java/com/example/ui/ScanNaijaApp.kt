package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.ManufacturerEntity
import com.example.data.local.ProductEntity
import com.example.data.local.SalesListEntity
import com.example.data.local.ScanHistoryEntity
import com.example.data.repository.VerificationResult
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanNaijaApp(viewModel: ScanNaijaViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val manufacturer by viewModel.manufacturer.collectAsStateWithLifecycle()
    val activePiracyFlags by viewModel.activePiracyFlags.collectAsStateWithLifecycle()
    val salesListItems by viewModel.salesListItems.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()

    val showConfirmModal by viewModel.showConfirmModal.collectAsStateWithLifecycle()
    val confirmTitle by viewModel.confirmModalTitle.collectAsStateWithLifecycle()
    val confirmDesc by viewModel.confirmModalDesc.collectAsStateWithLifecycle()

    val showReportModal by viewModel.showReportModal.collectAsStateWithLifecycle()

    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val syncBadgeState by viewModel.syncBadgeState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Observe active counterfeit count for badge
    val piracyCount = activePiracyFlags.size

    // Observe expiring items (days <= 30) for badge
    val now = Date()
    val expiringCount = allProducts.count { p ->
        val expDate = viewModel.parseExpiryDate(p.expiry)
        if (expDate != null) {
            val diff = expDate.time - now.time
            diff in 0..(30L * 24 * 60 * 60 * 1000)
        } else false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SpaceNavy
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isWide = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (isWide) {
                    // SIDEBAR NAVIGATION FOR WIDE SCREENS (adaptive canonical rail layout)
                    SidebarNavigation(
                        currentTab = currentTab,
                        manufacturer = manufacturer,
                        piracyCount = piracyCount,
                        expiringCount = expiringCount,
                        salesCount = salesListItems.size,
                        dbCount = allProducts.size,
                        onTabSelect = { viewModel.changeTab(it) }
                    )
                    VerticalDivider(color = SpaceNavyBorder, thickness = 1.dp)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // TOP BAR
                    TopBarSection(
                        manufacturerName = manufacturer?.nameOfCompany ?: "Guest Manufacturer",
                        syncStatus = syncStatus,
                        syncBadgeState = syncBadgeState,
                        currentTab = currentTab,
                        onWebPortalClick = { viewModel.changeTab("landing") },
                        onSyncTrigger = { viewModel.triggerServerSync() }
                    )

                    HorizontalDivider(color = SpaceNavyBorder, thickness = 1.dp)

                    // MAIN SCROLLABLE CONTENT AREA
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "TabContentTransition"
                        ) { targetTab ->
                            when (targetTab) {
                                "landing" -> WebLandingScreen(onNavigateToTab = { viewModel.changeTab(it) })
                                "dashboard" -> DashboardScreen(viewModel = viewModel)
                                "database" -> DatabaseScreen(viewModel = viewModel)
                                "piracy" -> PiracyAlertsScreen(viewModel = viewModel)
                                "expiry" -> ExpiryWatchScreen(viewModel = viewModel)
                                "saleslist" -> SalesListScreen(viewModel = viewModel)
                                "buyer" -> BuyerVerifyScreen(viewModel = viewModel)
                                "settings" -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }

                    if (!isWide) {
                        // BOTTOM NAVIGATION FOR MOBILE SCREENS
                        HorizontalDivider(color = SpaceNavyBorder, thickness = 1.dp)
                        BottomNavigationBar(
                            currentTab = currentTab,
                            piracyCount = piracyCount,
                            expiringCount = expiringCount,
                            onTabSelect = { viewModel.changeTab(it) }
                        )
                    }
                }
            }
        }
    }

    // Dynamic confirmation dialog
    if (showConfirmModal) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirm() },
            title = { Text(confirmTitle, color = LightBlueText, fontWeight = FontWeight.Bold) },
            text = { Text(confirmDesc, color = SlateText) },
            confirmButton = {
                Button(
                    onClick = { viewModel.executeConfirm() },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Confirm", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirm() }) {
                    Text("Cancel", color = SlateText)
                }
            },
            containerColor = SpaceNavyDarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dynamic Report to Authority Dialog
    if (showReportModal) {
        ReportToAuthorityDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showReportModal.value = false }
        )
    }
}

// ═══════════════════════════════════════════
// SHARED WIDGETS
// ═══════════════════════════════════════════

@Composable
fun TopBarSection(
    manufacturerName: String,
    syncStatus: String,
    syncBadgeState: String,
    currentTab: String,
    onWebPortalClick: () -> Unit,
    onSyncTrigger: () -> Unit
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "ScanNaija Secure Platform",
                color = SlateText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = manufacturerName,
                color = NeonEmerald,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Web Portal Quick Switch Button
            IconButton(
                onClick = onWebPortalClick,
                modifier = Modifier
                    .background(SpaceNavyDarkCard, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, if (currentTab == "landing") NeonEmerald else SpaceNavyBorder, shape = RoundedCornerShape(8.dp))
                    .size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Web Landing Page",
                    tint = if (currentTab == "landing") NeonEmerald else SlateText,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Live UTC/Local Time Badge
            Box(
                modifier = Modifier
                    .background(SpaceNavyDarkCard, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = currentTime,
                    color = SlateText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Sync Indicator Badge
            val badgeBg = when (syncBadgeState) {
                "ok" -> NeonEmerald.copy(alpha = 0.1f)
                "err" -> AlertRed.copy(alpha = 0.1f)
                else -> WarnOrange.copy(alpha = 0.1f)
            }
            val badgeBorder = when (syncBadgeState) {
                "ok" -> NeonEmerald.copy(alpha = 0.3f)
                "err" -> AlertRed.copy(alpha = 0.3f)
                else -> WarnOrange.copy(alpha = 0.3f)
            }
            val badgeText = when (syncBadgeState) {
                "ok" -> NeonEmerald
                "err" -> AlertRed
                else -> WarnOrange
            }

            Row(
                modifier = Modifier
                    .background(badgeBg, shape = RoundedCornerShape(20.dp))
                    .border(1.dp, badgeBorder, shape = RoundedCornerShape(20.dp))
                    .clickable { onSyncTrigger() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(badgeText, shape = CircleShape)
                )
                Text(
                    text = syncStatus,
                    color = badgeText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun SidebarNavigation(
    currentTab: String,
    manufacturer: ManufacturerEntity?,
    piracyCount: Int,
    expiringCount: Int,
    salesCount: Int,
    dbCount: Int,
    onTabSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(SpaceNavyDarkCard)
            .padding(vertical = 24.dp, horizontal = 12.dp)
    ) {
        // Logo Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp, start = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(NeonEmerald, NeonPurple)),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("◈", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("ScanNaija", color = LightBlueText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("v2.1 · Secure", color = DarkSlateText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Nav Section: Public Web
        Text(
            "Public Website",
            color = DarkSlateText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            letterSpacing = 1.2.sp
        )
        SidebarItem(
            label = "Web Landing Page",
            icon = Icons.Default.Language,
            active = currentTab == "landing",
            onClick = { onTabSelect("landing") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nav Section: Overview
        Text(
            "Overview",
            color = DarkSlateText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            letterSpacing = 1.2.sp
        )
        SidebarItem(
            label = "Dashboard",
            icon = Icons.Default.Home,
            active = currentTab == "dashboard",
            onClick = { onTabSelect("dashboard") }
        )
        SidebarItem(
            label = "Database",
            icon = Icons.Default.Storage,
            active = currentTab == "database",
            badgeCount = dbCount,
            onClick = { onTabSelect("database") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nav Section: Security
        Text(
            "Security",
            color = DarkSlateText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            letterSpacing = 1.2.sp
        )
        SidebarItem(
            label = "Piracy Alerts",
            icon = Icons.Default.Warning,
            active = currentTab == "piracy",
            badgeCount = piracyCount,
            badgeColor = AlertRed,
            onClick = { onTabSelect("piracy") }
        )
        SidebarItem(
            label = "Expiry Watch",
            icon = Icons.Default.Timer,
            active = currentTab == "expiry",
            badgeCount = expiringCount,
            badgeColor = WarnOrange,
            onClick = { onTabSelect("expiry") }
        )
        SidebarItem(
            label = "Sales List",
            icon = Icons.Default.LocalOffer,
            active = currentTab == "saleslist",
            badgeCount = salesCount,
            badgeColor = NeonPurple,
            onClick = { onTabSelect("saleslist") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nav Section: Buyer View
        Text(
            "Buyer Client",
            color = DarkSlateText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            letterSpacing = 1.2.sp
        )
        SidebarItem(
            label = "Buyer Verify",
            icon = Icons.Default.QrCodeScanner,
            active = currentTab == "buyer",
            onClick = { onTabSelect("buyer") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nav Section: Settings
        Text(
            "Account",
            color = DarkSlateText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            letterSpacing = 1.2.sp
        )
        SidebarItem(
            label = "Settings",
            icon = Icons.Default.Settings,
            active = currentTab == "settings",
            onClick = { onTabSelect("settings") }
        )

        Spacer(modifier = Modifier.weight(1.0f))

        // Footer User Info Pill
        val initials = if (manufacturer != null && manufacturer!!.nameOfCompany.isNotEmpty()) {
            manufacturer!!.nameOfCompany.split(" ")
                .mapNotNull { it.firstOrNull() }
                .take(2)
                .joinToString("")
                .uppercase()
        } else "?"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onTabSelect("settings") }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(NeonEmerald, NeonPurple)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = manufacturer?.nameOfCompany ?: "Unregistered",
                    color = LightBlueText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = manufacturer?.regNo ?: "No Company ID",
                    color = DarkSlateText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SidebarItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    badgeCount: Int = 0,
    badgeColor: Color = AlertRed,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) NeonEmerald.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) NeonEmerald else SlateText,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = if (active) LightBlueText else SlateText,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .background(badgeColor, shape = CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    badgeCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentTab: String,
    piracyCount: Int,
    expiringCount: Int,
    onTabSelect: (String) -> Unit
) {
    NavigationBar(
        containerColor = SpaceNavyDarkCard,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = currentTab == "dashboard",
            onClick = { onTabSelect("dashboard") },
            label = { Text("Home", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonEmerald,
                selectedTextColor = NeonEmerald,
                indicatorColor = NeonEmerald.copy(alpha = 0.1f),
                unselectedIconColor = SlateText,
                unselectedTextColor = SlateText
            ),
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") }
        )
        NavigationBarItem(
            selected = currentTab == "database",
            onClick = { onTabSelect("database") },
            label = { Text("Products", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonEmerald,
                selectedTextColor = NeonEmerald,
                indicatorColor = NeonEmerald.copy(alpha = 0.1f),
                unselectedIconColor = SlateText,
                unselectedTextColor = SlateText
            ),
            icon = { Icon(Icons.Default.Storage, contentDescription = "Database") }
        )
        NavigationBarItem(
            selected = currentTab == "piracy" || currentTab == "expiry" || currentTab == "saleslist",
            onClick = { onTabSelect("piracy") },
            label = { Text("Alerts", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonEmerald,
                selectedTextColor = NeonEmerald,
                indicatorColor = NeonEmerald.copy(alpha = 0.1f),
                unselectedIconColor = SlateText,
                unselectedTextColor = SlateText
            ),
            icon = {
                BadgedBox(
                    badge = {
                        val totalBadge = piracyCount + expiringCount
                        if (totalBadge > 0) {
                            Badge(containerColor = AlertRed) {
                                Text(totalBadge.toString())
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Alerts")
                }
            }
        )
        NavigationBarItem(
            selected = currentTab == "buyer",
            onClick = { onTabSelect("buyer") },
            label = { Text("Verify", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonEmerald,
                selectedTextColor = NeonEmerald,
                indicatorColor = NeonEmerald.copy(alpha = 0.1f),
                unselectedIconColor = SlateText,
                unselectedTextColor = SlateText
            ),
            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Verify") }
        )
        NavigationBarItem(
            selected = currentTab == "settings",
            onClick = { onTabSelect("settings") },
            label = { Text("Settings", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonEmerald,
                selectedTextColor = NeonEmerald,
                indicatorColor = NeonEmerald.copy(alpha = 0.1f),
                unselectedIconColor = SlateText,
                unselectedTextColor = SlateText
            ),
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
        )
    }
}

@Composable
fun StatsCard(
    label: String,
    value: String,
    subText: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SpaceNavyDarkCard, shape = RoundedCornerShape(12.dp))
            .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                color = SlateText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subText,
                color = DarkSlateText,
                fontSize = 11.sp
            )
        }
    }
}

// Custom canvas vertical-line drawing simulating realistic CODE128 lines barcode
@Composable
fun CustomBarcodeDrawing(
    value: String,
    modifier: Modifier = Modifier,
    lineColor: Color = NeonEmerald,
    heightDp: Dp = 65.dp
) {
    Canvas(
        modifier = modifier
            .height(heightDp)
            .fillMaxWidth()
    ) {
        val width = size.width
        val height = size.height

        // Hash to create a stable unique pattern for this specific barcode string
        val seed = value.hashCode().toLong()
        val random = Random(seed)

        var currentX = 10f
        val minBarWidth = 3f
        val gapWidth = 2.5f

        while (currentX < width - 10f) {
            val barWeight = random.nextInt(3) + 1 // 1x, 2x, 3x bar width
            val barWidth = barWeight * minBarWidth

            drawRect(
                color = lineColor,
                topLeft = Offset(currentX, 0f),
                size = Size(barWidth, height)
            )

            val gapWeight = random.nextInt(3) + 1
            currentX += barWidth + (gapWeight * gapWidth)
        }
    }
}

// ═══════════════════════════════════════════
// DASHBOARD SCREEN VIEW
// ═══════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: ScanNaijaViewModel) {
    val manufacturer by viewModel.manufacturer.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val activePiracyFlags by viewModel.activePiracyFlags.collectAsStateWithLifecycle()
    val barcodesSessionCount by viewModel.barcodesGeneratedSessionCount.collectAsStateWithLifecycle()
    val showDashboardAlerts by viewModel.showDashboardAlerts.collectAsStateWithLifecycle()
    val tempGeneration by viewModel.tempGeneration.collectAsStateWithLifecycle()
    val useDarkBars by viewModel.useDarkBars.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Expiry Count calculation
    val now = Date()
    val expiringCount = allProducts.count { p ->
        val expDate = viewModel.parseExpiryDate(p.expiry)
        if (expDate != null) {
            val diff = expDate.time - now.time
            diff in 0..(30L * 24 * 60 * 60 * 1000)
        } else false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome and intro
        item {
            Column {
                val greetName = manufacturer?.nameOfCompany ?: "Guest"
                Text(
                    text = "Good day 👋 $greetName",
                    color = LightBlueText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (manufacturer == null) "Complete Section A below to generate your locked Manufacturer ID." else "Here is your registered product security overview.",
                    color = SlateText,
                    fontSize = 13.sp
                )
            }
        }

        // Stats grid
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 4
            ) {
                val itemWidth = if (allProducts.size > 0) Modifier.widthIn(min = 130.dp) else Modifier.weight(1f)
                StatsCard(
                    label = "Total Registered",
                    value = allProducts.size.toString(),
                    subText = "products in registry",
                    valueColor = LightBlueText,
                    modifier = itemWidth
                )
                StatsCard(
                    label = "Piracy Flagged",
                    value = activePiracyFlags.size.toString(),
                    subText = "scans mismatched",
                    valueColor = if (activePiracyFlags.size > 0) AlertRed else SlateText,
                    modifier = itemWidth
                )
                StatsCard(
                    label = "Expiring Soon",
                    value = expiringCount.toString(),
                    subText = "within 30 days",
                    valueColor = if (expiringCount > 0) WarnOrange else SlateText,
                    modifier = itemWidth
                )
                StatsCard(
                    label = "Barcodes Generated",
                    value = barcodesSessionCount.toString(),
                    subText = "current session",
                    valueColor = NeonEmerald,
                    modifier = itemWidth
                )
            }
        }

        // Alerts box if enabled
        if (showDashboardAlerts && (activePiracyFlags.isNotEmpty() || expiringCount > 0)) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Active Alerts", color = LightBlueText, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    if (activePiracyFlags.isNotEmpty()) {
                        AlertBanner(
                            title = "Piracy detected — ${activePiracyFlags.size} counterfeit scans",
                            desc = "A buyer verified a counterfeit barcode that does not exist or mismatches your company registry.",
                            bannerColor = AlertRed,
                            actionLabel = "Review piracy flags ›",
                            onAction = { viewModel.changeTab("piracy") }
                        )
                    }

                    if (expiringCount > 0) {
                        AlertBanner(
                            title = "Products expiring soon — move to Sales List",
                            desc = "You have $expiringCount products nearing expiry. Discount them before the due date to avoid losses.",
                            bannerColor = WarnOrange,
                            actionLabel = "Move expiring products to Sales List ›",
                            onAction = {
                                viewModel.moveAllExpiringToSales()
                                Toast.makeText(context, "$expiringCount items moved to Sales List!", Toast.LENGTH_SHORT).show()
                                viewModel.changeTab("saleslist")
                            }
                        )
                    }
                }
            }
        }

        // SECTION A — MANUFACTURER REGISTRY
        item {
            SectionAPanel(viewModel = viewModel, manufacturer = manufacturer, clipboardManager = clipboardManager)
        }

        // SECTION B — REGISTER PRODUCTS
        item {
            SectionBPanel(viewModel = viewModel, manufacturer = manufacturer, tempGeneration = tempGeneration, useDarkBars = useDarkBars)
        }
    }
}

@Composable
fun AlertBanner(
    title: String,
    desc: String,
    bannerColor: Color,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bannerColor.copy(alpha = 0.07f), shape = RoundedCornerShape(12.dp))
            .border(1.dp, bannerColor.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(bannerColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alert",
                    tint = bannerColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = bannerColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, color = SlateText, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = actionLabel,
                    color = NeonEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onAction() }
                )
            }
        }
    }
}

@Composable
fun SectionAPanel(
    viewModel: ScanNaijaViewModel,
    manufacturer: ManufacturerEntity?,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    var editMode by remember { mutableStateOf(false) }

    var companyName by remember { mutableStateOf("") }
    var regNo by remember { mutableStateOf("") }
    var dateIssued by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var ceoName by remember { mutableStateOf("") }
    var affiliate by remember { mutableStateOf(false) }
    var affiliateName by remember { mutableStateOf("") }

    // Synchronize inputs when manufacturer changes or loaded
    LaunchedEffect(manufacturer, editMode) {
        if (manufacturer != null && !editMode) {
            companyName = manufacturer.nameOfCompany
            regNo = manufacturer.regNo
            dateIssued = manufacturer.dateIssued
            email = manufacturer.email
            country = manufacturer.country
            ceoName = manufacturer.ceo
            affiliate = manufacturer.affiliate
            affiliateName = manufacturer.affiliateCompanyName ?: ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceNavyDarkCard, shape = RoundedCornerShape(16.dp))
            .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(16.dp))
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Section A — Manufacturer Details",
                        color = LightBlueText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Fill this once to lock in your verified company registries.",
                        color = DarkSlateText,
                        fontSize = 11.sp
                    )
                }

                if (manufacturer != null && !editMode) {
                    IconButton(onClick = { editMode = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit details", tint = NeonEmerald)
                    }
                }
            }

            if (manufacturer == null || editMode) {
                // REGISTRATION FORM
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonEmerald,
                        unfocusedBorderColor = SpaceNavyBorder,
                        focusedLabelColor = NeonEmerald,
                        unfocusedLabelColor = SlateText,
                        focusedTextColor = LightBlueText,
                        unfocusedTextColor = LightBlueText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = regNo,
                        onValueChange = { regNo = it },
                        label = { Text("Reg No") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonEmerald,
                            unfocusedBorderColor = SpaceNavyBorder,
                            focusedTextColor = LightBlueText,
                            unfocusedTextColor = LightBlueText
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dateIssued,
                        onValueChange = { dateIssued = it },
                        label = { Text("Date Issued (DD/MM/YYYY)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonEmerald,
                            unfocusedBorderColor = SpaceNavyBorder,
                            focusedTextColor = LightBlueText,
                            unfocusedTextColor = LightBlueText
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Company Email") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonEmerald,
                            unfocusedBorderColor = SpaceNavyBorder,
                            focusedTextColor = LightBlueText,
                            unfocusedTextColor = LightBlueText
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonEmerald,
                            unfocusedBorderColor = SpaceNavyBorder,
                            focusedTextColor = LightBlueText,
                            unfocusedTextColor = LightBlueText
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = ceoName,
                    onValueChange = { ceoName = it },
                    label = { Text("CEO / Director Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonEmerald,
                        unfocusedBorderColor = SpaceNavyBorder,
                        focusedTextColor = LightBlueText,
                        unfocusedTextColor = LightBlueText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Is this company affiliated with another brand?", color = SlateText, fontSize = 12.sp)
                    Switch(
                        checked = affiliate,
                        onCheckedChange = { affiliate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonEmerald)
                    )
                }

                if (affiliate) {
                    OutlinedTextField(
                        value = affiliateName,
                        onValueChange = { affiliateName = it },
                        label = { Text("Affiliated Brand Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonEmerald,
                            unfocusedBorderColor = SpaceNavyBorder,
                            focusedTextColor = LightBlueText,
                            unfocusedTextColor = LightBlueText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        if (companyName.isNotBlank() && regNo.isNotBlank() && dateIssued.isNotBlank()) {
                            viewModel.saveManufacturer(
                                name = companyName,
                                regNo = regNo,
                                date = dateIssued,
                                email = email,
                                country = country,
                                ceo = ceoName,
                                affiliate = affiliate,
                                affiliateName = if (affiliate) affiliateName else null
                            )
                            editMode = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.CardMembership, contentDescription = "Generate ID")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Manufacturer ID", color = SpaceNavy)
                }
            } else {
                // READONLY PREVIEW & MANUFACTURER ID
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Company: $companyName",
                        color = LightBlueText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Reg No: $regNo", color = SlateText, fontSize = 12.sp)
                        Text("CEO: $ceoName", color = SlateText, fontSize = 12.sp)
                        Text("Country: $country", color = SlateText, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Stylized Manufacturer ID locked block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeonEmerald.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, NeonEmerald.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("LOCKED MANUFACTURER ID KEY", color = DarkSlateText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = manufacturer.mfrId,
                                    color = NeonEmerald,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(manufacturer.mfrId))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.3f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = NeonEmerald, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", color = NeonEmerald, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionBPanel(
    viewModel: ScanNaijaViewModel,
    manufacturer: ManufacturerEntity?,
    tempGeneration: ScanNaijaViewModel.TempGenerationDetails?,
    useDarkBars: Boolean
) {
    val context = LocalContext.current

    var productName by remember { mutableStateOf("") }
    var productSize by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var mfdDate by remember { mutableStateOf("") }
    var expDate by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("1") }
    var govAgency by remember { mutableStateOf("") }
    var govRegNo by remember { mutableStateOf("") }
    var packagedBy by remember { mutableStateOf("") }
    var nutrients by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf("") }

    val categories = listOf(
        "Beverages", "Eatables", "Machines", "Furnitures",
        "Textiles", "Farm Produce", "Pharmaceuticals", "Cosmetics", "Electronics"
    )

    var showCategoryMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceNavyDarkCard, shape = RoundedCornerShape(16.dp))
            .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(16.dp))
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Register Product(s) & Generate Barcodes",
                color = LightBlueText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Add product details below to dynamically mint secure product wrappers and barcodes.",
                color = DarkSlateText,
                fontSize = 11.sp
            )

            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Product Name *") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = productSize,
                    onValueChange = { productSize = it },
                    label = { Text("Product Size (e.g. 60CL) *") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    modifier = Modifier.weight(1f)
                )

                // Category Dropdown Selection
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = productCategory,
                        onValueChange = {},
                        label = { Text("Category *") },
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                        trailingIcon = {
                            IconButton(onClick = { showCategoryMenu = !showCategoryMenu }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false },
                        modifier = Modifier.background(SpaceNavyLightCard)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = LightBlueText) },
                                onClick = {
                                    productCategory = cat
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Unit Price (₦) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Batch Quantity (Max 500) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = mfdDate,
                    onValueChange = { mfdDate = it },
                    label = { Text("MFD Date (DD/MM/YYYY) *") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = expDate,
                    onValueChange = { expDate = it },
                    label = { Text("Expiry Date (DD/MM/YYYY) *") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = govAgency,
                    onValueChange = { govAgency = it },
                    label = { Text("Reg Agency (e.g. NAFDAC)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = govRegNo,
                    onValueChange = { govRegNo = it },
                    label = { Text("Agency Reg Number") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = packagedBy,
                onValueChange = { packagedBy = it },
                label = { Text("Packaged By Address") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nutrients,
                    onValueChange = { nutrients = it },
                    label = { Text("Nutritional Value / 100ml") },
                    placeholder = { Text("e.g. Fat | 0g | 0%\nSugar | 8g | 9%") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    modifier = Modifier.weight(1f),
                    maxLines = 4
                )
                OutlinedTextField(
                    value = ingredients,
                    onValueChange = { ingredients = it },
                    label = { Text("Ingredients List") },
                    placeholder = { Text("e.g. Carbonated Water, Sugar, Citric Acid") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    modifier = Modifier.weight(1f),
                    maxLines = 4
                )
            }

            // CTA action panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val qty = quantityStr.toIntOrNull() ?: 1
                        if (productName.isNotBlank() && productCategory.isNotBlank() && price.isNotBlank() && expDate.isNotBlank()) {
                            if (manufacturer == null) {
                                Toast.makeText(context, "Complete Section A to set up Manufacturer ID first!", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.generateBarcodes(
                                    productName = productName,
                                    category = productCategory,
                                    size = productSize,
                                    price = price,
                                    mfd = mfdDate,
                                    expiry = expDate,
                                    quantity = qty,
                                    govAgency = govAgency,
                                    govRegNo = govRegNo,
                                    packagedBy = packagedBy,
                                    bgTheme = "#0f1003",
                                    textTheme = "#d3e3fd",
                                    nutrients = nutrients,
                                    ingredients = ingredients
                                )
                                Toast.makeText(context, "$qty Barcodes minted!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Fill required fields marked with *", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = "Generate", tint = SpaceNavy)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mint Barcodes", color = SpaceNavy, fontSize = 13.sp)
                }

                if (tempGeneration != null && !viewModel.autosave.value) {
                    Button(
                        onClick = {
                            viewModel.saveTempBarcodesToDb()
                            Toast.makeText(context, "Saved registry to local SQLite DB!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save DB")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Database", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            // BARCODE OUTPUT PREVIEW SHEET
            if (tempGeneration != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = SpaceNavyBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Generated Barcodes Batch", color = NeonEmerald, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("${tempGeneration.barcodes.size} items ready", color = DarkSlateText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                // Scrollable barcode labels list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    tempGeneration.barcodes.forEach { code ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SpaceNavy, shape = RoundedCornerShape(8.dp))
                                .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CustomBarcodeDrawing(
                                    value = code,
                                    lineColor = if (useDarkBars) Color.Black else NeonEmerald,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = code,
                                    color = SlateText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// DATABASE SCREEN VIEW
// ═══════════════════════════════════════════
@Composable
fun DatabaseScreen(viewModel: ScanNaijaViewModel) {
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    var searchStr by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    val categories = listOf("All", "Beverages", "Eatables", "Machines", "Furnitures", "Textiles", "Farm Produce", "Pharmaceuticals", "Cosmetics", "Electronics")

    val filteredList = allProducts.filter { p ->
        val textMatch = searchStr.isEmpty() ||
                p.productName.contains(searchStr, ignoreCase = true) ||
                p.barcode.contains(searchStr, ignoreCase = true) ||
                p.category.contains(searchStr, ignoreCase = true)

        val catMatch = categoryFilter.isEmpty() || categoryFilter == "All" || p.category == categoryFilter

        textMatch && catMatch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Product Database", color = LightBlueText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Persistent offline registry synced with ScanNaija secure server.", color = SlateText, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    viewModel.triggerConfirm(
                        title = "Clear Database",
                        desc = "Are you sure you want to delete all registered product barcodes? This cannot be undone."
                    ) {
                        viewModel.clearAllProducts()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Clear All", tint = AlertRed)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear All", color = AlertRed)
            }
        }

        // Search and Filter section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchStr,
                onValueChange = { searchStr = it },
                label = { Text("Search by name, category, or barcode…") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SlateText) }
            )

            Box {
                OutlinedTextField(
                    value = if (categoryFilter.isEmpty()) "All Categories" else categoryFilter,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filter Category") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    trailingIcon = {
                        IconButton(onClick = { menuOpen = !menuOpen }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = SlateText)
                        }
                    },
                    modifier = Modifier.width(180.dp)
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.background(SpaceNavyLightCard)
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat, color = LightBlueText) },
                            onClick = {
                                categoryFilter = cat
                                menuOpen = false
                            }
                        )
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, contentDescription = "Empty", tint = DarkSlateText, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No registered products in registry", color = SlateText, fontSize = 14.sp)
                    Text("Generate barcodes on the dashboard to populate products.", color = DarkSlateText, fontSize = 12.sp)
                }
            }
        } else {
            // PRODUCTS SCROLL LIST TABLE
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.barcode }) { item ->
                    ProductItemRow(product = item, onMoveToSales = { viewModel.moveSingleToSales(item) }, onDelete = { viewModel.deleteProduct(item.barcode) })
                }
            }
        }
    }
}

@Composable
fun ProductItemRow(
    product: ProductEntity,
    onMoveToSales: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedDetails by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceNavyDarkCard, shape = RoundedCornerShape(12.dp))
            .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(12.dp))
            .clickable { expandedDetails = !expandedDetails }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product.productName,
                        color = LightBlueText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .background(NeonEmerald.copy(alpha = 0.08f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(product.category, color = NeonEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(product.size, color = SlateText, fontSize = 11.sp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(product.price, color = LightBlueText, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)

                    IconButton(onClick = onMoveToSales) {
                        Icon(Icons.Default.LocalOffer, contentDescription = "Discount item", tint = WarnOrange, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete barcode", tint = AlertRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Expose detailed barcodes & wrapper themes if expanded
            if (expandedDetails) {
                HorizontalDivider(color = SpaceNavyBorder, thickness = 1.dp)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Barcode Signature ID", color = DarkSlateText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    CustomBarcodeDrawing(value = product.barcode, modifier = Modifier.padding(vertical = 4.dp))
                    Text(product.barcode, color = SlateText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Manufacture MFD", color = DarkSlateText, fontSize = 10.sp)
                            Text(product.mfd, color = LightBlueText, fontSize = 12.sp)
                        }
                        Column {
                            Text("Expiry Watch / BB", color = DarkSlateText, fontSize = 10.sp)
                            Text(product.expiry, color = LightBlueText, fontSize = 12.sp)
                        }
                        Column {
                            Text("Agency Reg ID", color = DarkSlateText, fontSize = 10.sp)
                            Text("${product.govAgency} ${product.govRegNo}".trim(), color = LightBlueText, fontSize = 12.sp)
                        }
                    }

                    if (product.nutrients.isNotBlank() || product.ingredients.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = SpaceNavyBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(4.dp))

                        if (product.nutrients.isNotBlank()) {
                            Text("Nutritional Value:", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(product.nutrients, color = DarkSlateText, fontSize = 11.sp, lineHeight = 14.sp)
                        }

                        if (product.ingredients.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Ingredients:", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(product.ingredients, color = DarkSlateText, fontSize = 11.sp, lineHeight = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// PIRACY ALERTS SCREEN VIEW
// ═══════════════════════════════════════════
@Composable
fun PiracyAlertsScreen(viewModel: ScanNaijaViewModel) {
    val activePiracyFlags by viewModel.activePiracyFlags.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Piracy Alerts Tracker", color = LightBlueText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Detect scans representing unauthorized counterfeit matching in real-time.", color = SlateText, fontSize = 12.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.showReportModal.value = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = "File Report", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Report to Authority", color = Color.White)
                }

                Button(
                    onClick = {
                        viewModel.triggerConfirm(
                            title = "Dismiss All",
                            desc = "Mark all flagged piracy locations as read?"
                        ) {
                            viewModel.dismissAllFlags()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpaceNavyDarkCard),
                    border = BorderStroke(1.dp, SpaceNavyBorder)
                ) {
                    Text("Dismiss All", color = SlateText)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AlertRed.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                .border(1.dp, AlertRed.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AlertRed.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡", fontSize = 20.sp)
                }

                Column {
                    Text("${activePiracyFlags.size} Counterfeit Scans Detected", color = AlertRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your secure barcodes were scanned from unauthorized locations or using registered brands mismatching owner registry keys.",
                        color = SlateText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        if (activePiracyFlags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = "Safe", tint = NeonEmerald, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No counterfeiting flags recorded", color = SlateText, fontSize = 14.sp)
                    Text("Your product codes are fully secure and unique.", color = DarkSlateText, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activePiracyFlags) { flag ->
                    PiracyFlagItemRow(flag = flag, onDismiss = { viewModel.dismissFlag(flag.id) })
                }
            }
        }

        // Action workflow card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceNavyDarkCard, shape = RoundedCornerShape(12.dp))
                .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What should you do?", color = LightBlueText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1.", color = NeonEmerald, fontWeight = FontWeight.Bold)
                    Text("Document and save report files containing timestamp metrics.", color = SlateText, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("2.", color = NeonEmerald, fontWeight = FontWeight.Bold)
                    Text("Open standard Report wizard and submit complaints to NAFDAC or SON.", color = SlateText, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("3.", color = NeonEmerald, fontWeight = FontWeight.Bold)
                    Text("Revoke or deprecate flagged batch codes from historical databases.", color = SlateText, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PiracyFlagItemRow(flag: com.example.data.local.PiracyFlagEntity, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceNavyDarkCard, shape = RoundedCornerShape(12.dp))
            .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(AlertRed, shape = CircleShape))
                    Text(
                        "Code: ${flag.barcode}",
                        color = LightBlueText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Claimed Brand: ${flag.claimedCompany} (Real Owner: ${flag.realCompany})",
                    color = SlateText,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Scanned: ${flag.timestamp}",
                    color = DarkSlateText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Check, contentDescription = "Mark Read", tint = SlateText)
            }
        }
    }
}

// ═══════════════════════════════════════════
// EXPIRY WATCH SCREEN VIEW
// ═══════════════════════════════════════════
@Composable
fun ExpiryWatchScreen(viewModel: ScanNaijaViewModel) {
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val now = Date()
    val expiringItems = allProducts.mapNotNull { p ->
        val expDate = viewModel.parseExpiryDate(p.expiry)
        if (expDate != null) {
            val diffMs = expDate.time - now.time
            val daysLeft = Math.ceil(diffMs.toDouble() / (24 * 60 * 60 * 1000)).toInt()
            if (daysLeft <= 30) {
                Pair(p, daysLeft)
            } else null
        } else null
    }.sortedBy { it.second }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Expiry Watch List", color = LightBlueText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Priority monitoring of items approaching expiry within 30 days.", color = SlateText, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    if (expiringItems.isNotEmpty()) {
                        viewModel.moveAllExpiringToSales()
                        Toast.makeText(context, "${expiringItems.size} items added to Sales List!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarnOrange),
                enabled = expiringItems.isNotEmpty()
            ) {
                Icon(Icons.Default.DoubleArrow, contentDescription = "Move all")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Move All to Sales List")
            }
        }

        if (expiringItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Timer, contentDescription = "Safe", tint = NeonEmerald, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No products expiring soon", color = SlateText, fontSize = 14.sp)
                    Text("Your inventory records are safely fresh.", color = DarkSlateText, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(expiringItems, key = { it.first.barcode }) { (p, days) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpaceNavyDarkCard, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(p.productName, color = LightBlueText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("BB: ${p.expiry}", color = SlateText, fontSize = 12.sp)
                                    Text("Barcode: ${p.barcode.take(18)}…", color = DarkSlateText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val labelColor = if (days < 0) AlertRed else if (days <= 7) AlertRed else if (days <= 14) WarnOrange else SlateText
                                val daysLabel = if (days < 0) "Expired ${Math.abs(days)}d ago" else "$days days left"

                                Text(
                                    text = daysLabel,
                                    color = labelColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                Button(
                                    onClick = {
                                        viewModel.moveSingleToSales(p)
                                        Toast.makeText(context, "Moved to sales list!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SpaceNavyLightCard),
                                    border = BorderStroke(1.dp, SpaceNavyBorder),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Move", color = NeonEmerald, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// SALES LIST SCREEN VIEW
// ═══════════════════════════════════════════
@Composable
fun SalesListScreen(viewModel: ScanNaijaViewModel) {
    val salesListItems by viewModel.salesListItems.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Priority Sales Discounts", color = LightBlueText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Products marked for priority pricing reduction based on expiring alerts.", color = SlateText, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    viewModel.triggerConfirm(
                        title = "Clear Sales List",
                        desc = "Remove all priority items from sales discount list?"
                    ) {
                        viewModel.clearSalesList()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SpaceNavyDarkCard),
                border = BorderStroke(1.dp, SpaceNavyBorder),
                enabled = salesListItems.isNotEmpty()
            ) {
                Text("Clear List", color = SlateText)
            }
        }

        if (salesListItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalOffer, contentDescription = "Empty", tint = DarkSlateText, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Sales List is empty", color = SlateText, fontSize = 14.sp)
                    Text("Items nearing expiry added via Expiry Watch will show here.", color = DarkSlateText, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(salesListItems, key = { it.barcode }) { item ->
                    val now = Date()
                    val expDate = viewModel.parseExpiryDate(item.expiry)
                    val daysLeft = if (expDate != null) {
                        Math.ceil((expDate.time - now.time).toDouble() / (24 * 60 * 60 * 1000)).toInt()
                    } else null

                    val discountLabel = when {
                        daysLeft == null -> "—"
                        daysLeft < 0 -> "Remove Shelf"
                        daysLeft <= 7 -> "40-60% OFF"
                        daysLeft <= 14 -> "20-30% OFF"
                        else -> "10-15% OFF"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpaceNavyDarkCard, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.productName, color = LightBlueText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(item.price, color = SlateText, fontSize = 12.sp)
                                    Text("BB: ${item.expiry}", color = DarkSlateText, fontSize = 11.sp)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (discountLabel == "Remove Shelf") AlertRed.copy(alpha = 0.15f) else NeonEmerald.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (discountLabel == "Remove Shelf") AlertRed else NeonEmerald,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = discountLabel,
                                        color = if (discountLabel == "Remove Shelf") AlertRed else NeonEmerald,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                IconButton(onClick = { viewModel.removeFromSalesList(item.barcode) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = SlateText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// BUYER VERIFICATION SCREEN VIEW
// ═══════════════════════════════════════════
@Composable
fun BuyerVerifyScreen(viewModel: ScanNaijaViewModel) {
    var businessInput by remember { mutableStateOf("") }
    var barcodeInput by remember { mutableStateOf("") }

    val recentScans by viewModel.recentScans.collectAsStateWithLifecycle()
    val loading by viewModel.verificationLoading.collectAsStateWithLifecycle()
    val result by viewModel.verificationResult.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(NeonEmerald, NeonPurple)),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("◈", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("ScanNaija Verify Portal", color = LightBlueText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Verify product genuineness straight from dynamic manufacturer registry data.", color = SlateText, fontSize = 12.sp, textAlign = TextAlign.Center)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceNavyDarkCard, shape = RoundedCornerShape(16.dp))
                .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Verify Brand & Barcode", color = LightBlueText, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = businessInput,
                    onValueChange = { businessInput = it },
                    label = { Text("Brand / Business Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = "Business", tint = SlateText) }
                )

                OutlinedTextField(
                    value = barcodeInput,
                    onValueChange = { barcodeInput = it },
                    label = { Text("Barcode Number or Pasted Link") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = "Barcode", tint = SlateText) }
                )

                Button(
                    onClick = {
                        if (businessInput.isNotBlank() && barcodeInput.isNotBlank()) {
                            viewModel.runVerification(businessInput, barcodeInput)
                        } else {
                            Toast.makeText(context, "Fill both fields first!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SpaceNavy, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Verify", tint = SpaceNavy)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify Authenticity", color = SpaceNavy, fontWeight = FontWeight.Bold)
                    }
                }

                // VERIFICATION RESULT BANNERS
                if (result != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = SpaceNavyBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    when (val r = result!!) {
                        is VerificationResult.Authentic -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NeonEmerald.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, NeonEmerald.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(NeonEmerald.copy(alpha = 0.15f), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Genuine", tint = NeonEmerald)
                                        }
                                        Column {
                                            Text("✓ AUTHENTIC PRODUCT", color = NeonEmerald, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            Text("Verified genuine registered brand from ${r.product.companyName}.", color = SlateText, fontSize = 11.sp)
                                        }
                                    }

                                    HorizontalDivider(color = SpaceNavyBorder, thickness = 1.dp)

                                    // Display Details list
                                    VerifyDetailRow("Product Name", r.product.productName)
                                    VerifyDetailRow("Registry Size", r.product.size)
                                    VerifyDetailRow("Unit Price", r.product.price)
                                    VerifyDetailRow("Category", r.product.category)
                                    VerifyDetailRow("Expiry Watch", r.product.expiry)

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Button(
                                        onClick = {
                                            val shareText = "✅ Verified Authentic! Product: ${r.product.productName} by ${r.product.companyName} checked via ScanNaija."
                                            clipboardManager.setText(AnnotatedString(shareText))
                                            Toast.makeText(context, "Verification copied to share!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SpaceNavyBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = NeonEmerald)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share Verification Result", color = NeonEmerald)
                                    }
                                }
                            }
                        }
                        is VerificationResult.FakeMismatch -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AlertRed.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, AlertRed.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(AlertRed.copy(alpha = 0.15f), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PriorityHigh, contentDescription = "Mismatch", tint = AlertRed)
                                        }
                                        Column {
                                            Text("✕ COUNTERFEIT DETECTED", color = AlertRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("Barcode belongs to different company owner registry key!", color = SlateText, fontSize = 11.sp)
                                        }
                                    }

                                    HorizontalDivider(color = SpaceNavyBorder, thickness = 1.dp)

                                    VerifyDetailRow("Pasted Barcode", r.barcode)
                                    VerifyDetailRow("Claimed Brand", r.claimedCompany)
                                    VerifyDetailRow("Registered Owner", r.registeredCompany)

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Button(
                                        onClick = {
                                            viewModel.showReportModal.value = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                                    ) {
                                        Icon(Icons.Default.Flag, contentDescription = "Flag", tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Report Mismatch Piracy", color = Color.White)
                                    }
                                }
                            }
                        }
                        is VerificationResult.BarcodeNotRegistered -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AlertRed.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, AlertRed.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(AlertRed.copy(alpha = 0.15f), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Help, contentDescription = "Not Registered", tint = AlertRed)
                                        }
                                        Column {
                                            Text("✕ CODE NOT REGISTERED", color = AlertRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("Company exists but this barcode is not found in their registry.", color = SlateText, fontSize = 11.sp)
                                        }
                                    }

                                    HorizontalDivider(color = SpaceNavyBorder, thickness = 1.dp)

                                    VerifyDetailRow("Business Brand", r.companyName)
                                    VerifyDetailRow("Pasted Barcode", r.barcode)
                                    VerifyDetailRow("Match Status", "✕ NOT FOUND")
                                }
                            }
                        }
                        is VerificationResult.CompanyNotFound -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(WarnOrange.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, WarnOrange.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(WarnOrange.copy(alpha = 0.15f), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.HelpOutline, contentDescription = "Not Found", tint = WarnOrange)
                                        }
                                        Column {
                                            Text("? COMPANY UNKNOWN", color = WarnOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("The claimed company is not registered on ScanNaija registry.", color = SlateText, fontSize = 11.sp)
                                        }
                                    }

                                    HorizontalDivider(color = SpaceNavyBorder, thickness = 1.dp)

                                    VerifyDetailRow("Claimed Brand", r.companyName)
                                    VerifyDetailRow("Barcode Passed", r.barcode)
                                    VerifyDetailRow("Recommendation", "Ask seller for official authentication certifications.")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Scans History Section
        if (recentScans.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Verify Scans History", color = LightBlueText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Clear History",
                    color = AlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.clearScanHistory() }
                )
            }

            recentScans.take(6).forEach { scan ->
                val dotColor = when (scan.status) {
                    "authentic" -> NeonEmerald
                    "fake" -> AlertRed
                    else -> WarnOrange
                }
                val label = when (scan.status) {
                    "authentic" -> "✓ Authentic"
                    "fake" -> "✕ Counterfeit"
                    else -> "? Unknown"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpaceNavyDarkCard, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(10.dp))
                        .clickable {
                            businessInput = scan.company
                            barcodeInput = scan.barcode
                            viewModel.runVerification(scan.company, scan.barcode)
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(dotColor, shape = CircleShape))
                            Column {
                                Text(scan.barcode, color = LightBlueText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Brand: ${scan.company}", color = SlateText, fontSize = 11.sp)
                            }
                        }

                        Text(label, color = dotColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VerifyDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SlateText, fontSize = 12.sp)
        Text(
            text = value,
            color = LightBlueText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Right,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.6f)
        )
    }
}

// ═══════════════════════════════════════════
// REPORT COMPLAINT TO AUTHORITY MULTI-STEP DIALOG
// ═══════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportToAuthorityDialog(
    viewModel: ScanNaijaViewModel,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Authority choices
    var selectedAuthorities by remember { mutableStateOf(setOf<String>()) }

    // Incident states
    var rptDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var rptLocation by remember { mutableStateOf("") }
    var rptUnits by remember { mutableStateOf("") }
    var rptLoss by remember { mutableStateOf("") }
    var rptProducts by remember { mutableStateOf("") }
    var rptDescription by remember { mutableStateOf("") }

    // Generate stable Reference number
    val refNo = remember {
        val now = Date()
        val yr = SimpleDateFormat("yyyy", Locale.getDefault()).format(now)
        val mo = SimpleDateFormat("MM", Locale.getDefault()).format(now)
        val rand = Math.floor(Math.random() * 900000 + 100000).toInt()
        "SN-$yr-$mo-$rand"
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(620.dp)
                .heightIn(max = 560.dp)
                .background(SpaceNavyDarkCard, shape = RoundedCornerShape(16.dp))
                .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🚨 Report Piracy to Authority", color = LightBlueText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Submit formal complaint reports straight to Nigerian regulatory bodies.", color = SlateText, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateText)
                    }
                }

                // Step Indicator Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepItem(number = 1, label = "Authority", active = step == 1, done = step > 1)
                    HorizontalDivider(color = SpaceNavyBorder, modifier = Modifier.weight(1f))
                    StepItem(number = 2, label = "Incident", active = step == 2, done = step > 2)
                    HorizontalDivider(color = SpaceNavyBorder, modifier = Modifier.weight(1f))
                    StepItem(number = 3, label = "Preview", active = step == 3, done = step > 3)
                    HorizontalDivider(color = SpaceNavyBorder, modifier = Modifier.weight(1f))
                    StepItem(number = 4, label = "Submit", active = step == 4, done = step > 4)
                }

                HorizontalDivider(color = SpaceNavyBorder, thickness = 1.dp)

                // Render dynamic pane based on active step
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (step) {
                        1 -> {
                            Text("Select regulatory authorities to report this counterfeit incident:", color = SlateText, fontSize = 12.sp)

                            AuthorityCard(
                                name = "NAFDAC",
                                fullname = "National Agency for Food and Drug Administration and Control",
                                type = "Food · Drugs · Cosmetics · Beverages",
                                icon = "🏥",
                                isSelected = selectedAuthorities.contains("NAFDAC"),
                                onSelect = {
                                    selectedAuthorities = if (selectedAuthorities.contains("NAFDAC")) selectedAuthorities - "NAFDAC" else selectedAuthorities + "NAFDAC"
                                }
                            )
                            AuthorityCard(
                                name = "SON",
                                fullname = "Standards Organisation of Nigeria",
                                type = "Manufacturing · Industrial · Electronics",
                                icon = "🏭",
                                isSelected = selectedAuthorities.contains("SON"),
                                onSelect = {
                                    selectedAuthorities = if (selectedAuthorities.contains("SON")) selectedAuthorities - "SON" else selectedAuthorities + "SON"
                                }
                            )
                            AuthorityCard(
                                name = "FCCPC",
                                fullname = "Federal Competition & Consumer Protection Commission",
                                type = "Consumer Protection · Market Fraud",
                                icon = "⚖️",
                                isSelected = selectedAuthorities.contains("FCCPC"),
                                onSelect = {
                                    selectedAuthorities = if (selectedAuthorities.contains("FCCPC")) selectedAuthorities - "FCCPC" else selectedAuthorities + "FCCPC"
                                }
                            )
                        }
                        2 -> {
                            Text("Please enter the counterfeiting incident details:", color = SlateText, fontSize = 12.sp)

                            OutlinedTextField(
                                value = rptLocation,
                                onValueChange = { rptLocation = it },
                                label = { Text("Incident Location (e.g. Lagos Island Market) *") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = rptUnits,
                                    onValueChange = { rptUnits = it },
                                    label = { Text("Estimated Fake Units") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = rptLoss,
                                    onValueChange = { rptLoss = it },
                                    label = { Text("Estimated Financial Loss (₦)") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = rptProducts,
                                onValueChange = { rptProducts = it },
                                label = { Text("Affected Product / Barcodes *") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = rptDescription,
                                onValueChange = { rptDescription = it },
                                label = { Text("Incident Description *") },
                                placeholder = { Text("Describe how you found the counterfeits, who distributed them, shop addresses, etc…") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 4
                            )
                        }
                        3 -> {
                            Text("Formal generated report document preview:", color = SlateText, fontSize = 12.sp)

                            val mfr = viewModel.manufacturer.collectAsStateWithLifecycle().value
                            val reportTemplate = """
FORMAL COUNTERFEIT PRODUCT COMPLAINT
========================================
Reference No : $refNo
Date Filed   : ${SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())}
Filed Via    : ScanNaija Product Security Platform v2.1
Submitted To : ${selectedAuthorities.joinToString(" / ")}

COMPLAINANT DETAILS
Company Name     : ${mfr?.nameOfCompany ?: "Guest Brand"}
Registration No  : ${mfr?.regNo ?: "N/A"}
CEO / Director   : ${mfr?.ceo ?: "N/A"}
Manufacturer ID  : ${mfr?.mfrId ?: "N/A"}

INCIDENT DETAILS
Date of Incident         : $rptDate
Location                 : $rptLocation
Est. Counterfeit Units   : ${rptUnits.ifEmpty { "Unknown" }}
Estimated Financial Loss : ₦${rptLoss.ifEmpty { "Not estimated" }}

Affected Products / Barcodes:
${rptProducts.split("\n").map { "  • $it" }.joinToString("\n")}

DESCRIPTION
$rptDescription

DECLARATION
I request the relevant authority to investigate and take appropriate legal action against the counterfeiters.

Signed : ${mfr?.ceo ?: mfr?.nameOfCompany ?: "Complainant"}
                            """.trimIndent()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SpaceNavy, shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(8.dp))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = reportTemplate,
                                    color = SlateText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp
                                )
                            }

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(reportTemplate))
                                    Toast.makeText(context, "Complaint document copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SpaceNavyLightCard),
                                border = BorderStroke(1.dp, SpaceNavyBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy text")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Complete Document", color = NeonEmerald)
                            }
                        }
                        4 -> {
                            // Submission Success Screen
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(NeonEmerald.copy(alpha = 0.15f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Success", tint = NeonEmerald, modifier = Modifier.size(32.dp))
                                }

                                Text("Complaint Submitted Successfully", color = LightBlueText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Your complaint was successfully logged and submitted to:", color = SlateText, fontSize = 12.sp, textAlign = TextAlign.Center)
                                Text(selectedAuthorities.joinToString(", "), color = NeonEmerald, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(6.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(NeonEmerald.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp))
                                        .border(1.dp, NeonEmerald.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("REFERENCE ID", color = DarkSlateText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    Text(refNo, color = NeonEmerald, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 1 && step < 4) {
                        TextButton(onClick = { step-- }) {
                            Text("Back", color = SlateText)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (step < 3) {
                        Button(
                            onClick = {
                                if (step == 1 && selectedAuthorities.isEmpty()) {
                                    Toast.makeText(context, "Select at least one authority first!", Toast.LENGTH_SHORT).show()
                                } else if (step == 2 && (rptLocation.isEmpty() || rptProducts.isEmpty() || rptDescription.isEmpty())) {
                                    Toast.makeText(context, "Please fill required fields!", Toast.LENGTH_SHORT).show()
                                } else {
                                    step++
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
                        ) {
                            Text("Next", color = SpaceNavy)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = SpaceNavy, modifier = Modifier.size(16.dp))
                        }
                    } else if (step == 3) {
                        Button(
                            onClick = { step = 4 },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Submit")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit Report", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
                        ) {
                            Text("Done", color = SpaceNavy)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepItem(number: Int, label: String, active: Boolean, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    if (done) NeonEmerald else if (active) NeonEmerald.copy(alpha = 0.2f) else Color.Transparent,
                    shape = CircleShape
                )
                .border(1.dp, if (done || active) NeonEmerald else SpaceNavyBorder, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Default.Check, contentDescription = "Done", tint = SpaceNavy, modifier = Modifier.size(12.dp))
            } else {
                Text(
                    text = number.toString(),
                    color = if (active) NeonEmerald else SlateText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = label,
            color = if (active || done) LightBlueText else DarkSlateText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AuthorityCard(
    name: String,
    fullname: String,
    type: String,
    icon: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NeonEmerald.copy(alpha = 0.05f) else SpaceNavyLightCard)
            .border(
                2.dp,
                if (isSelected) NeonEmerald else SpaceNavyBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(SpaceNavy, shape = RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 18.sp)
            }

            Column {
                Text(name, color = LightBlueText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(fullname, color = SlateText, fontSize = 11.sp)
                Text(type, color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ═══════════════════════════════════════════
// SETTINGS SCREEN VIEW
// ═══════════════════════════════════════════
@Composable
fun SettingsScreen(viewModel: ScanNaijaViewModel) {
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val autoSaveDb by viewModel.autosave.collectAsStateWithLifecycle()
    val dashboardAlerts by viewModel.showDashboardAlerts.collectAsStateWithLifecycle()
    val useDarkBars by viewModel.useDarkBars.collectAsStateWithLifecycle()
    val formatSelection by viewModel.barcodeFormat.collectAsStateWithLifecycle()

    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf(serverUrl) }
    var keyInput by remember { mutableStateOf(apiKey) }
    var keyVisible by remember { mutableStateOf(false) }

    var formatDropdownOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Platform Settings", color = LightBlueText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Manage server registries, scan behaviors, and system clear keys.", color = SlateText, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    viewModel.triggerConfirm(
                        title = "Reset Company Config",
                        desc = "This will completely clear your verified company registration, manufacturer ID, and ALL databases. This is irreversible!"
                    ) {
                        viewModel.clearAllCompanyData()
                        Toast.makeText(context, "System fully reset!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f))
            ) {
                Text("Reset App Data", color = AlertRed)
            }
        }

        // SERVER CONFIG PANEL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceNavyDarkCard, shape = RoundedCornerShape(16.dp))
                .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("ScanNaija Server Configuration", color = LightBlueText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Integrate with standard API server registry endpoints to sync security keys globally.", color = DarkSlateText, fontSize = 11.sp)

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Server Base URL") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Registry API Key") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Visibility",
                                tint = SlateText
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (connectionStatus != null) {
                    Text(
                        text = connectionStatus!!,
                        color = if (connectionStatus!!.contains("Connected")) NeonEmerald else WarnOrange,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveServerSettings(
                                url = urlInput,
                                key = keyInput,
                                autoSaveDb = autoSaveDb,
                                alerts = dashboardAlerts,
                                darkBars = useDarkBars,
                                format = formatSelection
                            )
                            Toast.makeText(context, "Server credentials saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Server Config", color = SpaceNavy, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.testServerConnection(urlInput, keyInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpaceNavyLightCard),
                        border = BorderStroke(1.dp, SpaceNavyBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Connection", color = NeonEmerald)
                    }
                }
            }
        }

        // PREFERENCES CONFIG PANEL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceNavyDarkCard, shape = RoundedCornerShape(16.dp))
                .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Application Preferences", color = LightBlueText, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                PreferenceSwitchRow(
                    title = "Auto-save to database",
                    desc = "Automatically saves products to Room local DB when barcodes are minted.",
                    checked = autoSaveDb,
                    onCheckedChange = {
                        viewModel.saveServerSettings(
                            url = serverUrl,
                            key = apiKey,
                            autoSaveDb = it,
                            alerts = dashboardAlerts,
                            darkBars = useDarkBars,
                            format = formatSelection
                        )
                    }
                )

                PreferenceSwitchRow(
                    title = "Show alerts on dashboard",
                    desc = "Displays warning banners on home dashboard screen for active flags.",
                    checked = dashboardAlerts,
                    onCheckedChange = {
                        viewModel.saveServerSettings(
                            url = serverUrl,
                            key = apiKey,
                            autoSaveDb = autoSaveDb,
                            alerts = it,
                            darkBars = useDarkBars,
                            format = formatSelection
                        )
                    }
                )

                PreferenceSwitchRow(
                    title = "Dark barcode lines",
                    desc = "Mints barcode vertical line drawings in black (for high contrast printing).",
                    checked = useDarkBars,
                    onCheckedChange = {
                        viewModel.saveServerSettings(
                            url = serverUrl,
                            key = apiKey,
                            autoSaveDb = autoSaveDb,
                            alerts = dashboardAlerts,
                            darkBars = it,
                            format = formatSelection
                        )
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Barcode Format standard", color = LightBlueText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("CODE128 is highly recommended for Nigerian product codes.", color = SlateText, fontSize = 11.sp)
                    }

                    Box {
                        OutlinedTextField(
                            value = formatSelection,
                            onValueChange = {},
                            readOnly = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonEmerald, unfocusedBorderColor = SpaceNavyBorder),
                            trailingIcon = {
                                IconButton(onClick = { formatDropdownOpen = !formatDropdownOpen }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                            },
                            modifier = Modifier.width(150.dp)
                        )
                        DropdownMenu(
                            expanded = formatDropdownOpen,
                            onDismissRequest = { formatDropdownOpen = false },
                            modifier = Modifier.background(SpaceNavyLightCard)
                        ) {
                            listOf("CODE128", "EAN13", "CODE39").forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format, color = LightBlueText) },
                                    onClick = {
                                        viewModel.saveServerSettings(
                                            url = serverUrl,
                                            key = apiKey,
                                            autoSaveDb = autoSaveDb,
                                            alerts = dashboardAlerts,
                                            darkBars = useDarkBars,
                                            format = format
                                        )
                                        formatDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceSwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LightBlueText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = SlateText, fontSize = 11.sp)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = NeonEmerald)
        )
    }
}
