package com.rishabh.budgetapp.data

import android.content.Context
import com.rishabh.budgetapp.widget.BudgetWidget
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).transactionDao()

    fun getAll(): Flow<List<Transaction>> = dao.getAll()

    fun getSpentSince(start: Long): Flow<Double> = dao.getSpentSince(start)

    suspend fun add(label: String, amount: Double) {
        dao.insert(Transaction(label = label, amount = amount))
        BudgetWidget.refreshAll(context)
    }

    suspend fun update(transaction: Transaction) {
        dao.update(transaction)
        BudgetWidget.refreshAll(context)
    }

    suspend fun delete(transaction: Transaction) {
        dao.delete(transaction)
        BudgetWidget.refreshAll(context)
    }

    // Puts a deleted transaction back exactly as it was (same id and time). Used by "Undo".
    suspend fun restore(transaction: Transaction) {
        dao.insert(transaction)
        BudgetWidget.refreshAll(context)
    }

    suspend fun clearAll() {
        dao.deleteAll()
        BudgetWidget.refreshAll(context)
    }

    suspend fun deleteOlderThan(cutoff: Long) = dao.deleteOlderThan(cutoff)
}
