package com.example.controlgastos.data

import com.example.controlgastos.data.local.Gasto
import com.example.controlgastos.data.local.GastoDao

class GastoRepository(private val dao: GastoDao) {
    fun observar(usuarioId: Long) = dao.observarPorUsuario(usuarioId)
    fun observarDetalle(id: Long, usuarioId: Long) = dao.observarDetalle(id, usuarioId)
    suspend fun buscar(id: Long, usuarioId: Long) = dao.buscarPorId(id, usuarioId)
    suspend fun crear(gasto: Gasto): Long {
        validar(gasto)
        return dao.insertar(gasto)
    }
    suspend fun actualizar(gasto: Gasto): Boolean {
        validar(gasto)
        require(gasto.id > 0)
        return dao.actualizar(gasto.id, gasto.usuarioId, gasto.descripcion,
            gasto.montoCentavos, gasto.categoria, gasto.fecha) == 1
    }
    suspend fun eliminar(id: Long, usuarioId: Long): Boolean {
        require(id > 0 && usuarioId > 0)
        return dao.eliminar(id, usuarioId) == 1
    }
    private fun validar(gasto: Gasto) {
        require(gasto.usuarioId > 0 && gasto.descripcion.isNotBlank() && gasto.montoCentavos > 0)
        require(gasto.categoria.isNotBlank() && gasto.fecha.isNotBlank())
    }
}
