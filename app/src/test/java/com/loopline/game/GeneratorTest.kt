package com.loopline.game

import com.loopline.game.game.BoardModel
import com.loopline.game.game.Level
import com.loopline.game.game.LevelGenerator
import com.loopline.game.game.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests (no Android runtime). They verify that every generated level is
 * structurally valid AND actually completable under the real [BoardModel] rules,
 * and that generation is fast enough to never risk an ANR on the UI thread.
 */
class GeneratorTest {

    private fun validate(level: Level) {
        assertTrue("level must have >= 2 dots", level.size >= 2)

        val seen = HashSet<Long>()
        for (c in level.dots) {
            assertTrue("dot x in bounds", c.x in 0 until level.cols)
            assertTrue("dot y in bounds", c.y in 0 until level.rows)
            assertTrue("dots must be unique", seen.add(c.x.toLong() * 100000 + c.y))
        }

        assertEquals("solution covers every dot", level.size, level.solution.size)
        assertEquals((0 until level.size).toSet(), level.solution.toSet())

        // Replay the stored solution through the real rules engine.
        val model = BoardModel(level)
        level.solution.forEachIndexed { i, dot ->
            val move = model.moveTo(dot)
            if (i == 0) {
                assertEquals("first move starts the path", Move.STARTED, move)
            } else {
                assertEquals("step $i must be a legal extension", Move.EXTENDED, move)
            }
        }
        assertTrue("the solution must complete the level", model.isComplete)
    }

    @Test
    fun allLevelsAreValidAndSolvable() {
        for (n in 1..120) validate(LevelGenerator.forLevel(n))
    }

    @Test
    fun dailyLevelsAreValid() {
        for (day in longArrayOf(0, 1, 19000, 20000, 20234, 50000, 99999)) {
            validate(LevelGenerator.daily(day))
        }
    }

    @Test
    fun fallbackIsValid() {
        validate(LevelGenerator.fallback(false))
        validate(LevelGenerator.fallback(true))
    }

    @Test
    fun generationIsDeterministic() {
        val a = LevelGenerator.forLevel(42)
        val b = LevelGenerator.forLevel(42)
        assertEquals(a.dots, b.dots)
        assertEquals(a.solution, b.solution)
    }

    @Test
    fun generationIsFast() {
        val start = System.nanoTime()
        for (n in 1..120) LevelGenerator.forLevel(n)
        LevelGenerator.daily(20234)
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("generating 121 levels took ${ms}ms (expected < 1500ms)", ms < 1500)
    }
}
