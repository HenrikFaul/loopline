package com.loopline.game.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.loopline.game.R
import com.loopline.game.databinding.ActivityGameBinding
import com.loopline.game.game.GameView
import com.loopline.game.game.Level
import com.loopline.game.game.LevelGenerator
import com.loopline.game.util.Haptics
import com.loopline.game.util.LineThemes
import com.loopline.game.util.Prefs
import com.loopline.game.util.SoundManager
import com.loopline.game.util.setupEdgeToEdge
import java.time.LocalDate
import java.util.Locale

class GameActivity : AppCompatActivity(), GameView.Listener {

    private lateinit var binding: ActivityGameBinding
    private lateinit var prefs: Prefs
    private lateinit var haptics: Haptics
    private val sound = SoundManager()

    private lateinit var level: Level
    private var isDaily = false
    private var completed = false

    private val handler = Handler(Looper.getMainLooper())
    private var timerRunning = false
    private var baseElapsed = 0L
    private var accumulatedMs = 0L
    private val tick = object : Runnable {
        override fun run() {
            binding.tvTime.text = formatTime(currentMs())
            if (timerRunning) handler.postDelayed(this, 200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge(binding.root)
        prefs = Prefs(this)
        haptics = Haptics(this).also { it.enabled = prefs.hapticsOn }
        sound.enabled = prefs.soundOn

        isDaily = intent.getBooleanExtra(EXTRA_DAILY, false)
        val levelNumber = intent.getIntExtra(EXTRA_LEVEL, prefs.maxUnlocked)

        binding.gameView.listener = this
        binding.gameView.setLineColor(
            ContextCompat.getColor(this, LineThemes.resFor(prefs.themeIndex))
        )

        binding.tvHintCost.text = "-$HINT_COST"
        binding.btnBack.setOnClickListener { finish() }
        binding.btnUndo.setOnClickListener { binding.gameView.undoMove() }
        binding.btnRestart.setOnClickListener { restartLevel() }
        binding.btnHint.setOnClickListener { useHint() }

        binding.btnWinRetry.setOnClickListener { restartLevel() }
        binding.btnWinMenu.setOnClickListener { finish() }
        binding.btnWinNext.setOnClickListener {
            if (isDaily) {
                finish()
            } else {
                startActivity(intentForLevel(this, level.number + 1))
                finish()
            }
        }

        loadLevel(levelNumber)
    }

    private fun loadLevel(number: Int) {
        level = if (isDaily) {
            LevelGenerator.daily(LocalDate.now().toEpochDay())
        } else {
            LevelGenerator.forLevel(number)
        }
        completed = false
        binding.winOverlay.visibility = View.GONE
        binding.gameView.setLevel(level)

        binding.tvLevel.text = if (isDaily) {
            getString(R.string.daily_label)
        } else {
            getString(R.string.level_label, number)
        }
        binding.tvCoins.text = prefs.coins.toString()
        binding.tvProgress.text = "0 / ${level.size}"
        resetTimer()
    }

    private fun restartLevel() {
        binding.winOverlay.visibility = View.GONE
        completed = false
        binding.gameView.restart()
        resetTimer()
    }

    private fun useHint() {
        if (completed) return
        if (prefs.coins < HINT_COST) {
            Toast.makeText(this, "Not enough coins for a hint", Toast.LENGTH_SHORT).show()
            return
        }
        prefs.addCoins(-HINT_COST)
        binding.tvCoins.text = prefs.coins.toString()
        haptics.tick()
        binding.gameView.useHint()
    }

    // ---- GameView.Listener ----

    override fun onProgress(connected: Int, total: Int) {
        binding.tvProgress.text = "$connected / $total"
    }

    override fun onConnect(step: Int) {
        sound.connect(step)
        haptics.tick()
    }

    override fun onUndo() {
        sound.undo()
        haptics.tick()
    }

    override fun onComplete() {
        if (completed) return
        completed = true
        pauseTimer()
        val elapsed = currentMs()
        sound.win()
        haptics.success()
        handleResult(elapsed)
    }

    private fun handleResult(elapsedMs: Long) {
        val dots = level.size
        val threeStarMs = dots * 900L
        val twoStarMs = dots * 1600L
        val stars = when {
            elapsedMs <= threeStarMs -> 3
            elapsedMs <= twoStarMs -> 2
            else -> 1
        }
        val coinsEarned = 10 + (stars - 1) * 5

        val isNewBest: Boolean
        if (isDaily) {
            prefs.addCoins(coinsEarned)
            isNewBest = false
        } else {
            isNewBest = prefs.recordResult(level.number, elapsedMs, stars)
            prefs.addCoins(coinsEarned)
        }
        binding.tvCoins.text = prefs.coins.toString()
        showWinOverlay(elapsedMs, stars, coinsEarned, isNewBest)
    }

    private fun showWinOverlay(elapsedMs: Long, stars: Int, coinsEarned: Int, isNewBest: Boolean) {
        binding.tvWinTitle.text = if (isDaily) "CHALLENGE DONE!" else getString(R.string.level_complete)
        binding.tvWinTime.text = formatTime(elapsedMs)
        binding.tvWinCoins.text = getString(R.string.coins_reward, coinsEarned)
        binding.tvNewBest.visibility = if (isNewBest) View.VISIBLE else View.INVISIBLE
        binding.btnWinNext.text = if (isDaily) getString(R.string.menu) else getString(R.string.next)

        val stars3 = arrayOf(binding.star1, binding.star2, binding.star3)
        val gold = ContextCompat.getColor(this, R.color.star_gold)
        for (i in stars3.indices) {
            val v = stars3[i]
            if (i < stars) v.setColorFilter(gold) else v.clearColorFilter()
            v.scaleX = 0.4f; v.scaleY = 0.4f; v.alpha = 0f
        }

        val overlay = binding.winOverlay
        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f
        overlay.animate().alpha(1f).setDuration(180).start()
        binding.winCard.scaleX = 0.85f
        binding.winCard.scaleY = 0.85f
        binding.winCard.animate().scaleX(1f).scaleY(1f).setDuration(220).start()

        for (i in stars3.indices) {
            val v: ImageView = stars3[i]
            v.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setStartDelay(220L + i * 130L)
                .setDuration(260)
                .start()
        }
    }

    // ---- timer ----

    private fun currentMs(): Long =
        accumulatedMs +
            if (timerRunning) (SystemClock.elapsedRealtime() - baseElapsed).coerceAtLeast(0L) else 0L

    private fun resetTimer() {
        accumulatedMs = 0L
        baseElapsed = SystemClock.elapsedRealtime()
        timerRunning = true
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    private fun pauseTimer() {
        if (timerRunning) {
            accumulatedMs += SystemClock.elapsedRealtime() - baseElapsed
            timerRunning = false
            handler.removeCallbacks(tick)
        }
    }

    private fun resumeTimer() {
        if (!timerRunning && !completed) {
            baseElapsed = SystemClock.elapsedRealtime()
            timerRunning = true
            handler.post(tick)
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        return String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
    }

    override fun onPause() {
        super.onPause()
        pauseTimer()
    }

    override fun onResume() {
        super.onResume()
        haptics.enabled = prefs.hapticsOn
        sound.enabled = prefs.soundOn
        if (!completed) resumeTimer()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        sound.release()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_LEVEL = "extra_level"
        private const val EXTRA_DAILY = "extra_daily"
        private const val HINT_COST = 20

        fun intentForLevel(context: Context, level: Int): Intent =
            Intent(context, GameActivity::class.java).putExtra(EXTRA_LEVEL, level.coerceAtLeast(1))

        fun intentForDaily(context: Context): Intent =
            Intent(context, GameActivity::class.java).putExtra(EXTRA_DAILY, true)
    }
}
