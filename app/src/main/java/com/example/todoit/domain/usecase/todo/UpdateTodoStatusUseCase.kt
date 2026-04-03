package com.example.todoit.domain.usecase.todo

import com.example.todoit.domain.model.TodoStatus
import com.example.todoit.domain.repository.TodoRepository
import javax.inject.Inject

class UpdateTodoStatusUseCase @Inject constructor(
    private val repository: TodoRepository,
) {
    suspend operator fun invoke(id: String, status: TodoStatus) =
        repository.updateTodoStatus(id, status)
}

