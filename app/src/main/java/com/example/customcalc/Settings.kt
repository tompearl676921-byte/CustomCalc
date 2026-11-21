package com.example.customcalc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/** Все настройки внешнего вида. Цвета хранятся как ARGB Int. */
data class CalcStyle(
    val bgVersion: Int = 0,                 // 0 = фото нет; растёт при смене фото
    val bgBlur: Float = 0f,                 // dp, 0..25 (работает на Android 12+)
    val bgDim: Float = 0.3f,                // затемнение фона 0..0.8
    val gradTop: Int = 0xFF1A2980.toInt(),  // градиент, если нет фото
    val gradBottom: Int = 0xFF26D0CE.toInt(),
    val accent: Int = 0xFFFF9500.toInt(),
    val btnColor: Int = 0xFFFFFFFF.toInt(),
    val btnAlpha: Float = 0.18f,
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val corner: Float = 50f,                // % скругления: 0 квадрат … 50 круг
    val fontScale: Float = 1f,
    val gap: Float = 10f,                   // dp между кнопками
    val glow: Boolean = true,
    val glass: Boolean = true,              // «стеклянная» рамка
    val particles: Boolean = true,
    val particleShape: Int = 0,             // 0 круги, 1 конфетти, 2 искры
    val haptics: Boolean = true,
    val pressAnim: Boolean = true
)

object Presets {
    private fun c(v: Long) = v.toInt()

    val all: List<Pair<String, (CalcStyle) -> CalcStyle>> = listOf(
        "Стекло" to { s: CalcStyle ->
            s.copy(
                gradTop = c(0xFF1A2980), gradBottom = c(0xFF26D0CE),
                accent = c(0xFFFF9500), btnColor = c(0xFFFFFFFF), btnAlpha = 0.18f,
                textColor = c(0xFFFFFFFF), corner = 50f, glass = true, glow = true,
                particles = true, particleShape = 0
            )
        },
        "Неон" to { s: CalcStyle ->
            s.copy(
                gradTop = c(0xFF0A0014), gradBottom = c(0xFF1B0B3A),
                accent = c(0xFF00E5FF), btnColor = c(0xFF000000), btnAlpha = 0.40f,
                textColor = c(0xFFE0FFFF), corner = 24f, glass = true, glow = true,
                particles = true, particleShape = 2
            )
        },
        "Тёмный" to { s: CalcStyle ->
            s.copy(
                gradTop = c(0xFF000000), gradBottom = c(0xFF111111),
                accent = c(0xFFFF9F0A), btnColor = c(0xFF3A3A3C), btnAlpha = 0.95f,
                textColor = c(0xFFFFFFFF), corner = 50f, glass = false, glow = false,
                particles = false
            )
        },
        "Закат" to { s: CalcStyle ->
            s.copy(
                gradTop = c(0xFFFF512F), gradBottom = c(0xFFDD2476),
                accent = c(0xFFFFD200), btnColor = c(0xFFFFFFFF), btnAlpha = 0.22f,
                textColor = c(0xFFFFFFFF), corner = 40f, glass = true, glow = true,
                particles = true, particleShape = 1
            )
        }
    )
}

class StyleStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("calc_style", Context.MODE_PRIVATE)

    fun load(): CalcStyle {
        val d = CalcStyle()
        return CalcStyle(
            bgVersion = p.getInt("bgVersion", d.bgVersion),
            bgBlur = p.getFloat("bgBlur", d.bgBlur),
            bgDim = p.getFloat("bgDim", d.bgDim),
            gradTop = p.getInt("gradTop", d.gradTop),
            gradBottom = p.getInt("gradBottom", d.gradBottom),
            accent = p.getInt("accent", d.accent),
            btnColor = p.getInt("btnColor", d.btnColor),
            btnAlpha = p.getFloat("btnAlpha", d.btnAlpha),
            textColor = p.getInt("textColor", d.textColor),
            corner = p.getFloat("corner", d.corner),
            fontScale = p.getFloat("fontScale", d.fontScale),
            gap = p.getFloat("gap", d.gap),
            glow = p.getBoolean("glow", d.glow),
            glass = p.getBoolean("glass", d.glass),
            particles = p.getBoolean("particles", d.particles),
            particleShape = p.getInt("particleShape", d.particleShape),
            haptics = p.getBoolean("haptics", d.haptics),
            pressAnim = p.getBoolean("pressAnim", d.pressAnim)
        )
    }

    fun save(s: CalcStyle) {
        p.edit()
            .putInt("bgVersion", s.bgVersion)
            .putFloat("bgBlur", s.bgBlur)
            .putFloat("bgDim", s.bgDim)
            .putInt("gradTop", s.gradTop)
            .putInt("gradBottom", s.gradBottom)
            .putInt("accent", s.accent)
            .putInt("btnColor", s.btnColor)
            .putFloat("btnAlpha", s.btnAlpha)
            .putInt("textColor", s.textColor)
            .putFloat("corner", s.corner)
            .putFloat("fontScale", s.fontScale)
            .putFloat("gap", s.gap)
            .putBoolean("glow", s.glow)
            .putBoolean("glass", s.glass)
            .putBoolean("particles", s.particles)
            .putInt("particleShape", s.particleShape)
            .putBoolean("haptics", s.haptics)
            .putBoolean("pressAnim", s.pressAnim)
            .apply()
    }
}

/** Фото фона копируется во внутреннее хранилище приложения (уменьшенное, с учётом поворота). */
object BackgroundStore {
    private fun file(ctx: Context) = File(ctx.filesDir, "bg.jpg")

    fun save(ctx: Context, uri: Uri): Boolean = try {
        val src = ImageDecoder.createSource(ctx.contentResolver, uri)
        val bmp = ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val m = maxOf(w, h)
            if (m > 1920) {
                val k = 1920f / m
                decoder.setTargetSize((w * k).toInt(), (h * k).toInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        file(ctx).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        true
    } catch (e: Exception) {
        false
    }

    fun load(ctx: Context): ImageBitmap? =
        file(ctx).takeIf { it.exists() }
            ?.let { BitmapFactory.decodeFile(it.path) }
            ?.asImageBitmap()

    fun clear(ctx: Context) {
        file(ctx).delete()
    }
}
