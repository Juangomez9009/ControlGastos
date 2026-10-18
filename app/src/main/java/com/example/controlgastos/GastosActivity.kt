package com.example.controlgastos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.controlgastos.ui.gastos.GastoAdapter
import com.example.controlgastos.ui.gastos.GastosViewModel
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class GastosActivity : AppCompatActivity() {
    private val viewModel: GastosViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getLongExtra(EXTRA_USUARIO_ID, 0) <= 0) {
            volverAlLogin()
            return
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_gastos)
        ajustarBarrasDelSistema()
        val main = findViewById<android.view.View>(R.id.main)
        val padding = main.paddingLeft
        ViewCompat.setOnApplyWindowInsetsListener(main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(padding + bars.left, padding + bars.top,
                padding + bars.right, padding + bars.bottom)
            insets
        }
        val usuarioId = intent.getLongExtra(EXTRA_USUARIO_ID, 0)
        val adapter = GastoAdapter { gasto ->
            startActivity(Intent(this, DetalleGastoActivity::class.java)
                .putExtra(DetalleGastoActivity.EXTRA_GASTO_ID, gasto.id)
                .putExtra(EXTRA_USUARIO_ID, usuarioId))
        }
        val lista = findViewById<RecyclerView>(R.id.rvGastos)
        lista.layoutManager = LinearLayoutManager(this)
        lista.adapter = adapter
        val vacio = findViewById<TextView>(R.id.tvGastosVacios)
        findViewById<Button>(R.id.btnAnadirGasto).setOnClickListener {
            startActivity(Intent(this, FormularioGastoActivity::class.java)
                .putExtra(EXTRA_USUARIO_ID, usuarioId))
        }
        viewModel.estado.observe(this) { estado ->
            adapter.submitList(estado.gastos)
            vacio.text = estado.error ?: "Aún no has añadido gastos."
            vacio.visibility = if (estado.gastos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
        viewModel.cargar(usuarioId)
        val nombre = intent.getStringExtra(EXTRA_NOMBRE).orEmpty()
        findViewById<TextView>(R.id.tvBienvenida).text = "Bienvenido, $nombre"
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener { volverAlLogin() }
    }

    private fun volverAlLogin() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    companion object {
        const val EXTRA_USUARIO_ID = "usuario_id"
        const val EXTRA_NOMBRE = "usuario_nombre"
    }
}
