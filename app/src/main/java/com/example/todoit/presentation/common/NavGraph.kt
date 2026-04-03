package com.example.todoit.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.todoit.presentation.browse.BrowseScreen
import com.example.todoit.presentation.groups.GroupDetailScreen
import com.example.todoit.presentation.groups.GroupsScreen
import com.example.todoit.presentation.home.HomeScreen
import com.example.todoit.presentation.settings.ScheduleEditScreen
import com.example.todoit.presentation.settings.ScheduleListScreen
import com.example.todoit.presentation.settings.SettingsScreen
import com.example.todoit.presentation.tasks.TaskEditScreen
import com.example.todoit.presentation.todos.TodoDetailScreen

object Screen {
    const val HOME      = "home"
    const val GROUPS    = "groups"
    const val BROWSE    = "browse"
    const val SETTINGS  = "settings"
    const val SCHEDULES = "schedules"

    const val GROUP_DETAIL  = "group_detail/{groupId}"
    const val TODO_DETAIL   = "todo_detail/{todoId}"
    const val TASK_EDIT     = "task_edit?taskId={taskId}&todoId={todoId}"
    const val SCHEDULE_EDIT = "schedule_edit?scheduleId={scheduleId}"

    fun groupDetail(groupId: String)  = "group_detail/$groupId"
    fun todoDetail(todoId: String)    = "todo_detail/$todoId"
    fun taskEdit(todoId: String, taskId: String? = null) =
        "task_edit?taskId=${taskId ?: ""}&todoId=$todoId"
    fun scheduleEdit(scheduleId: String? = null) =
        "schedule_edit?scheduleId=${scheduleId ?: ""}"
}

@Composable
fun TodoItNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.HOME,
        modifier = modifier,
    ) {
        composable(Screen.HOME) {
            HomeScreen(navController = navController)
        }

        composable(Screen.GROUPS) {
            GroupsScreen(navController = navController)
        }

        composable(Screen.BROWSE) {
            BrowseScreen(navController = navController)
        }

        composable(
            route = Screen.GROUP_DETAIL,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
        ) { back ->
            val groupId = back.arguments?.getString("groupId") ?: return@composable
            GroupDetailScreen(groupId = groupId, navController = navController)
        }

        composable(
            route = Screen.TODO_DETAIL,
            arguments = listOf(navArgument("todoId") { type = NavType.StringType }),
        ) { back ->
            val todoId = back.arguments?.getString("todoId") ?: return@composable
            TodoDetailScreen(todoId = todoId, navController = navController)
        }

        composable(
            route = Screen.TASK_EDIT,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("todoId") { type = NavType.StringType },
            ),
        ) { back ->
            val taskId = back.arguments?.getString("taskId")?.ifBlank { null }
            val todoId = back.arguments?.getString("todoId") ?: return@composable
            TaskEditScreen(todoId = todoId, taskId = taskId, navController = navController)
        }

        composable(Screen.SETTINGS) {
            SettingsScreen(navController = navController)
        }

        composable(Screen.SCHEDULES) {
            ScheduleListScreen(navController = navController)
        }

        composable(
            route = Screen.SCHEDULE_EDIT,
            arguments = listOf(
                navArgument("scheduleId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { back ->
            val scheduleId = back.arguments?.getString("scheduleId")?.ifBlank { null }
            ScheduleEditScreen(scheduleId = scheduleId, navController = navController)
        }
    }
}

