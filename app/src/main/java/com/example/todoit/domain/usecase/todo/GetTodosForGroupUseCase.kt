package com.example.todoit.domain.usecase.todo

import com.example.todoit.domain.model.TodoItem
import com.example.todoit.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTodosForGroupUseCase @Inject constructor(
    private val repository: TodoRepository,
) {
    fun observe(groupId: String): Flow<List<TodoItem>> =
        repository.observeTodosByGroup(groupId)
}

