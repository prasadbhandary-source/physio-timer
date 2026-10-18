package com.prasadbhandary.physiotimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    private lateinit var cues: Cues

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = Store(this)
        cues = Cues(this)
        setContent {
            PhysioTheme {
                PhysioApp(store, cues)
            }
        }
    }

    override fun onDestroy() {
        cues.shutdown()
        super.onDestroy()
    }
}
