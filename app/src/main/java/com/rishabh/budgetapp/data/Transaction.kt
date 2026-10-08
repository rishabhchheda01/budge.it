package com.rishabh.budgetapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// The index on timestamp keeps "spent since X" and "delete older than X" fast.
@Entity(tableName = "transactions", indices = [Index("timestamp")])
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)
