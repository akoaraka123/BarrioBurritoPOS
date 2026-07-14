package com.example.barrioburritopos.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.barrioburritopos.data.local.dao.CustomizeOptionDao
import com.example.barrioburritopos.data.local.dao.OrderDao
import com.example.barrioburritopos.data.local.dao.ProductDao
import com.example.barrioburritopos.data.local.entity.CustomizeOptionEntity
import com.example.barrioburritopos.data.local.entity.OrderEntity
import com.example.barrioburritopos.data.local.entity.OrderItemEntity
import com.example.barrioburritopos.data.local.entity.ProductEntity
import com.example.barrioburritopos.domain.model.DEFAULT_CUSTOMIZE_OPTIONS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        CustomizeOptionEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class BarrioBurritoDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun customizeOptionDao(): CustomizeOptionDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'CASH'")
                db.execSQL("ALTER TABLE orders ADD COLUMN amountReceived REAL")
                db.execSQL("ALTER TABLE orders ADD COLUMN changeAmount REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE order_items ADD COLUMN itemDetails TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS customize_options (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        stepType TEXT NOT NULL,
                        name TEXT NOT NULL,
                        price REAL NOT NULL,
                        imageUri TEXT,
                        isActive INTEGER NOT NULL,
                        isDefault INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                seedCustomizeOptions(db)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    DELETE FROM customize_options
                    WHERE EXISTS (
                        SELECT 1
                        FROM customize_options AS keep
                        WHERE keep.stepType = customize_options.stepType
                            AND keep.name = customize_options.name
                            AND (
                                keep.isActive > customize_options.isActive
                                OR (
                                    keep.isActive = customize_options.isActive
                                    AND keep.isDefault > customize_options.isDefault
                                )
                                OR (
                                    keep.isActive = customize_options.isActive
                                    AND keep.isDefault = customize_options.isDefault
                                    AND keep.id < customize_options.id
                                )
                            )
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_customize_options_stepType_name
                    ON customize_options(stepType, name)
                    """.trimIndent()
                )
            }
        }

        private fun seedCustomizeOptions(db: SupportSQLiteDatabase) {
            DEFAULT_CUSTOMIZE_OPTIONS.forEach { option ->
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO customize_options (stepType, name, price, imageUri, isActive, isDefault, createdAt)
                    VALUES (?, ?, ?, ?, 1, ?, ?)
                    """.trimIndent(),
                    arrayOf(
                        option.stepType,
                        option.name,
                        option.price,
                        option.imageUri,
                        if (option.isDefault) 1 else 0,
                        option.createdAt
                    )
                )
            }
        }

        @Volatile
        private var INSTANCE: BarrioBurritoDatabase? = null

        fun getDatabase(context: Context): BarrioBurritoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BarrioBurritoDatabase::class.java,
                    "barrio_burrito_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    seedProducts(database.productDao())
                                    database.customizeOptionDao().insertAll(DEFAULT_CUSTOMIZE_OPTIONS)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedProducts(dao: ProductDao) {
            val sampleProducts = listOf(
                ProductEntity(name = "Fries", price = 50.0, stockQuantity = 100, category = "Snacks"),
                ProductEntity(name = "Juice", price = 25.0, stockQuantity = 50, category = "Drinks"),
                ProductEntity(name = "Burrito", price = 89.0, stockQuantity = 30, category = "Mains"),
                ProductEntity(name = "Nachos", price = 65.0, stockQuantity = 40, category = "Snacks"),
                ProductEntity(name = "Taco", price = 45.0, stockQuantity = 60, category = "Mains"),
                ProductEntity(name = "Iced Tea", price = 30.0, stockQuantity = 50, category = "Drinks")
            )
            dao.insertAll(sampleProducts)
        }
    }
}
