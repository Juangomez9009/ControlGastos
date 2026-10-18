package com.example.controlgastos

import com.example.controlgastos.ui.gastos.ValidadorGasto
import org.junit.Assert.*
import org.junit.Test

class ValidadorGastoTest {
    @Test fun aceptaMontosExactosConPuntoOComa() {
        assertEquals(1250000L, ValidadorGasto.montoEnCentavos("12500"))
        assertEquals(1250050L, ValidadorGasto.montoEnCentavos("12500,50"))
        assertEquals(1250050L, ValidadorGasto.montoEnCentavos(" 12500.50 "))
        assertEquals(1L, ValidadorGasto.montoEnCentavos("0.01"))
    }
    @Test fun rechazaMontosInvalidosYDesbordamientos() {
        listOf("", "0", "0.00", "-1", "1.234", "1,234.56", "abc", "999999999999999999999")
            .forEach { assertNull("Monto inválido: $it", ValidadorGasto.montoEnCentavos(it)) }
    }
    @Test fun rechazaFechasImposibles() {
        assertTrue(ValidadorGasto.fechaValida("2024-02-29"))
        assertFalse(ValidadorGasto.fechaValida("2025-02-29"))
        assertFalse(ValidadorGasto.fechaValida("2026-13-01"))
        assertFalse(ValidadorGasto.fechaValida("2026-10-12extra"))
    }
}
