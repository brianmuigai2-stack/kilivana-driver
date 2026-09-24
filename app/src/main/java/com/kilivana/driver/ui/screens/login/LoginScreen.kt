package com.kilivana.driver.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.R
import com.example.kilivana_driver.ui.theme.Green700
import com.example.kilivana_driver.ui.theme.Green900
import com.example.kilivana_driver.ui.theme.KilivanadriverTheme
import com.kilivana.driver.ui.components.ScreenScaffold

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }

    ScreenScaffold(
        modifier = modifier,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Image(
            painter = painterResource(id = R.drawable.kilivana_logo),
            contentDescription = stringResource(id = R.string.kilivana_logo_content_desc),
            modifier = Modifier.size(150.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to Kilivana",
            color = Green900,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sign in with your driver credentials",
            color = Color(0xFF4A4A4A),
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Green700,
                unfocusedBorderColor = Color(0xFFBDBDBD),
                focusedLabelColor = Green700
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Green700,
                unfocusedBorderColor = Color(0xFFBDBDBD),
                focusedLabelColor = Green700
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        ElevatedButton(
            onClick = { onLoginSuccess() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Green700,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    KilivanadriverTheme {
        LoginScreen(onLoginSuccess = {})
    }
}