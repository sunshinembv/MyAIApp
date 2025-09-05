package com.example.myaiapp.auth.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
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
fun PinSetupStep(onEvent: (AuthEvents.Ui) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        Text("Set 4-digit PIN", style = MaterialTheme.typography.h5)
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
        OutlinedTextField(
            value = confirm,
            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) confirm = it },
            label = { Text("Confirm PIN") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onEvent(AuthEvents.Ui.SetPin(pin)) }, enabled = pin.length == 4 && pin == confirm) {
            Text("Save PIN")
        }
    }
}