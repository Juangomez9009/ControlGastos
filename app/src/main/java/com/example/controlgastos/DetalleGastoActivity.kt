package com.example.controlgastos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.controlgastos.ui.gastos.DetalleGastoViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class DetalleGastoActivity : AppCompatActivity() {
    private val viewModel: DetalleGastoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(EXTRA_GASTO_ID, 0)
        val usuarioId = intent.getLongExtra(GastosActivity.EXTRA_USUARIO_ID, 0)
        if (id <= 0 || usuarioId <= 0) { finish(); return }
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_gasto)
        ajustarBarrasDelSistema()
        val main = findViewById<android.view.View>(R.id.main)
        val padding = main.paddingLeft
        ViewCompat.setOnApplyWindowInsetsListener(main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(padding + bars.left, padding + bars.top,
                padding + bars.right, padding + bars.bottom)
            insets
        }
        val descripcion = findViewById<TextView>(R.id.tvDetalleDescripcion)
        val monto = findViewById<TextView>(R.id.tvDetalleMonto)
        val categoria = findViewById<TextView>(R.id.tvDetalleCategoria)
        val fecha = findViewById<TextView>(R.id.tvDetalleFecha)
        val editar = findViewById<Button>(R.id.btnEditarGasto)
        val eliminar = findViewById<Button>(R.id.btnEliminarGasto)
        val volver = findViewById<Button>(R.id.btnVolverGastos)
        volver.setOnClickListener { finish() }
        editar.setOnClickListener {
            startActivity(Intent(this, FormularioGastoActivity::class.java)
                .putExtra(EXTRA_GASTO_ID, id).putExtra(GastosActivity.EXTRA_USUARIO_ID, usuarioId))
        }
        eliminar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Eliminar gasto")
                .setMessage("¿Quieres eliminar este gasto? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar") { _, _ -> viewModel.eliminar() }
                .show()
        }
        viewModel.estado.observe(this) { estado ->
            val habilitado = !estado.cargando && !estado.borrando && estado.gasto != null
            editar.isEnabled = habilitado
            eliminar.isEnabled = habilitado
            volver.isEnabled = !estado.borrando
            eliminar.text = if (estado.borrando) "Eliminando…" else "Eliminar"
            if (estado.eliminado) {
                Toast.makeText(this, estado.mensaje ?: "Gasto eliminado", Toast.LENGTH_SHORT).show()
                finish()
                return@observe
            }
            if (estado.errorCarga || (!estado.cargando && !estado.borrando && estado.gasto == null)) {
                Toast.makeText(this, estado.mensaje ?: "El gasto ya no está disponible", Toast.LENGTH_LONG).show()
                finish()
                return@observe
            }
            estado.gasto?.let { gasto ->
                val formato = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO")).apply {
                    minimumFractionDigits = 2; maximumFractionDigits = 2
                }
                descripcion.text = gasto.descripcion
                monto.text = "$ " + formato.format(BigDecimal.valueOf(gasto.montoCentavos, 2))
                categoria.text = "Categoría: ${gasto.categoria}"
                fecha.text = "Fecha: ${gasto.fecha}"
            }
            if (estado.mensaje != null) {
                Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                viewModel.consumirMensaje()
            }
        }
        viewModel.cargar(id, usuarioId)
    }
    companion object { const val EXTRA_GASTO_ID = "gasto_id" }
}
