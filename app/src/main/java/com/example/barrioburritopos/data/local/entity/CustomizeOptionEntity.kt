package com.example.barrioburritopos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customize_options")
data class CustomizeOptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stepType: String,
    val name: String,
    val price: Double = 0.0,
    val imageUri: String? = null,
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class CustomizeStepType(val displayName: String) {
    RICE("RICE"),
    MAIN("MAIN"),
    BASE("BASE"),
    TOPPING("TOPPING"),
    SAUCE("SAUCE"),
    ADDON("ADDON");

    companion object {
        fun fromOrdinal(index: Int): CustomizeStepType = entries.getOrElse(index) { RICE }
    }
}
