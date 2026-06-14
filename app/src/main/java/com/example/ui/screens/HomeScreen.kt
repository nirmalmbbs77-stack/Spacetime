package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RoomEntity
import com.example.viewmodel.SpaceTimeViewModel
import com.example.ui.components.PicsartColorPickerDialog
import kotlinx.coroutines.delay

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
    
    var importLink by remember { mutableStateOf("") }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var isReorderMode by remember { mutableStateOf(false) }

    // Bottom Tab State
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    // Cosmic Theme Color Definitions
    val darkBg = Brush.verticalGradient(
        colors = listOf(Color(0xFF0C0916), Color(0xFF140F27), Color(0xFF0C0916))
    )
    val lightBg = Brush.verticalGradient(
        colors = listOf(Color(0xFFF4F2FA), Color(0xFFECE7F4), Color(0xFFF4F2FA))
    )
    val bgBrush = if (isDarkMode) darkBg else lightBg
    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF1C1433)
    val textSecondaryColor = if (isDarkMode) Color(0xFFA29BBF) else Color(0xFF5E577A)
    val glassColor = if (isDarkMode) Color(0x33100B21) else Color(0x33FFFFFF)
    val outlineColor = if (isDarkMode) Color(0xFF2D254F) else Color(0xFFDCCFEF)

    // Animated dynamic background lighting for ambient space-time atmosphere
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha_1"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha_2"
    )
    val offsetAnimX1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset_x_1"
    )
    val offsetAnimY1 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset_y_1"
    )
    val offsetAnimX2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset_x_2"
    )
    val offsetAnimY2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset_y_2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = bgBrush)
    ) {
        // Gorgeous, immersive glassmorphic blur orbs in background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val accentPulse1 = if (isDarkMode) Color(0xFFA200FF) else Color(0xFFDCCFEF)
                val accentPulse2 = if (isDarkMode) Color(0xFF00FFCC) else Color(0xFFA200FF)

                drawCircle(
                    color = accentPulse1.copy(alpha = pulseAlpha1),
                    radius = size.width * 0.75f,
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * offsetAnimX1,
                        size.height * offsetAnimY1
                    )
                )

                drawCircle(
                    color = accentPulse2.copy(alpha = pulseAlpha2),
                    radius = size.width * 0.65f,
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * offsetAnimX2,
                        size.height * offsetAnimY2
                    )
                )
            }
        }
        // Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp) // Leave space for bottom floating bar
        ) {
            // 1. Sleek Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar left
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .border(1.5.dp, if (isDarkMode) Color(0xFF00FFCC) else Color(0xFFA200FF), CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color(0xFF1C133A) else Color(0xFFEBE3FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = if (isDarkMode) Color(0xFF00FFCC) else Color(0xFFA200FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "SpaceTime.",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = textPrimaryColor
                        )
                        Text(
                            text = "Task & Time bank",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = textSecondaryColor
                        )
                    }
                }

                // Header Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Preset Rooms Button (if empty)
                    if (rooms.isEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.createManualRoom("Guided Meditation", Color(0xFFA200FF).toArgb().toLong())
                                viewModel.createManualRoom("Reduce Anxiety", Color(0xFF00E5FF).toArgb().toLong())
                                viewModel.createManualRoom("Relieve Stress", Color(0xFFFF0080).toArgb().toLong())
                                android.widget.Toast.makeText(context, "Spawned Premium Core Rooms!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Spawn Presets",
                                tint = if (isDarkMode) Color(0xFF00FFCC) else Color(0xFFA200FF)
                            )
                        }
                    }

                    // Theme button
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier
                            .background(glassColor, CircleShape)
                            .border(1.dp, outlineColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = textPrimaryColor
                        )
                    }

                    // Settings button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .background(glassColor, CircleShape)
                            .border(1.dp, outlineColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = textPrimaryColor
                        )
                    }
                }
            }

            // Cross-fade views based on selectedTab
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        // HOME VIEW (The Video Redesign)
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Section: Today's Pick (Large Pinned Room)
                            item {
                                Text(
                                    text = "Today's Pick",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = textPrimaryColor,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                val featuredRoom = rooms.firstOrNull()
                                if (featuredRoom != null) {
                                    val fTimeBlocks by viewModel.getTimeBlocks(featuredRoom.roomId).collectAsStateWithLifecycle(emptyList())
                                    val totalMins = fTimeBlocks.sumOf { it.durationMin }
                                    FeaturedRoomCard(
                                        room = featuredRoom,
                                        totalMins = totalMins,
                                        onClick = { onNavigateToRoom(featuredRoom.roomId) },
                                        onEdit = {
                                            roomToEdit = featuredRoom
                                            editRoomName = featuredRoom.name
                                            editSelectedColor = Color(featuredRoom.colorArgb.toInt())
                                        },
                                        onDelete = { viewModel.deleteRoom(featuredRoom) },
                                        isDarkMode = isDarkMode
                                    )
                                } else {
                                    // Empty State Welcome Pinned Card
                                    WelcomePlaceholderCard(
                                        onCreatePresets = {
                                            viewModel.createManualRoom("Guided Meditation", Color(0xFFA200FF).toArgb().toLong())
                                            viewModel.createManualRoom("Reduce Anxiety", Color(0xFF00FFCC).toArgb().toLong())
                                            viewModel.createManualRoom("Relieve Stress", Color(0xFFFF0080).toArgb().toLong())
                                        },
                                        onCreateManual = { showCreateRoomDialog = true },
                                        isDarkMode = isDarkMode
                                    )
                                }
                            }

                            // Section: All Rooms / New Releases
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "New Releases",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = textPrimaryColor
                                    )
                                    
                                    if (rooms.isNotEmpty()) {
                                        TextButton(onClick = { isReorderMode = !isReorderMode }) {
                                            Text(
                                                text = if (isReorderMode) "Done Reordering" else "Reorder Rooms",
                                                color = if (isDarkMode) Color(0xFF00FFCC) else Color(0xFFA200FF),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }

                                if (isReorderMode && rooms.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDarkMode) Color(0x7F2A214C) else Color(0xFFECE7F7),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.Menu,
                                                "Reorder",
                                                tint = textPrimaryColor,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                "Tap Arrow Keys on room items below to move position.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = textSecondaryColor
                                            )
                                        }
                                    }
                                }
                            }

                            // Horizontal or Vertical List of Wide Cards matching "New Releases"
                            val otherRooms = if (rooms.isNotEmpty()) rooms.drop(1) else emptyList()
                            if (otherRooms.isEmpty() && rooms.isNotEmpty()) {
                                // If they only have one room, prompt to add another
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(glassColor)
                                            .border(1.dp, outlineColor, RoundedCornerShape(20.dp))
                                            .clickable { showCreateRoomDialog = true }
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Filled.AddCircle, "Add", tint = textSecondaryColor, modifier = Modifier.size(32.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Add more rooms to populate New Releases!", color = textSecondaryColor, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            } else if (rooms.isEmpty()) {
                                item {
                                    Text(
                                        "Setup your SpaceTime dashboard to start.",
                                        color = textSecondaryColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                    )
                                }
                            } else {
                                items(otherRooms, key = { it.roomId }) { room ->
                                    val rTimeBlocks by viewModel.getTimeBlocks(room.roomId).collectAsStateWithLifecycle(emptyList())
                                    val totalMins = rTimeBlocks.sumOf { it.durationMin }
                                    WideReleaseRoomCard(
                                        room = room,
                                        totalMins = totalMins,
                                        isReorderMode = isReorderMode,
                                        onMoveUp = {
                                            val idx = rooms.indexOf(room)
                                            if (idx > 0) {
                                                val mutable = rooms.toMutableList()
                                                val temp = mutable[idx]
                                                mutable[idx] = mutable[idx - 1]
                                                mutable[idx - 1] = temp
                                                viewModel.updateRoomOrder(mutable)
                                            }
                                        },
                                        onMoveDown = {
                                            val idx = rooms.indexOf(room)
                                            if (idx != -1 && idx < rooms.size - 1) {
                                                val mutable = rooms.toMutableList()
                                                val temp = mutable[idx]
                                                mutable[idx] = mutable[idx + 1]
                                                mutable[idx + 1] = temp
                                                viewModel.updateRoomOrder(mutable)
                                            }
                                        },
                                        onClick = { onNavigateToRoom(room.roomId) },
                                        onEdit = {
                                            roomToEdit = room
                                            editRoomName = room.name
                                            editSelectedColor = Color(room.colorArgb.toInt())
                                        },
                                        onDelete = { viewModel.deleteRoom(room) },
                                        isDarkMode = isDarkMode
                                    )
                                }
                            }

                            // Info Footer
                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.AutoAwesome,
                                        contentDescription = "Cosmic",
                                        tint = textSecondaryColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Long-press room card to edit. Double-tap to reorder.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = textSecondaryColor.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // BREATHE VIEW - Highly interactive concentric breathing rings with guide
                        AnimatedBreatheView(isDarkMode = isDarkMode)
                    }

                    2 -> {
                        // MEDITATE VIEW - SERENE ambient soundscape generator
                        AnimatedMeditateView(isDarkMode = isDarkMode)
                    }

                    3 -> {
                        // MOVE VIEW - Quick micro-break fitness generator for Pomodoro
                        AnimatedMoveView(isDarkMode = isDarkMode)
                    }

                    4 -> {
                        // SLEEP VIEW - Matches the exact sleep duration wheel in the second image!
                        AnimatedSleepView(isDarkMode = isDarkMode)
                    }
                }
            }
        }

        // Floating Action Button to Add Room (Aligned clean)
        if (selectedTab == 0) {
            FloatingActionButton(
                onClick = { showCreateRoomDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 104.dp, end = 20.dp),
                containerColor = if (isDarkMode) Color(0xFF00FFCC) else Color(0xFFA200FF),
                contentColor = Color.Black
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Room")
            }
        }

        // 2. Glassmorphic Translucent Bottom Navigation Bar (Matching the video)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .background(
                    color = if (isDarkMode) Color(0xCC0D091A) else Color(0xDDFAF5FE),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = if (isDarkMode) {
                            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
                        } else {
                            listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.02f))
                        }
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Filled.Home,
                    label = "Home",
                    isActive = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    isDarkMode = isDarkMode
                )
                BottomNavItem(
                    icon = Icons.Filled.Adjust,
                    label = "Breathe",
                    isActive = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    isDarkMode = isDarkMode
                )
                BottomNavItem(
                    icon = Icons.Filled.Spa,
                    label = "Meditate",
                    isActive = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    isDarkMode = isDarkMode
                )
                BottomNavItem(
                    icon = Icons.Filled.DirectionsRun,
                    label = "Move",
                    isActive = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    isDarkMode = isDarkMode
                )
                BottomNavItem(
                    icon = Icons.Filled.NightsStay,
                    label = "Sleep",
                    isActive = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    isDarkMode = isDarkMode
                )
            }
        }

        // CREATE ROOM DIALOG
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
                        Text("Room Color Accent", style = MaterialTheme.typography.labelLarge)
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
                                        .background(color, CircleShape)
                                        .clickable { selectedColor = color }
                                        .drawBehind {
                                            if (selectedColor == color) {
                                                drawCircle(
                                                    color = if (isDarkMode) Color.White else Color.Black,
                                                    radius = size.width / 2 + 4.dp.toPx(),
                                                    style = Stroke(width = 2.dp.toPx())
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
                                            shape = CircleShape
                                        )
                                        .clickable { showCustomColorPicker = true }
                                        .drawBehind {
                                            if (!colors.contains(selectedColor)) {
                                                drawCircle(
                                                    color = if (isDarkMode) Color.White else Color.Black,
                                                    radius = size.width / 2 + 4.dp.toPx(),
                                                    style = Stroke(width = 2.dp.toPx())
                                                )
                                            }
                                        }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Or Import Dynamic Room Structure", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = importLink,
                                onValueChange = { importLink = it },
                                label = { Text("Paste 'spacetime://' Link") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                if (importLink.startsWith("spacetime://")) {
                                    try {
                                        val encoded = importLink.removePrefix("spacetime://")
                                        val jsonStr = String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
                                        val jsonObj = org.json.JSONObject(jsonStr)
                                        viewModel.createImportedRoomFromJson(jsonObj)
                                        showCreateRoomDialog = false
                                        importLink = ""
                                        android.widget.Toast.makeText(context, "Room Imported!", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Invalid format/link", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Rippled links must start with spacetime://", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Import")
                            }
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

        // EDIT ROOM DIALOG
        if (roomToEdit != null) {
            com.example.ui.components.IOSAlertDialog(
                onDismissRequest = { roomToEdit = null },
                title = "Edit Room Parameters",
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
                        Text("Theme Color Accent", style = MaterialTheme.typography.labelLarge)
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
                                        .background(color, CircleShape)
                                        .clickable { editSelectedColor = color }
                                        .drawBehind {
                                            if (editSelectedColor == color) {
                                                drawCircle(
                                                    color = if (isDarkMode) Color.White else Color.Black,
                                                    radius = size.width / 2 + 4.dp.toPx(),
                                                    style = Stroke(width = 2.dp.toPx())
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
                                        shape = CircleShape
                                    )
                                    .clickable { showEditCustomColorPicker = true }
                                    .drawBehind {
                                        if (!colors.contains(editSelectedColor)) {
                                            drawCircle(
                                                color = if (isDarkMode) Color.White else Color.Black,
                                                radius = size.width / 2 + 4.dp.toPx(),
                                                style = Stroke(width = 2.dp.toPx())
                                            )
                                        }
                                    }
                            )
                        }
                    }
                },
                confirmButtonText = "Save Changes",
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
            com.example.ui.components.PicsartColorPickerDialog(
                initialColor = selectedColor,
                onColorSelected = { selectedColor = it },
                onDismiss = { showCustomColorPicker = false }
            )
        }

        if (showEditCustomColorPicker) {
            com.example.ui.components.PicsartColorPickerDialog(
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

// ==========================================
// COMPONENT 1: TODAY'S PICK FEATURED CARD
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeaturedRoomCard(
    room: RoomEntity,
    totalMins: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isDarkMode: Boolean
) {
    val roomColor = Color(room.colorArgb.toInt())
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, label = "scale")

    // Premium multi-layered sweeping glass gradient
    val cardBgGradient = if (isDarkMode) {
        Brush.linearGradient(
            colors = listOf(
                roomColor.copy(alpha = 0.7f),
                roomColor.copy(alpha = 0.25f),
                Color(0xBB0A0718)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                roomColor.copy(alpha = 0.55f),
                roomColor.copy(alpha = 0.15f),
                Color(0xDDFFFFFF)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(32.dp))
            .background(cardBgGradient)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        roomColor.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onEdit
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header stats inside card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "FEATURED ROOM",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF00FFCC)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Edit, "Edit", tint = Color.White)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Delete, "Delete", tint = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            // Room Name & Details
            Column {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "$totalMins min",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Text(
                        text = "•  ${room.totalSessionsCompleted} Sessions completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Bottom row: Interactive Round Play button + TimeBank details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Round White Play Button (As in the video)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color.White, CircleShape)
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Start Room Focus Timer",
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Transparent glowing TimeBank badge
                val tbMins = room.timeBank / 60
                val tbColor = if (room.timeBank > 0) Color(0xFF00FFCC) else if (room.timeBank < 0) Color(0xFFFF3366) else Color.White
                val tbSign = if (room.timeBank > 0) "+" else if (room.timeBank < 0) "-" else ""
                
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(tbColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TimeBank: $tbSign${kotlin.math.abs(tbMins)}m",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT 2: WELCOME PLACEHOLDER FEATURED CARD
// ==========================================
@Composable
fun WelcomePlaceholderCard(
    onCreatePresets: () -> Unit,
    onCreateManual: () -> Unit,
    isDarkMode: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = if (isDarkMode) {
                        listOf(Color(0x993B0066), Color(0x7F140F2D))
                    } else {
                        listOf(Color(0xCCE7DFFE), Color(0x99EBE5FC))
                    }
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color(0xFFA200FF).copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome to SpaceTime",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF00FFCC)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Redefined celestial Pomodoro with real Time-Banking logic.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCreatePresets,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Auto Spawn Presets", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
                OutlinedButton(
                    onClick = onCreateManual,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+ Create Room", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ==========================================
// COMPONENT 3: WIDE CARD (NEW RELEASES LIST)
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WideReleaseRoomCard(
    room: RoomEntity,
    totalMins: Int,
    isReorderMode: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isDarkMode: Boolean
) {
    val roomColor = Color(room.colorArgb.toInt())
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, label = "scale")

    val cardBg = if (isDarkMode) {
        Color(0x3D130E26)
    } else {
        Color(0xD9FFFFFF)
    }
    val cardBorderBrush = if (isDarkMode) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                roomColor.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.05f),
                roomColor.copy(alpha = 0.15f),
                Color.Black.copy(alpha = 0.02f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(
                width = 1.2.dp,
                brush = cardBorderBrush,
                shape = RoundedCornerShape(24.dp)
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { if (!isReorderMode) onClick() },
                onLongClick = onEdit
            )
    ) {
        // Glowing left side bar based on room accent color
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(roomColor)
                .align(Alignment.CenterStart)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Room Name Title
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = if (isDarkMode) Color.White else Color(0xFF130E26),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stats row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "$totalMins min",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = roomColor
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkMode) Color(0x3CFFFFFF) else Color(0x3C000000)
                    )
                    Text(
                        text = "${room.totalSessionsCompleted} Sessions done",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkMode) Color(0xA1FFFFFF) else Color(0xA1000000)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timebank Pill Tag
                val tbMins = room.timeBank / 60
                val tbColor = if (room.timeBank > 0) Color(0xFF00FFCC) else if (room.timeBank < 0) Color(0xFFFF3366) else if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                val tbSign = if (room.timeBank > 0) "+" else if (room.timeBank < 0) "-" else ""
                
                Box(
                    modifier = Modifier
                        .background(
                            color = roomColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "TimeBank: $tbSign${kotlin.math.abs(tbMins)}m",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                }
            }

            // Controls Column
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isReorderMode) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Move Up", tint = roomColor)
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Move Down", tint = roomColor)
                    }
                } else {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, "Edit Room", tint = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, "Delete Room", tint = roomColor.copy(alpha = 0.8f))
                    }
                    // Arrow button to play/open
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(roomColor.copy(alpha = 0.15f), CircleShape)
                            .clickable { onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Open",
                            tint = roomColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT 4: NAVIGATION COZY BUTTON
// ==========================================
@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    val activeColor = if (isDarkMode) Color(0xFF00FFCC) else Color(0xFFA200FF)
    val inactiveColor = if (isDarkMode) Color(0xFF746C91) else Color(0xFF9E95B8)

    val scale by animateFloatAsState(targetValue = if (isActive) 1.12f else 1.0f, label = "tabScale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium
            ),
            color = if (isActive) activeColor else inactiveColor
        )

        // Glowing cyan/purple dot active line at the very bottom
        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(width = 12.dp, height = 3.dp)
                    .background(activeColor, RoundedCornerShape(1.5.dp))
            )
        }
    }
}

// =======================================================
// COMPONENT 5: INTERACTIVE CELESTIAL BREATHE VISUALIZER
// =======================================================
@Composable
fun AnimatedBreatheView(isDarkMode: Boolean) {
    var breatheState by remember { mutableStateOf("Breathe In") }
    var scaleAnim = remember { Animatable(1.0f) }
    var guideText by remember { mutableStateOf("Expand your mind, prepare to sync.") }

    LaunchedEffect(Unit) {
        while (true) {
            breatheState = "Breathe In"
            guideText = "Inhale serene, fresh cosmic energy..."
            scaleAnim.animateTo(
                targetValue = 1.6f,
                animationSpec = tween(durationMillis = 4000, easing = LinearOutSlowInEasing)
            )
            
            breatheState = "Hold"
            guideText = "Retain focus, anchor the mind..."
            delay(3500)
            
            breatheState = "Breathe Out"
            guideText = "Exhale stale pressure into the void..."
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 4000, easing = FastOutLinearInEasing)
            )
            
            breatheState = "Sustain"
            guideText = "Be present. Still."
            delay(2000)
        }
    }

    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF1C1433)
    val textSecondaryColor = if (isDarkMode) Color(0xFFA29BBF) else Color(0xFF5E577A)
    val circleColor = if (isDarkMode) Color(0xFF00FFCC) else Color(0xFFA200FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BREATHING SYNC",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = circleColor
        )
        
        Spacer(modifier = Modifier.height(40.dp))

        // Concentric breathing rings matching organic ripples
        Box(
            modifier = Modifier
                .size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ripple Ring 1 (Static)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(circleColor.copy(alpha = 0.04f), CircleShape)
                    .border(1.dp, circleColor.copy(alpha = 0.15f), CircleShape)
            )

            // Ripple Ring 2 (Animated Elastic Scale)
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                    }
                    .background(circleColor.copy(alpha = 0.08f), CircleShape)
                    .border(2.dp, circleColor.copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Heart Center Circle
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(circleColor.copy(alpha = 0.25f), CircleShape)
                        .border(1.5.dp, circleColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (breatheState) {
                            "Breathe In" -> "IN"
                            "Breathe Out" -> "OUT"
                            "Hold" -> "HOLD"
                            else -> "STILL"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = textPrimaryColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Dynamic State label
        Text(
            text = breatheState.uppercase(),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = textPrimaryColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = guideText,
            style = MaterialTheme.typography.bodyLarge,
            color = textSecondaryColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

// =======================================================
// COMPONENT 6: ZEN SOUNDSCAPE GENERATOR
// =======================================================
@Composable
fun AnimatedMeditateView(isDarkMode: Boolean) {
    var isPlayingSound by remember { mutableStateOf(false) }
    var selectedAtmosphere by remember { mutableStateOf("Rain Forest") }
    
    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF1C1433)
    val textSecondaryColor = if (isDarkMode) Color(0xFFA29BBF) else Color(0xFF5E577A)
    val circleColor = if (isDarkMode) Color(0xFFA200FF) else Color(0xFF00E5FF)

    val listSounds = listOf("Rain Forest", "Celestial Hubm", "Tibetan Singing Bowls", "Oceanic Waves")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MEDITATION SOUNDSCAPE",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = circleColor
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Cosmic sphere that glows when meditating
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val glowAnim by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = if (isPlayingSound) 0.65f else 0.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(circleColor.copy(alpha = glowAnim), Color.Transparent),
                                radius = size.width
                            )
                        )
                    }
            )

            IconButton(
                onClick = { isPlayingSound = !isPlayingSound },
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlayingSound) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Trigger sound",
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (isPlayingSound) "Synthesized feedback active" else "Sound player ready",
            style = MaterialTheme.typography.titleMedium,
            color = textPrimaryColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Selectable atmospheric sounds
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listSounds.forEach { sound ->
                val isSelected = selectedAtmosphere == sound
                val soundColor = if (isSelected) circleColor else textSecondaryColor.copy(alpha = 0.5f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) circleColor.copy(alpha = 0.1f) else Color.Transparent)
                        .border(1.2.dp, soundColor, RoundedCornerShape(16.dp))
                        .clickable { selectedAtmosphere = sound }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sound,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                            color = textPrimaryColor
                        )
                        if (isSelected) {
                            Icon(Icons.Filled.CheckCircle, "Active", tint = circleColor)
                        }
                    }
                }
            }
        }
    }
}

// =======================================================
// COMPONENT 7: ENERGY MOVE MENTUM TRACKER
// =======================================================
@Composable
fun AnimatedMoveView(isDarkMode: Boolean) {
    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF1C1433)
    val textSecondaryColor = if (isDarkMode) Color(0xFFA29BBF) else Color(0xFF5E577A)
    val accentColor = if (isDarkMode) Color(0xFFFF0080) else Color(0xFFA200FF)

    val listExercises = listOf(
        "Diaphragmatic loop stretching" to "Breath deeply, roll shoulders backward in sync (2 min)",
        "Cosmic neck-release" to "Tilt ears toward respective shoulders, tracking time (1 min)",
        "Chair pose isometric holds" to "Engage lower core muscle group and thighs (1.5 min)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "COSMIC MICRO BREAKS",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = accentColor
        )

        Text(
            text = "Active during long pomodoro pauses to restore cerebral blood flow:",
            style = MaterialTheme.typography.bodyMedium,
            color = textSecondaryColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        listExercises.forEachIndexed { i, exercise ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDarkMode) Color(0x66181232) else Color(0xFFFFFFFF))
                    .border(1.dp, if (isDarkMode) Color(0xFF2E224F) else Color(0xFFECE5F9), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (i + 1).toString(),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = accentColor
                        )
                    }
                    Column {
                        Text(
                            text = exercise.first,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textPrimaryColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = exercise.second,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondaryColor
                        )
                    }
                }
            }
        }
    }
}

// =======================================================
// COMPONENT 8: APPLE DESIGN SLEEP SECTOR & MOON CLOCK
// =======================================================
@Composable
fun AnimatedSleepView(isDarkMode: Boolean) {
    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF1C1433)
    val textSecondaryColor = if (isDarkMode) Color(0xFFA29BBF) else Color(0xFF5E577A)
    val accentColor = if (isDarkMode) Color(0xFF00FFCC) else Color(0xFFA200FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SLEEP CHRONOMETRY",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = accentColor
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Time indicators Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NightsStay, "Bedtime", tint = accentColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bedtime", style = MaterialTheme.typography.bodySmall, color = textSecondaryColor)
                }
                Text("23:00", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = textPrimaryColor)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WbSunny, "Wake Up", tint = Color.Yellow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Wake Up", style = MaterialTheme.typography.bodySmall, color = textSecondaryColor)
                }
                Text("06:30", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = textPrimaryColor)
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // 1-to-1 sleep ring visualizer imitating the second image!
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // Draw clean Sleep Arc dial using Canvas
            Canvas(modifier = Modifier.size(200.dp)) {
                // outer dial marker
                drawCircle(
                    color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                    style = Stroke(width = 6.dp.toPx())
                )

                // sleep duration arc (7.5h implies 7.5 / 24 of a circle = ~ 112.5 deg)
                drawArc(
                    color = accentColor,
                    startAngle = -20f,
                    sweepAngle = 130f,
                    useCenter = false,
                    style = Stroke(width = 14.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.NightsStay,
                    contentDescription = "Moon",
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sleep Duration",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondaryColor
                )
                Text(
                    text = "7h 30m",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = textPrimaryColor
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.OfflineBolt, "Battery", tint = accentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Syncs sleep debt directly to SpaceTime bank",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = textPrimaryColor
            )
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
