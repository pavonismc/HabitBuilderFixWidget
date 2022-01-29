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

package com.ofalvai.habittracker

import com.ofalvai.habittracker.persistence.HabitDao
import com.ofalvai.habittracker.repo.ActionRepository
import com.ofalvai.habittracker.ui.model.Action
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import com.ofalvai.habittracker.persistence.entity.Action as ActionEntity

@ExperimentalCoroutinesApi
class ActionRepositoryTest {

    private val dao = mock<HabitDao>()

    @Test
    fun `Given empty habit When action is created Then inserted action date is correct`() = runTest {
        // Given
        val repo = givenRepo()
        val now = Instant.now()
        val today = LocalDate.now()

        // When
        repo.toggleAction(
            habitId = 0,
            updatedAction = Action(0, true, now),
            date = today
        )

        // Then
        // Mockito doesn't support capturing vararg methods correctly
        val argument = argumentCaptor<Any>()
        verify(dao).insertAction(argument.capture() as ActionEntity)
        @Suppress("UNCHECKED_CAST")
        val insertedAction = (argument.allValues[0] as Array<ActionEntity>)[0]
        assertEquals(0, insertedAction.habit_id)
        assertEquals(0, insertedAction.id)
        val actionDateTime = LocalDateTime.ofInstant(
            insertedAction.timestamp, ZoneId.systemDefault()
        )
        assertEquals(today, actionDateTime.toLocalDate())
    }

    @Test
    fun `Given habit with action When action is removed Then inserted action date is correct`() = runTest {
        // Given
        val repo = givenRepo()
        val now = Instant.now()
        val today = LocalDate.now()

        // When
        repo.toggleAction(
            habitId = 0,
            updatedAction = Action(1, false, now),
            date = today
        )

        // Then
        verify(dao).deleteAction(1)

    }

    private fun givenRepo() = ActionRepository(dao)
}