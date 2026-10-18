package com.example.controlgastos

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.controlgastos.ui.registro.CampoRegistro
import com.example.controlgastos.ui.registro.RegistroViewModel

class RegistroActivity : AppCompatActivity() {
    private val viewModel: RegistroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)
        ajustarBarrasDelSistema()
        val main = findViewById<android.view.View>(R.id.main)
        val padding = main.paddingLeft
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                padding + bars.left, padding + bars.top,
                padding + bars.right, padding + maxOf(bars.bottom, keyboard.bottom)
            )
            insets
        }
        val nombre = findViewById<EditText>(R.id.etNombre)
        val correo = findViewById<EditText>(R.id.etCorreoRegistro)
        val contrasena = findViewById<EditText>(R.id.etContrasenaRegistro)
        val confirmacion = findViewById<EditText>(R.id.etConfirmarContrasena)
        val registrar = findViewById<Button>(R.id.btnRegistrarse)
        val volver = findViewById<TextView>(R.id.tvVolverLogin)
        val campos = linkedMapOf(
            CampoRegistro.NOMBRE to nombre,
            CampoRegistro.CORREO to correo,
            CampoRegistro.CONTRASENA to contrasena,
            CampoRegistro.CONFIRMACION to confirmacion
        )
        volver.setOnClickListener { finish() }
        registrar.setOnClickListener {
            viewModel.registrar(
                nombre.text.toString(), correo.text.toString(),
                contrasena.text.toString(), confirmacion.text.toString()
            )
        }
        viewModel.estado.observe(this) { estado ->
            registrar.isEnabled = !estado.guardando
            registrar.text = if (estado.guardando) "Guardando…" else "Registrarse"
            volver.isEnabled = !estado.guardando
            campos.values.forEach { it.isEnabled = !estado.guardando }
            campos.forEach { (campo, input) -> input.error = estado.errores[campo] }
            estado.errores.keys.firstOrNull()?.let { campos[it]?.requestFocus() }
            if (estado.cuentaCreada) {
                Toast.makeText(this, "Cuenta creada. Ahora inicia sesión.", Toast.LENGTH_LONG).show()
                viewModel.consumirResultado()
                finish()
            } else if (estado.mensaje != null) {
                Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                viewModel.consumirResultado()
            }
        }
    }
}
