package com.loopline.game.util

import android.content.Context

/** Lightweight wrapper around SharedPreferences holding all persistent game state. */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("loopline_prefs", Context.MODE_PRIVATE)

    var coins: Int
        get() = sp.getInt(KEY_COINS, 0)
        set(value) = sp.edit().putInt(KEY_COINS, value.coerceAtLeast(0)).apply()

    /** Highest level the player has unlocked (always at least 1). */
    var maxUnlocked: Int
        get() = sp.getInt(KEY_MAX_UNLOCKED, 1).coerceAtLeast(1)
        set(value) = sp.edit().putInt(KEY_MAX_UNLOCKED, value).apply()

    var soundOn: Boolean
        get() = sp.getBoolean(KEY_SOUND, true)
        set(value) = sp.edit().putBoolean(KEY_SOUND, value).apply()

    var hapticsOn: Boolean
        get() = sp.getBoolean(KEY_HAPTICS, true)
        set(value) = sp.edit().putBoolean(KEY_HAPTICS, value).apply()

    /** 0 = Amber, 1 = Pink, 2 = Teal. */
    var themeIndex: Int
        get() = sp.getInt(KEY_THEME, 0)
        set(value) = sp.edit().putInt(KEY_THEME, value).apply()

    /** Total stars earned across all levels (used as the headline "best score"). */
    var totalStars: Int
        get() = sp.getInt(KEY_TOTAL_STARS, 0)
        private set(value) = sp.edit().putInt(KEY_TOTAL_STARS, value).apply()

    fun starsFor(level: Int): Int = sp.getInt(starKey(level), 0)

    fun bestTimeFor(level: Int): Long = sp.getLong(timeKey(level), 0L)

    fun isUnlocked(level: Int): Boolean = level <= maxUnlocked

    /**
     * Records the result of a finished level. Returns true if this run set a new best time.
     */
    fun recordResult(level: Int, timeMs: Long, stars: Int): Boolean {
        val prevStars = starsFor(level)
        val prevTime = bestTimeFor(level)
        val isNewBest = prevTime == 0L || timeMs < prevTime

        val editor = sp.edit()
        if (stars > prevStars) {
            editor.putInt(starKey(level), stars)
            totalStars = totalStars + (stars - prevStars)
        }
        if (isNewBest) editor.putLong(timeKey(level), timeMs)
        editor.apply()

        if (level + 1 > maxUnlocked) maxUnlocked = level + 1
        return isNewBest
    }

    fun addCoins(amount: Int) {
        coins += amount
    }

    /** Stack trace of the last uncaught crash (written by LooplineApp), or null. */
    var lastCrash: String?
        get() = sp.getString(KEY_LAST_CRASH, null)
        set(value) {
            val e = sp.edit()
            if (value == null) e.remove(KEY_LAST_CRASH) else e.putString(KEY_LAST_CRASH, value)
            e.apply()
        }

    fun resetAll() {
        // Preserve any pending crash report across a progress reset.
        val crash = lastCrash
        sp.edit().clear().apply()
        if (crash != null) lastCrash = crash
    }

    private fun starKey(level: Int) = "stars_$level"
    private fun timeKey(level: Int) = "time_$level"

    companion object {
        private const val KEY_COINS = "coins"
        private const val KEY_MAX_UNLOCKED = "max_unlocked"
        private const val KEY_SOUND = "sound_on"
        private const val KEY_HAPTICS = "haptics_on"
        private const val KEY_THEME = "theme_index"
        private const val KEY_TOTAL_STARS = "total_stars"
        private const val KEY_LAST_CRASH = "last_crash"
    }
}
