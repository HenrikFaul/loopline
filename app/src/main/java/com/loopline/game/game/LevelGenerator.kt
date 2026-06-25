package com.loopline.game.game

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Procedurally generates deterministic levels. Paths are traced with a
 * non-backtracking Warnsdorff heuristic (always step to the neighbour with the
 * fewest onward moves), which produces long, organic, non-crossing paths in
 * O(cells) time — so generation is always fast and can never hang the UI thread.
 * The traced path is itself a guaranteed solution, so every level is solvable.
 */
object LevelGenerator {

    private val DX = intArrayOf(1, -1, 0, 0, 1, 1, -1, -1)
    private val DY = intArrayOf(0, 0, 1, -1, 1, -1, 1, -1)

    fun forLevel(number: Int): Level {
        val safeNumber = number.coerceAtLeast(1)
        val size = when {
            safeNumber <= 3 -> 3
            safeNumber <= 8 -> 4
            safeNumber <= 16 -> 5
            safeNumber <= 28 -> 6
            else -> 7
        }
        val cap = size * size
        val desired = when {
            safeNumber <= 3 -> safeNumber + 3 // 4, 5, 6
            else -> (cap * (0.5 + (safeNumber - 3) * 0.035)).roundToInt()
        }.coerceIn(4, cap)
        val seed = safeNumber.toLong() * 1_103_515_245L + 12_345L
        return build(safeNumber, size, size, desired, seed, isDaily = false)
    }

    fun daily(epochDay: Long): Level {
        val size = 5
        val cap = size * size
        val seed = epochDay * 2_654_435_761L + 7L
        return build(-1, size, size, cap, seed, isDaily = true)
    }

    /** A tiny hard-coded square level used as a last-resort fallback. */
    fun fallback(isDaily: Boolean = false): Level {
        val dots = listOf(Cell(0, 0), Cell(1, 0), Cell(1, 1), Cell(0, 1))
        return Level(if (isDaily) -1 else 1, 2, 2, dots, listOf(0, 1, 2, 3), isDaily)
    }

    private fun build(
        number: Int,
        cols: Int,
        rows: Int,
        desired: Int,
        seed: Long,
        isDaily: Boolean
    ): Level {
        return try {
            val rnd = Random(seed)
            val total = cols * rows
            var best = IntArray(0)
            val starts = total.coerceAtMost(24).coerceAtLeast(1)
            repeat(starts) {
                val walk = warnsdorffWalk(rnd.nextInt(total), cols, rows, rnd)
                if (walk.size > best.size) best = walk
            }

            if (best.size < 2) return fallback(isDaily)

            val len = desired.coerceIn(2, best.size)
            val chosen = if (best.size > len) best.copyOfRange(0, len) else best

            // crop to bounding box so the puzzle is centered
            val cells = chosen.map { Cell(it % cols, it / cols) }
            val minX = cells.minOf { it.x }
            val maxX = cells.maxOf { it.x }
            val minY = cells.minOf { it.y }
            val maxY = cells.maxOf { it.y }
            val dots = cells.map { Cell(it.x - minX, it.y - minY) }

            Level(number, maxX - minX + 1, maxY - minY + 1, dots, dots.indices.toList(), isDaily)
        } catch (e: Throwable) {
            fallback(isDaily)
        }
    }

    private fun warnsdorffWalk(start: Int, cols: Int, rows: Int, rnd: Random): IntArray {
        val total = cols * rows
        val visited = BooleanArray(total)
        val edges = HashSet<Long>()
        val path = ArrayList<Int>(total)
        var cur = start
        visited[cur] = true
        path.add(cur)

        while (true) {
            val cands = candidates(cur, cols, rows, visited, edges)
            if (cands.isEmpty()) break
            // Warnsdorff: pick the candidate with the fewest onward moves; random tie-break.
            var bestDeg = Int.MAX_VALUE
            val bestList = ArrayList<Int>(cands.size)
            for (nb in cands) {
                val deg = candidates(nb, cols, rows, visited, edges).size
                when {
                    deg < bestDeg -> { bestDeg = deg; bestList.clear(); bestList.add(nb) }
                    deg == bestDeg -> bestList.add(nb)
                }
            }
            val next = bestList[rnd.nextInt(bestList.size)]
            edges.add(edgeKey(cur, next))
            visited[next] = true
            path.add(next)
            cur = next
        }
        return path.toIntArray()
    }

    /** Unvisited, in-bounds, non-crossing neighbours of [cur]. */
    private fun candidates(
        cur: Int,
        cols: Int,
        rows: Int,
        visited: BooleanArray,
        edges: HashSet<Long>
    ): List<Int> {
        val cx = cur % cols
        val cy = cur / cols
        val out = ArrayList<Int>(8)
        for (d in 0 until 8) {
            val nx = cx + DX[d]
            val ny = cy + DY[d]
            if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) continue
            val nb = ny * cols + nx
            if (visited[nb]) continue
            if (abs(DX[d]) == 1 && abs(DY[d]) == 1) {
                val o1 = cy * cols + nx // (nx, cy)
                val o2 = ny * cols + cx // (cx, ny)
                if (edges.contains(edgeKey(o1, o2))) continue
            }
            out.add(nb)
        }
        return out
    }

    private fun edgeKey(a: Int, b: Int): Long {
        val lo = minOf(a, b)
        val hi = maxOf(a, b)
        return lo.toLong() * 100_003L + hi
    }
}
