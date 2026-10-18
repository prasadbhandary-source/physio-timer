package com.prasadbhandary.physiotimer

import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale

private val PrepColor = Color(0xFFFB8C00)
private val HoldColor = Color(0xFF43A047)
private val RestColor = Color(0xFF1E88E5)

@Composable
fun PhysioTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private data class Run(
    val exercises: List<Exercise>,
    val isSession: Boolean,
    val token: Long = System.nanoTime(),
)

@Composable
fun PhysioApp(store: Store, cues: Cues) {
    var exercises by remember { mutableStateOf(store.loadExercises()) }
    var settings by remember { mutableStateOf(store.loadSettings()) }
    var history by remember { mutableStateOf(store.loadHistory()) }
    var run by remember { mutableStateOf<Run?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    SideEffect { cues.settings = settings }

    fun updateExercises(list: List<Exercise>) {
        exercises = list
        store.saveExercises(list)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val current = run
        if (current != null) {
            key(current.token) {
                TimerScreen(
                    exercises = current.exercises,
                    prepSec = settings.prepSec,
                    cues = cues,
                    onCompleted = {
                        if (current.isSession) {
                            store.addHistory(System.currentTimeMillis())
                            history = store.loadHistory()
                        }
                    },
                    onExit = {
                        cues.silence()
                        run = null
                    },
                )
            }
        } else {
            HomeScreen(
                exercises = exercises,
                prepSec = settings.prepSec,
                history = history,
                onStartSession = {
                    val list = exercises.filter { it.enabled }
                    if (list.isNotEmpty()) run = Run(list, isSession = true)
                },
                onStartOne = { run = Run(listOf(it), isSession = false) },
                onChange = { updateExercises(it) },
                onOpenSettings = { showSettings = true },
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onDismiss = { showSettings = false },
            onChange = {
                settings = it
                store.saveSettings(it)
            },
            onResetExercises = { updateExercises(defaultExercises()) },
        )
    }
}

// ---------------------------------------------------------------- Home

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    exercises: List<Exercise>,
    prepSec: Int,
    history: List<Long>,
    onStartSession: () -> Unit,
    onStartOne: (Exercise) -> Unit,
    onChange: (List<Exercise>) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var editing by remember { mutableStateOf<Exercise?>(null) }
    var adding by remember { mutableStateOf(false) }
    val enabled = exercises.filter { it.enabled }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Physio Timer") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add exercise") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SessionCard(enabled, prepSec, history, onStartSession) }
            item {
                Column(Modifier.padding(top = 8.dp)) {
                    Text("Exercises", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tick to include in the session · tap a name to edit · ▶ to do just that one",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (exercises.isEmpty()) {
                item { Text("No exercises yet. Tap “Add exercise”.") }
            }
            itemsIndexed(exercises, key = { _, e -> e.id }) { i, e ->
                ExerciseCard(
                    e = e,
                    canUp = i > 0,
                    canDown = i < exercises.lastIndex,
                    onToggle = { checked ->
                        onChange(exercises.map { if (it.id == e.id) it.copy(enabled = checked) else it })
                    },
                    onEdit = { editing = e },
                    onMove = { dir ->
                        val list = exercises.toMutableList()
                        Collections.swap(list, i, i + dir)
                        onChange(list)
                    },
                    onPlay = { onStartOne(e) },
                )
            }
        }
    }

    if (adding) {
        ExerciseDialog(
            initial = null,
            onDismiss = { adding = false },
            onSave = {
                onChange(exercises + it)
                adding = false
            },
            onDelete = null,
        )
    }
    editing?.let { ex ->
        key(ex.id) {
            ExerciseDialog(
                initial = ex,
                onDismiss = { editing = null },
                onSave = { updated ->
                    onChange(exercises.map { if (it.id == updated.id) updated else it })
                    editing = null
                },
                onDelete = {
                    onChange(exercises.filterNot { it.id == ex.id })
                    editing = null
                },
            )
        }
    }
}

@Composable
private fun SessionCard(
    enabled: List<Exercise>,
    prepSec: Int,
    history: List<Long>,
    onStart: () -> Unit,
) {
    val totalSec = buildSteps(enabled, prepSec).sumOf { it.durSec }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Today's session",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (enabled.isEmpty()) "No exercises selected"
                else "${enabled.size} exercise${if (enabled.size == 1) "" else "s"} · about ${formatDuration(totalSec)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onStart,
                enabled = enabled.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start session", style = MaterialTheme.typography.titleMedium)
            }
            val now = System.currentTimeMillis()
            val today = history.count { isSameDay(it, now) }
            val week = history.count { now - it < 7L * 24 * 3600 * 1000 }
            Text(
                if (history.isEmpty()) "No sessions logged yet"
                else "Sessions today: $today · last 7 days: $week",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            history.maxOrNull()?.let {
                Text(
                    "Last session: ${formatDate(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    e: Exercise,
    canUp: Boolean,
    canDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit,
    onPlay: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = e.enabled, onCheckedChange = onToggle)
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onEdit)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
            ) {
                Text(e.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    describe(e),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                IconButton(onClick = { onMove(-1) }, enabled = canUp, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(onClick = { onMove(1) }, enabled = canDown, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                }
            }
            FilledTonalIconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start ${e.name}")
            }
        }
    }
}

// ---------------------------------------------------------------- Dialogs

@Composable
private fun ExerciseDialog(
    initial: Exercise?,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var reps by remember { mutableIntStateOf(initial?.reps ?: 10) }
    var hold by remember { mutableIntStateOf(initial?.holdSec ?: 10) }
    var rest by remember { mutableIntStateOf(initial?.restSec ?: 5) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add exercise" else "Edit exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Right head turn)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Stepper("Reps", reps, 1..50) { reps = it }
                Stepper("Hold (sec)", hold, 1..300, bigSteps = true) { hold = it }
                Stepper("Rest between reps (sec)", rest, 0..120) { rest = it }
                if (onDelete != null) {
                    TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                        Text(
                            if (confirmDelete) "Tap again to delete" else "Delete exercise",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        Exercise(
                            id = initial?.id ?: System.currentTimeMillis(),
                            name = name.trim(),
                            reps = reps,
                            holdSec = hold,
                            restSec = rest,
                            enabled = initial?.enabled ?: true,
                        )
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SettingsDialog(
    settings: Settings,
    onDismiss: () -> Unit,
    onChange: (Settings) -> Unit,
    onResetExercises: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper("Get-ready time before each exercise (sec)", settings.prepSec, 3..30) {
                    onChange(settings.copy(prepSec = it))
                }
                SwitchRow("Beeps", settings.sound) { onChange(settings.copy(sound = it)) }
                SwitchRow("Voice prompts", settings.voice) { onChange(settings.copy(voice = it)) }
                SwitchRow("Vibration", settings.vibrate) { onChange(settings.copy(vibrate = it)) }
                TextButton(onClick = {
                    if (confirmReset) {
                        onResetExercises()
                        confirmReset = false
                    } else {
                        confirmReset = true
                    }
                }) {
                    Text(if (confirmReset) "Tap again to reset" else "Reset exercises to the original 4")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    range: IntRange,
    bigSteps: Boolean = false,
    onChange: (Int) -> Unit,
) {
    val up = if (bigSteps && value >= 30) 5 else 1
    val down = if (bigSteps && value > 30) 5 else 1
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        FilledTonalIconButton(
            onClick = { onChange((value - down).coerceIn(range)) },
            enabled = value > range.first,
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text(
            "$value",
            Modifier.widthIn(min = 44.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )
        FilledTonalIconButton(
            onClick = { onChange((value + up).coerceIn(range)) },
            enabled = value < range.last,
        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ---------------------------------------------------------------- Timer

@Composable
fun TimerScreen(
    exercises: List<Exercise>,
    prepSec: Int,
    cues: Cues,
    onCompleted: () -> Unit,
    onExit: () -> Unit,
) {
    val steps = remember { buildSteps(exercises, prepSec) }
    val starts = remember { steps.runningFold(0L) { acc, s -> acc + s.durSec * 1000L } }
    val totalMs = starts.last().coerceAtLeast(1L)

    var index by remember { mutableIntStateOf(0) }
    var remainingMs by remember { mutableLongStateOf(steps.firstOrNull()?.let { it.durSec * 1000L } ?: 0L) }
    var paused by remember { mutableStateOf(false) }
    var announced by remember { mutableIntStateOf(-1) }
    val done = index >= steps.size

    // Keep the screen awake while the timer is open.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(onBack = onExit)

    fun jumpTo(next: Int) {
        remainingMs = steps.getOrNull(next)?.let { it.durSec * 1000L } ?: 0L
        index = next
    }

    LaunchedEffect(index, paused) {
        if (paused || index >= steps.size) return@LaunchedEffect
        val step = steps[index]
        if (announced != index) {
            announced = index
            cueStart(step, cues)
        }
        val end = SystemClock.elapsedRealtime() + remainingMs
        var lastWhole = ((remainingMs + 999) / 1000).toInt()
        while (true) {
            val left = end - SystemClock.elapsedRealtime()
            if (left <= 0) break
            remainingMs = left
            val whole = ((left + 999) / 1000).toInt()
            if (whole != lastWhole) {
                lastWhole = whole
                if (whole <= 3 && step.phase != Phase.REST) cues.tick()
            }
            delay(minOf(left, 50L))
        }
        cueEnd(step, steps.getOrNull(index + 1), cues)
        jumpTo(index + 1)
    }

    LaunchedEffect(done) {
        if (done) onCompleted()
    }

    if (done) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("✓", fontSize = 96.sp, color = HoldColor, fontWeight = FontWeight.Bold)
            Text("All done!", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "${exercises.size} exercise${if (exercises.size == 1) "" else "s"} · ${formatDuration((totalMs / 1000).toInt())}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Back to exercises", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    val step = steps[index]
    val ex = step.exercise
    val color = when (step.phase) {
        Phase.PREP -> PrepColor
        Phase.HOLD -> HoldColor
        Phase.REST -> RestColor
    }
    val label = when (step.phase) {
        Phase.PREP -> "GET READY"
        Phase.HOLD -> "HOLD"
        Phase.REST -> "RELAX"
    }
    val repText = when (step.phase) {
        Phase.PREP -> describe(ex)
        Phase.HOLD -> "Rep ${step.rep} of ${ex.reps}"
        Phase.REST -> "Next: rep ${step.rep + 1} of ${ex.reps}"
    }
    val durMs = step.durSec * 1000L
    val frac = if (durMs > 0) (remainingMs.toFloat() / durMs).coerceIn(0f, 1f) else 0f
    val secs = ((remainingMs + 999) / 1000).toInt()
    val overall = ((starts[index] + (durMs - remainingMs)).toFloat() / totalMs).coerceIn(0f, 1f)

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Exercise ${step.exIndex + 1} of ${exercises.size}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onExit) {
                Icon(Icons.Default.Close, contentDescription = "Stop")
            }
        }
        LinearProgressIndicator(progress = { overall }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        Text(
            ex.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            repText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { frac },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 18.dp,
                trackColor = color.copy(alpha = 0.18f),
                strokeCap = StrokeCap.Round,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleLarge,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Text("$secs", fontSize = 104.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (paused) "Paused" else "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (!paused) cues.silence()
                paused = !paused
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Text(if (paused) "Resume" else "Pause", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { jumpTo(index + 1) },
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text("Skip step") }
            OutlinedButton(
                onClick = {
                    val next = steps.indexOfFirst { it.exIndex > step.exIndex }
                    jumpTo(if (next == -1) steps.size else next)
                },
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text("Next exercise") }
        }
    }
}

private fun cueStart(step: Step, cues: Cues) {
    val e = step.exercise
    when (step.phase) {
        Phase.PREP -> cues.say(
            "${e.name}. ${e.reps} ${if (e.reps == 1) "rep" else "reps"}, ${e.holdSec} seconds each. Get ready.",
            flush = false,
        )
        Phase.HOLD -> {
            cues.startCue()
            cues.say(if (e.reps > 1) "Rep ${step.rep}. Hold." else "Hold.")
        }
        Phase.REST -> Unit
    }
}

private fun cueEnd(step: Step, next: Step?, cues: Cues) {
    if (step.phase != Phase.HOLD) return
    cues.endCue()
    when {
        next == null -> cues.say("All done. Great work!")
        next.phase == Phase.REST -> cues.say("Relax.")
        next.phase == Phase.PREP -> cues.say("${step.exercise.name} done.")
        else -> Unit
    }
}

// ---------------------------------------------------------------- Helpers

private fun describe(e: Exercise): String = buildString {
    append("${e.reps} rep${if (e.reps == 1) "" else "s"} × ${e.holdSec}s hold")
    if (e.reps > 1 && e.restSec > 0) append(" · ${e.restSec}s rest")
}

private fun formatDuration(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return when {
        m == 0 -> "${s}s"
        s == 0 -> "$m min"
        else -> "$m min ${s}s"
    }
}

private fun formatDate(t: Long): String =
    SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault()).format(Date(t))

private fun isSameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}
