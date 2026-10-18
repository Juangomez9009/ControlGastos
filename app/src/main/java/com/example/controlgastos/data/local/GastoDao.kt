package com.example.controlgastos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GastoDao {
    @Insert suspend fun insertar(gasto: Gasto): Long

    @Query("SELECT * FROM gastos WHERE usuarioId = :usuarioId ORDER BY fecha DESC, id DESC")
    fun observarPorUsuario(usuarioId: Long): Flow<List<Gasto>>

    @Query("SELECT * FROM gastos WHERE id = :id AND usuarioId = :usuarioId LIMIT 1")
    suspend fun buscarPorId(id: Long, usuarioId: Long): Gasto?

    @Query("SELECT * FROM gastos WHERE id = :id AND usuarioId = :usuarioId LIMIT 1")
    fun observarDetalle(id: Long, usuarioId: Long): Flow<Gasto?>

    @Query("UPDATE gastos SET descripcion = :descripcion, montoCentavos = :montoCentavos, categoria = :categoria, fecha = :fecha WHERE id = :id AND usuarioId = :usuarioId")
    suspend fun actualizar(id: Long, usuarioId: Long, descripcion: String,
        montoCentavos: Long, categoria: String, fecha: String): Int

    @Query("DELETE FROM gastos WHERE id = :id AND usuarioId = :usuarioId")
    suspend fun eliminar(id: Long, usuarioId: Long): Int
}
