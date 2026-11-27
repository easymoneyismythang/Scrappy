package com.scrappy.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Dark ScrapRight-style theme: deep green-black surfaces, bright mint accent,
 * white text, serif display headings, rounded cards, pill chips, bottom tab bar.
 * All screens are built with programmatic Views; these helpers keep them consistent.
 */
object ScrappyTheme {
    val BG = 0xFF0B1F14.toInt()
    val CARD = 0xFF12291C.toInt()
    val CARD2 = 0xFF1A3A28.toInt()
    val MINT = 0xFF5EE8A0.toInt()
    val WHITE = 0xFFFFFFFF.toInt()
    val MUTED = 0xFFA9C6B4.toInt()
    val DIM = 0xFF7E9C8A.toInt()
    val RED = 0xFFF08080.toInt()
    val LINE = 0xFF21402F.toInt()

    fun dp(ctx: Context, v: Float): Int = (v * ctx.resources.displayMetrics.density + 0.5f).toInt()

    fun rounded(ctx: Context, color: Int, radiusDp: Float, strokeColor: Int? = null, strokeDp: Float = 0f): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = dp(ctx, radiusDp).toFloat()
            if (strokeColor != null) setStroke(dp(ctx, strokeDp), strokeColor)
        }
    }

    fun roundedTop(ctx: Context, color: Int, radiusDp: Float): GradientDrawable {
        val r = dp(ctx, radiusDp).toFloat()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        }
    }

    /** Full-width mint filled button with dark text. */
    fun mintButton(ctx: Context, text: String, onClick: () -> Unit): Button {
        return Button(ctx).apply {
            this.text = text
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setAllCaps(false)
            setTextColor(BG)
            background = rounded(ctx, MINT, 16f)
            setPadding(dp(ctx, 16f), dp(ctx, 14f), dp(ctx, 16f), dp(ctx, 14f))
            setOnClickListener { onClick() }
        }
    }

    /** Full-width dark button with a subtle green outline and white text. */
    fun darkButton(ctx: Context, text: String, onClick: () -> Unit): Button {
        return Button(ctx).apply {
            this.text = text
            textSize = 16f
            setAllCaps(false)
            setTextColor(WHITE)
            background = rounded(ctx, CARD2, 16f, LINE, 1f)
            setPadding(dp(ctx, 16f), dp(ctx, 12f), dp(ctx, 16f), dp(ctx, 12f))
            setOnClickListener { onClick() }
        }
    }

    /** Pill filter chip; selected chips are mint, the rest are dark cards. */
    fun chipButton(ctx: Context, text: String, selected: Boolean, onClick: () -> Unit): Button {
        return Button(ctx).apply {
            this.text = text
            textSize = 14f
            typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setAllCaps(false)
            if (selected) {
                setTextColor(BG)
                background = rounded(ctx, MINT, 28f)
            } else {
                setTextColor(MUTED)
                background = rounded(ctx, CARD, 28f, LINE, 1f)
            }
            setPadding(dp(ctx, 18f), dp(ctx, 8f), dp(ctx, 18f), dp(ctx, 8f))
            setOnClickListener { onClick() }
        }
    }

    /** Rounded dark card container with standard padding and bottom spacing. */
    fun cardLayout(ctx: Context): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(ctx, CARD, 20f)
            setPadding(dp(ctx, 20f), dp(ctx, 18f), dp(ctx, 20f), dp(ctx, 18f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(ctx, 14f)
            }
        }
    }

    fun serifText(ctx: Context, text: String, size: Float): TextView {
        return TextView(ctx).apply {
            this.text = text
            textSize = size
            typeface = Typeface.SERIF
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            setTextColor(WHITE)
        }
    }
}

enum class Tab(val label: String, val glyph: String) {
    PRICES("Prices", "\u2302"),
    SCAN("Scan", "\u25CE"),
    YARDS("Yards", "\u2316"),
    MORE("More", "\u25A6")
}

/** Bottom tab bar matching the ScrapRight design. Tabs are TextViews, not Buttons. */
fun tabBar(ctx: Context, selected: Tab, onSelect: (Tab) -> Unit): LinearLayout {
    val bar = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        setBackgroundColor(ScrappyTheme.CARD2)
        setPadding(0, ScrappyTheme.dp(ctx, 8f), 0, ScrappyTheme.dp(ctx, 8f))
    }
    Tab.values().forEach { tab ->
        val isSelected = tab == selected
        val cell = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (isSelected) background = ScrappyTheme.rounded(ctx, ScrappyTheme.CARD, 16f)
            setPadding(0, ScrappyTheme.dp(ctx, 4f), 0, ScrappyTheme.dp(ctx, 4f))
            setOnClickListener { if (tab != selected) onSelect(tab) }
        }
        cell.addView(TextView(ctx).apply {
            text = tab.glyph
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(if (isSelected) ScrappyTheme.MINT else ScrappyTheme.DIM)
        })
        cell.addView(TextView(ctx).apply {
            text = tab.label
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(if (isSelected) ScrappyTheme.MINT else ScrappyTheme.MUTED)
        })
        bar.addView(cell)
    }
    return bar
}

/** Corner-bracket scan frame drawn over the camera preview. */
class ScanFrameView(ctx: Context) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ScrappyTheme.MINT
        strokeWidth = 5f * ctx.resources.displayMetrics.density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ScrappyTheme.MINT
        strokeWidth = 3f * ctx.resources.displayMetrics.density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val left = w * 0.16f
        val right = w * 0.84f
        val top = h * 0.28f
        val bottom = h * 0.58f
        val len = 48f * resources.displayMetrics.density
        // Top-left
        canvas.drawLine(left, top + len, left, top, paint)
        canvas.drawLine(left, top, left + len, top, paint)
        // Top-right
        canvas.drawLine(right - len, top, right, top, paint)
        canvas.drawLine(right, top, right, top + len, paint)
        // Bottom-left
        canvas.drawLine(left, bottom - len, left, bottom, paint)
        canvas.drawLine(left, bottom, left + len, bottom, paint)
        // Bottom-right
        canvas.drawLine(right - len, bottom, right, bottom, paint)
        canvas.drawLine(right, bottom, right, bottom - len, paint)
        // Center crosshair
        val cx = w / 2f
        val cy = (top + bottom) / 2f
        val c = 12f * resources.displayMetrics.density
        canvas.drawLine(cx - c, cy, cx + c, cy, crossPaint)
        canvas.drawLine(cx, cy - c, cx, cy + c, crossPaint)
    }
}

/**
 * Confidence ring: a mint arc whose sweep reflects the AI confidence level
 * (low / medium / high) with the level word in the middle. The level is a real
 * AI output, never a fabricated percentage.
 */
class ConfidenceRingView(ctx: Context) : View(ctx) {
    var fraction: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }
    var label: String = "High"
        set(value) {
            field = value
            invalidate()
        }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ScrappyTheme.LINE
        strokeWidth = 10f * ctx.resources.displayMetrics.density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ScrappyTheme.MINT
        strokeWidth = 10f * ctx.resources.displayMetrics.density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 15f * ctx.resources.displayMetrics.density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        if (size <= 0f) return
        val pad = bgPaint.strokeWidth
        val rect = RectF(pad, pad, size - pad, size - pad)
        canvas.drawArc(rect, 0f, 360f, false, bgPaint)
        if (fraction > 0f) canvas.drawArc(rect, -90f, 360f * fraction, false, fgPaint)
        val y = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, size / 2f, y, textPaint)
    }
}
