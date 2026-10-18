package com.example.controlgastos.ui.gastos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.data.GastoRepository
import com.example.controlgastos.data.local.ControlGastosDatabase
import com.example.controlgastos.data.local.Gasto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

data class EstadoDetalleGasto(
    val cargando: Boolean = true,
    val gasto: Gasto? = null,
    val borrando: Boolean = false,
    val eliminado: Boolean = false,
    val mensaje: String? = null,
    val errorCarga: Boolean = false
)

class DetalleGastoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GastoRepository(ControlGastosDatabase.obtener(application).gastoDao())
    private val mutable = MutableLiveData(EstadoDetalleGasto())
    val estado: LiveData<EstadoDetalleGasto> = mutable
    private var cargado = false
    private var gastoId = 0L
    private var usuarioId = 0L

    fun cargar(id: Long, usuario: Long) {
        if (cargado) return
        cargado = true
        gastoId = id
        usuarioId = usuario
        viewModelScope.launch {
            try {
                repository.observarDetalle(id, usuario).collect { gasto ->
                    mutable.value = mutable.value!!.copy(gasto = gasto, cargando = false)
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                mutable.value = mutable.value!!.copy(cargando = false, errorCarga = true,
                    mensaje = "No se pudo cargar el gasto. Intenta nuevamente.")
            }
        }
    }

    fun eliminar() {
        val actual = mutable.value ?: return
        if (actual.cargando || actual.borrando || actual.eliminado || actual.gasto == null) return
        mutable.value = actual.copy(borrando = true, mensaje = null)
        viewModelScope.launch {
            try {
                val borrado = repository.eliminar(gastoId, usuarioId)
                mutable.value = mutable.value!!.copy(borrando = false, eliminado = true,
                    mensaje = if (borrado) "Gasto eliminado" else "El gasto ya no está disponible")
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                mutable.value = mutable.value!!.copy(borrando = false,
                    mensaje = "No se pudo eliminar el gasto. Intenta nuevamente.")
            }
        }
    }
    fun consumirMensaje() { mutable.value = mutable.value?.copy(mensaje = null) }
}
