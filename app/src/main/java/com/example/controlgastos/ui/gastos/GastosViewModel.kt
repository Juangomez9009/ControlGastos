package com.example.controlgastos.ui.gastos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.example.controlgastos.data.GastoRepository
import com.example.controlgastos.data.local.ControlGastosDatabase
import com.example.controlgastos.data.local.Gasto
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class EstadoListaGastos(val gastos: List<Gasto> = emptyList(), val error: String? = null)

class GastosViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GastoRepository(ControlGastosDatabase.obtener(application).gastoDao())
    private val usuario = MutableLiveData<Long>()
    val estado = usuario.switchMap { id ->
        repository.observar(id).map { EstadoListaGastos(gastos = it) }
            .catch { emit(EstadoListaGastos(error = "No se pudieron cargar los gastos.")) }
            .asLiveData()
    }
    fun cargar(usuarioId: Long) { if (usuario.value != usuarioId) usuario.value = usuarioId }
}
