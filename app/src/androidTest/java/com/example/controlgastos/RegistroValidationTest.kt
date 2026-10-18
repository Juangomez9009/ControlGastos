package com.example.controlgastos

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.example.controlgastos.ui.registro.CampoRegistro
import com.example.controlgastos.ui.registro.RegistroViewModel
import org.junit.Assert.*
import org.junit.Test

class RegistroValidationTest {
    @Test
    fun camposVaciosNoInicianGuardado() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val vm = RegistroViewModel(instrumentation.targetContext.applicationContext as Application)
            vm.registrar("", "", "", "")
            val estado = vm.estado.value!!
            assertEquals(CampoRegistro.entries.toSet(), estado.errores.keys)
            assertFalse(estado.guardando)
            assertFalse(estado.cuentaCreada)
        }
    }

    @Test
    fun correoInvalidoYContrasenasDistintasSeRechazan() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val vm = RegistroViewModel(instrumentation.targetContext.applicationContext as Application)
            vm.registrar("Ana", "correo-invalido", "123", "456")
            val estado = vm.estado.value!!
            assertEquals(
                setOf(CampoRegistro.CORREO, CampoRegistro.CONTRASENA, CampoRegistro.CONFIRMACION),
                estado.errores.keys
            )
            assertFalse(estado.guardando)
        }
    }
}
