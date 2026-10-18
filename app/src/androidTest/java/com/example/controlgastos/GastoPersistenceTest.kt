package com.example.controlgastos

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.controlgastos.data.GastoRepository
import com.example.controlgastos.data.local.ControlGastosDatabase
import com.example.controlgastos.data.local.Gasto
import com.example.controlgastos.data.local.Usuario
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GastoPersistenceTest {
    @Test fun migracionConservaUsuarioYPermiteCrearGastos() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nombre = "migracion-test-${UUID.randomUUID()}.db"
        val file = context.getDatabasePath(nombre)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL, correo TEXT NOT NULL, contrasenaHash TEXT NOT NULL, contrasenaSalt TEXT NOT NULL, creadoEn INTEGER NOT NULL)")
            old.execSQL("CREATE UNIQUE INDEX index_usuarios_correo ON usuarios (correo)")
            old.execSQL("INSERT INTO usuarios VALUES (1, 'Ana', 'ana@example.com', 'hash-original', 'salt-original', 100)")
            old.version = 1
        }
        var db = Room.databaseBuilder(context, ControlGastosDatabase::class.java, nombre)
            .addMigrations(ControlGastosDatabase.MIGRATION_1_2).build()
        try {
            val usuario = db.usuarioDao().buscarPorId(1)!!
            assertEquals("ana@example.com", usuario.correo)
            assertEquals("hash-original", usuario.contrasenaHash)
            val repo = GastoRepository(db.gastoDao())
            repo.crear(Gasto(usuarioId = 1, descripcion = "Almuerzo", montoCentavos = 1250000,
                categoria = "Alimentación", fecha = "2026-10-12"))
            assertEquals("Almuerzo", repo.observar(1).first().single().descripcion)
            db.close()
            db = Room.databaseBuilder(context, ControlGastosDatabase::class.java, nombre)
                .addMigrations(ControlGastosDatabase.MIGRATION_1_2).build()
            assertEquals(1250000L, db.gastoDao().observarPorUsuario(1).first().single().montoCentavos)
        } finally {
            db.close()
            context.deleteDatabase(nombre)
        }
    }

    @Test fun cadaUsuarioVeSusGastosYNoLosDeOtros() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, ControlGastosDatabase::class.java).build()
        try {
            val ana = db.usuarioDao().insertar(Usuario(nombre = "Ana", correo = "ana@example.com",
                contrasenaHash = "hash", contrasenaSalt = "salt"))
            val luis = db.usuarioDao().insertar(Usuario(nombre = "Luis", correo = "luis@example.com",
                contrasenaHash = "hash", contrasenaSalt = "salt"))
            val repo = GastoRepository(db.gastoDao())
            assertTrue(repo.observar(ana).first().isEmpty())
            repo.crear(Gasto(usuarioId = ana, descripcion = "Bus", montoCentavos = 300000,
                categoria = "Transporte", fecha = "2026-10-12"))
            assertEquals(1, repo.observar(ana).first().size)
            assertTrue(repo.observar(luis).first().isEmpty())
        } finally { db.close() }
    }
}
