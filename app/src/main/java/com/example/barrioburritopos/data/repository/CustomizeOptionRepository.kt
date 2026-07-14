package com.example.barrioburritopos.data.repository

import android.content.Context
import android.net.Uri
import com.example.barrioburritopos.data.local.dao.CustomizeOptionDao
import com.example.barrioburritopos.data.local.entity.CustomizeOptionEntity
import com.example.barrioburritopos.data.local.entity.CustomizeStepType
import com.example.barrioburritopos.domain.model.DEFAULT_CUSTOMIZE_OPTIONS
import com.example.barrioburritopos.util.CustomizeImageStorage
import kotlinx.coroutines.flow.Flow

class CustomizeOptionRepository(
    private val dao: CustomizeOptionDao,
    private val context: Context
) {
    fun getByStepType(stepType: CustomizeStepType): Flow<List<CustomizeOptionEntity>> =
        dao.getByStepType(stepType.name)

    suspend fun ensureDefaultsSeeded() {
        if (dao.countAll() == 0) {
            dao.insertAll(DEFAULT_CUSTOMIZE_OPTIONS)
        }
    }

    suspend fun addOption(
        stepType: CustomizeStepType,
        name: String,
        price: Double,
        sourceImageUri: String?
    ): Long {
        val trimmedName = name.trim()
        if (dao.getByNameAndStepType(trimmedName, stepType.name) != null) {
            return -1
        }
        val imagePath = sourceImageUri?.let { 
            if (it.startsWith("drawable://")) {
                // Store drawable resource name as-is
                it
            } else {
                // Handle file URI
                CustomizeImageStorage.saveImage(context, android.net.Uri.parse(it))
            }
        }
        return dao.insert(
            CustomizeOptionEntity(
                stepType = stepType.name,
                name = trimmedName,
                price = price,
                imageUri = imagePath,
                isDefault = false
            )
        )
    }

    suspend fun updateOption(option: CustomizeOptionEntity) {
        dao.update(option)
    }

    suspend fun deleteOption(option: CustomizeOptionEntity) {
        dao.delete(option)
    }

    suspend fun getOptionByName(name: String, stepType: CustomizeStepType): CustomizeOptionEntity? {
        return dao.getByNameAndStepType(name, stepType.name)
    }
}
