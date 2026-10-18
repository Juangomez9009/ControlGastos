package com.example.controlgastos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Usuario::class, Gasto::class], version = 2, exportSchema = true)
abstract class ControlGastosDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun gastoDao(): GastoDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `gastos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `usuarioId` INTEGER NOT NULL, `descripcion` TEXT NOT NULL, `montoCentavos` INTEGER NOT NULL, `categoria` TEXT NOT NULL, `fecha` TEXT NOT NULL, FOREIGN KEY(`usuarioId`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gastos_usuarioId` ON `gastos` (`usuarioId`)")
            }
        }
        @Volatile
        private var instancia: ControlGastosDatabase? = null

        fun obtener(context: Context): ControlGastosDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    ControlGastosDatabase::class.java,
                    "control_gastos.db"
                ).addMigrations(MIGRATION_1_2).build().also { instancia = it }
            }
    }
}
