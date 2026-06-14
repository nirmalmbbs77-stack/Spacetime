package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val roomId: Int = 0,
    val name: String,
    val colorArgb: Long,
    val iconName: String = "Star",
    val focusDurationMin: Int = 25,
    val shortBreakMin: Int = 5,
    val longBreakMin: Int = 15,
    val isCompleted: Boolean = false,
    val totalSessionsCompleted: Int = 0,
    val orderIndex: Int = 0,
    val totalTimeLeft: Int = 0,
    val totalOvertime: Int = 0,
    val timeBank: Int = 0
)

@Entity(
    tableName = "time_blocks",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["roomId"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("roomId")]
)
data class TimeBlockEntity(
    @PrimaryKey(autoGenerate = true) val blockId: Int = 0,
    val roomId: Int,
    val title: String,
    val durationMin: Int,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0,
    val colorArgb: Long = 0xFFFFFFFF, // White default for task independent colors
    val assignedTime: Int = 0,
    val timeTaken: Int = 0,
    val timeLeft: Int = 0,
    val overtime: Int = 0,
    val completedAt: Long = 0L,
    val overtimeStartedAt: Long = 0L
)
