package com.example.customcalc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PALETTE = listOf(
    0xFFFF9500, 0xFFFF3B30, 0xFFFF2D92, 0xFFAF52DE, 0xFF5856D6, 0xFF007AFF,
    0xFF00E5FF, 0xFF30D158, 0xFFFFD60A, 0xFFFFFFFF, 0xFF8E8E93, 0xFF000000
).map { Color(it) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    style: CalcStyle,
    onStyle: (CalcStyle) -> Unit,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF1C1C1E)
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Внешний вид", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)

            Title("Фон")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPickPhoto) { Text("Выбрать фото") }
                OutlinedButton(onClick = onClearPhoto, enabled = style.bgVersion != 0) {
                    Text("Убрать фото")
                }
            }
            SliderRow("Размытие фона (Android 12+)", style.bgBlur, 0f..25f) {
                onStyle(style.copy(bgBlur = it))
            }
            SliderRow("Затемнение фона", style.bgDim, 0f..0.8f) {
                onStyle(style.copy(bgDim = it))
            }
            ColorRow("Градиент: верх (если нет фото)", style.gradTop) { onStyle(style.copy(gradTop = it)) }
            ColorRow("Градиент: низ (если нет фото)", style.gradBottom) { onStyle(style.copy(gradBottom = it)) }

            Title("Готовые темы")
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Presets.all.forEach { (name, apply) ->
                    AssistChip(onClick = { onStyle(apply(style)) }, label = { Text(name) })
                }
            }

            Title("Кнопки")
            ColorRow("Цвет акцента (операции, «=»)", style.accent) { onStyle(style.copy(accent = it)) }
            ColorRow("Цвет кнопок", style.btnColor) { onStyle(style.copy(btnColor = it)) }
            ColorRow("Цвет текста", style.textColor) { onStyle(style.copy(textColor = it)) }
            SliderRow("Прозрачность кнопок", style.btnAlpha, 0f..0.9f) {
                onStyle(style.copy(btnAlpha = it))
            }
            SliderRow("Скругление (0 — квадрат, 50 — круг)", style.corner, 0f..50f, { "${it.toInt()}%" }) {
                onStyle(style.copy(corner = it))
            }
            SliderRow("Размер шрифта", style.fontScale, 0.7f..1.4f) {
                onStyle(style.copy(fontScale = it))
            }
            SliderRow("Отступы между кнопками", style.gap, 0f..24f, { "${it.toInt()} dp" }) {
                onStyle(style.copy(gap = it))
            }

            Title("Эффекты")
            SwitchRow("Свечение при нажатии", style.glow) { onStyle(style.copy(glow = it)) }
            SwitchRow("«Стеклянная» рамка", style.glass) { onStyle(style.copy(glass = it)) }
            SwitchRow("Пружинная анимация нажатия", style.pressAnim) { onStyle(style.copy(pressAnim = it)) }
            SwitchRow("Частицы при нажатии", style.particles) { onStyle(style.copy(particles = it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Круги", "Конфетти", "Искры").forEachIndexed { i, name ->
                    FilterChip(
                        selected = style.particleShape == i,
                        onClick = { onStyle(style.copy(particleShape = i)) },
                        label = { Text(name) }
                    )
                }
            }
            SwitchRow("Вибрация", style.haptics) { onStyle(style.copy(haptics = it)) }

            OutlinedButton(
                onClick = { onStyle(CalcStyle().copy(bgVersion = style.bgVersion)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Сбросить настройки") }

            Box(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    label: (Float) -> String = { "%.2f".format(it) },
    onChange: (Float) -> Unit
) {
    Column {
        Row {
            Text(title, color = Color.White, modifier = Modifier.weight(1f))
            Text(label(value), color = Color.White.copy(alpha = 0.6f))
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ColorRow(title: String, color: Int, onChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
        }
        if (expanded) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PALETTE.forEach { c ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            .clickable { onChange(c.toArgb()) }
                    )
                }
            }
            val c = Color(color)
            SliderRow("Красный", c.red, 0f..1f) { onChange(Color(it, c.green, c.blue, 1f).toArgb()) }
            SliderRow("Зелёный", c.green, 0f..1f) { onChange(Color(c.red, it, c.blue, 1f).toArgb()) }
            SliderRow("Синий", c.blue, 0f..1f) { onChange(Color(c.red, c.green, it, 1f).toArgb()) }
        }
    }
}
