package com.nmanojlovski.briefing

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

// Monthly totals, categories and percentages for the budget screen. Right-click this file → Run.
class BudgetSummaryTest {
    private fun spent(amount: Long, category: Category, date: String) =
        BudgetEntry(amount = amount, isDeposit = false, category = category, note = "", date = date)

    private fun deposit(amount: Long, date: String) =
        BudgetEntry(amount = amount, isDeposit = true, category = Category.SALARY, note = "", date = date)

    private val entries = listOf(
        deposit(45_000, "2026-11-01"),
        spent(1_250, Category.FOOD, "2026-11-26"),
        spent(750, Category.FOOD, "2026-11-20"),
        spent(12_000, Category.RENT, "2026-11-02"),
        spent(9_999, Category.FUN, "2026-10-31"),   // October: not part of November
        deposit(40_000, "2026-10-01")
    )

    @Test
    fun totalsOnlyCountTheChosenMonth() {
        val november = summarize(entries, YearMonth.of(2026, 11))
        assertEquals(45_000L, november.income)
        assertEquals(14_000L, november.spent)
        assertEquals(31_000L, november.balance)
    }

    @Test
    fun categoriesAreAddedUpAndSortedBiggestFirst() {
        val november = summarize(entries, YearMonth.of(2026, 11))
        assertEquals(listOf(Category.RENT to 12_000L, Category.FOOD to 2_000L), november.byCategory)
    }

    @Test
    fun negativeBalanceWhenSpendingMoreThanIncome() {
        val october = summarize(entries, YearMonth.of(2026, 10))
        assertEquals(30_001L, october.balance)
        assertEquals(-9_999L, summarize(listOf(spent(9_999, Category.FUN, "2026-10-31")), YearMonth.of(2026, 10)).balance)
    }

    @Test
    fun lastSixMonthsAreOldestFirstAndCrossTheYear() {
        val months = lastMonths(entries, YearMonth.of(2027, 2))
        assertEquals(
            listOf(
                YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11),
                YearMonth.of(2026, 12), YearMonth.of(2027, 1), YearMonth.of(2027, 2)
            ),
            months.map { it.month }
        )
        assertEquals(14_000L, months[2].spent) // November
    }

    @Test
    fun percentages() {
        assertEquals(86, percentOf(12_000, 14_000))
        assertEquals(14, percentOf(2_000, 14_000))
        assertEquals(0, percentOf(5, 0)) // nothing spent: no division by zero
    }

    @Test
    fun amountsAreShownWithThousandSeparators() {
        assertEquals("12,450 MKD", formatMkd(12_450))
        assertEquals("-9,999 MKD", formatMkd(-9_999))
    }
}
