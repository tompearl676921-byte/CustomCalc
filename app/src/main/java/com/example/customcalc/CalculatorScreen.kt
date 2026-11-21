package com.example.customcalc

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Kind { NUM, FUNC, OP, EQ }

private val KEYS: List<List<Pair<String, Kind>>> = listOf(
    listOf("AC" to Kind.FUNC, "⌫" to Kind.FUNC, "%" to Kind.FUNC, "÷" to Kind.OP),
    listOf("7" to Kind.NUM, "8" to Kind.NUM, "9" to Kind.NUM, "×" to Kind.OP),
    listOf("4" to Kind.NUM, "5" to Kind.NUM, "6" to Kind.NUM, "−" to Kind.OP),
    listOf("1" to Kind.NUM, "2" to Kind.NUM, "3" to Kind.NUM, "+" to Kind.OP),
    listOf("( )" to Kind.FUNC, "0" to Kind.NUM, "." to Kind.NUM, "=" to Kind.EQ)
)

@Composable
fun CalculatorApp(style: CalcStyle, onStyle: (CalcStyle) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val currentStyle by rememberUpdatedState(style)
    val currentOnStyle by rememberUpdatedState(onStyle)

    val bg by produceState<ImageBitmap?>(null, style.bgVersion) {
        value = if (style.bgVersion == 0) null
        else withContext(Dispatchers.IO) { BackgroundStore.load(ctx) }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { BackgroundStore.save(ctx, uri) }
                if (ok) currentOnStyle(currentStyle.copy(bgVersion = currentStyle.bgVersion + 1))
            }
        }
    }

    var expr by rememberSaveable { mutableStateOf("") }
    var fresh by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val particles = remember { ParticleSystem() }

    fun onKey(key: String, pos: Offset) {
        val n = applyKey(InputState(expr, fresh, error), key)
        expr = n.expr
        fresh = n.fresh
        error = n.error
        if (style.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (style.particles) {
            val accent = Color(style.accent)
            particles.burst(
                pos.x, pos.y,
                listOf(accent, Color(style.textColor), lerp(accent, Color.White, 0.5f)),
                count = if (style.particleShape == 2) 24 else 16
            )
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Background(style, bg)

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "⚙",
                    color = Color(style.textColor).copy(alpha = 0.85f),
                    fontSize = 28.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showSettings = true }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Display(
                expr = expr,
                preview = previewOf(InputState(expr, fresh, error)),
                error = error,
                style = style,
                modifier = Modifier.weight(0.9f)
            )
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.weight(2.3f),
                verticalArrangement = Arrangement.spacedBy(style.gap.dp)
            ) {
                KEYS.forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(style.gap.dp)
                    ) {
                        row.forEach { (label, kind) ->
                            CalcButton(
                                label = label,
                                kind = kind,
                                style = style,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { pos -> onKey(label, pos) }
                            )
                        }
                    }
                }
            }
        }

        ParticleLayer(particles, style.particleShape)

        if (showSettings) {
            SettingsSheet(
                style = style,
                onStyle = onStyle,
                onPickPhoto = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClearPhoto = {
                    BackgroundStore.clear(ctx)
                    onStyle(style.copy(bgVersion = 0))
                },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
private fun Background(style: CalcStyle, bg: ImageBitmap?) {
    Box(Modifier.fillMaxSize()) {
        if (bg != null) {
            val blurMod = if (style.bgBlur > 0.5f) {
                Modifier
                    .blur(style.bgBlur.dp, BlurredEdgeTreatment.Unbounded)
                    .graphicsLayer { scaleX = 1.12f; scaleY = 1.12f }
            } else Modifier
            Image(
                bitmap = bg,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().then(blurMod)
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(style.gradTop), Color(style.gradBottom)))
                )
            )
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = style.bgDim)))
    }
}

@Composable
private fun Display(
    expr: String,
    preview: String,
    error: Boolean,
    style: CalcStyle,
    modifier: Modifier
) {
    val tc = Color(style.textColor)
    val shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 2f), 8f)
    val size = when {
        expr.length <= 7 -> 72
        expr.length <= 10 -> 60
        expr.length <= 14 -> 48
        expr.length <= 20 -> 38
        else -> 30
    } * style.fontScale

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = if (error) "Ошибка" else preview.ifEmpty { " " },
            color = if (error) Color(0xFFFF6B6B) else tc.copy(alpha = 0.65f),
            fontSize = (28 * style.fontScale).sp,
            style = TextStyle(shadow = shadow),
            maxLines = 1
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = expr.ifEmpty { "0" },
            color = tc,
            fontSize = size.sp,
            fontWeight = FontWeight.Light,
            style = TextStyle(shadow = shadow),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.horizontalScroll(rememberScrollState(), reverseScrolling = true)
        )
    }
}

@Composable
private fun CalcButton(
    label: String,
    kind: Kind,
    style: CalcStyle,
    modifier: Modifier,
    onClick: (Offset) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var globalCenter by remember { mutableStateOf(Offset.Zero) }
    val click by rememberUpdatedState(onClick)

    val scale by animateFloatAsState(
        targetValue = if (pressed && style.pressAnim) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f),
        label = "scale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (pressed && style.glow) 1f else 0f,
        animationSpec = tween(150),
        label = "glow"
    )

    val accent = Color(style.accent)
    val base = Color(style.btnColor)
    val fill = when (kind) {
        Kind.NUM -> base.copy(alpha = style.btnAlpha)
        Kind.FUNC -> base.copy(alpha = (style.btnAlpha * 1.7f).coerceAtMost(1f))
        Kind.OP -> accent.copy(alpha = (style.btnAlpha + 0.45f).coerceAtMost(0.95f))
        Kind.EQ -> accent
    }
    val textColor = if (kind == Kind.EQ) Color.White else Color(style.textColor)
    val shape = RoundedCornerShape(percent = style.corner.roundToInt().coerceIn(0, 50))
    val fontSize = (if (label.length > 1 || label == "⌫") 24 else 32) * style.fontScale

    Box(
        modifier = modifier
            .onGloballyPositioned { c ->
                globalCenter = c.positionInRoot() + Offset(c.size.width / 2f, c.size.height / 2f)
            }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                if (glowAlpha > 0f) {
                    val r = size.maxDimension * 0.9f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.7f * glowAlpha), Color.Transparent),
                            center = this.center,
                            radius = r
                        ),
                        radius = r,
                        center = this.center
                    )
                }
            }
            .clip(shape)
            .background(fill)
            .then(
                if (style.glass) Modifier.border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.6f),
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.3f)
                        )
                    ),
                    shape
                ) else Modifier
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { click(globalCenter) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 6f))
        )
    }
}
