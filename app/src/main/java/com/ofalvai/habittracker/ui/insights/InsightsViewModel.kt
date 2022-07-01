/*
 * Copyright 2021 Olivér Falvai
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

package com.ofalvai.habittracker.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ofalvai.habittracker.core.database.HabitDao
import com.ofalvai.habittracker.core.ui.state.Result
import com.ofalvai.habittracker.mapper.mapHabitActionCount
import com.ofalvai.habittracker.mapper.mapHabitTopDay
import com.ofalvai.habittracker.mapper.mapSumActionCountByDay
import com.ofalvai.habittracker.telemetry.Telemetry
import com.ofalvai.habittracker.ui.dashboard.OnboardingManager
import com.ofalvai.habittracker.ui.model.HeatmapMonth
import com.ofalvai.habittracker.ui.model.TopDayItem
import com.ofalvai.habittracker.ui.model.TopHabitItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class InsightsViewModel(
    private val habitDao: HabitDao,
    private val telemetry: Telemetry,
    onboardingManager: OnboardingManager
): ViewModel() {

    val heatmapState = MutableStateFlow<Result<HeatmapMonth>>(Result.Loading)
    val topHabits = MutableStateFlow<Result<List<TopHabitItem>>>(Result.Loading)
    val habitTopDays = MutableStateFlow<Result<List<TopDayItem>>>(Result.Loading)

    private val habitCount: SharedFlow<Int> = habitDao.getTotalHabitCount().shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        replay = 1
    )

    init {
        fetchStats()
        onboardingManager.insightsOpened()
    }

    fun fetchHeatmap(yearMonth: YearMonth) {
        viewModelScope.launch {
            reloadHeatmap(yearMonth)
        }
    }

    private fun fetchStats() {
        viewModelScope.launch {
            val deferreds = listOf(
                async { reloadHeatmap(yearMonth = YearMonth.now()) },
                async { reloadTopHabits() },
                async { reloadHabitTopDays() },
            )
            deferreds.awaitAll()
        }
    }

    private suspend fun reloadHeatmap(yearMonth: YearMonth) {
        try {
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()
            val actionCountList = habitDao.getSumActionCountByDay(startDate, endDate)
            val habitCount = habitCount.first()

            heatmapState.value = Result.Success(
                mapSumActionCountByDay(
                    entityList = actionCountList,
                    yearMonth,
                    habitCount
                )
            )
        } catch (e: Throwable) {
            telemetry.logNonFatal(e)
            heatmapState.value = Result.Failure(e)
        }
    }

    private suspend fun reloadTopHabits() {
        try {
            topHabits.value = habitDao
                .getMostSuccessfulHabits(100) // TODO: smaller number when "See all" screen is done
                .filter { it.first_day != null }
                .map { mapHabitActionCount(it, LocalDate.now()) }
                .let { Result.Success(it) }
        } catch (e: Throwable) {
            telemetry.logNonFatal(e)
            topHabits.value = Result.Failure(e)
        }
    }

    private suspend fun reloadHabitTopDays() {
        try {
            habitTopDays.value = habitDao
                .getTopDayForHabits()
                .map { mapHabitTopDay(it, Locale.getDefault()) }
                .let { Result.Success(it) }
        } catch (e: Throwable) {
            telemetry.logNonFatal(e)
            habitTopDays.value = Result.Failure(e)
        }
    }
}