package dev.neonlauncher.dream

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.View
import dev.neonlauncher.NeonLauncherApp
import dev.neonlauncher.data.BackgroundStyle
import dev.neonlauncher.data.NeonThemeConfig
import dev.neonlauncher.data.ThemeJsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Lightweight native Android [Canvas] Daydream screensaver showing a dark aurora/grid
 * backdrop and a slowly drifting digital clock to prevent OLED burn-in.
 */
class NeonDreamService : DreamService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var dreamView: NeonDreamCanvasView? = null
    private var isDreaming = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isDreaming) return
            dreamView?.invalidate()
            mainHandler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false

        val view = NeonDreamCanvasView(this)
        dreamView = view
        setContentView(view)

        val app = application as? NeonLauncherApp ?: return
        serviceScope.launch {
            val (theme, style, use24Hour) = withContext(Dispatchers.IO) {
                val prefs = app.preferencesRepository.preferencesFlow.first()
                val themes = app.themeRepository.loadThemes()
                val resolved = app.themeRepository.resolveTheme(themes, prefs.themeId)
                Triple(
                    resolved,
                    prefs.backgroundStyleOverride ?: resolved.backgroundStyle,
                    prefs.use24HourClock,
                )
            }
            view.applyConfiguration(theme, style, use24Hour)
        }
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        isDreaming = true
        mainHandler.removeCallbacks(tickRunnable)
        mainHandler.post(tickRunnable)
    }

    override fun onDreamingStopped() {
        isDreaming = false
        mainHandler.removeCallbacks(tickRunnable)
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        isDreaming = false
        mainHandler.removeCallbacks(tickRunnable)
        serviceScope.cancel()
        dreamView = null
        super.onDetachedFromWindow()
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L
    }
}

private class NeonDreamCanvasView(context: Context) : View(context) {
    private var theme: NeonThemeConfig = ThemeJsonParser.CyberCyanFallback
    private var backgroundStyle: BackgroundStyle = theme.backgroundStyle
    private var use24HourClock: Boolean = true

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPrimaryPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowSecondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.04f
    }
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }
    private val accentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    private val currentDate = Date()
    private val format24h = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val format12h = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

    private val startedAtMs = System.currentTimeMillis()
    private var cachedWidth = 0
    private var cachedHeight = 0

    init {
        applyPalette()
    }

    fun applyConfiguration(
        newTheme: NeonThemeConfig,
        newStyle: BackgroundStyle,
        use24Hour: Boolean,
    ) {
        theme = newTheme
        backgroundStyle = newStyle
        use24HourClock = use24Hour
        applyPalette()
        rebuildShaders()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cachedWidth = w
        cachedHeight = h
        rebuildShaders()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        when (backgroundStyle) {
            BackgroundStyle.AURORA -> drawAurora(canvas, w, h)
            BackgroundStyle.GRID -> drawGrid(canvas, w, h)
            BackgroundStyle.SOLID -> Unit
        }

        val elapsedSec = (System.currentTimeMillis() - startedAtMs) / 1000.0
        val driftX = sin(elapsedSec * 0.035).toFloat() * (w * 0.14f)
        val driftY = cos(elapsedSec * 0.025).toFloat() * (h * 0.12f)

        val centerX = (w * 0.5f) + driftX
        val baseY = (h * 0.5f) + driftY

        currentDate.time = System.currentTimeMillis()
        val timeText = (if (use24HourClock) format24h else format12h).format(currentDate)
        val dateText = dateFormat.format(currentDate).uppercase(Locale.getDefault())

        canvas.drawText(timeText, centerX, baseY, timePaint)

        val lineHalfWidth = min(w, h) * 0.08f
        val lineY = baseY + (timePaint.textSize * 0.22f)
        canvas.drawLine(
            centerX - lineHalfWidth,
            lineY,
            centerX + lineHalfWidth,
            lineY,
            accentLinePaint,
        )

        val dateY = lineY + (datePaint.textSize * 1.45f)
        canvas.drawText(dateText, centerX, dateY, datePaint)
    }

    private fun drawAurora(canvas: Canvas, w: Float, h: Float) {
        canvas.drawCircle(w * 0.28f, h * 0.35f, min(w, h) * 0.55f, glowPrimaryPaint)
        canvas.drawCircle(w * 0.72f, h * 0.68f, min(w, h) * 0.50f, glowSecondaryPaint)
    }

    private fun drawGrid(canvas: Canvas, w: Float, h: Float) {
        val step = min(w, h) / 12f
        if (step <= 4f) return
        var x = 0f
        while (x <= w) {
            canvas.drawLine(x, 0f, x, h, gridPaint)
            x += step
        }
        var y = 0f
        while (y <= h) {
            canvas.drawLine(0f, y, w, y, gridPaint)
            y += step
        }
    }

    private fun applyPalette() {
        val colors = theme.colors
        timePaint.color = colors.textPrimary.toInt()
        datePaint.color = colors.textSecondary.toInt()
        accentLinePaint.color = colors.accentPrimary.toInt()
        gridPaint.color = withAlpha(colors.accentPrimary.toInt(), 0x1A)
    }

    private fun rebuildShaders() {
        val w = cachedWidth.toFloat()
        val h = cachedHeight.toFloat()
        if (w <= 0f || h <= 0f) return

        val colors = theme.colors
        val startArgb = colors.backgroundStart.toInt()
        val endArgb = colors.backgroundEnd.toInt()
        bgPaint.shader = LinearGradient(
            0f,
            0f,
            w,
            h,
            startArgb,
            endArgb,
            Shader.TileMode.CLAMP,
        )

        val primaryGlow = withAlpha(colors.accentPrimary.toInt(), 0x26)
        val secondaryGlow = withAlpha(colors.accentSecondary.toInt(), 0x22)
        val radius = min(w, h) * 0.55f

        glowPrimaryPaint.shader = RadialGradient(
            w * 0.28f,
            h * 0.35f,
            radius,
            primaryGlow,
            0x00000000,
            Shader.TileMode.CLAMP,
        )
        glowSecondaryPaint.shader = RadialGradient(
            w * 0.72f,
            h * 0.68f,
            min(w, h) * 0.50f,
            secondaryGlow,
            0x00000000,
            Shader.TileMode.CLAMP,
        )

        val baseDim = min(w, h)
        timePaint.textSize = (baseDim * 0.14f).coerceIn(48f, 160f)
        datePaint.textSize = (baseDim * 0.032f).coerceIn(18f, 36f)
    }

    private fun withAlpha(argb: Int, alpha: Int): Int =
        (argb and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)
}
