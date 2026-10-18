package com.example.controlgastos

import android.app.DatePickerDialog
import android.os.Bundle
import android.content.res.ColorStateList
import android.graphics.Rect
import android.widget.AdapterView
import com.google.android.material.color.MaterialColors
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.controlgastos.ui.gastos.FormularioGastoViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FormularioGastoActivity : AppCompatActivity() {
    private val viewModel: FormularioGastoViewModel by viewModels()
    private var datosAplicados = false
    private var fecha = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(java.util.Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val usuarioId = intent.getLongExtra(GastosActivity.EXTRA_USUARIO_ID, 0)
        if (usuarioId <= 0) { finish(); return }
        fecha = savedInstanceState?.getString("fecha") ?: fecha
        val gastoId = intent.getLongExtra(DetalleGastoActivity.EXTRA_GASTO_ID, 0)
        datosAplicados = savedInstanceState?.getBoolean("datosAplicados") ?: false
        enableEdgeToEdge()
        setContentView(R.layout.activity_formulario_gasto)
        ajustarBarrasDelSistema()
        findViewById<TextView>(R.id.tvTituloFormularioGasto).text =
            if (gastoId > 0) "Editar gasto" else "Añadir gasto"
        val main = findViewById<android.view.View>(R.id.main)
        val padding = main.paddingLeft
        ViewCompat.setOnApplyWindowInsetsListener(main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(padding + bars.left, padding + bars.top,
                padding + bars.right, padding + maxOf(bars.bottom, ime.bottom))
            insets
        }
        val descripcion = findViewById<EditText>(R.id.etDescripcionGasto)
        val monto = findViewById<EditText>(R.id.etMontoGasto)
        val categoria = findViewById<Spinner>(R.id.spCategoriaGasto)
        val errorCategoria = findViewById<TextView>(R.id.tvErrorCategoria)
        val tintCategoria = ColorStateList.valueOf(MaterialColors.getColor(categoria, com.google.android.material.R.attr.colorOnSurfaceVariant))
        val colorError = MaterialColors.getColor(errorCategoria, com.google.android.material.R.attr.colorError)
        errorCategoria.setTextColor(colorError)
        categoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position > 0) viewModel.limpiarErrorCategoria()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        val fechaBoton = findViewById<Button>(R.id.btnFechaGasto)
        val guardar = findViewById<Button>(R.id.btnGuardarGasto)
        val cancelar = findViewById<Button>(R.id.btnCancelarGasto)
        categoria.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Selecciona una categoría", "Alimentación", "Transporte", "Servicios", "Salud", "Educación", "Ocio", "Otros"))
        fechaBoton.text = "Fecha: $fecha"
        fechaBoton.setOnClickListener {
            val calendar = Calendar.getInstance().apply {
                time = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(fecha)!!
            }
            DatePickerDialog(this, { _, year, month, day ->
                fecha = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day)
                fechaBoton.text = "Fecha: $fecha"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        cancelar.setOnClickListener { finish() }
        guardar.setOnClickListener {
            viewModel.guardar(usuarioId, descripcion.text.toString(), monto.text.toString(),
                if (categoria.selectedItemPosition > 0) categoria.selectedItem.toString() else "", fecha)
            viewModel.estado.value?.errores?.get("categoria")?.let { mensaje ->
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        }
        viewModel.estado.observe(this) { estado ->
            if (estado.gastoInicial != null && !datosAplicados) {
                val gasto = estado.gastoInicial
                descripcion.setText(gasto.descripcion)
                monto.setText(BigDecimal.valueOf(gasto.montoCentavos, 2).toPlainString())
                val adapter = categoria.adapter as ArrayAdapter<String>
                val posicion = adapter.getPosition(gasto.categoria)
                categoria.setSelection(if (posicion >= 0) posicion else 0)
                fecha = gasto.fecha
                fechaBoton.text = "Fecha: $fecha"
                datosAplicados = true
            }
            val habilitado = !estado.cargando && !estado.guardando && !estado.noDisponible
            guardar.isEnabled = habilitado
            guardar.text = if (estado.cargando) "Cargando…" else if (estado.guardando) "Guardando…" else if (gastoId > 0) "Guardar cambios" else "Guardar gasto"
            cancelar.isEnabled = !estado.guardando
            descripcion.isEnabled = habilitado
            monto.isEnabled = habilitado
            categoria.isEnabled = habilitado
            fechaBoton.isEnabled = habilitado
            descripcion.error = estado.errores["descripcion"]
            monto.error = estado.errores["monto"]
            val faltaCategoria = estado.errores["categoria"]
            errorCategoria.text = faltaCategoria.orEmpty()
            errorCategoria.visibility = if (faltaCategoria != null) android.view.View.VISIBLE else android.view.View.GONE
            categoria.backgroundTintList = if (faltaCategoria != null) ColorStateList.valueOf(colorError) else tintCategoria
            if (estado.errores.containsKey("descripcion")) descripcion.requestFocus()
            else if (estado.errores.containsKey("monto")) monto.requestFocus()
            else if (faltaCategoria != null) {
                ViewCompat.getWindowInsetsController(main)?.hide(WindowInsetsCompat.Type.ime())
                categoria.requestFocus()
                errorCategoria.post {
                    errorCategoria.requestRectangleOnScreen(Rect(0, 0, errorCategoria.width, errorCategoria.height), false)
                }
            }
            if (estado.noDisponible) {
                Toast.makeText(this, estado.mensaje ?: "El gasto ya no está disponible", Toast.LENGTH_LONG).show()
                finish()
                return@observe
            }
            if (estado.guardado) {
                Toast.makeText(this, if (gastoId > 0) "Gasto actualizado" else "Gasto guardado", Toast.LENGTH_SHORT).show()
                viewModel.consumirResultado()
                finish()
            } else if (estado.mensaje != null) {
                Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                viewModel.consumirResultado()
            }
        }
        viewModel.preparar(usuarioId, gastoId)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("fecha", fecha)
        outState.putBoolean("datosAplicados", datosAplicados)
        super.onSaveInstanceState(outState)
    }
}
