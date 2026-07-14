package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface ManufacturerDao {
    @Query("SELECT * FROM manufacturers LIMIT 1")
    fun getManufacturer(): Flow<ManufacturerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(manufacturer: ManufacturerEntity)

    @Update
    suspend fun update(manufacturer: ManufacturerEntity)

    @Query("DELETE FROM manufacturers")
    suspend fun clear()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY dateAdded DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("DELETE FROM products WHERE barcode = :barcode")
    suspend fun deleteByBarcode(barcode: String)

    @Query("DELETE FROM products")
    suspend fun clearAll()
}

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentHistory(): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: ScanHistoryEntity)

    @Query("DELETE FROM scan_history")
    suspend fun clear()
}

@Dao
interface SalesListDao {
    @Query("SELECT * FROM sales_list ORDER BY addedAt DESC")
    fun getAllSalesItems(): Flow<List<SalesListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SalesListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SalesListEntity>)

    @Query("DELETE FROM sales_list WHERE barcode = :barcode")
    suspend fun deleteByBarcode(barcode: String)

    @Query("DELETE FROM sales_list")
    suspend fun clear()
}

@Dao
interface PiracyFlagDao {
    @Query("SELECT * FROM piracy_flags WHERE isDismissed = 0 ORDER BY timestamp DESC")
    fun getActiveFlags(): Flow<List<PiracyFlagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flag: PiracyFlagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(flags: List<PiracyFlagEntity>)

    @Query("UPDATE piracy_flags SET isDismissed = 1 WHERE id = :id")
    suspend fun dismissFlag(id: Int)

    @Query("UPDATE piracy_flags SET isDismissed = 1")
    suspend fun dismissAll()

    @Query("DELETE FROM piracy_flags")
    suspend fun clearAll()
}
