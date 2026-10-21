package com.example.kilivana_driver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaWhite

@Composable
fun LoginRoleSwitcher(
    isInspectorSelected: Boolean,
    onDriverSelected: () -> Unit,
    onInspectorSelected: () -> Unit,
    enabled: Boolean = true
) {
    Column {
        Text(
            text = "SIGN IN AS",
            color = KilivanaTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(KilivanaGreenTint)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoleOption(
                label = "Driver",
                selected = !isInspectorSelected,
                enabled = enabled,
                onClick = onDriverSelected,
                modifier = Modifier.weight(1f)
            )
            RoleOption(
                label = "Inspector",
                selected = isInspectorSelected,
                enabled = enabled,
                onClick = onInspectorSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RoleOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (enabled) {
                    Modifier.selectable(
                        selected = selected,
                        onClick = onClick,
                        role = Role.Tab
                    )
                } else {
                    Modifier
                }
            )
            .background(if (selected) KilivanaWhite else KilivanaGreenTint)
            .padding(vertical = 12.dp),
        color = if (selected) KilivanaGreen else KilivanaTextMuted,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}
