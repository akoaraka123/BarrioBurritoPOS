package com.example.barrioburritopos.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.barrioburritopos.data.local.dao.OrderDao
import com.example.barrioburritopos.data.local.dao.ProductDao
import com.example.barrioburritopos.data.local.entity.OrderEntity
import com.example.barrioburritopos.data.local.entity.OrderItemEntity
import com.example.barrioburritopos.data.local.entity.ProductEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ProductEntity::class, OrderEntity::class, OrderItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BarrioBurritoDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: BarrioBurritoDatabase? = null

        fun getDatabase(context: Context): BarrioBurritoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BarrioBurritoDatabase::class.java,
                    "barrio_burrito_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed initial data
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    seedProducts(database.productDao())
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
