package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RoomEntity
import com.example.viewmodel.SpaceTimeViewModel
import com.example.ui.components.PicsartColorPickerDialog
import androidx.compose.ui.platform.LocalContext
import android.media.RingtoneManager
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SpaceTimeViewModel,
    onNavigateToRoom: (Int) -> Unit
) {
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    val colors = listOf(Color(0xFF00FFCC), Color(0xFFA200FF), Color(0xFFFF0080), Color(0xFFFFB800), Color(0xFF00E5FF))
    var selectedColor by remember { mutableStateOf(colors[0]) }
    var showCustomColorPicker by remember { mutableStateOf(false) }
    var roomToEdit by remember { mutableStateOf<RoomEntity?>(null) }
    
    // For editing
    var editRoomName by remember { mutableStateOf("") }
    var editSelectedColor by remember { mutableStateOf(colors[0]) }
    var showEditCustomColorPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.spacetime_logo),
                        contentDescription = "SpaceTime Logo",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Text(
                        text = "SpaceTime.",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Filled.Settings, "Settings", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            if (rooms.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.spacetime_logo),
                            contentDescription = "SpaceTime Logo Banner",
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color.Black)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "SpaceTime.",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "A futuristic, minimal, highly immersive Pomodoro productivity ecosystem.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.widthIn(max = 280.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(rooms) { room ->
                        RoomCard(
                            room = room,
                            onClick = { onNavigateToRoom(room.roomId) },
                            onEdit = { 
                                roomToEdit = room
                                editRoomName = room.name
                                editSelectedColor = Color(room.colorArgb.toInt())
                            },
                            onDelete = { viewModel.deleteRoom(room) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }


        }

        FloatingActionButton(
            onClick = { showCreateRoomDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create Room")
        }

        if (showCreateRoomDialog) {
            com.example.ui.components.IOSAlertDialog(
                onDismissRequest = { showCreateRoomDialog = false },
                title = "Create Room",
                content = {
                    Column {
                        OutlinedTextField(
                            value = newRoomName,
                            onValueChange = { newRoomName = it },
                            label = { Text("Room Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Room Color", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(color, androidx.compose.foundation.shape.CircleShape)
                                        .clickable { selectedColor = color }
                                        .drawBehind {
                                            if (selectedColor == color) {
                                                drawCircle(
                                                    color = Color.White,
                                                    radius = size.width / 2 + 4.dp.toPx(),
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                                )
                                            }
                                        }
                                )
                            }
                            
                            // Beautiful custom Picsart HSB colorful sweep gradient wheel
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                                                Color.Blue, Color.Magenta, Color.Red
                                            )
                                        ),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .clickable { showCustomColorPicker = true }
                                    .drawBehind {
                                        if (!colors.contains(selectedColor)) {
                                            drawCircle(
                                                color = Color.White,
                                                radius = size.width / 2 + 4.dp.toPx(),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                            )
                                        }
                                    }
                            )
                        }
                    }
                },
                confirmButtonText = "Create",
                onConfirm = {
                    if (newRoomName.isNotBlank()) {
                        viewModel.createManualRoom(newRoomName, selectedColor.toArgb().toLong())
                        showCreateRoomDialog = false
                        newRoomName = ""
                    }
                },
                dismissButtonText = "Cancel",
                onDismiss = { showCreateRoomDialog = false }
            )
        }

        if (roomToEdit != null) {
            com.example.ui.components.IOSAlertDialog(
                onDismissRequest = { roomToEdit = null },
                title = "Edit Room",
                content = {
                    Column {
                        OutlinedTextField(
                            value = editRoomName,
                            onValueChange = { editRoomName = it },
                            label = { Text("Room Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Room Color", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(color, androidx.compose.foundation.shape.CircleShape)
                                        .clickable { editSelectedColor = color }
                                        .drawBehind {
                                            if (editSelectedColor == color) {
                                                drawCircle(
                                                    color = Color.White,
                                                    radius = size.width / 2 + 4.dp.toPx(),
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                                )
                                            }
                                        }
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                                                Color.Blue, Color.Magenta, Color.Red
                                            )
                                        ),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .clickable { showEditCustomColorPicker = true }
                                    .drawBehind {
                                        if (!colors.contains(editSelectedColor)) {
                                            drawCircle(
                                                color = Color.White,
                                                radius = size.width / 2 + 4.dp.toPx(),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                            )
                                        }
                                    }
                            )
                        }
                    }
                },
                confirmButtonText = "Save",
                onConfirm = {
                    if (editRoomName.isNotBlank()) {
                        viewModel.updateRoomNameAndColor(
                            room = roomToEdit!!, 
                            newName = editRoomName, 
                            newColorArgb = editSelectedColor.toArgb().toLong()
                        )
                        roomToEdit = null
                    }
                },
                dismissButtonText = "Cancel",
                onDismiss = { roomToEdit = null }
            )
        }

        if (showCustomColorPicker) {
            PicsartColorPickerDialog(
                initialColor = selectedColor,
                onColorSelected = { selectedColor = it },
                onDismiss = { showCustomColorPicker = false }
            )
        }

        if (showEditCustomColorPicker) {
            PicsartColorPickerDialog(
                initialColor = editSelectedColor,
                onColorSelected = { editSelectedColor = it },
                onDismiss = { showEditCustomColorPicker = false }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(onDismiss = { showSettingsDialog = false })
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RoomCard(room: RoomEntity, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val roomColor = Color(room.colorArgb.toInt())
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by androidx.compose.animation.core.animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "scale")
    val glowAlpha by androidx.compose.animation.core.animateFloatAsState(targetValue = if (isPressed) 0.5f else 0.2f, label = "glow")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(roomColor.copy(alpha = glowAlpha), roomColor.copy(alpha = 0f)),
                        radius = size.width
                    )
                )
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onEdit
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(roomColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Room Icon", tint = roomColor)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Room", tint = roomColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
                        val progress = ((room.totalSessionsCompleted % 10) / 10f).coerceAtLeast(0.1f)
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = roomColor,
                            trackColor = roomColor.copy(alpha = 0.2f),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            Column {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${room.totalSessionsCompleted} Sessions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    
    var syncAccount by remember { mutableStateOf(sharedPrefs.getString("sync_account", null)) }
    var multiDeviceSync by remember { mutableStateOf(sharedPrefs.getBoolean("multi_device_sync", false)) }
    var ringtoneUri by remember { mutableStateOf(sharedPrefs.getString("ringtone_uri", null)) }

    val accountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
            if (accountName != null) {
                syncAccount = accountName
                sharedPrefs.edit().putString("sync_account", accountName).apply()
            }
        }
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                ringtoneUri = uri.toString()
                sharedPrefs.edit().putString("ringtone_uri", uri.toString()).apply()
            }
        }
    }

    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        com.example.ui.components.IOSAlertDialog(
            title = "About SpaceTime",
            onDismissRequest = { showAboutDialog = false },
            confirmButtonText = "Close",
            onConfirm = { showAboutDialog = false },
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "SpaceTime is a productivity timer designed to help you focus. " +
                        "Create rooms to organize tasks, set durations, and keep track of your " +
                        "progress as time flows. Customize room colors and create focus time blocks to stay on track.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    com.example.ui.components.IOSAlertDialog(
        onDismissRequest = onDismiss,
        title = "Settings",
        confirmButtonText = "Close",
        onConfirm = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Account Sync
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val intent = android.accounts.AccountManager.newChooseAccountIntent(
                            null, null, arrayOf("com.google"), null, null, null, null
                        )
                        accountLauncher.launch(intent)
                    }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Google Account", style = MaterialTheme.typography.titleMedium)
                        Text(syncAccount ?: "Not connected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (syncAccount != null) {
                        TextButton(onClick = { 
                            syncAccount = null
                            sharedPrefs.edit().remove("sync_account").apply()
                        }) {
                            Text("Disconnect")
                        }
                    }
                }

                // Multi Device Sync
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Multi-Device Sync", style = MaterialTheme.typography.titleMedium)
                        Text("Keep data updated across devices", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = multiDeviceSync,
                        onCheckedChange = { 
                            multiDeviceSync = it 
                            sharedPrefs.edit().putBoolean("multi_device_sync", it).apply()
                        },
                        enabled = syncAccount != null
                    )
                }

                // Ringtone Chooser
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        }
                        ringtoneLauncher.launch(intent)
                    }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notification Ringtone", style = MaterialTheme.typography.titleMedium)
                        val ringtoneName = if (ringtoneUri != null) {
                            RingtoneManager.getRingtone(context, android.net.Uri.parse(ringtoneUri)).getTitle(context) ?: "Unknown"
                        } else "Default"
                        Text(ringtoneName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // About Section
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        showAboutDialog = true
                    }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("About SpaceTime", style = MaterialTheme.typography.titleMedium)
                        Text("View app details and how to use", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    )
}
