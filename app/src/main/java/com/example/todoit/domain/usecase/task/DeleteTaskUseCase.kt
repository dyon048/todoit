package com.example.todoit.domain.usecase.task

import com.example.todoit.domain.repository.TaskRepository
import com.example.todoit.domain.usecase.todo.SyncTodoStatusUseCase
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val syncTodoStatus: SyncTodoStatusUseCase,
) {
    suspend operator fun invoke(taskId: String) {
        val task = repository.getTaskById(taskId)
        // Also cascade-delete all recurrence instances of this parent task
        repository.softDeleteInstancesByParent(taskId)
        repository.softDeleteTask(taskId)
        // Re-sync parent todo status if we know the todoId
        if (task != null) syncTodoStatus(task.todoId)
    }
}
