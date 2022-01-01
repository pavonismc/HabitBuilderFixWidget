/*
 * Copyright 2022 Olivér Falvai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ofalvai.habittracker.ui.habitdetail

import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ofalvai.habittracker.Dependencies
import com.ofalvai.habittracker.R
import com.ofalvai.habittracker.ui.common.*
import com.ofalvai.habittracker.ui.dashboard.view.VIBRATE_PATTERN_TOGGLE
import com.ofalvai.habittracker.ui.dashboard.view.vibrateCompat
import com.ofalvai.habittracker.ui.model.*
import com.ofalvai.habittracker.ui.theme.HabitTrackerTheme
import com.ofalvai.habittracker.ui.theme.composeColor
import kotlinx.coroutines.cancel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.*

@Composable
fun HabitDetailScreen(habitId: Int, navController: NavController) {
    val viewModel: HabitDetailViewModel = viewModel(factory = Dependencies.viewModelFactory)
    val vibrator = LocalContext.current.getSystemService<Vibrator>()!!

    val habitDetailState by viewModel.habitWithActions.collectAsState()

    viewModel.habitDetailEvent.asEffect {
        when (it) {
            HabitDetailEvent.BackNavigation -> navController.popBackStack()
        }
    }

    val singleStats by viewModel.singleStats.collectAsState()
    val chartData by viewModel.chartData.collectAsState()

    DisposableEffect(habitId) {
        val job = viewModel.fetchHabitDetails(habitId)
        onDispose { job.cancel("Cancelled by Composable") }
    }
    DisposableEffect(habitId) {
        val job = viewModel.fetchHabitStats(habitId)
        onDispose { job.cancel("Cancelled by Composable") }
    }

    val onDayToggle: (LocalDate, Action) -> Unit = { date, action ->
        vibrator.vibrateCompat(VIBRATE_PATTERN_TOGGLE)
        viewModel.toggleActionFromDetail(habitId, action, date)
    }

    var showArchiveDialog by remember { mutableStateOf(false) }
    var pendingHabitToArchive by remember { mutableStateOf<Habit?>(null) }
    val onArchive: (Habit) -> Unit = {
        showArchiveDialog = true
        pendingHabitToArchive = it
    }

    ConfirmationDialog(
        showDialog = showArchiveDialog,
        title = stringResource(R.string.habitdetails_archive_title),
        description = stringResource(R.string.habitdetails_archive_description),
        confirmText = stringResource(R.string.habitdetails_archive_confirm),
        onDismiss = { showArchiveDialog = false },
        onConfirm = {
            pendingHabitToArchive?.let { viewModel.archiveHabit(it) }
            pendingHabitToArchive = null
            showArchiveDialog = false
        }
    )

    HabitDetailScreen(
        habitDetailState = habitDetailState,
        singleStats = singleStats,
        chartData = chartData,
        onChartTypeChange = { viewModel.switchChartType(it) },
        onBack = { navController.popBackStack() },
        onEdit = { viewModel.updateHabit(it) },
        onArchive = onArchive,
        onDayToggle = onDayToggle
    )
}

@Composable
private fun HabitDetailScreen(
    habitDetailState: Result<HabitWithActions>,
    singleStats: SingleStats,
    chartData: Result<ActionCountChart>,
    onChartTypeChange: (ActionCountChart.Type) -> Unit,
    onBack: () -> Unit,
    onEdit: (Habit) -> Unit,
    onArchive: (Habit) -> Unit,
    onDayToggle: (LocalDate, Action) -> Unit,
) {
    Column {
        HabitDetailHeader(habitDetailState, singleStats, onBack, onEdit, onArchive)

        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            when (habitDetailState) {
                is Result.Success -> Calendar(habitDetailState, onDayToggle)
                Result.Loading -> {
                    // No calendar and stats in loading state
                }
                is Result.Failure -> {
                    ErrorView(
                        label = stringResource(R.string.habitdetails_error_stats),
                        modifier = Modifier.padding(top = 64.dp)
                    )
                }
            }

            when (chartData) {
                is Result.Success -> HabitStats(chartData.value, onChartTypeChange)
                is Result.Failure -> ErrorView(
                    label = stringResource(R.string.habitdetails_error_stats),
                    modifier = Modifier.padding(top = 16.dp)
                )
                Result.Loading -> {
                    // No chart in loading state
                }
            }
        }
    }
}

@Composable
private fun Calendar(
    habitDetailState: Result.Success<HabitWithActions>,
    onDayToggle: (LocalDate, Action) -> Unit
) {
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    Column(
        Modifier.cardBackground().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
    ) {
        CalendarPager(
            yearMonth = yearMonth,
            onPreviousClick = { yearMonth = yearMonth.minusMonths(1) },
            onNextClick = { yearMonth = yearMonth.plusMonths(1) }
        )
        CalendarDayLegend(weekFields = WeekFields.of(Locale.getDefault()))
        HabitCalendar(
            yearMonth = yearMonth,
            habitColor = habitDetailState.value.habit.color.composeColor,
            actions = habitDetailState.value.actions,
            onDayToggle = onDayToggle
        )
    }
}

@Composable
private fun HabitStats(
    chartData: ActionCountChart,
    onStatTypeChange: (ActionCountChart.Type) -> Unit
) {
    Column(Modifier.padding(top = 16.dp).cardBackground()) {
        Row(Modifier.align(Alignment.End).padding(top = 8.dp, end = 16.dp)) {
            Text(
                modifier = Modifier.align(CenterVertically),
                text = stringResource(R.string.habitdetails_actioncount_selector_label),
                style = MaterialTheme.typography.body2
            )
            Spacer(Modifier.width(8.dp))
            ToggleButton(
                checked = chartData.type == ActionCountChart.Type.Weekly,
                onCheckedChange = { onStatTypeChange(chartData.type.invert()) }
            ) {
                Text(text = stringResource(R.string.habitdetails_actioncount_selector_weekly))
            }
            Spacer(Modifier.width(8.dp))
            ToggleButton(
                checked = chartData.type == ActionCountChart.Type.Monthly,
                onCheckedChange = { onStatTypeChange(chartData.type.invert()) }
            ) {
                Text(text = stringResource(R.string.habitdetails_actioncount_selector_monthly))
            }

        }
        ActionCountChart(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            values = chartData.items
        )
    }
}


@Composable
private fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    if (checked) {
        Button(onClick = { onCheckedChange(!checked) }) { content() }
    } else {
        OutlinedButton(onClick = { onCheckedChange(!checked) }) { content() }
    }
}

private fun Modifier.cardBackground() = composed {
    this.background(Color.White.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium)
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun PreviewHabitDetailScreen() {
    HabitTrackerTheme {
        HabitDetailScreen(
            habitDetailState = Result.Success(
                HabitWithActions(
                    Habit(0, "Meditation", Habit.Color.Red, ""),
                    listOf(Action(0, true, Instant.now())),
                    2,
                    ActionHistory.Clean
                )
            ),
            singleStats = SingleStats(LocalDate.now(), 2, 1, 0.15f),
            chartData = Result.Success(ActionCountChart(emptyList(), ActionCountChart.Type.Weekly)),
            onChartTypeChange = {},
            onBack = { },
            onEdit = { },
            onArchive = { },
            onDayToggle = { _, _ -> }
        )
    }
}