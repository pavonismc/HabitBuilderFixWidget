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

package com.ofalvai.habittracker.ui.dashboard.view

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.ofalvai.habittracker.R
import com.ofalvai.habittracker.ui.dashboard.ItemMoveEvent
import com.ofalvai.habittracker.ui.model.HabitId
import com.ofalvai.habittracker.ui.model.HabitWithActions
import com.ofalvai.habittracker.ui.theme.HabitTrackerTheme
import org.burnoutcrew.reorderable.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun ReorderableHabitList(
    habits: List<HabitWithActions>,
    verticalArrangement: Arrangement.Vertical,
    onMove: (ItemMoveEvent) -> Unit,
    onAddHabitClick: () -> Unit,
    itemContent: @Composable LazyListScope.(HabitWithActions, ReorderableState) -> Unit
) {
    val vibrator = LocalContext.current.getSystemService<Vibrator>()!!

    // An in-memory copy of the Habit list to make drag reorder a bit smoother (not perfect).
    // We update the in-memory list on every move (of distance 1), then persist to DB in the
    // background. The cache key is the original list so that any change (eg. action completion)
    // is reflected in the in-memory copy.
    val inMemoryList = remember(habits) { habits.toMutableStateList() }
    val reorderState = rememberReorderState()
    val onItemMove: (fromPos: ItemPosition, toPos: ItemPosition) -> (Unit) = { from, to ->
        vibrator.vibrateCompat(longArrayOf(0, 50))
        inMemoryList.move(from.index, to.index)
        onMove(ItemMoveEvent(from.key as HabitId, to.key as HabitId))
    }
    val canDragOver: (index: ItemPosition) -> Boolean = {
        // Last item of the list is the fixed CreateHabitButton, it's not reorderable
        it.index < inMemoryList.size
    }

    LazyColumn(
        state = reorderState.listState,
        contentPadding = PaddingValues(bottom = 48.dp),
        verticalArrangement = verticalArrangement,
        modifier = Modifier.reorderable(reorderState, onItemMove, canDragOver)
    ) {
        items(inMemoryList, key = { it.habit.id }) { item ->
            this@LazyColumn.itemContent(item, reorderState)
        }
        item {
            CreateHabitButton(onClick = onAddHabitClick)
        }
    }
}

@Composable
private fun CreateHabitButton(
    onClick: () -> Unit
) {
    Box(Modifier.fillMaxWidth().wrapContentWidth()) {
        OutlinedButton(
            modifier = Modifier.padding(16.dp),
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = MaterialTheme.colors.background)
        ) {
            Icon(Icons.Rounded.Add, null, Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.dashboard_create_habit))
        }
    }
}

@Composable
fun DayLegend(
    modifier: Modifier = Modifier,
    mostRecentDay: LocalDate,
    pastDayCount: Int
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        (pastDayCount downTo 0).map {
            DayLabel(day = mostRecentDay.minusDays(it.toLong()))
        }
    }
}

@Composable
private fun DayLabel(
    day: LocalDate,
) {
    val modifier = Modifier
        .wrapContentHeight(Alignment.Top)
        .padding(vertical = 8.dp)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = day.dayOfMonth.toString(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption
        )
        Text(
            text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold)
        )
    }
}

val VIBRATE_PATTERN_TOGGLE = longArrayOf(0, 75, 50, 75)

fun Modifier.satisfyingToggleable(
    vibrator: Vibrator,
    rippleRadius: Dp,
    rippleBounded: Boolean,
    toggled: Boolean,
    onToggle: (Boolean) -> Unit,
    onSinglePress: () -> Unit
): Modifier {
    return composed {
        // Using `toggled` as the cache key, otherwise the recomposition in the long press
        // would leave the InteractionSource in the pressed state
        val interactionSource = remember(toggled) { MutableInteractionSource() }
        var isSinglePress by remember { mutableStateOf(false) }

        this
            .pointerInput(key1 = toggled) {
                detectTapGestures(
                    onPress = {
                        isSinglePress = true

                        vibrator.vibrateCompat(longArrayOf(0, 50))
                        val press = PressInteraction.Press(it)
                        interactionSource.emit(press)

                        val released = tryAwaitRelease()

                        if (isSinglePress) {
                            onSinglePress()
                        }
                        isSinglePress = false

                        val endInteraction = if (released) {
                            PressInteraction.Release(press)
                        } else {
                            PressInteraction.Cancel(press)
                        }
                        interactionSource.emit(endInteraction)
                    },
                    onLongPress = {
                        isSinglePress = false
                        vibrator.vibrateCompat(VIBRATE_PATTERN_TOGGLE)
                        onToggle(!toggled)
                    }
                )
            }
            .indication(interactionSource, rememberRipple(radius = rippleRadius, bounded = rippleBounded))
    }
}

fun Vibrator.vibrateCompat(timings: LongArray, repeat: Int = -1) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrate(VibrationEffect.createWaveform(timings, repeat))
    } else {
        @Suppress("DEPRECATION")
        vibrate(timings, repeat)
    }
}

@Preview(showBackground = true, widthDp = 400, backgroundColor = 0xFFFDEDCE)
@Composable
private fun PreviewDayLabels() {
    HabitTrackerTheme {
        DayLegend(
            modifier = Modifier.padding(horizontal = 16.dp),
            mostRecentDay = LocalDate.now(),
            pastDayCount = 4
        )
    }
}