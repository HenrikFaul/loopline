package com.loopline.game

import com.loopline.game.game.BoardModel
import com.loopline.game.game.Cell
import com.loopline.game.game.Level
import com.loopline.game.game.LevelGenerator
import com.loopline.game.game.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests for the puzzle rules engine. */
class BoardModelTest {

    // 2x2 square: 0=(0,0) 1=(1,0) 2=(1,1) 3=(0,1)
    private fun square() = Level(
        1, 2, 2,
        listOf(Cell(0, 0), Cell(1, 0), Cell(1, 1), Cell(0, 1)),
        listOf(0, 1, 2, 3)
    )

    @Test
    fun startExtendUndoComplete() {
        val m = BoardModel(square())
        assertEquals(Move.STARTED, m.moveTo(0))
        assertEquals(Move.EXTENDED, m.moveTo(1))
        assertEquals(Move.EXTENDED, m.moveTo(2))
        assertEquals(Move.UNDONE, m.moveTo(1)) // drag back
        assertEquals(2, m.connectedCount)
        assertEquals(Move.EXTENDED, m.moveTo(2))
        assertEquals(Move.EXTENDED, m.moveTo(3))
        assertTrue(m.isComplete)
    }

    @Test
    fun cannotRevisitADot() {
        val m = BoardModel(square())
        m.moveTo(0); m.moveTo(1); m.moveTo(2)
        // 0 is adjacent (diagonal) to head 2 but already used -> rejected
        assertEquals(Move.NONE, m.moveTo(0))
    }

    @Test
    fun diagonalCrossingIsRejected() {
        val m = BoardModel(square())
        assertEquals(Move.STARTED, m.moveTo(1))
        assertEquals(Move.EXTENDED, m.moveTo(3)) // diagonal (1,0)-(0,1): edge 1-3 drawn
        assertEquals(Move.EXTENDED, m.moveTo(0)) // (0,1)-(0,0)
        // 0->2 is the crossing diagonal of edge 1-3 -> must be rejected
        assertEquals(Move.NONE, m.moveTo(2))
        assertFalse(m.isComplete)
    }

    @Test
    fun hintEventuallyCompletes() {
        val level = LevelGenerator.forLevel(7)
        val m = BoardModel(level)
        var guard = 0
        while (!m.isComplete && guard < level.size + 5) {
            m.applyHint()
            guard++
        }
        assertTrue("hints should be able to finish a level", m.isComplete)
    }

    @Test
    fun restartClearsState() {
        val m = BoardModel(square())
        m.moveTo(0); m.moveTo(1)
        m.reset()
        assertEquals(0, m.connectedCount)
        assertFalse(m.isComplete)
        assertEquals(Move.STARTED, m.moveTo(2))
    }
}
