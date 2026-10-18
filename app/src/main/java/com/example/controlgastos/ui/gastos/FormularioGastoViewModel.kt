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

data class EstadoFormularioGasto(
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val gastoInicial: Gasto? = null,
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null,
    val guardado: Boolean = false,
    val noDisponible: Boolean = false
)

class FormularioGastoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GastoRepository(ControlGastosDatabase.obtener(application).gastoDao())
    private val mutable = MutableLiveData(EstadoFormularioGasto())
    val estado: LiveData<EstadoFormularioGasto> = mutable
    private var preparado = false
    private var gastoId = 0L
    private var usuarioActual = 0L

    fun preparar(usuarioId: Long, id: Long) {
        if (preparado) return
        preparado = true
        gastoId = id
        usuarioActual = usuarioId
        if (id <= 0) return
        mutable.value = EstadoFormularioGasto(cargando = true)
        viewModelScope.launch {
            try {
                val gasto = repository.buscar(id, usuarioId)
                mutable.value = if (gasto != null) EstadoFormularioGasto(gastoInicial = gasto)
                    else EstadoFormularioGasto(noDisponible = true)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                mutable.value = EstadoFormularioGasto(noDisponible = true,
                    mensaje = "No se pudo cargar el gasto. Intenta nuevamente.")
            }
        }
    }

    fun guardar(usuarioId: Long, descripcion: String, monto: String, categoria: String, fecha: String) {
        val actual = mutable.value ?: return
        if (!preparado || actual.cargando || actual.guardando || actual.guardado || actual.noDisponible) return
        val errores = mutableMapOf<String, String>()
        val centavos = ValidadorGasto.montoEnCentavos(monto)
        if (descripcion.isBlank()) errores["descripcion"] = "Ingresa una descripción"
        if (centavos == null) errores["monto"] = "Ingresa un monto mayor que cero con máximo 2 decimales"
        if (categoria.isBlank()) errores["categoria"] = "Selecciona una categoría"
        if (!ValidadorGasto.fechaValida(fecha)) errores["fecha"] = "Selecciona una fecha válida"
        if (usuarioId <= 0 || usuarioId != usuarioActual) {
            mutable.value = actual.copy(mensaje = "Inicia sesión nuevamente.")
            return
        }
        if (errores.isNotEmpty()) { mutable.value = actual.copy(errores = errores); return }
        mutable.value = actual.copy(guardando = true, errores = emptyMap(), mensaje = null)
        viewModelScope.launch {
            try {
                val gasto = Gasto(id = gastoId, usuarioId = usuarioId, descripcion = descripcion.trim(),
                    montoCentavos = centavos!!, categoria = categoria, fecha = fecha)
                if (gastoId == 0L) repository.crear(gasto)
                else if (!repository.actualizar(gasto)) {
                    mutable.value = actual.copy(noDisponible = true)
                    return@launch
                }
                mutable.value = actual.copy(guardado = true, errores = emptyMap())
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                mutable.value = actual.copy(mensaje = "No se pudo guardar el gasto. Intenta nuevamente.")
            }
        }
    }
    fun limpiarErrorCategoria() {
        val actual = mutable.value ?: return
        if (actual.errores.containsKey("categoria")) {
            mutable.value = actual.copy(errores = actual.errores - "categoria")
        }
    }
    fun consumirResultado() { mutable.value = mutable.value?.copy(mensaje = null, guardado = false) }
}
