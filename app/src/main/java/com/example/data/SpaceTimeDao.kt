package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceTimeDao {
    @Query("SELECT * FROM rooms ORDER BY orderIndex ASC, roomId DESC")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE roomId = :id")
    fun getRoomById(id: Int): Flow<RoomEntity?>

    @Query("SELECT * FROM rooms WHERE roomId = :id")
    suspend fun getRoomByIdOneShot(id: Int): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity): Long

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE roomId = :id")
    suspend fun deleteRoomById(id: Int)

    // Time Blocks
    @Query("SELECT * FROM time_blocks WHERE roomId = :roomId ORDER BY orderIndex ASC")
    fun getTimeBlocksForRoom(roomId: Int): Flow<List<TimeBlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeBlock(block: TimeBlockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeBlocks(blocks: List<TimeBlockEntity>)

    @Update
    suspend fun updateTimeBlock(block: TimeBlockEntity)

    @Update
    suspend fun updateTimeBlocks(blocks: List<TimeBlockEntity>)

    @Update
    suspend fun updateRooms(rooms: List<RoomEntity>)

    @Query("DELETE FROM time_blocks WHERE blockId = :id")
    suspend fun deleteTimeBlockById(id: Int)
}
