package com.nmanojlovski.briefing

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale
import kotlin.math.roundToInt

// ===================== Budget: data =====================
// Every deposit or withdrawal is one row in a small database on the phone (Room).
// Amounts are whole denars (MKD), stored as Long, so sums are always exact.

enum class Category(val label: String, val color: Color, val isIncome: Boolean = false) {
    // Money in
    SALARY("Salary", Color(0xFF2E9E5B), isIncome = true),
    GIFT("Gift", Color(0xFF6BBF59), isIncome = true),
    OTHER_INCOME("Other", Color(0xFF8E9AAF), isIncome = true),

    // Money out
    FOOD("Food", Color(0xFFFB8500)),
    RENT("Rent", Color(0xFF3E7CB1)),
    TRANSPORT("Transport", Color(0xFF219EBC)),
    BILLS("Bills", Color(0xFFE63946)),
    SHOPPING("Shopping", Color(0xFF9B5DE5)),
    FUN("Fun", Color(0xFFFFB703)),
    HEALTH("Health", Color(0xFF2A9D8F)),
    OTHER("Other", Color(0xFF8E9AAF)),
}

// One row of the "entries" table. Room creates the table from this class.
@Entity(tableName = "entries")
data class BudgetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // 0 = new; Room picks the real number
    val amount: Long,         // in denars, always positive
    val isDeposit: Boolean,   // true = money in, false = money spent
    val category: Category,   // Room saves the enum by its name, e.g. "FOOD"
    val note: String,
    val date: String          // "2026-11-27": as text it still sorts in date order
)

// The questions we ask the database. Room writes the code behind them.
@Dao
interface BudgetDao {
    // A Flow sends a new list every time the table changes, so the screen updates by itself.
    @Query("SELECT * FROM entries ORDER BY date DESC, id DESC")
    fun all(): Flow<List<BudgetEntry>>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun get(id: Long): BudgetEntry?

    // Upsert = insert a new row, or update the row with the same id.
    @Upsert
    suspend fun save(entry: BudgetEntry)

    @Delete
    suspend fun delete(entry: BudgetEntry)
}

// The database file ("budget.db" in the app's private storage).
// version = 1: if the table ever changes, this number goes up together with a migration.
@Database(entities = [BudgetEntry::class], version = 1, exportSchema = false)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun dao(): BudgetDao

    companion object {
        // Opening a database is slow, so the whole app shares one.
        @Volatile private var instance: BudgetDatabase? = null

        fun get(context: Context): BudgetDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, BudgetDatabase::class.java, "budget.db")
                .build()
                .also { instance = it }
        }
    }
}

class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = BudgetDatabase.get(app).dao()

    // All entries, newest first. stateIn turns the database Flow into a value screens can read.
    val entries: StateFlow<List<BudgetEntry>> =
        dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // The month the budget screen shows; the ‹ › arrows change it.
    var month by mutableStateOf(YearMonth.now())
        private set

    fun previousMonth() {
        month = month.minusMonths(1)
    }

    fun nextMonth() {
        if (month < YearMonth.now()) month = month.plusMonths(1)
    }

    suspend fun load(id: Long): BudgetEntry? = dao.get(id)

    fun save(entry: BudgetEntry) {
        viewModelScope.launch { dao.save(entry) }
    }

    fun delete(entry: BudgetEntry) {
        viewModelScope.launch { dao.delete(entry) }
    }
}

// ----- Totals -----
// Plain functions without Android, so they can be unit-tested (BudgetSummaryTest).

data class MonthSummary(
    val month: YearMonth,
    val income: Long,
    val spent: Long,
    val byCategory: List<Pair<Category, Long>> // spending per category, biggest first
) {
    val balance: Long get() = income - spent
}

fun summarize(entries: List<BudgetEntry>, month: YearMonth): MonthSummary {
    val inMonth = entries.filter { it.date.startsWith(month.toString()) } // "2026-11"
    val spending = inMonth.filter { !it.isDeposit }
    return MonthSummary(
        month = month,
        income = inMonth.filter { it.isDeposit }.sumOf { it.amount },
        spent = spending.sumOf { it.amount },
        byCategory = spending
            .groupBy { it.category }
            .map { (category, list) -> category to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
    )
}

// The last `count` months up to and including `end`, oldest first (for the bar chart).
fun lastMonths(entries: List<BudgetEntry>, end: YearMonth, count: Int = 6): List<MonthSummary> =
    (count - 1 downTo 0).map { summarize(entries, end.minusMonths(it.toLong())) }

// What part of `total` is `part`, in whole percent.
fun percentOf(part: Long, total: Long): Int =
    if (total == 0L) 0 else (part * 100.0 / total).roundToInt()

private val MKD_FORMAT = NumberFormat.getIntegerInstance(Locale.ENGLISH) // 12450 -> "12,450"

fun formatMkd(amount: Long): String = MKD_FORMAT.format(amount) + " MKD"
