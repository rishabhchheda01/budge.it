package com.rishabh.budgetapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Transaction>>

    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    // Total spent from [start] (a time in milliseconds) until now.
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE timestamp >= :start")
    fun getSpentSince(start: Long): Flow<Double>

    // "Clear all transactions" in Settings.
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    // Used to keep the database small: removes anything older than [cutoff].
    @Query("DELETE FROM transactions WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
