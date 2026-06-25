package com.loopline.game.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.loopline.game.R
import com.loopline.game.databinding.ActivitySettingsBinding
import com.loopline.game.util.Prefs
import com.loopline.game.util.setupEdgeToEdge

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge(binding.root)
        prefs = Prefs(this)

        binding.btnBack.setOnClickListener { finish() }

        binding.swSound.isChecked = prefs.soundOn
        binding.swSound.setOnCheckedChangeListener { _, checked -> prefs.soundOn = checked }

        binding.swHaptics.isChecked = prefs.hapticsOn
        binding.swHaptics.setOnCheckedChangeListener { _, checked -> prefs.hapticsOn = checked }

        binding.theme0.setOnClickListener { selectTheme(0) }
        binding.theme1.setOnClickListener { selectTheme(1) }
        binding.theme2.setOnClickListener { selectTheme(2) }
        updateThemeChecks()

        binding.rowHowto.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.how_to_play)
                .setMessage(R.string.howto_body)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
        binding.rowAbout.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about)
                .setMessage(R.string.about_body)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
        binding.rowReset.setOnClickListener { confirmReset() }
    }

    private fun selectTheme(index: Int) {
        prefs.themeIndex = index
        updateThemeChecks()
    }

    private fun updateThemeChecks() {
        val selected = prefs.themeIndex
        binding.check0.visibility = if (selected == 0) View.VISIBLE else View.INVISIBLE
        binding.check1.visibility = if (selected == 1) View.VISIBLE else View.INVISIBLE
        binding.check2.visibility = if (selected == 2) View.VISIBLE else View.INVISIBLE
    }

    private fun confirmReset() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reset_progress)
            .setMessage(R.string.reset_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                prefs.resetAll()
                binding.swSound.isChecked = prefs.soundOn
                binding.swHaptics.isChecked = prefs.hapticsOn
                updateThemeChecks()
                Toast.makeText(this, "Progress reset", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
