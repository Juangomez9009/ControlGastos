package com.example.controlgastos

import android.app.Application
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.controlgastos.data.UsuarioRepository
import com.example.controlgastos.data.local.ControlGastosDatabase
import com.example.controlgastos.ui.login.LoginViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LoginTest {
    @Test
    fun aceptaCredencialesCorrectasYRechazaIncorrectas() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, ControlGastosDatabase::class.java).build()
        try {
            val repo = UsuarioRepository(db.usuarioDao())
            val id = repo.registrar("Ana", "ana@example.com", "Prueba123")
            assertEquals(id, repo.iniciarSesion(" ANA@EXAMPLE.COM ", "Prueba123")!!.id)
            assertNull(repo.iniciarSesion("ana@example.com", "Incorrecta123"))
            assertNull(repo.iniciarSesion("otro@example.com", "Prueba123"))
            assertEquals("Ana", db.usuarioDao().buscarPorId(id)!!.nombre)
        } finally { db.close() }
    }

    @Test
    fun formularioVacioNoIniciaAutenticacion() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val vm = LoginViewModel(instrumentation.targetContext.applicationContext as Application)
            vm.ingresar("", "")
            val estado = vm.estado.value!!
            assertNotNull(estado.errorCorreo)
            assertNotNull(estado.errorContrasena)
            assertFalse(estado.cargando)
            assertNull(estado.usuarioId)
        }
    }
}
