package com.example.todoit.domain.usecase.todo

import com.example.todoit.domain.model.TaskStatus
import com.example.todoit.domain.model.TodoStatus
import com.example.todoit.domain.repository.TaskRepository
import com.example.todoit.domain.repository.TodoRepository
import javax.inject.Inject

/**
 * Re-computes a TodoItem's [TodoStatus] from the current state of its tasks.
 *
 * Rules:
 * - No tasks → PENDING (nothing to be "done" yet)
 * - All tasks DONE → DONE
 * - Otherwise → PENDING
 *
 * Called by [UpsertTaskUseCase] and [DeleteTaskUseCase] after every task mutation.
 */
class SyncTodoStatusUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val todoRepository: TodoRepository,
) {
    suspend operator fun invoke(todoId: String) {
        val tasks = taskRepository.getTasksByTodo(todoId)
        val newStatus = when {
            tasks.isEmpty() -> TodoStatus.PENDING
            tasks.all { it.status == TaskStatus.DONE } -> TodoStatus.DONE
            else -> TodoStatus.PENDING
        }
        todoRepository.updateTodoStatus(todoId, newStatus)
    }
}

