package com.loopline.game.game

import kotlin.math.abs

/** A dot position on the puzzle grid. */
data class Cell(val x: Int, val y: Int)

/** Result of attempting to move the line to a dot. */
enum class Move { NONE, STARTED, EXTENDED, UNDONE }

/**
 * Immutable description of a level: the set of dots and one guaranteed-valid
 * solution path (used for hints). [solution] holds dot indices in visiting order.
 */
class Level(
    val number: Int,
    val cols: Int,
    val rows: Int,
    val dots: List<Cell>,
    val solution: List<Int>,
    val isDaily: Boolean = false
) {
    val size: Int get() = dots.size

    private val indexByCell = HashMap<Long, Int>(dots.size * 2)

    init {
        dots.forEachIndexed { i, c -> indexByCell[key(c.x, c.y)] = i }
    }

    fun indexOf(x: Int, y: Int): Int = indexByCell[key(x, y)] ?: -1

    companion object {
        fun key(x: Int, y: Int): Long = (x.toLong() shl 20) or (y.toLong() and 0xFFFFF)
    }
}

/**
 * Mutable game state for one level. Enforces the rules:
 *  - every dot used at most once,
 *  - consecutive dots must be grid-neighbours (8 directions),
 *  - the line may not cross itself (two diagonals of the same cell).
 */
class BoardModel(val level: Level) {

    val path = ArrayList<Int>(level.size)
    private val inPath = BooleanArray(level.size)
    private val usedEdges = HashSet<Long>()

    val headIndex: Int get() = if (path.isEmpty()) -1 else path[path.size - 1]
    val startIndex: Int get() = if (path.isEmpty()) -1 else path[0]
    val connectedCount: Int get() = path.size
    val isComplete: Boolean get() = path.size == level.size && level.size > 0

    fun isInPath(i: Int): Boolean = i in 0 until level.size && inPath[i]

    fun reset() {
        path.clear()
        inPath.fill(false)
        usedEdges.clear()
    }

    private fun push(i: Int) {
        path.add(i)
        inPath[i] = true
    }

    private fun edgeKey(a: Int, b: Int): Long {
        val lo = minOf(a, b)
        val hi = maxOf(a, b)
        return lo.toLong() * 1_000_003L + hi.toLong()
    }

    fun areAdjacent(a: Int, b: Int): Boolean {
        val ca = level.dots[a]
        val cb = level.dots[b]
        val dx = abs(ca.x - cb.x)
        val dy = abs(ca.y - cb.y)
        return dx <= 1 && dy <= 1 && dx + dy > 0
    }

    /** A diagonal move crosses the line only if the cell's other diagonal is already drawn. */
    private fun crosses(a: Int, b: Int): Boolean {
        val ca = level.dots[a]
        val cb = level.dots[b]
        if (abs(ca.x - cb.x) == 1 && abs(ca.y - cb.y) == 1) {
            val o1 = level.indexOf(ca.x, cb.y)
            val o2 = level.indexOf(cb.x, ca.y)
            if (o1 >= 0 && o2 >= 0 && usedEdges.contains(edgeKey(o1, o2))) return true
        }
        return false
    }

    fun canExtendTo(to: Int): Boolean {
        val head = headIndex
        if (head < 0) return true
        if (inPath[to]) return false
        if (!areAdjacent(head, to)) return false
        if (crosses(head, to)) return false
        return true
    }

    /** Drives the line in response to the finger landing on dot [to]. */
    fun moveTo(to: Int): Move {
        val head = headIndex
        if (head < 0) {
            push(to)
            return Move.STARTED
        }
        if (to == head) return Move.NONE
        // Dragging back onto the previous dot undoes the last segment.
        if (path.size >= 2 && to == path[path.size - 2]) {
            undo()
            return Move.UNDONE
        }
        if (canExtendTo(to)) {
            usedEdges.add(edgeKey(head, to))
            push(to)
            return Move.EXTENDED
        }
        return Move.NONE
    }

    /** Removes the last dot from the path. */
    fun undo(): Boolean {
        if (path.isEmpty()) return false
        val removed = path.removeAt(path.size - 1)
        inPath[removed] = false
        if (path.isNotEmpty()) usedEdges.remove(edgeKey(removed, path[path.size - 1]))
        return true
    }

    /** Repeatedly undoes until [target] becomes the head (used for drag-back). */
    fun truncateTo(target: Int) {
        while (path.size > 1 && headIndex != target) undo()
    }

    /**
     * Reveals the next step of the known solution, correcting any wrong turn.
     * Returns the dot index that is now the head, or -1 if nothing to add.
     */
    fun applyHint(): Int {
        val sol = level.solution
        if (sol.isEmpty()) return -1
        var matchLen = 0
        while (matchLen < path.size && matchLen < sol.size && path[matchLen] == sol[matchLen]) {
            matchLen++
        }
        val target = (matchLen + 1).coerceAtMost(sol.size)
        reset()
        for (k in 0 until target) {
            if (k == 0) {
                push(sol[0])
            } else {
                usedEdges.add(edgeKey(sol[k - 1], sol[k]))
                push(sol[k])
            }
        }
        return headIndex
    }
}
