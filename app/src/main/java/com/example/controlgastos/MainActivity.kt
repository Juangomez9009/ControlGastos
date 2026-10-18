package com.example.controlgastos

import android.content.Intent
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
import com.example.controlgastos.ui.login.LoginViewModel

class MainActivity : AppCompatActivity() {
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ajustarBarrasDelSistema()
        val main = findViewById<android.view.View>(R.id.main)
        val padding = main.paddingLeft
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(padding + bars.left, padding + bars.top,
                padding + bars.right, padding + maxOf(bars.bottom, keyboard.bottom))
            insets
        }
        val correo = findViewById<EditText>(R.id.etCorreo)
        val contrasena = findViewById<EditText>(R.id.etContrasena)
        val ingresar = findViewById<Button>(R.id.btnIngresar)
        val registro = findViewById<TextView>(R.id.tvRegistrarse)
        registro.setOnClickListener { startActivity(Intent(this, RegistroActivity::class.java)) }
        ingresar.setOnClickListener {
            viewModel.ingresar(correo.text.toString(), contrasena.text.toString())
        }
        viewModel.estado.observe(this) { estado ->
            ingresar.isEnabled = !estado.cargando
            ingresar.text = if (estado.cargando) "Ingresando…" else "Ingresar"
            correo.isEnabled = !estado.cargando
            contrasena.isEnabled = !estado.cargando
            registro.isEnabled = !estado.cargando
            correo.error = estado.errorCorreo
            contrasena.error = estado.errorContrasena
            if (estado.errorCorreo != null) correo.requestFocus()
            else if (estado.errorContrasena != null) contrasena.requestFocus()
            if (estado.usuarioId != null) {
                val intent = Intent(this, GastosActivity::class.java)
                    .putExtra(GastosActivity.EXTRA_USUARIO_ID, estado.usuarioId)
                    .putExtra(GastosActivity.EXTRA_NOMBRE, estado.nombre)
                contrasena.text.clear()
                viewModel.consumirResultado()
                startActivity(intent)
                finish()
            } else if (estado.mensaje != null) {
                Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                viewModel.consumirResultado()
            }
        }
    }
}
