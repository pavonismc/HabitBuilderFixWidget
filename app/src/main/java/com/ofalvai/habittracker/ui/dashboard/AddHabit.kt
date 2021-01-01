package com.ofalvai.habittracker.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ofalvai.habittracker.ui.HabitTrackerTheme
import com.ofalvai.habittracker.ui.HabitViewModel
import com.ofalvai.habittracker.ui.model.Habit
import com.ofalvai.habittracker.ui.model.Suggestions

@Composable
fun AddHabitScreen(viewModel: HabitViewModel, navController: NavController) {
    val onSave: (Habit) -> Unit = {
        viewModel.addHabit(it)
        navController.popBackStack()
    }

    Column {
        Suggestions(habits = Suggestions.habits, onSelect = onSave)
        AddHabitForm(onSave)
    }
}

@Composable
fun AddHabitForm(
    onSave: (Habit) -> Unit
) {
    var name by savedInstanceState { "" }
    var color by savedInstanceState { Habit.DEFAULT_COLOR }

    val onSaveClick: () -> Unit = {
        val habit = Habit(
            name = name,
            color = color
        )
        onSave(habit)
    }

    Column(Modifier.fillMaxWidth()) {
        // TODO: keyboard IME actions, focus
        TextField(
            modifier = Modifier.padding(horizontal = 32.dp),
            value = name,
            onValueChange = { name = it },
            label = { Text("Habit name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        HabitColorPicker(onColorPick = { color = it })

        Button(
            modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
            onClick = onSaveClick
        ) {
            Text("Save")
        }
    }
}

@Composable
fun Suggestions(habits: List<Habit>, onSelect: (Habit) -> Unit) {
    Column(
        Modifier.padding(vertical = 32.dp).fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = "Try one of these:",
            style = MaterialTheme.typography.subtitle1
        )

        LazyRow(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            contentPadding = PaddingValues(start = 32.dp, end = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(habits) {
                SuggestionChip(habit = it, onClick = { onSelect(it) })
            }
        }
    }
}

@Composable
fun SuggestionChip(habit: Habit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(percent = 50),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            text = habit.name,
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
fun HabitColorPicker(
    onColorPick: (Habit.Color) -> Unit
) {
    val colors = remember { Habit.Color.values().toList() }
    var selectionIndex by remember { mutableStateOf(0) }

    LazyRow(
        Modifier.padding(vertical = 32.dp).fillMaxWidth(),
        contentPadding = PaddingValues(start = 32.dp, end = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(colors) {
            HabitColor(
                color = it,
                isSelected = selectionIndex == colors.indexOf(it),
                onClick = {
                    selectionIndex = colors.indexOf(it)
                    onColorPick(it)
                }
            )
        }
    }
}

@Composable
fun HabitColor(color: Habit.Color, isSelected: Boolean, onClick: () -> Unit) {
    val size = if (isSelected) 56.dp else 48.dp
    val padding = if (isSelected) {
        PaddingValues(all = 0.dp)
    } else {
        PaddingValues(start = 4.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)
    }

    Surface(
        modifier = Modifier
            .padding(padding)
            .size(size)
            .clickable(onClick = onClick, indication = rememberRipple(radius = size / 2)),
        shape = CircleShape,
        color = color.composeColor,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.15f))
    ) {
        if (isSelected) {
            Icon(Icons.Filled.Check, tint = Color.Black.copy(alpha = 0.75f))
        }
    }
}

@Composable
@Preview(showBackground = true, widthDp = 400, backgroundColor = 0xFFFDEDCE)
fun PreviewAddHabit() {
    HabitTrackerTheme {
        Column {
            Suggestions(habits = Suggestions.habits, onSelect = { })

            AddHabitForm(onSave = { })
        }
    }
}