package com.example.controlgastos

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.controlgastos.data.GastoRepository
import com.example.controlgastos.data.local.ControlGastosDatabase
import com.example.controlgastos.data.local.Gasto
import com.example.controlgastos.data.local.Usuario
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class GastoCrudTest {
    @Test fun listaObservadaCambiaAlCrearEditarYEliminar() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, ControlGastosDatabase::class.java).build()
        val emisiones = Channel<List<Gasto>>(Channel.UNLIMITED)
        val dao = db.usuarioDao()
        val usuario = dao.insertar(Usuario(nombre = "Ana", correo = "ana@example.com",
            contrasenaHash = "hash", contrasenaSalt = "salt"))
        val repo = GastoRepository(db.gastoDao())
        val observador = launch { repo.observar(usuario).collect { emisiones.send(it) } }
        try {
            assertTrue(withTimeout(30000) { emisiones.receive() }.isEmpty())
            val nuevo = Gasto(usuarioId = usuario, descripcion = "Almuerzo", montoCentavos = 1250000,
                categoria = "Alimentación", fecha = "2026-10-12")
            val id = repo.crear(nuevo)
            assertEquals(id, withTimeout(30000) { emisiones.receive() }.single().id)
            assertTrue(repo.actualizar(nuevo.copy(id = id, descripcion = "Cena", montoCentavos = 1800000)))
            val actualizado = withTimeout(30000) { emisiones.receive() }.single()
            assertEquals(id, actualizado.id)
            assertEquals("Cena", actualizado.descripcion)
            assertEquals(1800000L, actualizado.montoCentavos)
            assertEquals("Cena", repo.buscar(id, usuario)!!.descripcion)
            assertTrue(repo.eliminar(id, usuario))
            assertTrue(withTimeout(30000) { emisiones.receive() }.isEmpty())
            assertNull(repo.buscar(id, usuario))
        } finally {
            observador.cancel()
            db.close()
        }
    }

    @Test fun otroUsuarioNoPuedeLeerEditarNiEliminarElGasto() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, ControlGastosDatabase::class.java).build()
        try {
            val ana = db.usuarioDao().insertar(Usuario(nombre = "Ana", correo = "ana@example.com",
                contrasenaHash = "hash", contrasenaSalt = "salt"))
            val luis = db.usuarioDao().insertar(Usuario(nombre = "Luis", correo = "luis@example.com",
                contrasenaHash = "hash", contrasenaSalt = "salt"))
            val repo = GastoRepository(db.gastoDao())
            val gasto = Gasto(usuarioId = ana, descripcion = "Bus", montoCentavos = 300000,
                categoria = "Transporte", fecha = "2026-10-12")
            val id = repo.crear(gasto)
            assertNull(repo.buscar(id, luis))
            assertFalse(repo.actualizar(gasto.copy(id = id, usuarioId = luis, descripcion = "Otro")))
            assertFalse(repo.eliminar(id, luis))
            assertEquals("Bus", repo.buscar(id, ana)!!.descripcion)
            assertFalse(repo.eliminar(id + 1000, ana))
        } finally { db.close() }
    }
}
