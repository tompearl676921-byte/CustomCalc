package com.example.customcalc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Простая система частиц: вспышка в точке нажатия. */
class ParticleSystem {
    class P(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var life: Float, val maxLife: Float,
        val color: Color, val size: Float,
        var rot: Float, val vr: Float
    )

    val list = ArrayList<P>()
    var frame by mutableLongStateOf(0L)
    var running by mutableStateOf(false)

    fun burst(x: Float, y: Float, colors: List<Color>, count: Int = 18) {
        repeat(count) {
            val a = Random.nextFloat() * 6.2832f
            val speed = 250f + Random.nextFloat() * 750f
            val life = 0.55f + Random.nextFloat() * 0.5f
            list += P(
                x, y,
                cos(a) * speed, sin(a) * speed - 250f,
                life, life,
                colors.random(),
                8f + Random.nextFloat() * 14f,
                Random.nextFloat() * 360f,
                (Random.nextFloat() - 0.5f) * 720f
            )
        }
        running = true
    }

    fun step(dt: Float) {
        val it = list.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) {
                it.remove()
            } else {
                p.vy += 1800f * dt
                p.vx *= 0.985f
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.rot += p.vr * dt
            }
        }
    }
}

@Composable
fun ParticleLayer(ps: ParticleSystem, shape: Int) {
    LaunchedEffect(ps.running) {
        if (ps.running) {
            var last = withFrameNanos { it }
            while (ps.list.isNotEmpty()) {
                val now = withFrameNanos { it }
                val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                ps.step(dt)
                ps.frame = now
            }
            ps.running = false
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        @Suppress("UNUSED_VARIABLE")
        val redraw = ps.frame // подписка на кадры
        for (p in ps.list) {
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            val col = p.color.copy(alpha = a)
            when (shape) {
                1 -> rotate(p.rot, pivot = Offset(p.x, p.y)) {
                    drawRect(
                        col,
                        topLeft = Offset(p.x - p.size / 2, p.y - p.size / 2),
                        size = Size(p.size, p.size * 0.6f)
                    )
                }
                2 -> drawLine(
                    col,
                    start = Offset(p.x, p.y),
                    end = Offset(p.x - p.vx * 0.04f, p.y - p.vy * 0.04f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
                else -> drawCircle(col, radius = p.size * (0.4f + 0.6f * a), center = Offset(p.x, p.y))
            }
        }
    }
}
