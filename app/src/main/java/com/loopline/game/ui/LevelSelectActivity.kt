package com.loopline.game.ui

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loopline.game.R
import com.loopline.game.databinding.ActivityLevelSelectBinding
import com.loopline.game.databinding.ItemLevelBinding
import com.loopline.game.util.Prefs
import com.loopline.game.util.setupEdgeToEdge

class LevelSelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLevelSelectBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLevelSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge(binding.root)
        prefs = Prefs(this)

        binding.btnBack.setOnClickListener { finish() }
        binding.recycler.layoutManager = GridLayoutManager(this, 4)
        binding.recycler.adapter = LevelAdapter()
    }

    override fun onResume() {
        super.onResume()
        binding.tvCoins.text = prefs.coins.toString()
        binding.recycler.adapter?.notifyDataSetChanged()
    }

    private inner class LevelAdapter : RecyclerView.Adapter<LevelVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelVH {
            val b = ItemLevelBinding.inflate(layoutInflater, parent, false)
            return LevelVH(b)
        }

        override fun getItemCount(): Int = LEVEL_COUNT

        override fun onBindViewHolder(holder: LevelVH, position: Int) {
            holder.bind(position + 1)
        }
    }

    private inner class LevelVH(val b: ItemLevelBinding) : RecyclerView.ViewHolder(b.root) {

        private val gold = ContextCompat.getColor(this@LevelSelectActivity, R.color.star_gold)
        private val empty = ContextCompat.getColor(this@LevelSelectActivity, R.color.star_empty)

        fun bind(number: Int) {
            val unlocked = prefs.isUnlocked(number)
            val isCurrent = number == prefs.maxUnlocked
            b.tvNum.text = number.toString()

            b.root.setBackgroundResource(
                when {
                    !unlocked -> R.drawable.tile_locked
                    isCurrent -> R.drawable.tile_current
                    else -> R.drawable.tile_unlocked
                }
            )

            if (unlocked) {
                b.content.visibility = android.view.View.VISIBLE
                b.imgLock.visibility = android.view.View.GONE
                val stars = prefs.starsFor(number)
                val views = arrayOf(b.s1, b.s2, b.s3)
                for (i in views.indices) {
                    views[i].setColorFilter(if (i < stars) gold else empty)
                }
                b.root.setOnClickListener {
                    startActivity(GameActivity.intentForLevel(this@LevelSelectActivity, number))
                }
            } else {
                b.content.visibility = android.view.View.INVISIBLE
                b.imgLock.visibility = android.view.View.VISIBLE
                b.root.setOnClickListener {
                    Toast.makeText(
                        this@LevelSelectActivity,
                        R.string.locked,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    companion object {
        private const val LEVEL_COUNT = 120
    }
}
