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

package com.ofalvai.habittracker.ui.dashboard.view.fiveday

import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.getSystemService
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.ofalvai.habittracker.R
import com.ofalvai.habittracker.core.model.Action
import com.ofalvai.habittracker.core.model.ActionHistory
import com.ofalvai.habittracker.core.model.Habit
import com.ofalvai.habittracker.core.ui.theme.AppTextStyle
import com.ofalvai.habittracker.core.ui.theme.PreviewTheme
import com.ofalvai.habittracker.core.ui.theme.composeColor
import com.ofalvai.habittracker.core.ui.theme.gray2
import com.ofalvai.habittracker.ui.dashboard.view.satisfyingToggleable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import kotlin.random.Random
import com.ofalvai.habittracker.core.ui.R as coreR

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HabitCard(
    habit: Habit,
    actions: ImmutableList<Action>,
    totalActionCount: Int,
    actionHistory: ActionHistory,
    onActionToggle: (Action, Habit, Int) -> Unit,
    onDetailClick: (Habit) -> Unit,
    dragOffset: Float,
    modifier: Modifier = Modifier
) {
    val isDragging = dragOffset != 0f
    val backgroundColor = if (isDragging) {
        MaterialTheme.colors.surface
    } else {
        habit.color.composeColor.copy(alpha = 0.1f)
    }
    Card(
        onClick = { onDetailClick(habit) },
        elevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = backgroundColor,
        border = if (isDragging) BorderStroke(1.dp, habit.color.composeColor) else null,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .draggableCard(dragOffset),
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = habit.name,
                style = AppTextStyle.habitSubtitle
            )

            ActionHistoryLabel(totalActionCount, actionHistory)

            ActionCircles(
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                actions = actions.takeLast(Constants.DayCount).toImmutableList(),
                habitColor = habit.color,
                onActionToggle = { action, dayIndex ->
                    val daysInPast = Constants.DayCount - 1 - dayIndex
                    onActionToggle(action, habit, daysInPast)
                }
            )
        }
    }
}

@Composable
fun ActionCircles(
    modifier: Modifier,
    actions: ImmutableList<Action>,
    habitColor: Habit.Color,
    onActionToggle: (Action, Int) -> Unit
) {
    var singlePressCounter by remember { mutableStateOf(0) }

    Column(modifier) {
        Row {
            actions.mapIndexed { index, action ->
                ActionCircle(
                    activeColor = habitColor.composeColor,
                    toggled = action.toggled,
                    onToggle = { newState ->
                        singlePressCounter = 0
                        onActionToggle(
                            action.copy(toggled = newState),
                            index
                        )
                    },
                    isHighlighted = index == actions.size - 1,
                    onSinglePress = { singlePressCounter++ }
                )
            }
        }
        if (singlePressCounter >= 2) {
            Text(
                modifier = Modifier.align(Alignment.End),
                text = stringResource(R.string.dashboard_toggle_help),
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ActionCircle(
    activeColor: Color,
    toggled: Boolean,
    onToggle: (Boolean) -> Unit,
    isHighlighted: Boolean,
    onSinglePress: () -> Unit
) {
    AnimatedContent(
        targetState = toggled,
        transitionSpec = {
            (scaleIn(
                animationSpec = tween(250, delayMillis = 250, easing = CubicBezierEasing(0.46f, 1.83f, 0.64f, 1f))
            ) with scaleOut(
                animationSpec = tween(250)
            )).using(SizeTransform(clip = false))
        }
    ) { targetToggled ->
        val backgroundColor = if (targetToggled) activeColor else MaterialTheme.colors.surface
        val borderColor = if (targetToggled) MaterialTheme.colors.gray2 else activeColor
        val vibrator = LocalContext.current.getSystemService<Vibrator>()!!
        val rippleRadius =
            remember { Constants.CircleSize / 1.7f } // Make it a bit bigger than D / 2
        val shape = CircleShape
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Constants.CircleSize)
                .padding(Constants.CirclePadding)
                .satisfyingToggleable(
                    vibrator, rippleRadius, false, targetToggled, onToggle, onSinglePress
                )
                .border(BorderStroke(2.dp, borderColor), shape)
                .background(backgroundColor, shape)
                .clip(shape)
        ) {
            if (isHighlighted) {
                Box(
                    modifier = Modifier
                        .requiredSize(8.dp)
                        .clip(CircleShape)
                        .background(color = borderColor, shape = CircleShape),
                )
            }
        }
    }
}

@Composable
fun ActionHistoryLabel(totalActionCount: Int, actionHistory: ActionHistory) {
    val resources = LocalContext.current.resources
    val totalLabel = resources.getQuantityString(
        R.plurals.common_action_count_total,
        totalActionCount, totalActionCount
    )
    val actionHistoryLabel = when (actionHistory) {
        ActionHistory.Clean -> stringResource(R.string.common_action_count_clean)
        is ActionHistory.MissedDays -> resources.getQuantityString(
            R.plurals.common_action_count_missed_days, actionHistory.days, actionHistory.days
        )
        is ActionHistory.Streak -> resources.getQuantityString(
            R.plurals.common_action_count_streak, actionHistory.days, actionHistory.days
        )
    }
    val mergedLabel = stringResource(coreR.string.common_interpunct, totalLabel, actionHistoryLabel)

    Text(
        text = mergedLabel,
        style = MaterialTheme.typography.caption
    )
}

private fun Modifier.draggableCard(
    offset: Float
): Modifier = this
    .zIndex(if (offset == 0f) 0f else 1f)
    .graphicsLayer {
        translationY = if (offset == 0f) 0f else offset
        rotationZ = if (offset == 0f) 0f else -2f
    }


@Preview
@ShowkaseComposable(name = "Fiveday layout", group = "Dashboard")
@Composable
fun PreviewHabitCard() {
    val habit1 = Habit(
        id = 1,
        name = "Meditation",
        color = Habit.Color.Yellow,
        notes = ""
    )
    val habit2 = Habit(
        id = 2,
        name = "Workout",
        color = Habit.Color.Green,
        notes = ""
    )

    val actions1 = (1..5).map {
        Action(
            id = it,
            toggled = Random.nextBoolean(),
            timestamp = Instant.now()
        )
    }
    val actions2 = actions1.shuffled()

    PreviewTheme {
        Column(Modifier.padding(16.dp)) {
            HabitCard(habit1, actions1.toImmutableList(), 14, ActionHistory.Clean, { _, _, _ -> }, {}, 0f)
            Spacer(modifier = Modifier.height(16.dp))
            HabitCard(habit2, actions2.toImmutableList(), 3, ActionHistory.Streak(3), { _, _, _ -> }, {}, 0f)
        }
    }
}