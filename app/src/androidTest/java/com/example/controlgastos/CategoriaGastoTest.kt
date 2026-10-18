package com.example.controlgastos

import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class CategoriaGastoTest {
    @Test fun categoriaObligatoriaMuestraErrorYSeLimpiaAlSeleccionar() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(instrumentation.targetContext, FormularioGastoActivity::class.java)
            .putExtra(GastosActivity.EXTRA_USUARIO_ID, Long.MAX_VALUE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent)
        try {
            instrumentation.runOnMainSync {
                activity.findViewById<EditText>(R.id.etDescripcionGasto).setText("Prueba categoría")
                activity.findViewById<EditText>(R.id.etMontoGasto).setText("1000")
                activity.findViewById<Button>(R.id.btnGuardarGasto).performClick()
                val error = activity.findViewById<TextView>(R.id.tvErrorCategoria)
                assertEquals("Selecciona una categoría", error.text.toString())
                assertEquals(View.VISIBLE, error.visibility)
                assertFalse(activity.isFinishing)
                assertEquals("Guardar gasto", activity.findViewById<Button>(R.id.btnGuardarGasto).text.toString())
                activity.findViewById<Spinner>(R.id.spCategoriaGasto).setSelection(1)
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertEquals(View.GONE, activity.findViewById<TextView>(R.id.tvErrorCategoria).visibility)
                assertEquals("", activity.findViewById<TextView>(R.id.tvErrorCategoria).text.toString())
            }
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
}
