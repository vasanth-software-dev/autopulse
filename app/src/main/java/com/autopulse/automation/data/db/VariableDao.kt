package com.autopulse.automation.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.autopulse.automation.data.model.Variable
import kotlinx.coroutines.flow.Flow

@Dao
interface VariableDao {

    @Query("SELECT * FROM variables ORDER BY `key` ASC")
    fun getAllVariables(): Flow<List<Variable>>

    @Query("SELECT * FROM variables WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): Variable?

    @Query("SELECT value FROM variables WHERE `key` = :key LIMIT 1")
    suspend fun getValueByKey(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(variable: Variable)

    @Update
    suspend fun update(variable: Variable)

    @Delete
    suspend fun delete(variable: Variable)

    @Query("DELETE FROM variables WHERE `key` = :key")
    suspend fun deleteByKey(key: String)
}
