package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RoomEntity::class, TimeBlockEntity::class], version = 3, exportSchema = false)
abstract class SpaceTimeDatabase : RoomDatabase() {
    abstract fun spaceTimeDao(): SpaceTimeDao
}
