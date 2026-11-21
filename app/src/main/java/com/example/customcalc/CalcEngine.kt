package com.example.customcalc

import java.math.BigDecimal
import java.math.RoundingMode

/** Состояние ввода калькулятора. */
data class InputState(
    val expr: String = "",
    val fresh: Boolean = false,   // только что нажали "=" — на экране результат
    val error: Boolean = false
)

private const val OPS = "+−×÷"
private fun Char.isNumChar() = isDigit() || this == '.'

object CalcEngine {

    /** Вычисляет выражение вида 2+3×(4−1)÷5%. */
    fun evaluate(expr: String): Double {
        val s = expr.replace('×', '*').replace('÷', '/').replace('−', '-').replace(',', '.')
        val p = Parser(s)
        val v = p.parseExpression()
        if (p.pos < s.length) throw IllegalArgumentException("syntax")
        return v
    }

    fun format(v: Double): String {
        if (v.isNaN() || v.isInfinite()) throw ArithmeticException()
        val bd = BigDecimal(v).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
        return bd.toPlainString().replace('-', '−')
    }

    private class Parser(val s: String) {
        var pos = 0
        fun peek(): Char? = if (pos < s.length) s[pos] else null

        fun parseExpression(): Double {
            var v = parseTerm()
            while (true) {
                when (peek()) {
                    '+' -> { pos++; v += parseTerm() }
                    '-' -> { pos++; v -= parseTerm() }
                    else -> return v
                }
            }
        }

        fun parseTerm(): Double {
            var v = parseFactor()
            while (true) {
                when (peek()) {
                    '*' -> { pos++; v *= parseFactor() }
                    '/' -> {
                        pos++
                        val d = parseFactor()
                        if (d == 0.0) throw ArithmeticException("div0")
                        v /= d
                    }
                    else -> return v
                }
            }
        }

        fun parseFactor(): Double {
            var v = parseUnary()
            while (peek() == '%') { pos++; v /= 100.0 }
            return v
        }

        fun parseUnary(): Double = when (peek()) {
            '-' -> { pos++; -parseUnary() }
            '+' -> { pos++; parseUnary() }
            else -> parsePrimary()
        }

        fun parsePrimary(): Double {
            if (peek() == '(') {
                pos++
                val v = parseExpression()
                if (peek() == ')') pos++
                return v
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (start == pos) throw IllegalArgumentException("syntax")
            return s.substring(start, pos).toDouble()
        }
    }
}

/** Убирает хвостовые операторы и закрывает скобки — для "=" и предпросмотра. */
fun cleanExpr(e: String): String {
    var s = e
    while (s.isNotEmpty() && (s.last() in OPS || s.last() == '(')) s = s.dropLast(1)
    val missing = s.count { it == '(' } - s.count { it == ')' }
    return s + ")".repeat(maxOf(0, missing))
}

/** Применяет нажатие кнопки к состоянию. */
fun applyKey(st0: InputState, key: String): InputState {
    val st = if (st0.error) st0.copy(error = false) else st0
    val startsNew = st.fresh &&
        ((key.length == 1 && (key[0].isDigit() || key[0] == '.')) || key == "( )")
    val e = if (startsNew) "" else st.expr
    val last = e.lastOrNull()

    return when {
        key.length == 1 && key[0].isDigit() -> {
            val num = e.takeLastWhile { it.isNumChar() }
            val ne = when {
                num == "0" -> e.dropLast(1) + key
                last == ')' || last == '%' -> "$e×$key"
                else -> e + key
            }
            InputState(ne)
        }

        key == "." -> {
            val num = e.takeLastWhile { it.isNumChar() }
            when {
                '.' in num -> st
                num.isNotEmpty() -> InputState("$e.")
                last == ')' || last == '%' -> InputState("$e×0.")
                else -> InputState("${e}0.")
            }
        }

        key.length == 1 && key[0] in OPS -> when {
            e.isEmpty() -> if (key == "−") InputState("−") else st
            last == '(' -> if (key == "−") InputState(e + key) else st
            last != null && last in OPS -> {
                val head = e.dropLast(1)
                if (head.isEmpty() || head.last() == '(') st else InputState(head + key)
            }
            else -> InputState(e + key)
        }

        key == "%" ->
            if (last != null && (last.isDigit() || last == ')')) InputState("$e%") else st

        key == "( )" -> {
            val open = e.count { it == '(' }
            val close = e.count { it == ')' }
            val afterValue = last != null && (last.isDigit() || last == ')' || last == '%')
            when {
                open > close && afterValue -> InputState("$e)")
                afterValue -> InputState("$e×(")
                else -> InputState("$e(")
            }
        }

        key == "⌫" -> if (st.fresh) InputState() else InputState(e.dropLast(1))

        key == "AC" -> InputState()

        key == "=" -> {
            if (e.isBlank()) st else try {
                InputState(CalcEngine.format(CalcEngine.evaluate(cleanExpr(e))), fresh = true)
            } catch (x: Exception) {
                st.copy(error = true)
            }
        }

        else -> st
    }
}

/** Живой предпросмотр результата под выражением. */
fun previewOf(st: InputState): String {
    if (st.fresh || st.expr.isEmpty()) return ""
    val c = cleanExpr(st.expr)
    if (c.isEmpty()) return ""
    if (c.drop(1).none { it in "+−×÷%()" }) return ""
    return try {
        "= " + CalcEngine.format(CalcEngine.evaluate(c))
    } catch (e: Exception) {
        ""
    }
}
