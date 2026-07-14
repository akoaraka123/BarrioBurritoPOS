package com.example.barrioburritopos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.barrioburritopos.data.local.entity.CustomizeOptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomizeOptionDao {

    @Query("SELECT * FROM customize_options WHERE stepType = :stepType AND isActive = 1 ORDER BY isDefault DESC, name ASC")
    fun getByStepType(stepType: String): Flow<List<CustomizeOptionEntity>>

    @Query("SELECT COUNT(*) FROM customize_options")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(option: CustomizeOptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(options: List<CustomizeOptionEntity>)

    @Update
    suspend fun update(option: CustomizeOptionEntity)

    @Delete
    suspend fun delete(option: CustomizeOptionEntity)

    @Query("SELECT * FROM customize_options WHERE name = :name AND stepType = :stepType LIMIT 1")
    suspend fun getByNameAndStepType(name: String, stepType: String): CustomizeOptionEntity?
}
