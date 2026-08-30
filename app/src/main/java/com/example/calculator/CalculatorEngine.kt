package com.example.calculator

import java.text.DecimalFormat
import kotlin.math.*

object CalculatorEngine {

    private val format = DecimalFormat("#,##0.########").apply {
        isGroupingUsed = false
    }

    data class EvaluationResult(
        val success: Boolean,
        val resultString: String,
        val numericValue: Double? = null,
        val errorMessage: String? = null
    )

    fun evaluate(expression: String): EvaluationResult {
        if (expression.isBlank()) {
            return EvaluationResult(true, "0", 0.0)
        }

        try {
            val sanitized = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("π", "${Math.PI}")
                .replace("e", "${Math.E}")

            val result = Parser(sanitized).parse()
            if (result.isNaN() || result.isInfinite()) {
                return EvaluationResult(false, "Error", null, "Math Error")
            }

            // Format result
            val formatted = if (result == result.toLong().toDouble() && !result.toString().contains("E")) {
                result.toLong().toString()
            } else {
                format.format(result)
            }

            return EvaluationResult(true, formatted, result)
        } catch (e: ArithmeticException) {
            return EvaluationResult(false, "Error", null, e.message ?: "Arithmetic Error")
        } catch (e: Exception) {
            return EvaluationResult(false, "Error", null, "Invalid Expression")
        }
    }

    // Recursive descent expression evaluator
    private class Parser(private val str: String) {
        private var pos = -1
        private var ch = ' '

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected: $ch")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*') -> x *= parseFactor()
                    eat('/') -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Cannot divide by 0")
                        x /= divisor
                    }
                    eat('%') -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+')) return parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpression()
                eat(')')
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (ch in 'a'..'z' || ch in 'A'..'Z' || ch == '√') {
                while (ch in 'a'..'z' || ch in 'A'..'Z' || ch == '√') nextChar()
                val func = str.substring(startPos, pos)
                x = parseFactor()
                x = when (func) {
                    "sqrt", "√" -> sqrt(x)
                    "sin" -> sin(Math.toRadians(x))
                    "cos" -> cos(Math.toRadians(x))
                    "tan" -> tan(Math.toRadians(x))
                    "log" -> log10(x)
                    "ln" -> ln(x)
                    "abs" -> abs(x)
                    else -> throw RuntimeException("Unknown function: $func")
                }
            } else {
                throw RuntimeException("Unexpected: $ch")
            }

            if (eat('^')) x = x.pow(parseFactor())
            if (eat('!')) x = factorial(x.toInt()).toDouble()

            return x
        }

        private fun factorial(n: Int): Long {
            if (n < 0) throw ArithmeticException("Factorial of negative")
            if (n > 20) throw ArithmeticException("Overflow")
            var res = 1L
            for (i in 2..n) res *= i
            return res
        }
    }
}
