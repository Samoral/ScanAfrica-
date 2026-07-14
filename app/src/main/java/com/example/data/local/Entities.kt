package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manufacturers")
data class ManufacturerEntity(
    @PrimaryKey val id: Long, // Unique ID generated on setup
    val nameOfCompany: String,
    val regNo: String,
    val dateIssued: String,
    val email: String,
    val country: String,
    val ceo: String,
    val ceoImage: String? = null,
    val affiliate: Boolean = false,
    val affiliateCompanyName: String? = null,
    val mfrId: String
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val barcode: String,
    val dateAdded: String,
    val productName: String,
    val size: String,
    val category: String,
    val price: String,
    val mfd: String,
    val expiry: String,
    val nutrients: String,
    val ingredients: String,
    val govAgency: String,
    val govRegNo: String,
    val packagedBy: String,
    val imageUri: String? = null,
    val wrapperThemeBg: String = "#0f1003",
    val wrapperThemeTextColor: String = "#d3e3fd"
)

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val company: String,
    val status: String, // authentic, fake, unknown
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales_list")
data class SalesListEntity(
    @PrimaryKey val barcode: String,
    val productName: String,
    val category: String,
    val size: String,
    val price: String,
    val expiry: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "piracy_flags")
data class PiracyFlagEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val claimedCompany: String,
    val realCompany: String,
    val timestamp: String,
    val isDismissed: Boolean = false
)
