package com.example.todoit.di

import com.example.todoit.data.repository.GroupRepositoryImpl
import com.example.todoit.data.repository.LocationRepositoryImpl
import com.example.todoit.data.repository.RecurrenceRepositoryImpl
import com.example.todoit.data.repository.ScheduleRepositoryImpl
import com.example.todoit.data.repository.TaskRepositoryImpl
import com.example.todoit.data.repository.TodoRepositoryImpl
import com.example.todoit.domain.repository.GroupRepository
import com.example.todoit.domain.repository.LocationRepository
import com.example.todoit.domain.repository.RecurrenceRepository
import com.example.todoit.domain.repository.ScheduleRepository
import com.example.todoit.domain.repository.TaskRepository
import com.example.todoit.domain.repository.TodoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository

    @Binds @Singleton
    abstract fun bindTodoRepository(impl: TodoRepositoryImpl): TodoRepository

    @Binds @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @Binds @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds @Singleton
    abstract fun bindRecurrenceRepository(impl: RecurrenceRepositoryImpl): RecurrenceRepository
}

