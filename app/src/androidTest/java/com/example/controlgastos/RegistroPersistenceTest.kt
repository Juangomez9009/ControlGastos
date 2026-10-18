package com.example.controlgastos

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.controlgastos.data.CorreoRegistradoException
import com.example.controlgastos.data.UsuarioRepository
import com.example.controlgastos.data.local.ControlGastosDatabase
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RegistroPersistenceTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun registroNormalizaCorreoYNoGuardaContrasenaEnTexto() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, ControlGastosDatabase::class.java).build()
        try {
            val repo = UsuarioRepository(db.usuarioDao())
            val id = repo.registrar(" Ana ", " ANA@EXAMPLE.COM ", "Prueba123")
            val usuario = db.usuarioDao().buscarPorId(id)!!
            assertEquals("Ana", usuario.nombre)
            assertEquals("ana@example.com", usuario.correo)
            assertNotEquals("Prueba123", usuario.contrasenaHash)
            assertTrue(usuario.contrasenaHash.isNotBlank())
            assertTrue(usuario.contrasenaSalt.isNotBlank())
            assertTrue(usuario.creadoEn > 0)
            repo.registrar("Luis", "luis@example.com", "Prueba123")
            val otro = db.usuarioDao().buscarPorCorreo("luis@example.com")!!
            assertNotEquals(usuario.contrasenaSalt, otro.contrasenaSalt)
            assertNotEquals(usuario.contrasenaHash, otro.contrasenaHash)
        } finally { db.close() }
    }

    @Test
    fun correoDuplicadoNoReemplazaUsuarioExistente() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, ControlGastosDatabase::class.java).build()
        try {
            val repo = UsuarioRepository(db.usuarioDao())
            repo.registrar("Ana", "ana@example.com", "Prueba123")
            try {
                repo.registrar("Otra", " ANA@EXAMPLE.COM ", "Otra123")
                fail("Se esperaba rechazar el correo duplicado")
            } catch (expected: CorreoRegistradoException) {
                assertEquals("Ana", db.usuarioDao().buscarPorCorreo("ana@example.com")!!.nombre)
            }
        } finally { db.close() }
    }

    @Test
    fun usuarioPermaneceAlReabrirBaseDeDatos() = runBlocking {
        val nombreDb = "registro-test-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, ControlGastosDatabase::class.java, nombreDb).build()
        try {
            val id = UsuarioRepository(db.usuarioDao()).registrar("Ana", "ana@example.com", "Prueba123")
            db.close()
            db = Room.databaseBuilder(context, ControlGastosDatabase::class.java, nombreDb).build()
            assertEquals("ana@example.com", db.usuarioDao().buscarPorId(id)!!.correo)
        } finally {
            db.close()
            context.deleteDatabase(nombreDb)
        }
    }
}
