package com.example.data.repository

import android.util.Log
import com.example.data.local.*
import com.example.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanNaijaRepository(
    private val manufacturerDao: ManufacturerDao,
    private val productDao: ProductDao,
    private val scanHistoryDao: ScanHistoryDao,
    private val salesListDao: SalesListDao,
    private val piracyFlagDao: PiracyFlagDao,
    private val apiClient: ScanNaijaApiClient = ScanNaijaApiClient()
) {
    val manufacturer: Flow<ManufacturerEntity?> = manufacturerDao.getManufacturer()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val recentScans: Flow<List<ScanHistoryEntity>> = scanHistoryDao.getRecentHistory()
    val salesListItems: Flow<List<SalesListEntity>> = salesListDao.getAllSalesItems()
    val activePiracyFlags: Flow<List<PiracyFlagEntity>> = piracyFlagDao.getActiveFlags()

    // Preferences cached locally (or we can inject)
    var serverUrl: String = "http://localhost:3000"
    var apiKey: String = ""

    suspend fun saveManufacturer(manufacturer: ManufacturerEntity) = withContext(Dispatchers.IO) {
        manufacturerDao.clear()
        manufacturerDao.insert(manufacturer)
    }

    suspend fun clearManufacturer() = withContext(Dispatchers.IO) {
        manufacturerDao.clear()
    }

    suspend fun insertProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.insert(product)
    }

    suspend fun deleteProductByBarcode(barcode: String) = withContext(Dispatchers.IO) {
        productDao.deleteByBarcode(barcode)
    }

    suspend fun clearAllProducts() = withContext(Dispatchers.IO) {
        productDao.clearAll()
    }

    suspend fun insertScanHistory(scan: ScanHistoryEntity) = withContext(Dispatchers.IO) {
        scanHistoryDao.insert(scan)
    }

    suspend fun clearScanHistory() = withContext(Dispatchers.IO) {
        scanHistoryDao.clear()
    }

    suspend fun insertSalesListItem(item: SalesListEntity) = withContext(Dispatchers.IO) {
        salesListDao.insert(item)
    }

    suspend fun deleteSalesItemByBarcode(barcode: String) = withContext(Dispatchers.IO) {
        salesListDao.deleteByBarcode(barcode)
    }

    suspend fun clearSalesList() = withContext(Dispatchers.IO) {
        salesListDao.clear()
    }

    suspend fun dismissPiracyFlag(id: Int) = withContext(Dispatchers.IO) {
        piracyFlagDao.dismissFlag(id)
    }

    suspend fun dismissAllPiracyFlags() = withContext(Dispatchers.IO) {
        piracyFlagDao.dismissAll()
    }

    // Server health check
    suspend fun testConnection(url: String, key: String): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val service = apiClient.getService(url)
            val response = service.getHealth(key.ifEmpty { null })
            Result.success(response)
        } catch (e: Exception) {
            Log.e("Repository", "Connection test failed", e)
            Result.failure(e)
        }
    }

    // Sync products with dynamic server configuration
    suspend fun syncWithServer(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val mfr = manufacturer.firstOrNull() ?: return@withContext Result.failure(Exception("No manufacturer setup"))
            val productsList = productDao.getAllProducts().firstOrNull() ?: emptyList()

            val service = apiClient.getService(serverUrl)
            val companySlug = slugify(mfr.nameOfCompany)
            val authKey = apiKey.ifEmpty { null }

            // 1. Register company on server
            val companyReq = CompanyRequest(
                slug = companySlug,
                nameOfCompany = mfr.nameOfCompany,
                regNo = mfr.regNo,
                email = mfr.email,
                mfrId = mfr.mfrId
            )
            val companyRes = service.registerCompany(authKey, companyReq)
            if (!companyRes.isSuccessful) {
                return@withContext Result.failure(Exception("Company registration failed: ${companyRes.code()}"))
            }

            // 2. Sync products bulk
            val dtoList = productsList.map { p ->
                ProductDto(
                    barcode = p.barcode,
                    companyName = mfr.nameOfCompany,
                    companySlug = companySlug,
                    mfrId = mfr.mfrId,
                    productName = p.productName,
                    category = p.category,
                    size = p.size,
                    price = p.price,
                    mfd = p.mfd,
                    expiry = p.expiry,
                    registeredAt = p.dateAdded
                )
            }
            val productsRes = service.syncBulkProducts(authKey, BulkProductsRequest(dtoList))
            if (!productsRes.isSuccessful) {
                return@withContext Result.failure(Exception("Product sync failed: ${productsRes.code()}"))
            }

            // 3. Poll piracy flags
            try {
                val flagsRes = service.getPiracyFlags(authKey, mfr.nameOfCompany)
                val existingFlags = piracyFlagDao.getActiveFlags().firstOrNull() ?: emptyList()
                val newEntities = flagsRes.flags.map { dto ->
                    PiracyFlagEntity(
                        barcode = dto.barcode,
                        claimedCompany = dto.claimedCompany,
                        realCompany = dto.realCompany,
                        timestamp = dto.ts
                    )
                }.filter { entity ->
                    existingFlags.none { it.barcode == entity.barcode && it.timestamp == entity.timestamp }
                }
                if (newEntities.isNotEmpty()) {
                    piracyFlagDao.insertAll(newEntities)
                }
            } catch (e: Exception) {
                Log.e("Repository", "Failed polling piracy flags", e)
            }

            Result.success("Synced ${productsList.size} products")
        } catch (e: Exception) {
            Log.e("Repository", "Sync failed", e)
            Result.failure(e)
        }
    }

    // Verify barcode (authentic, fake, unknown)
    suspend fun verifyProduct(barcode: String, companyName: String): VerificationResult = withContext(Dispatchers.IO) {
        val searchCompanySlug = slugify(companyName)
        val mfr = manufacturer.firstOrNull()

        try {
            // Attempt to query server
            val service = apiClient.getService(serverUrl)
            val barcodeResponse = service.verifyBarcode(barcode)

            if (barcodeResponse.found && barcodeResponse.product != null) {
                val registeredCompany = barcodeResponse.product.companyName
                val match = slugify(registeredCompany) == searchCompanySlug

                if (match) {
                    val entity = ScanHistoryEntity(barcode = barcode, company = companyName, status = "authentic")
                    scanHistoryDao.insert(entity)
                    return@withContext VerificationResult.Authentic(barcodeResponse.product)
                } else {
                    val entity = ScanHistoryEntity(barcode = barcode, company = companyName, status = "fake")
                    scanHistoryDao.insert(entity)

                    // Flag piracy on server
                    try {
                        service.flagPiracy(
                            PiracyFlagRequest(
                                barcode = barcode,
                                claimedCompany = companyName,
                                realCompany = registeredCompany,
                                ts = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("Repository", "Failed to send piracy flag to server", e)
                    }

                    return@withContext VerificationResult.FakeMismatch(
                        barcode = barcode,
                        claimedCompany = companyName,
                        registeredCompany = registeredCompany
                    )
                }
            }

            // Look up company
            val companyResponse = service.verifyCompany(companyName)
            if (companyResponse.found && companyResponse.company != null) {
                val entity = ScanHistoryEntity(barcode = barcode, company = companyName, status = "fake")
                scanHistoryDao.insert(entity)

                // Flag piracy on server
                try {
                    service.flagPiracy(
                        PiracyFlagRequest(
                            barcode = barcode,
                            claimedCompany = companyName,
                            realCompany = companyResponse.company.nameOfCompany,
                            ts = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                        )
                    )
                } catch (e: Exception) { /* ignore */ }

                return@withContext VerificationResult.BarcodeNotRegistered(barcode, companyName)
            }

            // Both not found on server -> check local Room fallback as secondary
            return@withContext verifyLocally(barcode, companyName, searchCompanySlug)

        } catch (e: Exception) {
            Log.e("Repository", "Network verification failed, using local database fallback", e)
            return@withContext verifyLocally(barcode, companyName, searchCompanySlug)
        }
    }

    private suspend fun verifyLocally(barcode: String, companyName: String, searchSlug: String): VerificationResult {
        // Query local DB
        val localProduct = productDao.getProductByBarcode(barcode)
        if (localProduct != null) {
            val mfr = manufacturer.firstOrNull()
            val registeredCompany = mfr?.nameOfCompany ?: "Our Company"
            val match = slugify(registeredCompany) == searchSlug

            if (match) {
                val dto = ProductDto(
                    barcode = localProduct.barcode,
                    companyName = registeredCompany,
                    companySlug = slugify(registeredCompany),
                    mfrId = mfr?.mfrId ?: "",
                    productName = localProduct.productName,
                    category = localProduct.category,
                    size = localProduct.size,
                    price = localProduct.price,
                    mfd = localProduct.mfd,
                    expiry = localProduct.expiry,
                    registeredAt = localProduct.dateAdded
                )
                val entity = ScanHistoryEntity(barcode = barcode, company = companyName, status = "authentic")
                scanHistoryDao.insert(entity)
                return VerificationResult.Authentic(dto)
            } else {
                val entity = ScanHistoryEntity(barcode = barcode, company = companyName, status = "fake")
                scanHistoryDao.insert(entity)

                // Add piracy flag locally
                val piracyFlag = PiracyFlagEntity(
                    barcode = barcode,
                    claimedCompany = companyName,
                    realCompany = registeredCompany,
                    timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                )
                piracyFlagDao.insert(piracyFlag)

                return VerificationResult.FakeMismatch(
                    barcode = barcode,
                    claimedCompany = companyName,
                    registeredCompany = registeredCompany
                )
            }
        }

        // Check if company is registered locally
        val mfr = manufacturer.firstOrNull()
        if (mfr != null && slugify(mfr.nameOfCompany) == searchSlug) {
            val entity = ScanHistoryEntity(barcode = barcode, company = companyName, status = "fake")
            scanHistoryDao.insert(entity)

            val piracyFlag = PiracyFlagEntity(
                barcode = barcode,
                claimedCompany = companyName,
                realCompany = mfr.nameOfCompany,
                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
            )
            piracyFlagDao.insert(piracyFlag)

            return VerificationResult.BarcodeNotRegistered(barcode, companyName)
        }

        // Unknown
        val entity = ScanHistoryEntity(barcode = barcode, company = companyName, status = "unknown")
        scanHistoryDao.insert(entity)
        return VerificationResult.CompanyNotFound(barcode, companyName)
    }

    private fun slugify(str: String): String {
        return str.lowercase(Locale.getDefault())
            .replace("\\s+".toRegex(), "-")
            .replace("[^a-z0-9\\-]".toRegex(), "")
            .take(180)
    }
}

sealed class VerificationResult {
    data class Authentic(val product: ProductDto) : VerificationResult()
    data class FakeMismatch(val barcode: String, val claimedCompany: String, val registeredCompany: String) : VerificationResult()
    data class BarcodeNotRegistered(val barcode: String, val companyName: String) : VerificationResult()
    data class CompanyNotFound(val barcode: String, val companyName: String) : VerificationResult()
}
