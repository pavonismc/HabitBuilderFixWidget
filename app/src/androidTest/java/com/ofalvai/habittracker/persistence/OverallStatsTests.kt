package com.ofalvai.habittracker.persistence

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ofalvai.habittracker.persistence.entity.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class OverallStatsTests {

    private object TestData {
        val habit1 = Habit(id = 875, name = "Meditation", color = Habit.Color.Green)
        val habit2 = Habit(id = 876, name = "Drinking enough water", color = Habit.Color.Green)
        val habit3 = Habit(id = 877, name = "Workout", color = Habit.Color.Green)
        val habit4 = Habit(id = 878, name = "Habit I never do", color = Habit.Color.Yellow)
        val habit5 = Habit(id = 879, name = "Habit I do mostly on Friday", color = Habit.Color.Blue)
        val habits = arrayOf(habit1, habit2, habit3, habit4, habit5)

        val actions = arrayOf(
            Action(habit_id = habit1.id, timestamp = Instant.parse("2019-12-23T18:16:30Z")),// Mon
            Action(habit_id = habit1.id, timestamp = Instant.parse("2020-12-23T18:16:30Z")),// Wed
            Action(habit_id = habit1.id, timestamp = Instant.parse("2020-12-24T18:16:40Z")),// Thu
            Action(habit_id = habit2.id, timestamp = Instant.parse("2020-12-23T10:18:42Z")),// Wed
            Action(habit_id = habit3.id, timestamp = Instant.parse("2020-12-23T10:19:10Z")),// Wed
            Action(habit_id = habit1.id, timestamp = Instant.parse("2020-12-31T08:59:00Z")),// Thu
            Action(habit_id = habit1.id, timestamp = Instant.parse("2021-01-01T11:56:10Z")),// Fri
            Action(habit_id = habit1.id, timestamp = Instant.parse("2021-01-04T10:28:10Z")),// Mon
            Action(habit_id = habit1.id, timestamp = Instant.parse("2021-03-29T10:28:10Z")),// Mon
            Action(habit_id = habit5.id, timestamp = Instant.parse("2021-03-26T20:00:00Z")),// Fri
            Action(habit_id = habit5.id, timestamp = Instant.parse("2021-03-27T20:00:00Z")),// Sat
            Action(habit_id = habit5.id, timestamp = Instant.parse("2021-03-28T20:00:00Z")),// Sun
            Action(habit_id = habit5.id, timestamp = Instant.parse("2021-04-02T20:00:00Z")),// Fri
            Action(habit_id = habit5.id, timestamp = Instant.parse("2021-04-09T20:00:00Z")),// Fri
            Action(habit_id = habit5.id, timestamp = Instant.parse("2021-04-15T20:00:00Z")),// Thu
            Action(habit_id = habit5.id, timestamp = Instant.parse("2021-04-16T20:00:00Z")),// Fri
        )
    }

    private lateinit var habitDao: HabitDao
    private lateinit var db: AppDatabase
    private val testCoroutineScope = TestCoroutineScope()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun before() = testCoroutineScope.runBlockingTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        habitDao = db.habitDao()
    }

    @After
    @Throws(IOException::class)
    fun after() {
        db.close()
    }

    @Test
    fun testActionCountForHabits() = testCoroutineScope.runBlockingTest {
        // Given
        habitDao.insertHabit(*TestData.habits)
        habitDao.insertAction(*TestData.actions)

        // When
        val actionCounts = habitDao.getSumActionCountByDay(
            from = LocalDate.of(2019, 12, 1),
            to = LocalDate.of(2021, 4, 30)
        )

        // Then
        val expected = listOf(
            SumActionCountByDay(LocalDate.of(2019, 12, 23), action_count = 1),
            SumActionCountByDay(LocalDate.of(2020, 12, 23), action_count = 3),
            SumActionCountByDay(LocalDate.of(2020, 12, 24), action_count = 1),
            SumActionCountByDay(LocalDate.of(2020, 12, 31), action_count = 1),
            SumActionCountByDay(LocalDate.of(2021, 1, 1), action_count = 1),
            SumActionCountByDay(LocalDate.of(2021, 1, 4), action_count = 1),
            SumActionCountByDay(LocalDate.of(2021, 3, 26), action_count = 1),
            SumActionCountByDay(LocalDate.of(2021, 3, 27), action_count = 1),
            SumActionCountByDay(LocalDate.of(2021, 3, 28), action_count = 1),
            SumActionCountByDay(LocalDate.of(2021, 3, 29), action_count = 1),
            SumActionCountByDay(LocalDate.of(2021, 4, 2), action_count = 1),
            SumActionCountByDay(LocalDate.of(2021, 4, 9), action_count = 1),
            SumActionCountByDay(LocalDate.of(2021, 4, 15), action_count = 1),
            SumActionCountByDay(LocalDate.of(2021, 4, 16), action_count = 1),
        )
        assertEquals(expected, actionCounts)
    }

    @Test
    fun testActionCountForHabitsForOneMonth() = testCoroutineScope.runBlockingTest {
        // Given
        habitDao.insertHabit(*TestData.habits)
        habitDao.insertAction(*TestData.actions)

        // When
        val actionCounts = habitDao.getSumActionCountByDay(
            from = LocalDate.of(2021, 1, 1),
            to = LocalDate.of(2021, 1, 31)
        )

        // Then
        val expected = listOf(
            SumActionCountByDay(LocalDate.of(2021, 1, 1), action_count = 1),
            SumActionCountByDay(LocalDate.of(2021, 1, 4), action_count = 1),
        )
        assertEquals(expected, actionCounts)
    }

    @Test
    fun testActionCountForEmptyHabits() = testCoroutineScope.runBlockingTest {
        // Given

        // When
        val actionCounts = habitDao.getSumActionCountByDay(from = LocalDate.now(), to = LocalDate.now().plusDays(1))

        // Then
        assertEquals(emptyList<SumActionCountByDay>(), actionCounts)
    }

    @Test
    fun testMostSuccessfulHabits() = testCoroutineScope.runBlockingTest {
        // Given
        habitDao.insertHabit(*TestData.habits)
        habitDao.insertAction(*TestData.actions)

        // When
        val mostSuccessfulHabits = habitDao.getMostSuccessfulHabits(5)

        // Then
        val expected = listOf(
            HabitActionCount(
                TestData.habit1.id,
                TestData.habit1.name,
                first_day = LocalDate.of(2019, 12, 23),
                count = 7
            ),
            HabitActionCount(
                TestData.habit5.id,
                TestData.habit5.name,
                first_day = LocalDate.of(2021, 3, 26),
                count = 7
            ),
            HabitActionCount(
                TestData.habit2.id,
                TestData.habit2.name,
                first_day = LocalDate.of(2020, 12, 23),
                count = 1
            ),
            HabitActionCount(
                TestData.habit3.id,
                TestData.habit3.name,
                first_day = LocalDate.of(2020, 12, 23),
                count = 1
            ),
            HabitActionCount(
                TestData.habit4.id,
                TestData.habit4.name,
                first_day = null,
                count = 0
            )
        )
        assertEquals(expected, mostSuccessfulHabits)
    }

    @Test
    fun testMostSuccessfulEmptyHabits() = testCoroutineScope.runBlockingTest {
        // Given
        habitDao.insertHabit(*TestData.habits)

        // When
        val mostSuccessfulHabits = habitDao.getMostSuccessfulHabits(5)

        // Then
        val expected = listOf(
            HabitActionCount(
                TestData.habit1.id,
                TestData.habit1.name,
                first_day = null,
                count = 0
            ),
            HabitActionCount(
                TestData.habit2.id,
                TestData.habit2.name,
                first_day = null,
                count = 0
            ),
            HabitActionCount(
                TestData.habit3.id,
                TestData.habit3.name,
                first_day = null,
                count = 0
            ),
            HabitActionCount(
                TestData.habit4.id,
                TestData.habit4.name,
                first_day = null,
                count = 0
            ),
            HabitActionCount(
                TestData.habit5.id,
                TestData.habit5.name,
                first_day = null,
                count = 0
            )
        )
        assertEquals(expected, mostSuccessfulHabits)
    }

    @Test
    fun testMostSuccessfulNoHabits() = testCoroutineScope.runBlockingTest {
        // Given

        // When
        val mostSuccessfulHabits = habitDao.getMostSuccessfulHabits(5)

        // Then
        assertEquals(emptyList<HabitActionCount>(), mostSuccessfulHabits)
    }

    @Test
    fun testMostSuccessfulHabitsWithLimit() = testCoroutineScope.runBlockingTest {
        // Given
        habitDao.insertHabit(*TestData.habits)
        habitDao.insertAction(*TestData.actions)

        // When
        val mostSuccessfulHabits = habitDao.getMostSuccessfulHabits(2)

        // Then
        val expected = listOf(
            HabitActionCount(
                TestData.habit1.id,
                TestData.habit1.name,
                first_day = LocalDate.of(2019, 12, 23),
                count = 7
            ),
            HabitActionCount(
                TestData.habit5.id,
                TestData.habit5.name,
                first_day = LocalDate.of(2021, 3, 26),
                count = 7
            )
        )
        assertEquals(expected, mostSuccessfulHabits)
    }

    @Test
    fun testTopDayForHabits() = testCoroutineScope.runBlockingTest {
        // Given
        habitDao.insertHabit(*TestData.habits)
        habitDao.insertAction(*TestData.actions)

        // When
        val topDayForHabits = habitDao.getTopDayForHabits()

        // Then
        val expected = listOf(
            HabitTopDay(
                TestData.habit1.id,
                TestData.habit1.name,
                top_day_of_week = DayOfWeek.MONDAY,
                action_count_on_day = 3
            ),
            HabitTopDay(
                TestData.habit2.id,
                TestData.habit2.name,
                top_day_of_week = DayOfWeek.WEDNESDAY,
                action_count_on_day = 1
            ),
            HabitTopDay(
                TestData.habit3.id,
                TestData.habit3.name,
                top_day_of_week = DayOfWeek.WEDNESDAY,
                action_count_on_day = 1
            ),
            HabitTopDay(
                TestData.habit4.id,
                TestData.habit4.name,
                top_day_of_week = DayOfWeek.SUNDAY,
                action_count_on_day = 0
            ),
            HabitTopDay(
                TestData.habit5.id,
                TestData.habit5.name,
                top_day_of_week = DayOfWeek.FRIDAY,
                action_count_on_day = 4
            )
        )
        assertEquals(expected, topDayForHabits)
    }

    @Test
    fun testTopDayForEmptyHabits() = testCoroutineScope.runBlockingTest {
        // Given
        habitDao.insertHabit(*TestData.habits)

        // When
        val topDayForHabits = habitDao.getTopDayForHabits()

        // Then
        val expected = listOf(
            HabitTopDay(
                TestData.habit1.id,
                TestData.habit1.name,
                top_day_of_week = DayOfWeek.SUNDAY,
                action_count_on_day = 0
            ),
            HabitTopDay(
                TestData.habit2.id,
                TestData.habit2.name,
                top_day_of_week = DayOfWeek.SUNDAY,
                action_count_on_day = 0
            ),
            HabitTopDay(
                TestData.habit3.id,
                TestData.habit3.name,
                top_day_of_week = DayOfWeek.SUNDAY,
                action_count_on_day = 0
            ),
            HabitTopDay(
                TestData.habit4.id,
                TestData.habit4.name,
                top_day_of_week = DayOfWeek.SUNDAY,
                action_count_on_day = 0
            ),
            HabitTopDay(
                TestData.habit5.id,
                TestData.habit5.name,
                top_day_of_week = DayOfWeek.SUNDAY,
                action_count_on_day = 0
            )
        )
        assertEquals(expected, topDayForHabits)
    }

    @Test
    fun testTopDayForNoHabits() = testCoroutineScope.runBlockingTest {
        // Given

        // When
        val topDayForHabits = habitDao.getTopDayForHabits()

        // Then
        assertEquals(emptyList<HabitTopDay>(), topDayForHabits)
    }

    @Test
    fun testDayOfWeekConverter() {
        val converter = EntityTypeConverters()

        assertEquals(DayOfWeek.SUNDAY, converter.toDayOfWeek(0))
        assertEquals(DayOfWeek.MONDAY, converter.toDayOfWeek(1))
        assertEquals(DayOfWeek.TUESDAY, converter.toDayOfWeek(2))
        assertEquals(DayOfWeek.WEDNESDAY, converter.toDayOfWeek(3))
        assertEquals(DayOfWeek.THURSDAY, converter.toDayOfWeek(4))
        assertEquals(DayOfWeek.FRIDAY, converter.toDayOfWeek(5))
        assertEquals(DayOfWeek.SATURDAY, converter.toDayOfWeek(6))
    }
}