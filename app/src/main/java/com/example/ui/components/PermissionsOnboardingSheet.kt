package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ZoyaDarkBackground
import com.example.ui.theme.ZoyaSurface
import com.example.ui.theme.ZoyaSurfaceVariant

@Composable
fun PermissionsOnboardingSheet(
    onDismiss: () -> Unit,
    onPermissionsUpdated: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    var permissionStates by remember {
        mutableStateOf(checkPermissions(context, requiredPermissions))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionStates = checkPermissions(context, requiredPermissions)
        val allGranted = permissionStates.values.all { it }
        onPermissionsUpdated(allGranted)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ZoyaSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.6f), NeonMagenta.copy(alpha = 0.6f))),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("permissions_onboarding_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(NeonMagenta.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = NeonMagenta,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Empower Zoya",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "I need a few permissions to unleash my full potential—calling, messaging, and listening 24/7.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Permission Items
                PermissionRow(
                    icon = Icons.Rounded.Mic,
                    title = "Microphone",
                    subtitle = "Real-time voice streaming & 'Hey Zoya' wake-word",
                    isGranted = permissionStates[Manifest.permission.RECORD_AUDIO] == true
                )

                Spacer(modifier = Modifier.height(12.dp))

                PermissionRow(
                    icon = Icons.Rounded.Contacts,
                    title = "Contacts",
                    subtitle = "Look up contacts for calls and WhatsApp messages",
                    isGranted = permissionStates[Manifest.permission.READ_CONTACTS] == true
                )

                Spacer(modifier = Modifier.height(12.dp))

                PermissionRow(
                    icon = Icons.Rounded.Call,
                    title = "Phone Calling",
                    subtitle = "Hands-free direct dialing via voice command",
                    isGranted = permissionStates[Manifest.permission.CALL_PHONE] == true
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Spacer(modifier = Modifier.height(12.dp))
                    PermissionRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Notifications",
                        subtitle = "Persistent presence for background listening",
                        isGranted = permissionStates[Manifest.permission.POST_NOTIFICATIONS] == true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val allGranted = permissionStates.values.all { it }

                Button(
                    onClick = {
                        if (allGranted) {
                            onDismiss()
                        } else {
                            launcher.launch(requiredPermissions)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allGranted) NeonEmerald else NeonMagenta
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("grant_permissions_button")
                ) {
                    Text(
                        text = if (allGranted) "Everything Ready! ✨" else "Grant All Permissions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (allGranted) Color.Black else Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Close", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ZoyaSurfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isGranted) NeonEmerald.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) NeonEmerald else NeonCyan,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Granted",
                tint = NeonEmerald,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonMagenta.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Required",
                    fontSize = 10.sp,
                    color = NeonMagenta,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun checkPermissions(context: Context, permissions: Array<String>): Map<String, Boolean> {
    return permissions.associateWith { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}
