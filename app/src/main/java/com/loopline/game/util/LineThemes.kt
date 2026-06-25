package com.loopline.game.util

import com.loopline.game.R

/** The selectable line/accent colors used by the board. */
object LineThemes {
    val colorRes = intArrayOf(
        R.color.accent_amber,
        R.color.accent_pink,
        R.color.accent_teal
    )

    fun resFor(index: Int): Int = colorRes[index.coerceIn(0, colorRes.size - 1)]
}
