package com.loopline.game

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.loopline.game.ui.GameActivity
import com.loopline.game.ui.LevelSelectActivity
import com.loopline.game.ui.MainActivity
import com.loopline.game.ui.SettingsActivity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * Robolectric smoke tests: drive each Activity through its full create/start/
 * resume lifecycle (and idle the main looper so posted work like the game timer
 * runs) to catch crashes that only happen at runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActivitySmokeTest {

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun pump(ms: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(ms, TimeUnit.MILLISECONDS)
    }

    @Test
    fun mainActivityLaunches() {
        val c = Robolectric.buildActivity(MainActivity::class.java).setup()
        pump(1500)
        assertNotNull(c.get())
    }

    @Test
    fun levelSelectLaunches() {
        val c = Robolectric.buildActivity(LevelSelectActivity::class.java).setup()
        pump(500)
        assertNotNull(c.get())
    }

    @Test
    fun settingsLaunches() {
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        pump(500)
        assertNotNull(c.get())
    }

    @Test
    fun gameLevelOneLaunchesAndTicks() {
        val c = Robolectric.buildActivity(
            GameActivity::class.java, GameActivity.intentForLevel(ctx, 1)
        ).setup()
        pump(2000) // run ~2s of timer ticks
        assertNotNull(c.get())
    }

    @Test
    fun gameHighLevelLaunches() {
        val c = Robolectric.buildActivity(
            GameActivity::class.java, GameActivity.intentForLevel(ctx, 60)
        ).setup()
        pump(2000)
        assertNotNull(c.get())
    }

    @Test
    fun dailyChallengeLaunchesAndTicks() {
        val c = Robolectric.buildActivity(
            GameActivity::class.java, GameActivity.intentForDaily(ctx)
        ).setup()
        pump(2000)
        assertNotNull(c.get())
    }
}
