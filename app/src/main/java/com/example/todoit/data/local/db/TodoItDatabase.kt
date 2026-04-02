package com.example.todoit.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.todoit.data.local.dao.GroupDao
import com.example.todoit.data.local.dao.LocationDao
import com.example.todoit.data.local.dao.RecurrenceDao
import com.example.todoit.data.local.dao.ScheduleDao
import com.example.todoit.data.local.dao.TaskDao
import com.example.todoit.data.local.dao.TodoDao
import com.example.todoit.data.local.entity.GroupEntity
import com.example.todoit.data.local.entity.LocationEntity
import com.example.todoit.data.local.entity.RecurrenceEntity
import com.example.todoit.data.local.entity.ScheduleEntity
import com.example.todoit.data.local.entity.TaskEntity
import com.example.todoit.data.local.entity.TodoEntity

@Database(
    entities = [
        GroupEntity::class,
        TodoEntity::class,
        TaskEntity::class,
        ScheduleEntity::class,
        RecurrenceEntity::class,
        LocationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TodoItDatabase : RoomDatabase() {

    abstract fun groupDao(): GroupDao
    abstract fun todoDao(): TodoDao
    abstract fun taskDao(): TaskDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun locationDao(): LocationDao
    abstract fun recurrenceDao(): RecurrenceDao

    companion object {
        fun create(context: Context): TodoItDatabase =
            Room.databaseBuilder(context, TodoItDatabase::class.java, "todoit.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}

