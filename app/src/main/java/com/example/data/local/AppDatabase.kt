package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ManufacturerEntity::class,
        ProductEntity::class,
        ScanHistoryEntity::class,
        SalesListEntity::class,
        PiracyFlagEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun manufacturerDao(): ManufacturerDao
    abstract fun productDao(): ProductDao
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun salesListDao(): SalesListDao
    abstract fun piracyFlagDao(): PiracyFlagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scannaija_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
