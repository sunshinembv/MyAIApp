package com.example.myaiapp.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myaiapp.auth.presentation.state.AuthEvents

@Composable
fun PinUnlockStep(
    retryInfo: String?,
    onEvent: (AuthEvents.Ui) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var showForgot by remember { mutableStateOf(false) }

    Column(Modifier.padding(16.dp)) {
        Text("Enter PIN", style = MaterialTheme.typography.h5)
        if (retryInfo != null) {
            Spacer(Modifier.height(6.dp))
            Text(retryInfo, style = MaterialTheme.typography.body2)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
            label = { Text("PIN") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onEvent(AuthEvents.Ui.VerifyPin(pin)) }, enabled = pin.length == 4) {
                Text("Unlock")
            }
            OutlinedButton(onClick = { showForgot = true }) { Text("Forgot PIN") }
        }
    }

    if (showForgot) {
        ForgotPinDialog(
            onSubmit = { pwd ->
                onEvent(AuthEvents.Ui.ResetPinWithPassword(pwd))
                showForgot = false
            },
            onDismiss = { showForgot = false }
        )
    }
}