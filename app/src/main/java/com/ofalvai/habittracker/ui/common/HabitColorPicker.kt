package com.ofalvai.habittracker.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ofalvai.habittracker.ui.model.Habit
import com.ofalvai.habittracker.ui.theme.HabitTrackerTheme
import com.ofalvai.habittracker.ui.theme.composeColor

enum class ColorPickerState { Default, Selected }

@Composable
fun HabitColorPicker(
    initialColor: Habit.Color,
    onColorPick: (Habit.Color) -> Unit
) {
    val colors = remember { Habit.Color.values().toList() }

    // Note: state is duplicated to make color picking responsive:
    // - UI is wired to this local state (otherwise UI would only update after the source of truth (DB) is updated)
    // - But remember() is invalidated if the outside state (source of truth) changes
    var color by remember(initialColor) { mutableStateOf(initialColor) }

    LazyRow(
        Modifier.padding(vertical = 32.dp).fillMaxWidth(),
        contentPadding = PaddingValues(start = 32.dp, end = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(colors) {
            val isSelected = it == color
            val transition: Transition<ColorPickerState> = updateTransition(
                targetState = if (isSelected) ColorPickerState.Selected else ColorPickerState.Default,
                label = "ColorPickerTransition"
            )

            HabitColor(
                color = it,
                isSelected = isSelected,
                transition = transition,
                onClick = {
                    color = it
                    onColorPick(it)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HabitColor(
    color: Habit.Color,
    isSelected: Boolean,
    transition: Transition<ColorPickerState>,
    onClick: () -> Unit
) {
    val animationSpec = tween<Dp>(easing = LinearOutSlowInEasing, durationMillis = 250)
    val size: Dp by transition.animateDp(
        transitionSpec = { animationSpec }, label = "ColorPickerSize"
    ) { state ->
        when (state) {
            ColorPickerState.Default -> 48.dp
            ColorPickerState.Selected -> 56.dp
        }
    }
    val horizontalPadding: Dp by transition.animateDp(
        transitionSpec = { animationSpec }, label = "ColorPickerHorizontalPadding"
    ) { state ->
        when (state) {
            ColorPickerState.Default -> 4.dp
            ColorPickerState.Selected -> 0.dp
        }
    }
    val verticalPadding: Dp by transition.animateDp(
        transitionSpec = { animationSpec }, label = "ColorPickerVerticalPadding"
    ) { state ->
        when (state) {
            ColorPickerState.Default -> 8.dp
            ColorPickerState.Selected -> 0.dp
        }
    }

    Box(
        modifier = Modifier
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .size(size)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(radius = size / 2, bounded = false)
            )
            .border(BorderStroke(1.dp, Color.Black.copy(alpha = 0.15f)), CircleShape)
            .background(color.composeColor, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                tint = Color.Black.copy(alpha = 0.75f),
                contentDescription = null,
                modifier = Modifier.requiredSize(24.dp)
            )
        }
    }
}

@Composable
@Preview(showBackground = true, widthDp = 400, backgroundColor = 0xFFFDEDCE)
fun PreviewHabitColorPicker() {
    HabitTrackerTheme {
        HabitColorPicker(initialColor = Habit.Color.Green, onColorPick = { })
    }
}