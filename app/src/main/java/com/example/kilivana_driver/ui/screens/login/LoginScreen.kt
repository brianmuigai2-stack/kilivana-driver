package com.example.kilivana_driver.ui.screens.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.R
import com.example.kilivana_driver.ui.components.KilivanaStatusBarScrim
import com.example.kilivana_driver.ui.components.KilivanaTextField
import com.example.kilivana_driver.ui.theme.KilivanaError
import com.example.kilivana_driver.ui.theme.KilivanaErrorTint
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaGreenDark
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaTheme
import com.example.kilivana_driver.ui.theme.KilivanaWhite

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPassword: () -> Unit,
    onForgotPasswordEmailChange: (String) -> Unit,
    onForgotPasswordSubmit: () -> Unit,
    onForgotPasswordDismiss: () -> Unit,
    onContactSupport: () -> Unit,
    onInspectorLoginClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val hasError = uiState.errorMessage != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KilivanaWhite)
            .imePadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
    ) {

        // ---------------------------------------------------------
        // TOP GREEN SECTION
        // ---------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(KilivanaGreenDark, KilivanaGreen)
                    )
                )
        ) {
            KilivanaStatusBarScrim()

            // Subtle decorative circles
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.85f, size.height * 0.15f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = size.width * 0.35f,
                    center = Offset(size.width * 0.1f, size.height * 0.75f)
                )
                // Dotted route line
                var x = size.width * 0.08f
                while (x < size.width * 0.55f) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f),
                        radius = 3f,
                        center = Offset(x, size.height * 0.82f)
                    )
                    x += 18f
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 28.dp, end = 28.dp, top = 52.dp, bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular logo badge
                Box(
                    modifier = Modifier
                        .shadow(10.dp, CircleShape)
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.kilivana_logo),
                        contentDescription = "Kilivana logo",
                        modifier = Modifier.size(56.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "DRIVER PORTAL",
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your route starts here.",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Access your trips, deliveries and dispatches.",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        // ---------------------------------------------------------
        // WAVE DIVIDER
        // ---------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(KilivanaWhite)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(0f, size.height)
                    cubicTo(
                        size.width * 0.25f, 0f,
                        size.width * 0.75f, size.height,
                        size.width, 0f
                    )
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                // Green fill behind the wave (top part)
                drawRect(
                    color = KilivanaGreen,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height)
                )
                // White wave on top
                drawPath(path = path, color = KilivanaWhite)
            }
        }

        // ---------------------------------------------------------
        // FORM SECTION
        // ---------------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(KilivanaWhite)
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 32.dp)
        ) {

            Text(
                text = "Sign in",
                color = KilivanaTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Use your assigned driver account.",
                color = KilivanaTextMuted,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // EMAIL
            LoginFieldLabel(text = "Email address")

            KilivanaTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                placeholder = "driver@example.com",
                leadingIcon = Icons.Outlined.Email,
                enabled = !uiState.isLoading,
                isError = hasError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                    }
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // PASSWORD
            LoginFieldLabel(text = "Password")

            KilivanaTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                placeholder = "Enter your password",
                leadingIcon = Icons.Outlined.Lock,
                enabled = !uiState.isLoading,
                isError = hasError,
                visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingContent = {
                    IconButton(
                        onClick = onTogglePasswordVisibility,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = if (uiState.passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (uiState.passwordVisible) "Hide password" else "Show password",
                            tint = KilivanaTextMuted
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onLoginClick()
                    }
                )
            )

            // REMEMBER ME / FORGOT PASSWORD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !uiState.isLoading) {
                            onRememberMeChange(!uiState.rememberMe)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.rememberMe,
                        onCheckedChange = { if (!uiState.isLoading) onRememberMeChange(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = KilivanaGreen,
                            uncheckedColor = KilivanaTextMuted,
                            checkmarkColor = KilivanaWhite
                        )
                    )
                    Text(
                        text = "Keep me signed in",
                        color = KilivanaTextPrimary,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = onForgotPassword,
                    enabled = !uiState.isLoading,
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Text(
                        text = "Forgot password?",
                        color = KilivanaGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ERROR
            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                LoginErrorBanner(message = message)
            }

            Spacer(modifier = Modifier.height(22.dp))

            // SIGN IN BUTTON
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onLoginClick()
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KilivanaGreen,
                    contentColor = Color.White,
                    disabledContainerColor = KilivanaGreen.copy(alpha = 0.55f),
                    disabledContentColor = Color.White.copy(alpha = 0.85f)
                ),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign in",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ADMIN SUPPORT
            AdminSupportRow(
                enabled = !uiState.isLoading,
                onClick = onContactSupport
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SWITCH TO INSPECTOR LOGIN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Not a driver? ",
                    color = KilivanaTextMuted,
                    fontSize = 13.sp
                )
                TextButton(
                    onClick = onInspectorLoginClick,
                    enabled = !uiState.isLoading,
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Text(
                        text = "Sign in as inspector",
                        color = KilivanaGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // FOOTER
            Text(
                text = "KILIVANA DRIVER",
                modifier = Modifier.fillMaxWidth(),
                color = KilivanaTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    if (uiState.isForgotPasswordDialogVisible) {
        AlertDialog(
            onDismissRequest = onForgotPasswordDismiss,
            title = { Text("Reset your password") },
            text = {
                Column {
                    Text("Enter the email address associated with your driver account.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.forgotPasswordEmail,
                        onValueChange = onForgotPasswordEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email address") },
                        placeholder = { Text("driver@example.com") },
                        singleLine = true,
                        enabled = !uiState.isForgotPasswordLoading && uiState.forgotPasswordMessage == null,
                        isError = uiState.forgotPasswordError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onForgotPasswordSubmit() }
                        )
                    )
                    uiState.forgotPasswordError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = error, color = KilivanaError, fontSize = 13.sp)
                    }
                    uiState.forgotPasswordMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = message, color = KilivanaGreen, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                if (uiState.forgotPasswordMessage != null) {
                    TextButton(onClick = onForgotPasswordDismiss) {
                        Text("Done")
                    }
                } else {
                    TextButton(
                        onClick = onForgotPasswordSubmit,
                        enabled = !uiState.isForgotPasswordLoading
                    ) {
                        if (uiState.isForgotPasswordLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send reset link")
                        }
                    }
                }
            },
            dismissButton = {
                if (uiState.forgotPasswordMessage == null) {
                    TextButton(
                        onClick = onForgotPasswordDismiss,
                        enabled = !uiState.isForgotPasswordLoading
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

// ================================================================
// FIELD LABEL
// ================================================================

@Composable
private fun LoginFieldLabel(text: String) {
    Text(
        text = text,
        color = KilivanaTextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
}

// ================================================================
// ADMIN SUPPORT
// ================================================================

@Composable
private fun AdminSupportRow(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KilivanaGreenTint)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(KilivanaWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Phone,
                contentDescription = null,
                tint = KilivanaGreenCard,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Need assistance?",
                color = KilivanaTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Contact your administrator",
                color = KilivanaTextMuted,
                fontSize = 12.sp
            )
        }

        Text(
            text = "Contact",
            color = KilivanaGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ================================================================
// ERROR BANNER
// ================================================================

@Composable
private fun LoginErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KilivanaErrorTint)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            tint = KilivanaError,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            color = KilivanaError,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

// ================================================================
// PREVIEW
// ================================================================

@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun LoginScreenPreview() {
    KilivanaTheme {
        LoginScreen(
            uiState = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onRememberMeChange = {},
            onLoginClick = {},
            onForgotPassword = {},
            onForgotPasswordEmailChange = {},
            onForgotPasswordSubmit = {},
            onForgotPasswordDismiss = {},
            onContactSupport = {},
            onInspectorLoginClick = {}
        )
    }
}
