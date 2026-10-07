package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ActionLog
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ZoyaSurface
import com.example.ui.theme.ZoyaSurfaceVariant

@Composable
fun ZoyaToolTesterSheet(
    recentActions: List<ActionLog>,
    onExecuteTest: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ZoyaSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Native Device Tools",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Test Zoya's native intent actions",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "QUICK TEST TRIGGERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                ToolTestItem(
                    icon = Icons.AutoMirrored.Rounded.Launch,
                    title = "openApp(\"youtube\")",
                    subtitle = "Launches YouTube application",
                    color = NeonMagenta,
                    onClick = { onExecuteTest("openYouTube") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ToolTestItem(
                    icon = Icons.Rounded.Smartphone,
                    title = "openApp(\"calculator\")",
                    subtitle = "Launches Calculator application",
                    color = NeonCyan,
                    onClick = { onExecuteTest("openCalculator") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ToolTestItem(
                    icon = Icons.Rounded.Call,
                    title = "searchAndCallContact(\"Mom\")",
                    subtitle = "Queries contacts and calls Mom",
                    color = NeonEmerald,
                    onClick = { onExecuteTest("callContact") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ToolTestItem(
                    icon = Icons.Rounded.PlayArrow,
                    title = "sendWhatsAppMessage()",
                    subtitle = "Deep-links WhatsApp chat with text",
                    color = NeonEmerald,
                    onClick = { onExecuteTest("whatsappMsg") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ToolTestItem(
                    icon = Icons.Rounded.Email,
                    title = "sendGmail()",
                    subtitle = "Opens Gmail draft composer",
                    color = NeonMagenta,
                    onClick = { onExecuteTest("sendGmail") }
                )

                if (recentActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RECENT EXECUTED ACTIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.height(120.dp)) {
                        items(recentActions) { action ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (action.isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (action.isSuccess) NeonEmerald else Color(0xFFFF5555),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = action.toolName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = action.description,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolTestItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ZoyaSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }

        Text(text = "RUN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
