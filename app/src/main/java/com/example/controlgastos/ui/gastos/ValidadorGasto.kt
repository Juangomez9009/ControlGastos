package com.example.controlgastos.ui.gastos

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

object ValidadorGasto {
    fun montoEnCentavos(texto: String): Long? {
        val limpio = texto.trim()
        if (!Regex("\\d+(?:[.,]\\d{1,2})?").matches(limpio)) return null
        return try {
            BigDecimal(limpio.replace(',', '.')).movePointRight(2).longValueExact()
                .takeIf { it > 0 }
        } catch (error: ArithmeticException) { null }
    }

    fun fechaValida(fecha: String): Boolean {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(fecha)) return false
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }.parse(fecha) != null
        } catch (error: java.text.ParseException) { false }
    }
}
