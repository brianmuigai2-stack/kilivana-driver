package com.example.kilivana_driver.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.data.model.AuthUser
import com.example.kilivana_driver.ui.components.KilivanaHeader
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaWhite

/**
 * Read-only view of the account details the backend owns.
 *
 * These come from GET /api/v1/auth/me (via [AuthUser]). They are deliberately
 * not editable here: the app has no endpoint to change a name, email, phone,
 * role or status, so offering an edit field would be a dead end. The driver
 * asks their admin for changes.
 */
@Composable
fun PersonalInfoScreen(
    user: AuthUser?,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KilivanaBackground)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        ProfileSubHeader(title = "Personal Information", onBack = onBack)

        Column(modifier = Modifier.padding(16.dp)) {
            InfoCard {
                InfoRow("Full name", user?.name)
                InfoDivider()
                InfoRow("Email", user?.email)
                InfoDivider()
                InfoRow("Phone", user?.phone)
                InfoDivider()
                InfoRow("Region", user?.region)
                InfoDivider()
                InfoRow("Role", user?.role?.replaceFirstChar { it.uppercase() })
                InfoDivider()
                InfoRow("Account status", user?.status?.replaceFirstChar { it.uppercase() })
                InfoDivider()
                InfoRow(
                    "Verification",
                    user?.verificationStatus?.replaceFirstChar { it.uppercase() }
                )
                InfoDivider()
                InfoRow("User ID", user?.id?.toString())
                InfoDivider()
                InfoRow("Member since", user?.createdAt?.take(10)?.ifBlank { null })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "These details are managed by your Kilivana admin. Contact them if " +
                    "your name, phone number or account status needs to change.",
                color = KilivanaTextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
internal fun ProfileSubHeader(title: String, onBack: () -> Unit) {
    KilivanaHeader(title = title, onBack = onBack)
}

@Composable
internal fun InfoCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KilivanaWhite)
            .padding(vertical = 4.dp)
    ) {
        content()
    }
}

@Composable
internal fun InfoRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = KilivanaTextMuted,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "—",
            color = if (value.isNullOrBlank()) KilivanaTextMuted else KilivanaTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
internal fun InfoDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(KilivanaGreen.copy(alpha = 0.08f))
    )
}
