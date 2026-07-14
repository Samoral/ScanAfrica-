package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.ScanNaijaRepository
import com.example.data.repository.VerificationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScanNaijaViewModel(
    application: Application,
    private val repository: ScanNaijaRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("scannaija_prefs", Context.MODE_PRIVATE)

    // Local Compose Tab UI state
    val currentTab = MutableStateFlow("landing")

    // Preferences state
    val serverUrl = MutableStateFlow(prefs.getString("server_url", "http://localhost:3000") ?: "http://localhost:3000")
    val apiKey = MutableStateFlow(prefs.getString("api_key", "") ?: "")
    val autosave = MutableStateFlow(prefs.getBoolean("pref_autosave", false))
    val showDashboardAlerts = MutableStateFlow(prefs.getBoolean("pref_alerts", true))
    val useDarkBars = MutableStateFlow(prefs.getBoolean("pref_darkbars", false))
    val barcodeFormat = MutableStateFlow(prefs.getString("pref_format", "CODE128") ?: "CODE128")

    // Expose Room queries directly as state flows
    val manufacturer: StateFlow<ManufacturerEntity?> = repository.manufacturer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentScans: StateFlow<List<ScanHistoryEntity>> = repository.recentScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesListItems: StateFlow<List<SalesListEntity>> = repository.salesListItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePiracyFlags: StateFlow<List<PiracyFlagEntity>> = repository.activePiracyFlags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Barcode generation temporary states
    data class TempGenerationDetails(
        val productName: String,
        val size: String,
        val category: String,
        val price: String,
        val mfd: String,
        val expiry: String,
        val govAgency: String,
        val govRegNo: String,
        val packagedBy: String,
        val nutrients: String,
        val ingredients: String,
        val wrapperThemeBg: String,
        val wrapperThemeTextColor: String,
        val barcodes: List<String>
    )

    val tempGeneration = MutableStateFlow<TempGenerationDetails?>(null)
    val barcodesGeneratedSessionCount = MutableStateFlow(0)

    // Verification temporary states (Buyer Portal)
    val verificationLoading = MutableStateFlow(false)
    val verificationResult = MutableStateFlow<VerificationResult?>(null)

    // Server connection test states
    val connectionStatus = MutableStateFlow<String?>(null)
    val syncStatus = MutableStateFlow<String>("Syncing…")
    val syncBadgeState = MutableStateFlow<String>("pending") // ok, pending, err

    // Navigation and Modals state
    val showConfirmModal = MutableStateFlow(false)
    val confirmModalTitle = MutableStateFlow("")
    val confirmModalDesc = MutableStateFlow("")
    private var confirmCallback: (() -> Unit)? = null

    val showReportModal = MutableStateFlow(false)

    init {
        // Sync cache to repository configuration
        repository.serverUrl = serverUrl.value
        repository.apiKey = apiKey.value
        pollPiracyFlagsPeriodically()
    }

    private fun pollPiracyFlagsPeriodically() {
        viewModelScope.launch {
            while (true) {
                if (manufacturer.value != null) {
                    val result = repository.syncWithServer()
                    if (result.isSuccess) {
                        syncBadgeState.value = "ok"
                        syncStatus.value = "${allProducts.value.size} synced"
                    } else {
                        syncBadgeState.value = "err"
                        syncStatus.value = "Sync failed"
                    }
                } else {
                    syncBadgeState.value = "pending"
                    syncStatus.value = "No setup"
                }
                kotlinx.coroutines.delay(30000) // Poll every 30 seconds
            }
        }
    }

    // Tab Navigation
    fun changeTab(tabName: String) {
        currentTab.value = tabName
    }

    // Modal Confirmation Utilities
    fun triggerConfirm(title: String, desc: String, onConfirm: () -> Unit) {
        confirmModalTitle.value = title
        confirmModalDesc.value = desc
        confirmCallback = onConfirm
        showConfirmModal.value = true
    }

    fun executeConfirm() {
        confirmCallback?.invoke()
        showConfirmModal.value = false
        confirmCallback = null
    }

    fun dismissConfirm() {
        showConfirmModal.value = false
        confirmCallback = null
    }

    // Setup Manufacturer
    fun saveManufacturer(
        name: String,
        regNo: String,
        date: String,
        email: String,
        country: String,
        ceo: String,
        affiliate: Boolean,
        affiliateName: String?
    ) {
        viewModelScope.launch {
            val uid = Math.abs(UUID.randomUUID().mostSignificantBits) % 1_000_000_000_000_000L
            val mfrId = "$name-$uid"
            val entity = ManufacturerEntity(
                id = uid,
                nameOfCompany = name,
                regNo = regNo,
                dateIssued = date,
                email = email,
                country = country,
                ceo = ceo,
                affiliate = affiliate,
                affiliateCompanyName = affiliateName,
                mfrId = mfrId
            )
            repository.saveManufacturer(entity)
            triggerServerSync()
        }
    }

    fun clearAllCompanyData() {
        viewModelScope.launch {
            repository.clearManufacturer()
            repository.clearAllProducts()
            repository.clearScanHistory()
            repository.clearSalesList()
            repository.dismissAllPiracyFlags()
            tempGeneration.value = null
            barcodesGeneratedSessionCount.value = 0
            verificationResult.value = null
        }
    }

    // Barcode Generation
    fun generateBarcodes(
        productName: String,
        category: String,
        size: String,
        price: String,
        mfd: String,
        expiry: String,
        quantity: Int,
        govAgency: String,
        govRegNo: String,
        packagedBy: String,
        bgTheme: String,
        textTheme: String,
        nutrients: String,
        ingredients: String
    ) {
        viewModelScope.launch {
            val baseId = manufacturer.value?.mfrId ?: "PRODUCT"
            val barcodes = (1..quantity).map {
                val rand = Math.abs(UUID.randomUUID().mostSignificantBits) % 1_000_000_000_000L
                "$baseId-$rand"
            }

            val details = TempGenerationDetails(
                productName = productName,
                size = size,
                category = category,
                price = "₦$price",
                mfd = mfd,
                expiry = expiry,
                govAgency = govAgency,
                govRegNo = govRegNo,
                packagedBy = packagedBy,
                nutrients = nutrients,
                ingredients = ingredients,
                wrapperThemeBg = bgTheme,
                wrapperThemeTextColor = textTheme,
                barcodes = barcodes
            )
            tempGeneration.value = details
            barcodesGeneratedSessionCount.value += quantity

            if (autosave.value) {
                saveTempBarcodesToDbInternal(details)
            }
        }
    }

    fun saveTempBarcodesToDb() {
        val details = tempGeneration.value ?: return
        viewModelScope.launch {
            saveTempBarcodesToDbInternal(details)
        }
    }

    private suspend fun saveTempBarcodesToDbInternal(details: TempGenerationDetails) {
        val nowStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        details.barcodes.forEach { code ->
            val product = ProductEntity(
                barcode = code,
                dateAdded = nowStr,
                productName = details.productName,
                size = details.size,
                category = details.category,
                price = details.price,
                mfd = details.mfd,
                expiry = details.expiry,
                nutrients = details.nutrients,
                ingredients = details.ingredients,
                govAgency = details.govAgency,
                govRegNo = details.govRegNo,
                packagedBy = details.packagedBy,
                wrapperThemeBg = details.wrapperThemeBg,
                wrapperThemeTextColor = details.wrapperThemeTextColor
            )
            repository.insertProduct(product)
        }
        triggerServerSync()
    }

    fun deleteProduct(barcode: String) {
        viewModelScope.launch {
            repository.deleteProductByBarcode(barcode)
            triggerServerSync()
        }
    }

    fun clearAllProducts() {
        viewModelScope.launch {
            repository.clearAllProducts()
            triggerServerSync()
        }
    }

    // Expiry and Sales List Management
    fun moveSingleToSales(product: ProductEntity) {
        viewModelScope.launch {
            val item = SalesListEntity(
                barcode = product.barcode,
                productName = product.productName,
                category = product.category,
                size = product.size,
                price = product.price,
                expiry = product.expiry
            )
            repository.insertSalesListItem(item)
        }
    }

    fun moveAllExpiringToSales() {
        viewModelScope.launch {
            val now = Date()
            val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
            val allProds = allProducts.value
            val salesItems = salesListItems.value

            allProds.forEach { p ->
                val expDate = parseExpiryDate(p.expiry)
                if (expDate != null) {
                    val diff = expDate.time - now.time
                    if (diff in 0..thirtyDaysMs) {
                        if (salesItems.none { it.barcode == p.barcode }) {
                            val item = SalesListEntity(
                                barcode = p.barcode,
                                productName = p.productName,
                                category = p.category,
                                size = p.size,
                                price = p.price,
                                expiry = p.expiry
                            )
                            repository.insertSalesListItem(item)
                        }
                    }
                }
            }
        }
    }

    fun removeFromSalesList(barcode: String) {
        viewModelScope.launch {
            repository.deleteSalesItemByBarcode(barcode)
        }
    }

    fun clearSalesList() {
        viewModelScope.launch {
            repository.clearSalesList()
        }
    }

    // Counterfeit & Alerts
    fun dismissFlag(id: Int) {
        viewModelScope.launch {
            repository.dismissPiracyFlag(id)
        }
    }

    fun dismissAllFlags() {
        viewModelScope.launch {
            repository.dismissAllPiracyFlags()
        }
    }

    // Buyer Verification
    fun runVerification(companyName: String, barcodeRaw: String) {
        viewModelScope.launch {
            verificationLoading.value = true
            verificationResult.value = null

            // Sanitize barcode in case of URL paste
            var barcode = barcodeRaw.trim()
            try {
                if (barcode.startsWith("http://") || barcode.startsWith("https://")) {
                    val uri = android.net.Uri.parse(barcode)
                    barcode = uri.getQueryParameter("barcode")
                        ?: uri.getQueryParameter("code")
                        ?: uri.getQueryParameter("b")
                        ?: uri.lastPathSegment
                        ?: barcodeRaw
                }
            } catch (e: Exception) { /* use raw */ }

            val res = repository.verifyProduct(barcode, companyName)
            verificationResult.value = res
            verificationLoading.value = false
        }
    }

    fun clearScanHistory() {
        viewModelScope.launch {
            repository.clearScanHistory()
        }
    }

    // Server Config
    fun saveServerSettings(
        url: String,
        key: String,
        autoSaveDb: Boolean,
        alerts: Boolean,
        darkBars: Boolean,
        format: String
    ) {
        prefs.edit().apply {
            putString("server_url", url)
            putString("api_key", key)
            putBoolean("pref_autosave", autoSaveDb)
            putBoolean("pref_alerts", alerts)
            putBoolean("pref_darkbars", darkBars)
            putString("pref_format", format)
            apply()
        }
        serverUrl.value = url
        apiKey.value = key
        autosave.value = autoSaveDb
        showDashboardAlerts.value = alerts
        useDarkBars.value = darkBars
        barcodeFormat.value = format

        repository.serverUrl = url
        repository.apiKey = key
        triggerServerSync()
    }

    fun testServerConnection(url: String, key: String) {
        viewModelScope.launch {
            connectionStatus.value = "Testing connection…"
            val res = repository.testConnection(url, key)
            if (res.isSuccess) {
                val data = res.getOrThrow()
                connectionStatus.value = "Connected — ${data.service} v${data.version}"
            } else {
                connectionStatus.value = "Could not reach server: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun triggerServerSync() {
        viewModelScope.launch {
            if (manufacturer.value != null) {
                syncStatus.value = "Syncing…"
                syncBadgeState.value = "pending"
                val res = repository.syncWithServer()
                if (res.isSuccess) {
                    syncStatus.value = "${allProducts.value.size} synced"
                    syncBadgeState.value = "ok"
                } else {
                    syncStatus.value = "Sync failed"
                    syncBadgeState.value = "err"
                }
            }
        }
    }

    fun parseExpiryDate(str: String): Date? {
        if (str.trim().isEmpty() || str == "—") return null
        return try {
            if (str.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(str)
            } else if (str.contains("-")) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(str)
            } else if (str.contains("/")) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(str)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

class ScanNaijaViewModelFactory(
    private val application: Application,
    private val repository: ScanNaijaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanNaijaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScanNaijaViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
