package com.example.kilivana_driver.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.ui.theme.KilivanaError
import com.example.kilivana_driver.ui.theme.KilivanaGreen

@Composable
fun ResetPasswordDialog(
    visible: Boolean,
    token: String,
    newPassword: String,
    confirmPassword: String,
    isLoading: Boolean,
    message: String?,
    error: String?,
    onTokenChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset password") },
        text = {
            Column {
                Text("Enter the token from your recovery email and choose a new password.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = token,
                    onValueChange = onTokenChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Recovery token") },
                    singleLine = true,
                    enabled = !isLoading && message == null
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !isLoading && message == null
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm new password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !isLoading && message == null
                )
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = KilivanaError, fontSize = 13.sp)
                }
                message?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = KilivanaGreen, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            if (message != null) {
                TextButton(onClick = onDismiss) { Text("Done") }
            } else {
                TextButton(onClick = onSubmit, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Update password")
                    }
                }
            }
        },
        dismissButton = {
            if (message == null) {
                TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") }
            }
        }
    )
}
