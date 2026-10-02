package com.example.coursey.features.course.domain.logic

import com.example.coursey.features.course.domain.entity.Lesson
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalculatorTest {

    @Test
    fun `2 of 4 lessons completed gives 50 percent`() {
        assertEquals(50, ProgressCalculator.calculate(lessons(true, true, false, false)))
    }

    @Test
    fun `0 of 4 lessons completed gives 0 percent`() {
        assertEquals(0, ProgressCalculator.calculate(lessons(false, false, false, false)))
    }

    @Test
    fun `4 of 4 lessons completed gives 100 percent`() {
        assertEquals(100, ProgressCalculator.calculate(lessons(true, true, true, true)))
    }

    private fun lessons(vararg completed: Boolean): List<Lesson> =
        completed.mapIndexed { index, done -> Lesson(index + 1, 1, "Lesson ${index + 1}", done) }
}
