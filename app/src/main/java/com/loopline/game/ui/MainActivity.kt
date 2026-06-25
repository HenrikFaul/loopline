package com.loopline.game.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.loopline.game.R
import com.loopline.game.databinding.ActivityMainBinding
import com.loopline.game.util.Prefs
import com.loopline.game.util.setupEdgeToEdge
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge(binding.root)
        prefs = Prefs(this)

        binding.btnPlay.setOnClickListener {
            startActivity(GameActivity.intentForLevel(this, prefs.maxUnlocked))
        }
        binding.btnLevels.setOnClickListener {
            startActivity(Intent(this, LevelSelectActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnHowto.setOnClickListener { showHowTo() }
        binding.btnDaily.setOnClickListener {
            startActivity(GameActivity.intentForDaily(this))
        }
    }

    override fun onResume() {
        super.onResume()
        val levelsDone = (prefs.maxUnlocked - 1).coerceAtLeast(0)
        val score = levelsDone * 100 + prefs.totalStars * 50 + prefs.coins
        binding.tvBest.text = score.toString()
        binding.tvCoins.text = prefs.coins.toString()
        binding.tvDailyDate.text = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()))
    }

    private fun showHowTo() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.how_to_play)
            .setMessage(R.string.howto_body)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}
