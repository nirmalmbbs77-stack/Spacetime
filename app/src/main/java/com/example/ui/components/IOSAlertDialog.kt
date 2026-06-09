package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun IOSAlertDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    dismissButtonText: String? = null,
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
                
                // Content
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp)
                        .fillMaxWidth()
                ) {
                    content()
                }
                
                // Divider
                HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))
                
                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    if (dismissButtonText != null && onDismiss != null) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text(dismissButtonText, color = Color(0xFF0A84FF), fontSize = 17.sp)
                        }
                        VerticalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))
                    }
                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text(confirmButtonText, color = Color(0xFF0A84FF), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    }
                }
            }
        }
    }
}
