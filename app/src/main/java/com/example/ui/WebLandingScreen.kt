package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebLandingScreen(
    onNavigateToTab: (String) -> Unit
) {
    val context = LocalContext.current
    var isReloading by remember { mutableStateOf(false) }

    LaunchedEffect(isReloading) {
        if (isReloading) {
            delay(800)
            isReloading = false
            Toast.makeText(context, "Web Portal Refreshed!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceNavy)
    ) {
        // 💻 WEB BROWSER CHROMIUM CHASSIS HEADER
        BrowserAddressBar(
            url = "https://www.scannaija.gov.ng/saas-security-portal",
            isReloading = isReloading,
            onReloadTrigger = { isReloading = true }
        )

        HorizontalDivider(color = SpaceNavyBorder, thickness = 1.dp)

        if (isReloading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonEmerald,
                    trackColor = SpaceNavyLightCard
                )
            }
        }

        // Main Scrollable Web Landing Page Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // 🌐 WEB HERO SECTION
            item {
                WebHeroSection(
                    onActionLaunch = { onNavigateToTab("dashboard") },
                    onActionVerify = { onNavigateToTab("buyer") }
                )
            }

            // 📈 SaaS LIVE SECURITY METRICS STRIP
            item {
                WebMetricsStrip()
            }

            // 🛡️ INTERACTIVE DECENTRALIZED VERIFIER (SANDBOX)
            item {
                InteractiveVerifierSandbox()
            }

            // 💰 INTERACTIVE ROI LOSS RECOVERY CALCULATOR
            item {
                InteractiveRoiCalculator()
            }

            // 🚀 COHESIVE SYSTEM FEATURES GRID
            item {
                WebFeaturesGrid()
            }

            // 💳 REASONABLE SAAS SUBSCRIPTION PRICING
            item {
                WebPricingSection(onNavigateToTab)
            }

            // ✉️ REGULATORY NEWSLETTER & FOOTER
            item {
                WebFooterSection()
            }
        }
    }
}

// ═══════════════════════════════════════════
// SUBCOMPONENTS
// ═══════════════════════════════════════════

@Composable
fun BrowserAddressBar(
    url: String,
    isReloading: Boolean,
    onReloadTrigger: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceNavyLightCard)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mock Window Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).background(AlertRed, shape = CircleShape))
            Box(modifier = Modifier.size(10.dp).background(WarnOrange, shape = CircleShape))
            Box(modifier = Modifier.size(10.dp).background(Color(0xFF22C55E), shape = CircleShape))
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Navigation controls
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = DarkSlateText,
            modifier = Modifier.size(16.dp)
        )
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Forward",
            tint = DarkSlateText,
            modifier = Modifier.size(16.dp)
        )
        Icon(
            imageVector = if (isReloading) Icons.Default.Close else Icons.Default.Refresh,
            contentDescription = "Refresh",
            tint = SlateText,
            modifier = Modifier
                .size(16.dp)
                .clickable { onReloadTrigger() }
        )

        // Address Field
        Row(
            modifier = Modifier
                .weight(1f)
                .background(SpaceNavyDarkCard, shape = RoundedCornerShape(20.dp))
                .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secure Connection",
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = url,
                    color = LightBlueText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "SECURE PROTOCOL",
                color = Color(0xFF22C55E),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Web App Badge
        Box(
            modifier = Modifier
                .background(NeonEmerald.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "WEB APP",
                color = NeonEmerald,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun WebHeroSection(
    onActionLaunch: () -> Unit,
    onActionVerify: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing Badge
        Row(
            modifier = Modifier
                .background(NeonEmerald.copy(alpha = 0.1f), shape = CircleShape)
                .border(1.dp, NeonEmerald.copy(alpha = 0.2f), shape = CircleShape)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(NeonEmerald, shape = CircleShape)
            )
            Text(
                text = "SaaS WEB PORTAL v2.1 IS LIVE IN NIGERIA",
                color = NeonEmerald,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Main Catchphrase
        Text(
            text = "Authenticate Branded Goods.\nProtect Nigerian Consumers.",
            color = LightBlueText,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Subtext
        Text(
            text = "ScanNaija is the nation's leading cloud-hosted product verification network. Generates cryptographically signed barcodes, tracks duplicate flags across major regions, and transmits instant reports directly to regulatory bodies.",
            color = SlateText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.widthIn(max = 520.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Hero CTA Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onActionLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = "Console",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Launch Admin Console", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onActionVerify,
                border = BorderStroke(1.dp, SpaceNavyBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateText),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Verify",
                    tint = NeonEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Buyer Verifier", color = LightBlueText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // SaaS Hero Visual Asset
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, SpaceNavyBorder),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.scannaija_web_hero_1784025439994),
                    contentDescription = "ScanNaija Web Dashboard Mockup",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
    }
}

@Composable
fun WebMetricsStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceNavyLightCard)
            .padding(vertical = 20.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("3.8M+", color = NeonEmerald, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Verified Scans", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        Box(modifier = Modifier.width(1.dp).height(24.dp).background(SpaceNavyBorder))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("99.8%", color = NeonEmerald, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Accuracy Rate", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        Box(modifier = Modifier.width(1.dp).height(24.dp).background(SpaceNavyBorder))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("100%", color = NeonEmerald, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Regulatory Sync", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun InteractiveVerifierSandbox() {
    var queryInput by remember { mutableStateOf("SN-MED-9402") }
    var searchResult by remember { mutableStateOf<String?>(null) }
    var isValidResult by remember { mutableStateOf(true) }
    var isVerifying by remember { mutableStateOf(false) }

    val sampleKeys = listOf("SN-MED-9402", "SN-DRK-1102", "SN-BEV-7840")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .background(SpaceNavyDarkCard, shape = RoundedCornerShape(16.dp))
            .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Text(
            text = "🛡️ Cryptographic Key Sandbox Verifier",
            color = LightBlueText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Test the ScanNaija secure blockchain hash ledger right from this browser sandbox. Use an authorized SKU or type your own.",
            color = SlateText,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Preset chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            sampleKeys.forEach { key ->
                Box(
                    modifier = Modifier
                        .background(
                            if (queryInput == key) NeonEmerald.copy(alpha = 0.15f) else SpaceNavyLightCard,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (queryInput == key) NeonEmerald else SpaceNavyBorder,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { queryInput = key }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(key, color = if (queryInput == key) NeonEmerald else SlateText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Input Field and Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = queryInput,
                onValueChange = { queryInput = it },
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                placeholder = { Text("Enter Cryptographic Signature Code", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonEmerald,
                    unfocusedBorderColor = SpaceNavyBorder,
                    focusedTextColor = LightBlueText,
                    unfocusedTextColor = SlateText
                )
            )

            Button(
                onClick = {
                    isVerifying = true
                    searchResult = null
                },
                enabled = queryInput.isNotEmpty() && !isVerifying,
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Verify Secure Hash", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        LaunchedEffect(isVerifying) {
            if (isVerifying) {
                delay(1200)
                isVerifying = false
                isValidResult = when (queryInput) {
                    "SN-MED-9402" -> true
                    "SN-DRK-1102" -> true
                    "SN-BEV-7840" -> true
                    else -> Random.nextBoolean()
                }
                searchResult = if (isValidResult) {
                    "SECURE INTEGRITY CHECK PASSED\n\n• Manufacturer: GSK Pharmaceuticals Nigeria\n• Product: Amoxicillin Batch 9402\n• Regulatory Status: Approved by NAFDAC (Reg: A4-1902)\n• Expiry Status: Valid (Expiry Date: 2027-12-01)\n• Safe Signature: 0x8F91B2C81E73A"
                } else {
                    "🚨 WARNING: BARCODE COLLISION DETECTED\n\n• Hash Signature: Invalid Signature Hash\n• Status: UNREGISTERED / SUSPECT FORGERY\n• Region Logged: Lagos Retail Sector\n• Action Recommended: DO NOT CONSUME. An automatic piracy flag has been recorded."
                }
            }
        }

        // Interactive Result Visualizer
        AnimatedVisibility(
            visible = isVerifying || searchResult != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            if (isVerifying) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpaceNavyLightCard, shape = RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = NeonEmerald)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Syncing decentralized cryptographic ledger...", color = SlateText, fontSize = 12.sp)
                }
            } else if (searchResult != null) {
                val bannerColor = if (isValidResult) NeonEmerald.copy(alpha = 0.08f) else AlertRed.copy(alpha = 0.08f)
                val borderColor = if (isValidResult) NeonEmerald.copy(alpha = 0.3f) else AlertRed.copy(alpha = 0.3f)
                val textColor = if (isValidResult) NeonEmerald else AlertRed

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bannerColor, shape = RoundedCornerShape(8.dp))
                        .border(1.dp, borderColor, shape = RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (isValidResult) "PASSED INTEGRITY CHECKS" else "CRITICAL THREAT REGISTERED",
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = searchResult ?: "",
                        color = LightBlueText,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveRoiCalculator() {
    var monthlyVolume by remember { mutableStateOf("150000") }
    var estimatedPiracyRate by remember { mutableStateOf("6") }

    val volumeVal = monthlyVolume.toDoubleOrNull() ?: 0.0
    val piracyRateVal = estimatedPiracyRate.toDoubleOrNull() ?: 0.0

    // Calculations based on standard product value of N2,500
    val itemCost = 2500.0
    val monthlyLoss = volumeVal * (piracyRateVal / 100.0) * itemCost
    val annualSavings = monthlyLoss * 12 * 0.95 // Assuming 95% recovery rate

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .background(SpaceNavyDarkCard, shape = RoundedCornerShape(16.dp))
            .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💰 SaaS Interactive Loss Recovery Estimator",
                color = LightBlueText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .background(WarnOrange.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ROI PLANNER",
                    color = WarnOrange,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            text = "Determine how much monthly revenue your company is losing to unlicensed counterfeiters and how much ScanNaija can safeguard annually.",
            color = SlateText,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Volume Input Box
            Column(modifier = Modifier.weight(1f)) {
                Text("Monthly Production Volume (units)", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = monthlyVolume,
                    onValueChange = { monthlyVolume = it.filter { char -> char.isDigit() } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonEmerald,
                        unfocusedBorderColor = SpaceNavyBorder,
                        focusedTextColor = LightBlueText,
                        unfocusedTextColor = SlateText
                    )
                )
            }

            // Rate Input Box
            Column(modifier = Modifier.weight(1f)) {
                Text("Est. Forgery / Piracy Rate (%)", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = estimatedPiracyRate,
                    onValueChange = { estimatedPiracyRate = it.filter { char -> char.isDigit() || char == '.' } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonEmerald,
                        unfocusedBorderColor = SpaceNavyBorder,
                        focusedTextColor = LightBlueText,
                        unfocusedTextColor = SlateText
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Calculations Display Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceNavyLightCard, shape = RoundedCornerShape(10.dp))
                .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(10.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Monthly Revenue Leaking", color = DarkSlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format("₦%,.2f", monthlyLoss),
                    color = AlertRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(SpaceNavyBorder)
            )

            Column {
                Text("Estimated Annual Safe Recovery", color = NeonEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format("₦%,.2f", annualSavings),
                    color = NeonEmerald,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "*Estimated recovery based on active security hash enforcement and official reports targeting counterfeit supply networks.",
            color = DarkSlateText,
            fontSize = 9.sp,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
fun WebFeaturesGrid() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = "🛡️ Cutting-Edge Product Security Ecosystem",
            color = LightBlueText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Everything you need to safeguard and authenticate your brand nationwide.",
            color = SlateText,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WebFeatureCard(
                icon = Icons.Default.QrCode,
                title = "Cryptographically Locked SKU Keys",
                desc = "Stop copying static barcodes. Our backend produces cryptographically verified keys generated sequentially with secure metadata hashes so replication triggers instant alarms."
            )
            WebFeatureCard(
                icon = Icons.Default.GpsFixed,
                title = "Predictive Duplication Map tracking",
                desc = "Track location footprints. Whenever a buyer attempts a verification of an unauthorized duplication, the scanning region and network telemetry are recorded to trace counterfeiter caches."
            )
            WebFeatureCard(
                icon = Icons.Default.CloudSync,
                title = "Unified Authority Filing Hub",
                desc = "Transmit instant official formal dossiers directly to NAFDAC, SON, or CPC dashboards. Streamlines administrative compliance and accelerates litigation with certified audit reports."
            )
            WebFeatureCard(
                icon = Icons.Default.DateRange,
                title = "Predictive Expiry & Warning System",
                desc = "SaaS watchdogs continuously poll your SQLite batch limits. Receive automated reminders of retail goods approaching expiry to pull stock safely before hazard triggers."
            )
        }
    }
}

@Composable
fun WebFeatureCard(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceNavyDarkCard, shape = RoundedCornerShape(12.dp))
            .border(1.dp, SpaceNavyBorder, shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .background(NeonEmerald.copy(alpha = 0.1f), shape = CircleShape)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonEmerald,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = LightBlueText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                color = SlateText,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun WebPricingSection(
    onNavigateToTab: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = "💳 Affordable Flexible Cloud SaaS Plans",
            color = LightBlueText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Select a plan that suits your production capacity. Upgrade or scale anytime.",
            color = SlateText,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 20.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PricingTierCard(
                title = "Startup Shield",
                price = "₦0",
                subText = "Free Forever",
                features = listOf(
                    "Up to 5 secure products listed",
                    "Max 100 scans generated monthly",
                    "Standard Local SQLite SQLite storage",
                    "Basic consumer client validation"
                ),
                buttonText = "Use Free Sandbox",
                isPopular = false,
                onClick = { onNavigateToTab("dashboard") }
            )

            PricingTierCard(
                title = "Naija Scale-Up",
                price = "₦45,000",
                subText = "per month",
                features = listOf(
                    "Unlimited secure product catalogs",
                    "Unlimited encrypted barcode hashes",
                    "Instant region telemetry tracking",
                    "1-Click dossiers to SON & CPC",
                    "24/7 dedicated security API endpoints"
                ),
                buttonText = "Deploy Plan Now",
                isPopular = true,
                onClick = { onNavigateToTab("dashboard") }
            )

            PricingTierCard(
                title = "Enterprise Sovereign",
                price = "Custom",
                subText = "contact legal team",
                features = listOf(
                    "Isolated sovereign cloud node hosting",
                    "Real-time direct API pipe with SON & NAFDAC",
                    "Specialized legal/counterfeiting expert counsel",
                    "Customized barcode layouts & batch encryption keys"
                ),
                buttonText = "Contact Enterprise Team",
                isPopular = false,
                onClick = { onNavigateToTab("settings") }
            )
        }
    }
}

@Composable
fun PricingTierCard(
    title: String,
    price: String,
    subText: String,
    features: List<String>,
    buttonText: String,
    isPopular: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isPopular) NeonEmerald.copy(alpha = 0.04f) else SpaceNavyDarkCard,
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = if (isPopular) 2.dp else 1.dp,
                color = if (isPopular) NeonEmerald else SpaceNavyBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(20.dp)
    ) {
        if (isPopular) {
            Row(
                modifier = Modifier
                    .background(NeonEmerald, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.Start)
            ) {
                Text(
                    text = "MOST POPULAR",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Text(title, color = LightBlueText, fontSize = 16.sp, fontWeight = FontWeight.Bold)

        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(price, color = NeonEmerald, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.width(4.dp))
            Text(subText, color = SlateText, fontSize = 11.sp)
        }

        HorizontalDivider(color = SpaceNavyBorder, modifier = Modifier.padding(vertical = 12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Included",
                        tint = NeonEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(feature, color = SlateText, fontSize = 12.sp)
                }
            }
        }

        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPopular) NeonEmerald else SpaceNavyLightCard
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = buttonText,
                color = if (isPopular) Color.White else LightBlueText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun WebFooterSection() {
    var emailInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceNavyLightCard)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📬 Get Live Security Bulletins",
            color = LightBlueText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Subscribe to receive automatic alerts when security updates or regulatory changes occur in Nigeria's safety sectors.",
            color = SlateText,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                placeholder = { Text("Enter company email address", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonEmerald,
                    unfocusedBorderColor = SpaceNavyBorder,
                    focusedTextColor = LightBlueText,
                    unfocusedTextColor = SlateText
                )
            )

            Button(
                onClick = {
                    Toast.makeText(context, "Subscribed successfully!", Toast.LENGTH_SHORT).show()
                    emailInput = ""
                },
                enabled = emailInput.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Subscribe", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Official partner logos text
        Text(
            text = "TRUSTED COMPLIANCE PLATFORM FOR NIGERIA",
            color = DarkSlateText,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NAFDAC CERTIFIED", color = SlateText, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.size(4.dp).background(DarkSlateText, shape = CircleShape))
            Text("CPC ALIGNED", color = SlateText, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.size(4.dp).background(DarkSlateText, shape = CircleShape))
            Text("SON REGISTERED", color = SlateText, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "© 2026 ScanNaija SaaS Inc. All rights reserved. Designed to uphold NAFDAC Act Cap N1 LFN.",
            color = DarkSlateText,
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
    }
}
