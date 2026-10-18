package com.example.controlgastos.ui.registro

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.data.CorreoRegistradoException
import com.example.controlgastos.data.UsuarioRepository
import com.example.controlgastos.data.local.ControlGastosDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

enum class CampoRegistro { NOMBRE, CORREO, CONTRASENA, CONFIRMACION }

data class EstadoRegistro(
    val guardando: Boolean = false,
    val errores: Map<CampoRegistro, String> = emptyMap(),
    val mensaje: String? = null,
    val cuentaCreada: Boolean = false
)

class RegistroViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UsuarioRepository(
        ControlGastosDatabase.obtener(application).usuarioDao()
    )
    private val estadoMutable = MutableLiveData(EstadoRegistro())
    val estado: LiveData<EstadoRegistro> = estadoMutable

    fun registrar(nombre: String, correo: String, contrasena: String, confirmacion: String) {
        if (estadoMutable.value?.guardando == true || estadoMutable.value?.cuentaCreada == true) return
        val errores = mutableMapOf<CampoRegistro, String>()
        if (nombre.isBlank()) errores[CampoRegistro.NOMBRE] = "Ingresa tu nombre"
        if (!Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()) {
            errores[CampoRegistro.CORREO] = "Ingresa un correo válido"
        }
        if (contrasena.isBlank() || contrasena.length < 6) {
            errores[CampoRegistro.CONTRASENA] = "Usa una contraseña de mínimo 6 caracteres"
        }
        if (confirmacion.isEmpty() || contrasena != confirmacion) {
            errores[CampoRegistro.CONFIRMACION] = "Las contraseñas deben coincidir"
        }
        if (errores.isNotEmpty()) {
            estadoMutable.value = EstadoRegistro(errores = errores)
            return
        }
        estadoMutable.value = EstadoRegistro(guardando = true)
        viewModelScope.launch {
            try {
                repository.registrar(nombre, correo, contrasena)
                estadoMutable.value = EstadoRegistro(cuentaCreada = true)
            } catch (error: CorreoRegistradoException) {
                estadoMutable.value = EstadoRegistro(
                    errores = mapOf(CampoRegistro.CORREO to "Este correo ya está registrado")
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                estadoMutable.value = EstadoRegistro(
                    mensaje = "No se pudo crear la cuenta. Intenta nuevamente."
                )
            }
        }
    }

    fun consumirResultado() {
        estadoMutable.value = EstadoRegistro()
    }
}
