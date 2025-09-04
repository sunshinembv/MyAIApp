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
fun RegisterStep(
    onEvent: (AuthEvents.Ui) -> Unit,
    toChatScreen: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {
        Text("Create account", style = MaterialTheme.typography.h5)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pwd, onValueChange = { pwd = it },
            label = { Text("Password (≥ 8)") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = confirm, onValueChange = { confirm = it },
            label = { Text("Confirm") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onEvent(AuthEvents.Ui.Register(email.trim(), pwd)) },
            enabled = email.isNotBlank() && pwd.length >= 8 && pwd == confirm
        ) { Text("Continue") }

        Button(
            onClick = { onEvent(AuthEvents.Ui.OpenChatScreen(toChatScreen)) },
        ) { Text("Guest") }
    }
}