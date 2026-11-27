package com.nmanojlovski.briefing

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nmanojlovski.briefing.ui.theme.Navy
import com.nmanojlovski.briefing.ui.theme.Sun
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ===================== Screen 6: add or edit a budget entry =====================

// entryId = 0 adds a new entry; any other id edits (or deletes) that entry.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetEntryScreen(viewModel: BudgetViewModel, entryId: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    // rememberSaveable: what you typed survives a screen rotation.
    var isDeposit by rememberSaveable { mutableStateOf(false) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<Category?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var filledIn by rememberSaveable { mutableStateOf(false) }
    var existing by remember { mutableStateOf<BudgetEntry?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Editing: read the entry from the database once and fill in the form.
    LaunchedEffect(entryId) {
        if (entryId == 0L) return@LaunchedEffect
        val entry = viewModel.load(entryId) ?: return@LaunchedEffect
        existing = entry
        if (!filledIn) {
            isDeposit = entry.isDeposit
            amountText = entry.amount.toString()
            category = entry.category
            note = entry.note
            date = LocalDate.parse(entry.date)
            filledIn = true
        }
    }

    val amount = amountText.toLongOrNull() ?: 0L
    val chosen = category

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == 0L) "New entry" else "Edit entry") },
                navigationIcon = { BackButton(onDone) },
                colors = brandTopBarColors()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Spent or deposit. Switching clears a category that belongs to the other kind.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeButton("− Spent", selected = !isDeposit, color = SpentColor, modifier = Modifier.weight(1f)) {
                    isDeposit = false
                    if (category?.isIncome == true) category = null
                }
                TypeButton("+ Deposit", selected = isDeposit, color = IncomeColor, modifier = Modifier.weight(1f)) {
                    isDeposit = true
                    if (category?.isIncome == false) category = null
                }
            }

            OutlinedTextField(
                value = amountText,
                // Digits only, no leading zeros, at most 9 digits (under a billion denars).
                onValueChange = { typed -> amountText = typed.filter { it.isDigit() }.trimStart('0').take(9) },
                label = { Text("Amount") },
                suffix = { Text("MKD") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Category", style = MaterialTheme.typography.titleSmall)
            // FlowRow puts the chips next to each other and wraps to a new line when full.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Category.entries.filter { it.isIncome == isDeposit }.forEach { option ->
                    CategoryChip(option, selected = option == category) { category = option }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(60) },
                label = { Text("Note (optional)") },
                placeholder = { Text("e.g. Groceries at Vero") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            // Android's own calendar dialog. Future dates are greyed out.
            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day -> date = LocalDate.of(year, month + 1, day) }, // month counts from 0 here
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth
                    ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Date: ${date.format(ENTRY_DATE)}")
            }

            Button(
                onClick = {
                    if (chosen != null && amount > 0) {
                        viewModel.save(
                            BudgetEntry(
                                id = entryId,
                                amount = amount,
                                isDeposit = isDeposit,
                                category = chosen,
                                note = note.trim(),
                                date = date.toString()
                            )
                        )
                        onDone()
                    }
                },
                enabled = chosen != null && amount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Sun, contentColor = Navy),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        amount == 0L -> "Enter an amount"
                        chosen == null -> "Pick a category"
                        else -> "Save"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            if (existing != null) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete this entry", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // Deleting can't be undone, so ask first.
    val toDelete = existing
    if (confirmDelete && toDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("${toDelete.category.label}, ${formatMkd(toDelete.amount)}. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(toDelete)
                    confirmDelete = false
                    onDone()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

// The selected kind is a filled button in its colour; the other one is an outline.
@Composable
private fun TypeButton(text: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
            modifier = modifier
        ) { Text(text, fontWeight = FontWeight.Bold) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
    }
}

// A rounded label you can tap; filled with the category's colour when selected.
@Composable
private fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) category.color else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            category.label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

private val ENTRY_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH) // Friday, 27 November 2026
