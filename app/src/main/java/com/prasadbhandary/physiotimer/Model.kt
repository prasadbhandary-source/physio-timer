package com.prasadbhandary.physiotimer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Exercise(
    val id: Long,
    val name: String,
    val reps: Int,
    val holdSec: Int,
    val restSec: Int,
    val enabled: Boolean = true,
)

data class Settings(
    val prepSec: Int = 8,
    val sound: Boolean = true,
    val voice: Boolean = true,
    val vibrate: Boolean = true,
)

fun defaultExercises(): List<Exercise> = listOf(
    Exercise(1, "Chin tucks", reps = 10, holdSec = 10, restSec = 5),
    Exercise(2, "Left head turn", reps = 5, holdSec = 10, restSec = 5),
    Exercise(3, "Left head tilt", reps = 5, holdSec = 10, restSec = 5),
    Exercise(4, "Left nose to armpit", reps = 5, holdSec = 10, restSec = 5),
)

enum class Phase { PREP, HOLD, REST }

data class Step(
    val phase: Phase,
    val exIndex: Int,
    val exercise: Exercise,
    val rep: Int,
    val durSec: Int,
)

/** Turns a list of exercises into a flat sequence of timed steps. */
fun buildSteps(list: List<Exercise>, prepSec: Int): List<Step> {
    val steps = mutableListOf<Step>()
    list.forEachIndexed { i, e ->
        steps += Step(Phase.PREP, i, e, 1, prepSec)
        for (r in 1..e.reps) {
            steps += Step(Phase.HOLD, i, e, r, e.holdSec)
            if (r < e.reps && e.restSec > 0) steps += Step(Phase.REST, i, e, r, e.restSec)
        }
    }
    return steps
}

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("physio_timer", Context.MODE_PRIVATE)

    fun loadExercises(): List<Exercise> {
        val raw = prefs.getString("exercises", null) ?: return defaultExercises()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Exercise(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    reps = o.getInt("reps"),
                    holdSec = o.getInt("holdSec"),
                    restSec = o.optInt("restSec", 5),
                    enabled = o.optBoolean("enabled", true),
                )
            }
        } catch (e: Exception) {
            defaultExercises()
        }
    }

    fun saveExercises(list: List<Exercise>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("name", e.name)
                put("reps", e.reps)
                put("holdSec", e.holdSec)
                put("restSec", e.restSec)
                put("enabled", e.enabled)
            })
        }
        prefs.edit().putString("exercises", arr.toString()).apply()
    }

    fun loadSettings(): Settings = Settings(
        prepSec = prefs.getInt("prepSec", 8),
        sound = prefs.getBoolean("sound", true),
        voice = prefs.getBoolean("voice", true),
        vibrate = prefs.getBoolean("vibrate", true),
    )

    fun saveSettings(s: Settings) {
        prefs.edit()
            .putInt("prepSec", s.prepSec)
            .putBoolean("sound", s.sound)
            .putBoolean("voice", s.voice)
            .putBoolean("vibrate", s.vibrate)
            .apply()
    }

    fun loadHistory(): List<Long> {
        val raw = prefs.getString("history", null) ?: return emptyList()
        return try {
            val a = JSONArray(raw)
            (0 until a.length()).map { a.getLong(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addHistory(time: Long) {
        val list = (loadHistory() + time).takeLast(1000)
        prefs.edit().putString("history", JSONArray(list).toString()).apply()
    }
}
