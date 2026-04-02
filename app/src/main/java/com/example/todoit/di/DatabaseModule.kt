package com.example.todoit.di

import android.content.Context
import com.example.todoit.data.local.dao.GroupDao
import com.example.todoit.data.local.dao.LocationDao
import com.example.todoit.data.local.dao.RecurrenceDao
import com.example.todoit.data.local.dao.ScheduleDao
import com.example.todoit.data.local.dao.TaskDao
import com.example.todoit.data.local.dao.TodoDao
import com.example.todoit.data.local.db.TodoItDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TodoItDatabase =
        TodoItDatabase.create(context)

    @Provides
    fun provideGroupDao(db: TodoItDatabase): GroupDao = db.groupDao()

    @Provides
    fun provideTodoDao(db: TodoItDatabase): TodoDao = db.todoDao()

    @Provides
    fun provideTaskDao(db: TodoItDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideScheduleDao(db: TodoItDatabase): ScheduleDao = db.scheduleDao()

    @Provides
    fun provideLocationDao(db: TodoItDatabase): LocationDao = db.locationDao()

    @Provides
    fun provideRecurrenceDao(db: TodoItDatabase): RecurrenceDao = db.recurrenceDao()
}

