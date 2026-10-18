package com.example.controlgastos.ui.login

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.controlgastos.data.UsuarioRepository
import com.example.controlgastos.data.local.ControlGastosDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class EstadoLogin(
    val cargando: Boolean = false,
    val errorCorreo: String? = null,
    val errorContrasena: String? = null,
    val mensaje: String? = null,
    val usuarioId: Long? = null,
    val nombre: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UsuarioRepository(ControlGastosDatabase.obtener(application).usuarioDao())
    private val estadoMutable = MutableLiveData(EstadoLogin())
    val estado: LiveData<EstadoLogin> = estadoMutable

    fun ingresar(correo: String, contrasena: String) {
        if (estadoMutable.value?.cargando == true || estadoMutable.value?.usuarioId != null) return
        val errorCorreo = if (Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()) null
            else "Ingresa un correo válido"
        val errorContrasena = if (contrasena.isBlank() || contrasena.length < 6)
            "Ingresa tu contraseña de mínimo 6 caracteres" else null
        if (errorCorreo != null || errorContrasena != null) {
            estadoMutable.value = EstadoLogin(errorCorreo = errorCorreo, errorContrasena = errorContrasena)
            return
        }
        estadoMutable.value = EstadoLogin(cargando = true)
        viewModelScope.launch {
            try {
                val usuario = repository.iniciarSesion(correo, contrasena)
                estadoMutable.value = if (usuario == null) {
                    EstadoLogin(mensaje = "Correo o contraseña incorrectos")
                } else {
                    EstadoLogin(usuarioId = usuario.id, nombre = usuario.nombre)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                estadoMutable.value = EstadoLogin(mensaje = "No se pudo iniciar sesión. Intenta nuevamente.")
            }
        }
    }

    fun consumirResultado() {
        estadoMutable.value = EstadoLogin()
    }
}
