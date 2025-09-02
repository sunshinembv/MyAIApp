package com.example.myaiapp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.myaiapp.chat.voice.VoiceState

@Composable
fun VoiceBar(
    state: VoiceState,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
) {
    val context = LocalContext.current
    val micPermission = Manifest.permission.RECORD_AUDIO

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onSpeak()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            onClick = {
                val granted = ContextCompat.checkSelfPermission(
                    context, micPermission
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) onSpeak() else micLauncher.launch(micPermission)
            },
            modifier = Modifier.padding(end = 10.dp),
            enabled = state !is VoiceState.Listening
        ) {
            Text("Speak")
        }
        Button(onClick = onStop) { Text("Stop") }
    }
}