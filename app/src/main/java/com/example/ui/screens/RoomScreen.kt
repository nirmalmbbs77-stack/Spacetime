package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RoomEntity
import com.example.data.TimeBlockEntity
import com.example.viewmodel.SpaceTimeViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun RoomScreen(
    roomId: Int,
    viewModel: SpaceTimeViewModel,
    onBack: () -> Unit
) {
    val room by viewModel.getRoom(roomId).collectAsStateWithLifecycle(initialValue = null)
    val timeBlocks by viewModel.getTimeBlocks(roomId).collectAsStateWithLifecycle(initialValue = emptyList())

    if (room == null) {
        Box(modifier = Modifier.fillMaxSize().background(SpaceBlack), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonCyan)
        }
        return
    }

    var activeBlock by remember { mutableStateOf<TimeBlockEntity?>(null) }
    var timeRemainingSecs by remember { mutableStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning, timeRemainingSecs) {
        if (isTimerRunning && timeRemainingSecs > 0) {
            delay(1000L)
            timeRemainingSecs -= 1
        } else if (isTimerRunning && timeRemainingSecs == 0) {
            isTimerRunning = false
            // Mark block complete if needed
            activeBlock?.let { viewModel.completeBlock(it) }
        }
    }

    val r = room!!
    val roomColor = Color(r.colorArgb)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(r.name, style = MaterialTheme.typography.titleLarge, color = roomColor)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Timer Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
                .background(SpaceSurface, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            val progress = if (activeBlock == null || timeRemainingSecs == 0) 0f
               else timeRemainingSecs.toFloat() / (activeBlock!!.durationMin * 60).toFloat()

            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(240.dp),
                color = roomColor,
                trackColor = roomColor.copy(alpha = 0.1f),
                strokeWidth = 8.dp
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val displaySecs = timeRemainingSecs % 60
                val displayMins = timeRemainingSecs / 60
                val timeStr = String.format("%02d:%02d", displayMins, displaySecs)

                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                    color = TextPrimary
                )
                Text(
                    text = activeBlock?.title ?: "Select a Block",
                    style = MaterialTheme.typography.titleMedium,
                    color = roomColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = {
                            if (activeBlock != null) {
                                isTimerRunning = !isTimerRunning
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(roomColor.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            if (isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = roomColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("TIME BLOCKS", style = MaterialTheme.typography.labelLarge, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))

        // Time Blocks Timeline
        LazyColumn(
            modifier = Modifier.weight(0.6f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(timeBlocks) { block ->
                val isCompleted = block.isCompleted
                val isCurrent = activeBlock?.blockId == block.blockId
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isCurrent) SpaceSurfaceVariant else SpaceSurface, RoundedCornerShape(16.dp))
                        .clickable {
                            activeBlock = block
                            timeRemainingSecs = block.durationMin * 60
                            isTimerRunning = false
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (isCompleted) roomColor else Color.Transparent, 
                                CircleShape
                            )
                            .padding(2.dp)
                    ) {
                        if (isCompleted) {
                            Icon(Icons.Filled.Check, contentDescription = "Done", tint = SpaceBlack, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(block.title, style = MaterialTheme.typography.bodyLarge, color = if (isCompleted) TextSecondary else TextPrimary)
                        Text("${block.durationMin} mins", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }
    }
}
