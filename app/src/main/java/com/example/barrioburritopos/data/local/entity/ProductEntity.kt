package com.example.barrioburritopos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Double,
    val stockQuantity: Int,
    val category: String? = null,
    val isAvailable: Boolean = true
)
