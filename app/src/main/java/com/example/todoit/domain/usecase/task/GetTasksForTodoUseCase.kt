package com.example.todoit.domain.usecase.task

import com.example.todoit.domain.model.Task
import com.example.todoit.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTasksForTodoUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    fun observe(todoId: String): Flow<List<Task>> =
        repository.observeTasksByTodo(todoId)
}

