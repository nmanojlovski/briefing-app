package com.nmanojlovski.briefing

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nmanojlovski.briefing.ui.theme.Navy
import com.nmanojlovski.briefing.ui.theme.NavyLight
import com.nmanojlovski.briefing.ui.theme.Sun
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val MAX_NAMES = 20

// Slice colours (background, text), in the app's navy-and-sun style.
private val SLICE_COLORS = listOf(
    Navy to Color.White,
    Sun to Navy,
    NavyLight to Color.White,
    Color(0xFFFB8500) to Color.White, // orange
    Color(0xFF8ECAE6) to Navy,        // light blue
    Color(0xFF219EBC) to Color.White, // teal
)

// ===================== Screen 4: spin the wheel =====================

// Type a few names, press SPIN, and the wheel picks one at random.
// The names are saved on the phone, so they're still there next time.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // mutableStateListOf: a list that redraws the screen whenever a name is added or removed.
    val names = remember { mutableStateListOf(*loadNames(context).toTypedArray()) }
    var newName by remember { mutableStateOf("") }
    // How far the wheel has turned, in degrees. Animatable can animate it smoothly to a new value.
    val angle = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<String?>(null) }

    fun addName() {
        val name = newName.trim()
        if (name.isEmpty() || names.size >= MAX_NAMES) return
        names.add(name)
        saveNames(context, names)
        newName = ""
    }

    fun removeName(index: Int) {
        names.removeAt(index)
        saveNames(context, names)
    }

    fun spin() {
        // launch: the animation runs for a few seconds without freezing the screen.
        scope.launch {
            spinning = true
            // 5 to 7 full turns plus a random extra bit. Where it stops decides the winner.
            val target = angle.value + 360f * Random.nextInt(5, 8) + Random.nextFloat() * 360f
            // Starts fast and slows down gently at the end, like a real wheel.
            angle.animateTo(target, tween(durationMillis = 4500, easing = CubicBezierEasing(0.1f, 0.6f, 0.15f, 1f)))
            winner = names[winnerIndex(angle.value, names.size)]
            haptics.performHapticFeedback(HapticFeedbackType.LongPress) // a short buzz
            spinning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spin the wheel") },
                navigationIcon = { BackButton(onBack) },
                colors = brandTopBarColors()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding() // keep the text field above the keyboard
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Wheel(names, angle = { angle.value }, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { spin() },
                enabled = names.size >= 2 && !spinning,
                colors = ButtonDefaults.buttonColors(containerColor = Sun, contentColor = Navy),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        spinning -> "Spinning…"
                        names.size < 2 -> "Add at least 2 names"
                        else -> "SPIN"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Add a name") },
                    singleLine = true,
                    enabled = !spinning && names.size < MAX_NAMES,
                    // The keyboard's ✓ key adds the name and stays open for the next one.
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { addName() }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { addName() },
                    enabled = !spinning && newName.isNotBlank() && names.size < MAX_NAMES
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
            Text(
                "${names.size} of $MAX_NAMES names",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp, bottom = 8.dp)
            )

            // One row per name, with the colour of its slice.
            names.forEachIndexed { index, name ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(sliceColors(index, names.size).first)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { removeName(index) }, enabled = !spinning) {
                        Icon(Icons.Default.Close, contentDescription = "Remove $name")
                    }
                }
            }
        }
    }

    // The result, in a dialog on top of the screen.
    winner?.let { name ->
        AlertDialog(
            onDismissRequest = { winner = null },
            title = { Text("The wheel picked") },
            text = { Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
            confirmButton = { TextButton(onClick = { winner = null }) { Text("OK") } },
            // Handy for picking an order: take the winner off and spin again.
            dismissButton = {
                TextButton(onClick = {
                    val index = names.indexOf(name)
                    if (index >= 0) removeName(index)
                    winner = null
                }) { Text("Remove from wheel") }
            }
        )
    }
}

// The wheel itself, with a fixed pointer at the top.
// `angle` is a function instead of a Float, so only the rotation updates during a spin,
// not the whole screen (the slices are drawn once and then just turned).
@Composable
private fun Wheel(names: List<String>, angle: () -> Float, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp) // room for the pointer to overlap the edge
                .graphicsLayer { rotationZ = angle() }
        ) {
            val radius = size.minDimension / 2
            if (names.isEmpty()) {
                drawCircle(emptyColor, radius)
                return@Canvas
            }
            val sweep = 360f / names.size
            names.forEachIndexed { i, name ->
                val (background, textColor) = sliceColors(i, names.size)
                // Angles start at "3 o'clock" and go clockwise; -90 is the top, where slice 0 starts.
                val start = -90f + i * sweep
                drawArc(background, startAngle = start, sweepAngle = sweep, useCenter = true)

                val label = textMeasurer.measure(
                    name,
                    style = TextStyle(color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    constraints = Constraints(maxWidth = (radius * 0.7f).toInt())
                )
                // Turn the drawing so "to the right" points through the middle of this slice,
                // then write the name there, ending just before the rim.
                rotate(degrees = start + sweep / 2, pivot = center) {
                    drawText(
                        label,
                        topLeft = Offset(
                            center.x + radius - 16.dp.toPx() - label.size.width,
                            center.y - label.size.height / 2f
                        )
                    )
                }
            }
            drawCircle(Color.White, radius, style = Stroke(width = 4.dp.toPx()))
            drawCircle(Navy, radius * 0.14f) // the hub in the middle
            drawCircle(Sun, radius * 0.06f)
        }

        // The pointer: a sun-coloured triangle at the top that doesn't turn.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val half = 14.dp.toPx()
            val pointer = Path().apply {
                moveTo(center.x - half, 0f)
                lineTo(center.x + half, 0f)
                lineTo(center.x, half * 2.2f)
                close()
            }
            drawPath(pointer, Sun)
            drawPath(pointer, Navy, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

// Which slice is under the pointer after the wheel has turned `angle` degrees clockwise.
// Example with 4 names: turning 45° brings the end of the last slice (index 3) under the pointer.
fun winnerIndex(angle: Float, count: Int): Int {
    val sweep = 360f / count
    // Turning the wheel clockwise moves earlier slices away from the pointer,
    // so the point under it is `angle` degrees back from the top (0..360).
    val underPointer = ((-angle % 360f) + 360f) % 360f
    return (underPointer / sweep).toInt().coerceIn(0, count - 1)
}

// Neighbouring slices get different colours, also the last one next to the first.
private fun sliceColors(index: Int, count: Int): Pair<Color, Color> {
    var i = index % SLICE_COLORS.size
    if (index == count - 1 && index > 0 && i == 0) i = 2
    return SLICE_COLORS[i]
}

// ----- Saved names -----
// SharedPreferences: a small key-value store on the phone, good for a few settings or a short list.

private const val PREFS = "wheel"
private const val KEY_NAMES = "names"

private fun loadNames(context: Context): List<String> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_NAMES, null)
        ?.split("\n")
        ?.filter { it.isNotBlank() }
        ?: emptyList()

private fun saveNames(context: Context, names: List<String>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_NAMES, names.joinToString("\n")) // names can't contain a line break (single-line field)
        .apply()
}
