package com.prasadbhandary.physiotimer

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Beeps, spoken prompts and vibration, so you don't have to watch the screen. */
class Cues(context: Context) : TextToSpeech.OnInitListener {
    var settings = Settings()

    @Volatile
    private var ttsReady = false
    private val tts = TextToSpeech(context.applicationContext, this)

    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    } catch (e: RuntimeException) {
        null
    }

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US)
            }
            ttsReady = true
        }
    }

    fun say(text: String, flush: Boolean = true) {
        if (!settings.voice || !ttsReady) return
        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts.speak(text, mode, null, "cue-${System.nanoTime()}")
    }

    /** Short tick for the last 3 seconds of a countdown. */
    fun tick() {
        if (settings.sound) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    }

    /** Hold starts. */
    fun startCue() {
        if (settings.sound) tone?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
        vibrate(longArrayOf(0, 300))
    }

    /** Hold ends. */
    fun endCue() {
        if (settings.sound) tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
        vibrate(longArrayOf(0, 150, 100, 150))
    }

    private fun vibrate(pattern: LongArray) {
        if (settings.vibrate) vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    fun silence() {
        if (ttsReady) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
        tone?.release()
    }
}
