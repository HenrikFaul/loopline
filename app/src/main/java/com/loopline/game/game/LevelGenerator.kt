package com.loopline.game.game

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Procedurally generates deterministic levels. A level is built by tracing a
 * random self-avoiding, non-crossing walk over a grid; the visited cells become
 * the dots and the walk itself is stored as a guaranteed solution. Because every
 * level is derived from a real solution it is always completable.
 */
object LevelGenerator {

    fun forLevel(number: Int): Level {
        val size = when {
            number <= 3 -> 3
            number <= 8 -> 4
            number <= 16 -> 5
            number <= 28 -> 6
            else -> 7
        }
        val cap = size * size
        val target = when (number) {
            1 -> 4
            2 -> 5
            3 -> 7
            else -> {
                val f = (0.5 + (number - 3) * 0.03).coerceAtMost(0.9)
                (cap * f).roundToInt().coerceIn(6, cap)
            }
        }
        val seed = number.toLong() * 1_103_515_245L + 12_345L
        return build(number, size, size, target, seed, isDaily = false)
    }

    fun daily(epochDay: Long): Level {
        val size = 5
        val cap = size * size
        val target = (cap * 0.8).roundToInt()
        val seed = epochDay * 2_654_435_761L + 7L
        return build(-1, size, size, target, seed, isDaily = true)
    }

    private fun build(
        number: Int,
        cols: Int,
        rows: Int,
        target: Int,
        seed: Long,
        isDaily: Boolean
    ): Level {
        val rnd = Random(seed)
        val total = cols * rows
        val want = target.coerceIn(3, total)

        var bestWalk: IntArray = IntArray(0)
        var attempts = 0
        while (attempts < 240 && bestWalk.size < want) {
            val start = rnd.nextInt(total)
            val walk = walk(start, want, cols, rows, rnd)
            if (walk.size > bestWalk.size) bestWalk = walk
            attempts++
        }

        // Map cell ids -> Cell, cropped to the bounding box so the puzzle is centered.
        val cells = bestWalk.map { Cell(it % cols, it / cols) }
        val minX = cells.minOf { it.x }
        val maxX = cells.maxOf { it.x }
        val minY = cells.minOf { it.y }
        val maxY = cells.maxOf { it.y }
        val dots = cells.map { Cell(it.x - minX, it.y - minY) }
        val solution = dots.indices.toList()

        return Level(
            number = number,
            cols = maxX - minX + 1,
            rows = maxY - minY + 1,
            dots = dots,
            solution = solution,
            isDaily = isDaily
        )
    }

    private val DX = intArrayOf(1, -1, 0, 0, 1, 1, -1, -1)
    private val DY = intArrayOf(0, 0, 1, -1, 1, -1, 1, -1)

    /**
     * Randomised DFS that returns a self-avoiding, non-crossing path of length
     * [want] when possible, otherwise the longest path it found within a budget.
     */
    private fun walk(start: Int, want: Int, cols: Int, rows: Int, rnd: Random): IntArray {
        val total = cols * rows
        val visited = BooleanArray(total)
        val edges = HashSet<Long>()
        val path = ArrayList<Int>(total)
        var best = ArrayList<Int>()
        var budget = 40_000

        fun id(x: Int, y: Int) = y * cols + x
        fun edgeKey(a: Int, b: Int): Long {
            val lo = minOf(a, b); val hi = maxOf(a, b)
            return lo.toLong() * 100_003L + hi
        }

        fun crosses(cur: Int, nb: Int): Boolean {
            val cx = cur % cols; val cy = cur / cols
            val nx = nb % cols; val ny = nb / cols
            if (abs(cx - nx) == 1 && abs(cy - ny) == 1) {
                val o1 = id(cx, ny)
                val o2 = id(nx, cy)
                return edges.contains(edgeKey(o1, o2))
            }
            return false
        }

        fun dfs(cur: Int): Boolean {
            visited[cur] = true
            path.add(cur)
            if (path.size > best.size) best = ArrayList(path)
            if (path.size >= want) return true
            if (budget-- <= 0) {
                visited[cur] = false
                path.removeAt(path.size - 1)
                return false
            }
            val order = (0 until 8).toMutableList()
            order.shuffle(rnd)
            val cx = cur % cols; val cy = cur / cols
            for (dir in order) {
                val nx = cx + DX[dir]
                val ny = cy + DY[dir]
                if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) continue
                val nb = id(nx, ny)
                if (visited[nb] || crosses(cur, nb)) continue
                val ek = edgeKey(cur, nb)
                edges.add(ek)
                if (dfs(nb)) return true
                edges.remove(ek)
            }
            visited[cur] = false
            path.removeAt(path.size - 1)
            return false
        }

        dfs(start)
        return if (path.size >= want) path.toIntArray() else best.toIntArray()
    }
}
