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

package com.ofalvai.habittracker.ui.insights.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ofalvai.habittracker.R
import com.ofalvai.habittracker.ui.Destination
import com.ofalvai.habittracker.ui.common.ErrorView
import com.ofalvai.habittracker.ui.common.Result
import com.ofalvai.habittracker.ui.insights.InsightsViewModel
import com.ofalvai.habittracker.ui.model.HabitId
import com.ofalvai.habittracker.ui.model.TopDayItem
import com.ofalvai.habittracker.ui.theme.AppIcons
import com.ofalvai.habittracker.ui.theme.HabitTrackerTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*

@Composable
fun TopDays(viewModel: InsightsViewModel, navController: NavController) {
    val topDays by viewModel.habitTopDays.collectAsState()

    val onHabitClick: (HabitId) -> Unit = {
        val route = Destination.HabitDetails.buildRoute(habitId = it)
        navController.navigate(route)
    }

    InsightCard(
        iconPainter = AppIcons.TopDays,
        title = stringResource(R.string.insights_topdays_title),
        description = stringResource(R.string.insights_topdays_description),
    ) {
        Crossfade(targetState = topDays) {
            when (it) {
                is Result.Success -> {
                    if (hasEnoughData(it.value)) {
                        TopDaysTable(items = it.value, onHabitClick = onHabitClick)
                    } else {
                        EmptyView(label = stringResource(R.string.insights_topdays_empty_label))
                    }
                }
                Result.Loading -> Spacer(modifier = Modifier.height(45.dp))
                is Result.Failure -> {
                    ErrorView(label = stringResource(R.string.insights_topdays_error))
                }
            }
        }
    }
}

@Composable
private fun TopDaysTable(
    items: List<TopDayItem>,
    onHabitClick: (HabitId) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach {
            TopDaysRow(item = it, onClick = onHabitClick)
        }
    }
}

@Composable
private fun TopDaysRow(
    item: TopDayItem,
    onClick: (HabitId) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.name,
            modifier = Modifier.weight(0.50f).padding(start = 4.dp),
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            style = MaterialTheme.typography.body1,
        )

        Text(
            text = item.day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            modifier = Modifier.weight(0.30f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )

        Spacer(Modifier.width(16.dp))

        Text(
            text = item.count.toString(),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier
        )

        IconButton(onClick = { onClick(item.habitId) }) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(
                    R.string.insights_tophabits_navigate,
                    item.name
                )
            )
        }
    }
}

private fun hasEnoughData(days: List<TopDayItem>): Boolean {
    return days.any { it.count >= 2 }
}

@Preview(showBackground = true, widthDp = 300, backgroundColor = 0xFFFDEDCE)
@Composable
private fun PreviewTopDaysTable() {
    val topDays = listOf(
        TopDayItem(
            habitId = 1,
            name = "Short name",
            count = 1567,
            day = DayOfWeek.MONDAY
        ),
        TopDayItem(
            habitId = 1,
            name = "Name",
            count = 153,
            day = DayOfWeek.THURSDAY
        ),
        TopDayItem(
            habitId = 1,
            name = "Loooong name lorem ipsum dolor sit amet",
            count = 10,
            day = DayOfWeek.SUNDAY
        ),
        TopDayItem(
            habitId = 1,
            name = "Meditation",
            count = 9,
            day = DayOfWeek.WEDNESDAY
        ),
        TopDayItem(
            habitId = 1,
            name = "Workout",
            count = 3,
            day = DayOfWeek.SATURDAY
        )
    )

    HabitTrackerTheme {
        TopDaysTable(
            items = topDays,
            onHabitClick = {  }
        )
    }
}