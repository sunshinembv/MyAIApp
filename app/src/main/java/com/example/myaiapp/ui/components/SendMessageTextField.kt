package com.example.myaiapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.myaiapp.R
import com.example.myaiapp.ui.theme.MyAIAppTheme

@Composable
fun SendMessageTextField(
    value: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onTextChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = stringResource(id = R.string.message),
                style = MyAIAppTheme.typography.body
            )
        },
        trailingIcon = {
            Row {
                IconButton(
                    onClick = onSend,
                    enabled = value.isNotEmpty()
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.baseline_send_24),
                        contentDescription = "Send icon",
                        tint = if (value.isNotEmpty()) {
                            MyAIAppTheme.colors.buttonColor
                        } else {
                            MyAIAppTheme.colors.iconTintColor
                        }
                    )
                }
            }
        },
        colors = TextFieldDefaults.textFieldColors(
            textColor = MyAIAppTheme.colors.primaryTextColor,
            backgroundColor = MyAIAppTheme.colors.backgroundColor,
            cursorColor = MyAIAppTheme.colors.outlinedEditTextColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            placeholderColor = MyAIAppTheme.colors.hintColor,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

@Preview(
    showBackground = true
)
@Composable
fun SendMessageTextFieldPreview() {
    MyAIAppTheme {
        SendMessageTextField(
            value = "",
            onTextChange = {},
            onSend = {}
        )
    }
}