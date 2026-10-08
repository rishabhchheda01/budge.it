package com.rishabh.budgetapp

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rishabh.budgetapp.data.AppDatabase
import com.rishabh.budgetapp.data.Transaction
import com.rishabh.budgetapp.data.TransactionDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the real database queries on a phone/emulator, using a throwaway in-memory database. */
@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun add(label: String, amount: Double, timestamp: Long) =
        dao.insert(Transaction(label = label, amount = amount, timestamp = timestamp))

    @Test
    fun spentSinceIsZeroWhenThereAreNoTransactions() = runBlocking {
        assertEquals(0.0, dao.getSpentSince(0).first(), 0.001)
    }

    @Test
    fun spentSinceCountsTheStartMomentItselfButNothingBeforeIt() = runBlocking {
        add("before", 5.0, timestamp = 199)
        add("exactly at the start", 10.0, timestamp = 200)
        add("after", 20.0, timestamp = 300)
        assertEquals(30.0, dao.getSpentSince(200).first(), 0.001)
    }

    @Test
    fun getAllReturnsNewestFirst() = runBlocking {
        add("oldest", 1.0, 100)
        add("newest", 3.0, 300)
        add("middle", 2.0, 200)
        assertEquals(listOf("newest", "middle", "oldest"), dao.getAll().first().map { it.label })
    }

    @Test
    fun deleteOlderThanKeepsTheCutoffMomentAndNewer() = runBlocking {
        add("old", 1.0, 99)
        add("edge", 2.0, 100)
        add("new", 3.0, 101)
        dao.deleteOlderThan(100)
        assertEquals(listOf("new", "edge"), dao.getAll().first().map { it.label })
    }

    @Test
    fun deleteAllRemovesEverything() = runBlocking {
        add("a", 1.0, 1)
        add("b", 2.0, 2)
        dao.deleteAll()
        assertTrue(dao.getAll().first().isEmpty())
        assertEquals(0.0, dao.getSpentSince(0).first(), 0.001)
    }

    @Test
    fun updateChangesTheAmountThatIsCounted() = runBlocking {
        add("coffee", 4.0, 100)
        val saved = dao.getAll().first().single()
        dao.update(saved.copy(amount = 6.5))
        assertEquals(6.5, dao.getSpentSince(0).first(), 0.001)
    }

    @Test
    fun deleteRemovesOnlyThatTransaction() = runBlocking {
        add("keep", 1.0, 100)
        add("remove", 2.0, 200)
        dao.delete(dao.getAll().first().first { it.label == "remove" })
        assertEquals(listOf("keep"), dao.getAll().first().map { it.label })
    }

    @Test
    fun restoringADeletedTransactionBringsBackTheSameIdAndTime() = runBlocking {
        add("undo me", 7.0, 500)
        val original = dao.getAll().first().single()
        dao.delete(original)
        dao.insert(original) // this is what the "Undo" button does
        val restored = dao.getAll().first().single()
        assertEquals(original, restored)
    }

    @Test
    fun spentSinceFlowUpdatesByItselfWhenATransactionIsAdded() = runBlocking {
        assertEquals(0.0, dao.getSpentSince(0).first(), 0.001)
        add("new", 12.0, 100)
        assertEquals(12.0, dao.getSpentSince(0).first(), 0.001)
    }

    @Test
    fun manySmallAmountsAddUpWithoutDrifting() = runBlocking {
        repeat(100) { add("cent $it", 0.10, 1000L + it) }
        assertEquals(10.0, dao.getSpentSince(0).first(), 0.0001)
    }
}
