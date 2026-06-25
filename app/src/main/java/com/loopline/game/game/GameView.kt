package com.loopline.game.game

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.loopline.game.R
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Renders the dots and the player's line, and translates touch gestures into
 * moves on a [BoardModel]. Drag across dots to connect them; drag back to undo.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    interface Listener {
        fun onProgress(connected: Int, total: Int)
        fun onConnect(step: Int)
        fun onUndo()
        fun onComplete()
    }

    var listener: Listener? = null

    private var model: BoardModel? = null

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    // geometry (recomputed each draw)
    private var cell = 0f
    private var originX = 0f
    private var originY = 0f
    private var dotR = dp(6f)
    private var lineW = dp(6f)
    private var hitR = dp(28f)

    private var fingerX = 0f
    private var fingerY = 0f
    private var dragging = false
    private var notifiedComplete = false

    private var lineColor = ContextCompat.getColor(context, R.color.accent_amber)
    private val idlePalette = intArrayOf(
        ContextCompat.getColor(context, R.color.dot_idle),
        ContextCompat.getColor(context, R.color.dot_a),
        ContextCompat.getColor(context, R.color.dot_b),
        ContextCompat.getColor(context, R.color.dot_d),
        ContextCompat.getColor(context, R.color.dot_e)
    )

    private var breathe = 0f
    private var popProgress = 0f
    private var popDot = -1

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val rubberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val idleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val idleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }

    private val linePath = Path()

    private val breatheAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1300
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener {
            breathe = it.animatedValue as Float
            invalidate()
        }
    }
    private val popAnim = ValueAnimator.ofFloat(1f, 0f).apply {
        duration = 220
        addUpdateListener {
            popProgress = it.animatedValue as Float
            invalidate()
        }
    }

    fun setLevel(level: Level) {
        model = BoardModel(level)
        notifiedComplete = false
        dragging = false
        popDot = -1
        listener?.onProgress(0, level.size)
        invalidate()
    }

    fun setLineColor(color: Int) {
        lineColor = color
        invalidate()
    }

    fun connectedCount(): Int = model?.connectedCount ?: 0
    fun totalCount(): Int = model?.level?.size ?: 0
    fun isComplete(): Boolean = model?.isComplete == true

    fun undoMove() {
        val m = model ?: return
        if (m.undo()) {
            notifiedComplete = false
            listener?.onUndo()
            listener?.onProgress(m.connectedCount, m.level.size)
            invalidate()
        }
    }

    fun restart() {
        val m = model ?: return
        m.reset()
        notifiedComplete = false
        listener?.onProgress(0, m.level.size)
        invalidate()
    }

    /** Reveals the next solution dot; returns the new connected count. */
    fun useHint(): Int {
        val m = model ?: return 0
        val head = m.applyHint()
        if (head >= 0) {
            popDot = head
            startPop()
        }
        listener?.onProgress(m.connectedCount, m.level.size)
        checkComplete()
        invalidate()
        return m.connectedCount
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!breatheAnim.isStarted) breatheAnim.start()
    }

    override fun onDetachedFromWindow() {
        breatheAnim.cancel()
        popAnim.cancel()
        super.onDetachedFromWindow()
    }

    private fun startPop() {
        popAnim.cancel()
        popAnim.start()
    }

    private fun computeGeometry() {
        val m = model ?: return
        val level = m.level
        val margin = dp(30f)
        val availW = width - 2 * margin
        val availH = height - 2 * margin
        val stepX = if (level.cols > 1) availW / (level.cols - 1) else availW
        val stepY = if (level.rows > 1) availH / (level.rows - 1) else availH
        cell = when {
            level.cols > 1 && level.rows > 1 -> min(stepX, stepY)
            level.cols > 1 -> stepX
            level.rows > 1 -> stepY
            else -> min(availW, availH)
        }
        if (cell <= 0f) cell = min(availW, availH).coerceAtLeast(dp(40f))
        val spanX = (level.cols - 1) * cell
        val spanY = (level.rows - 1) * cell
        originX = width / 2f - spanX / 2f
        originY = height / 2f - spanY / 2f
        dotR = max(cell * 0.15f, dp(5f))
        lineW = max(cell * 0.17f, dp(4f))
        hitR = max(cell * 0.52f, dotR * 2.4f)
    }

    private fun cx(i: Int): Float = originX + model!!.level.dots[i].x * cell
    private fun cy(i: Int): Float = originY + model!!.level.dots[i].y * cell

    private fun nearestDot(px: Float, py: Float): Int {
        val m = model ?: return -1
        var bestIdx = -1
        var bestDist = hitR
        for (i in 0 until m.level.size) {
            val d = hypot(px - cx(i), py - cy(i))
            if (d <= bestDist) {
                bestDist = d
                bestIdx = i
            }
        }
        return bestIdx
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val m = model ?: return false
        if (cell <= 0f) computeGeometry()
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                fingerX = x; fingerY = y
                if (m.isComplete) return true
                val d = nearestDot(x, y)
                if (d < 0) { dragging = false; return true }
                when {
                    m.connectedCount == 0 -> { apply(m.moveTo(d)); dragging = true }
                    d == m.headIndex -> dragging = true
                    m.isInPath(d) -> { dragging = true; m.truncateTo(d); afterUndo() }
                    m.canExtendTo(d) -> { dragging = true; apply(m.moveTo(d)) }
                    else -> dragging = false
                }
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                fingerX = x; fingerY = y
                if (!dragging) return true
                val d = nearestDot(x, y)
                if (d >= 0 && d != m.headIndex) {
                    if (m.isInPath(d)) {
                        m.truncateTo(d)
                        afterUndo()
                    } else {
                        apply(m.moveTo(d))
                    }
                }
                checkComplete()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                checkComplete()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun apply(move: Move) {
        val m = model ?: return
        when (move) {
            Move.STARTED, Move.EXTENDED -> {
                popDot = m.headIndex
                startPop()
                listener?.onConnect(m.connectedCount - 1)
                listener?.onProgress(m.connectedCount, m.level.size)
            }
            Move.UNDONE -> afterUndo()
            Move.NONE -> {}
        }
    }

    private fun afterUndo() {
        val m = model ?: return
        notifiedComplete = false
        listener?.onUndo()
        listener?.onProgress(m.connectedCount, m.level.size)
    }

    private fun checkComplete() {
        val m = model ?: return
        if (m.isComplete && !notifiedComplete) {
            notifiedComplete = true
            dragging = false
            listener?.onComplete()
        } else if (!m.isComplete) {
            notifiedComplete = false
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val m = model ?: return
        computeGeometry()

        // line + glow
        if (m.path.size >= 2) {
            linePath.reset()
            linePath.moveTo(cx(m.path[0]), cy(m.path[0]))
            for (i in 1 until m.path.size) linePath.lineTo(cx(m.path[i]), cy(m.path[i]))

            glowPaint.color = withAlpha(lineColor, 70)
            glowPaint.strokeWidth = lineW * 2.6f
            canvas.drawPath(linePath, glowPaint)

            linePaint.color = lineColor
            linePaint.strokeWidth = lineW
            canvas.drawPath(linePath, linePaint)
        }

        // rubber band from head to finger while drawing
        if (dragging && !m.isComplete && m.headIndex >= 0) {
            rubberPaint.color = withAlpha(lineColor, 130)
            rubberPaint.strokeWidth = lineW
            canvas.drawLine(cx(m.headIndex), cy(m.headIndex), fingerX, fingerY, rubberPaint)
        }

        // dots
        for (i in 0 until m.level.size) {
            val x = cx(i)
            val y = cy(i)
            var r = dotR
            if (i == m.headIndex && !m.isComplete) r *= (1f + 0.18f * breathe)
            if (i == popDot) r *= (1f + 0.5f * popProgress)

            if (m.isInPath(i)) {
                // start dot gets an extra ring
                if (i == m.startIndex && m.path.size > 1) {
                    ringPaint.color = withAlpha(lineColor, 150)
                    canvas.drawCircle(x, y, r * 1.7f, ringPaint)
                }
                fillPaint.color = lineColor
                canvas.drawCircle(x, y, r, fillPaint)
                innerPaint.color = lighten(lineColor, 0.55f)
                canvas.drawCircle(x, y, r * 0.42f, innerPaint)
            } else {
                val c = idlePalette[i % idlePalette.size]
                idleFillPaint.color = withAlpha(c, 45)
                canvas.drawCircle(x, y, r, idleFillPaint)
                idleStrokePaint.color = c
                canvas.drawCircle(x, y, r, idleStrokePaint)
            }
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun lighten(color: Int, f: Float): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * f).toInt()
        val g = (Color.green(color) + (255 - Color.green(color)) * f).toInt()
        val b = (Color.blue(color) + (255 - Color.blue(color)) * f).toInt()
        return Color.rgb(r, g, b)
    }
}
