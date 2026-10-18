package com.example.controlgastos

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.example.controlgastos.data.local.ControlGastosDatabase
import com.example.controlgastos.data.local.Gasto
import com.example.controlgastos.data.local.Usuario
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VerificacionFinalTest {
    private val instrument = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrument.targetContext
    private val db get() = ControlGastosDatabase.obtener(context)
    private var usuario = 0L
    private var gasto = 0L

    @Before fun preparar() = runBlocking {
        usuario = db.usuarioDao().insertar(Usuario(nombre = "Prueba temporal",
            correo = "qa-${UUID.randomUUID()}@example.com", contrasenaHash = "prueba", contrasenaSalt = "prueba"))
        gasto = db.gastoDao().insertar(Gasto(usuarioId = usuario, descripcion = "Gasto de verificación",
            montoCentavos = 1500000, categoria = "Alimentación", fecha = "2026-10-12"))
    }

    @After fun limpiar() {
        instrument.runOnMainSync { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
        if (usuario > 0) db.openHelper.writableDatabase.delete("usuarios", "id = ?", arrayOf(usuario))
    }

    private fun intent(clase: Class<out Activity>, editar: Boolean = false) = Intent(context, clase)
        .putExtra(GastosActivity.EXTRA_USUARIO_ID, usuario)
        .putExtra(GastosActivity.EXTRA_NOMBRE, "Prueba temporal")
        .apply { if (editar) putExtra(DetalleGastoActivity.EXTRA_GASTO_ID, gasto) }

    private fun <A : Activity> esperar(scenario: ActivityScenario<A>, condicion: (A) -> Boolean) {
        val limite = SystemClock.uptimeMillis() + 30000
        while (SystemClock.uptimeMillis() < limite) {
            var listo = false
            scenario.onActivity { listo = condicion(it) }
            if (listo) return
            SystemClock.sleep(100)
        }
        fail("La pantalla no alcanzó el estado esperado")
    }

    private fun rellenar(activity: FormularioGastoActivity) {
        activity.findViewById<EditText>(R.id.etDescripcionGasto).setText("Cambio sin guardar")
        activity.findViewById<EditText>(R.id.etMontoGasto).setText("23456.78")
        activity.findViewById<Spinner>(R.id.spCategoriaGasto).setSelection(2)
        activity.findViewById<Button>(R.id.btnFechaGasto).performClick()
    }

    private fun ventanas() = android.view.inspector.WindowInspector.getGlobalWindowViews().flatMap { recorrer(it) }

    private fun seleccionarFecha() {
        instrument.runOnMainSync {
            ventanas().filterIsInstance<DatePicker>().first().updateDate(2026, 9, 15)
            ventanas().filterIsInstance<Button>().first { it.id == android.R.id.button1 }.performClick()
        }
        instrument.waitForIdleSync()
    }

    private fun ocultarTeclado() {
        instrument.runOnMainSync {
            androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).forEach {
                    androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView)
                        .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
                }
        }
        instrument.waitForIdleSync()
    }

    private fun atras() {
        val ahora = SystemClock.uptimeMillis()
        instrument.uiAutomation.injectInputEvent(android.view.KeyEvent(ahora, ahora,
            android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_BACK, 0), true)
        instrument.uiAutomation.injectInputEvent(android.view.KeyEvent(ahora, SystemClock.uptimeMillis(),
            android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_BACK, 0), true)
        instrument.waitForIdleSync()
    }
    private fun comprobarCampos(activity: FormularioGastoActivity) {
        assertEquals("Cambio sin guardar", activity.findViewById<EditText>(R.id.etDescripcionGasto).text.toString())
        assertEquals("23456.78", activity.findViewById<EditText>(R.id.etMontoGasto).text.toString())
        assertEquals(2, activity.findViewById<Spinner>(R.id.spCategoriaGasto).selectedItemPosition)
        assertEquals("Fecha: 2026-10-15", activity.findViewById<Button>(R.id.btnFechaGasto).text.toString())
    }

    private fun rotarYComprobar(editar: Boolean) {
        ActivityScenario.launch<FormularioGastoActivity>(intent(FormularioGastoActivity::class.java, editar)).use { scenario ->
            esperar(scenario) { it.findViewById<Button>(R.id.btnGuardarGasto).isEnabled }
            scenario.onActivity { rellenar(it) }
            seleccionarFecha()
            ocultarTeclado()
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            esperar(scenario) { it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
            scenario.onActivity { comprobarCampos(it) }
            scenario.recreate()
            scenario.onActivity { comprobarCampos(it) }
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            esperar(scenario) { it.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
            scenario.onActivity { comprobarCampos(it) }
        }
        runBlocking {
            assertEquals(1, db.openHelper.readableDatabase.query("SELECT id FROM gastos WHERE usuarioId = ?", arrayOf(usuario)).use { it.count })
            assertEquals("Gasto de verificación", db.gastoDao().buscarPorId(gasto, usuario)!!.descripcion)
        }
    }

    @Test fun crearConservaTodosLosCamposAlRotarYRecrear() { rotarYComprobar(false) }
    @Test fun editarConservaCambiosSinGuardarAlRotarYRecrear() { rotarYComprobar(true) }

    @Test fun atrasYCancelaEliminacionNoAlteranDatos() {
        ActivityScenario.launch<GastosActivity>(intent(GastosActivity::class.java)).use { scenario ->
            esperar(scenario) { it.findViewById<RecyclerView>(R.id.rvGastos).findViewHolderForAdapterPosition(0) != null }
            val monitorDetalle = instrument.addMonitor(DetalleGastoActivity::class.java.name, null, false)
            scenario.onActivity { it.findViewById<RecyclerView>(R.id.rvGastos).findViewHolderForAdapterPosition(0)!!.itemView.performClick() }
            val detalle = instrument.waitForMonitorWithTimeout(monitorDetalle, 10000) as DetalleGastoActivity
            instrument.removeMonitor(monitorDetalle)
            val limite = SystemClock.uptimeMillis() + 15000
            var habilitado = false
            while (!habilitado && SystemClock.uptimeMillis() < limite) {
                instrument.runOnMainSync { habilitado = detalle.findViewById<Button>(R.id.btnEliminarGasto).isEnabled }
                if (!habilitado) SystemClock.sleep(100)
            }
            assertTrue(habilitado)
            instrument.runOnMainSync { detalle.findViewById<Button>(R.id.btnEliminarGasto).performClick() }
            instrument.runOnMainSync { ventanas().filterIsInstance<Button>().first { it.id == android.R.id.button2 }.performClick() }
            runBlocking { assertNotNull(db.gastoDao().buscarPorId(gasto, usuario)) }
            atras()
            esperar(scenario) { it.hasWindowFocus() }
            val monitorFormulario = instrument.addMonitor(FormularioGastoActivity::class.java.name, null, false)
            scenario.onActivity { it.findViewById<Button>(R.id.btnAnadirGasto).performClick() }
            val formulario = instrument.waitForMonitorWithTimeout(monitorFormulario, 10000) as FormularioGastoActivity
            instrument.removeMonitor(monitorFormulario)
            instrument.runOnMainSync { formulario.findViewById<EditText>(R.id.etDescripcionGasto).setText("No guardar") }
            ocultarTeclado()
            atras()
            esperar(scenario) { it.hasWindowFocus() }
            scenario.onActivity { assertEquals(1, it.findViewById<RecyclerView>(R.id.rvGastos).adapter!!.itemCount) }
        }
    }

    @Test fun registroRegresaAlLoginConAtrasYConEnlace() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            repeat(2) { vuelta ->
                val monitor = instrument.addMonitor(RegistroActivity::class.java.name, null, false)
                scenario.onActivity { it.findViewById<TextView>(R.id.tvRegistrarse).performClick() }
                val registro = instrument.waitForMonitorWithTimeout(monitor, 10000) as RegistroActivity
                instrument.removeMonitor(monitor)
                ocultarTeclado()
                if (vuelta == 0) atras()
                else instrument.runOnMainSync { registro.findViewById<TextView>(R.id.tvVolverLogin).performClick() }
                esperar(scenario) { it.hasWindowFocus() }
            }
        }
    }

    @Test fun cerrarSesionImpideVolverALaListaConAtras() {
        val scenario = ActivityScenario.launch<GastosActivity>(intent(GastosActivity::class.java))
        val monitor = instrument.addMonitor(MainActivity::class.java.name, null, false)
        var login: Activity? = null
        try {
            scenario.onActivity { it.findViewById<Button>(R.id.btnCerrarSesion).performClick() }
            login = instrument.waitForMonitorWithTimeout(monitor, 10000)
            assertNotNull(login)
            instrument.waitForIdleSync()
            instrument.runOnMainSync {
                val registry = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                val activas = listOf(androidx.test.runner.lifecycle.Stage.RESUMED,
                    androidx.test.runner.lifecycle.Stage.STARTED, androidx.test.runner.lifecycle.Stage.CREATED)
                    .flatMap { registry.getActivitiesInStage(it) }
                assertTrue(activas.any { it is MainActivity })
                assertFalse(activas.any { it is GastosActivity })
            }
            atras()
            instrument.waitForIdleSync()
            instrument.runOnMainSync {
                assertFalse(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).any { it is GastosActivity })
            }
        } finally {
            instrument.removeMonitor(monitor)
            instrument.runOnMainSync { login?.finish() }
            scenario.close()
        }
    }
    private fun recorrer(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { recorrer(view.getChildAt(it)) } else emptyList()

    private fun auditar(activity: Activity) {
        val root = activity.findViewById<View>(R.id.main)
        val oscuro = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val barras = androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        assertEquals("Iconos de barra de estado", !oscuro, barras.isAppearanceLightStatusBars)
        val vistas = recorrer(root)
        val density = activity.resources.displayMetrics.density
        val surface = MaterialColors.getColor(root, com.google.android.material.R.attr.colorSurface)
        for (view in vistas.filter { it.visibility == View.VISIBLE }) {
            if ((view is Button || view is EditText || view is Spinner || (view is TextView && view.isClickable)) && view.isEnabled) {
                assertTrue("Área táctil baja: ${activity.resources.getResourceEntryName(view.id)} ${view.height / density}dp", view.height / density >= 47.5f)
                assertTrue(view.width / density >= 47.5f)
            }
            if (view is Spinner) {
                val tint = requireNotNull(view.backgroundTintList)
                val color = tint.getColorForState(view.drawableState, tint.defaultColor)
                assertTrue("Contraste de flecha de categoría", ColorUtils.calculateContrast(color, surface) >= 3.0)
            }
            if (view is EditText) {
                assertTrue("Falta etiqueta permanente", vistas.filterIsInstance<TextView>().any { it.labelFor == view.id })
                if (!view.hint.isNullOrBlank()) {
                    val contrasteHint = ColorUtils.calculateContrast(view.currentHintTextColor, surface)
                    assertTrue("Contraste bajo en ayuda de campo: ${view.hint} = $contrasteHint", contrasteHint >= 4.5)
                }
            }
            if (view is TextView && view.text.isNotBlank() && view.isEnabled) {
                val tint = if (view is Button) androidx.core.view.ViewCompat.getBackgroundTintList(view) else null
                val fondo = tint?.getColorForState(view.drawableState, tint.defaultColor) ?: surface
                val opaco = ColorUtils.compositeColors(fondo, surface)
                val ratio = ColorUtils.calculateContrast(view.currentTextColor, opaco)
                val sp = view.textSize / activity.resources.displayMetrics.scaledDensity
                val minimo = if (sp >= 18 || (sp >= 14 && view.typeface?.isBold == true)) 3.0 else 4.5
                assertTrue("Contraste bajo: ${view.text} = $ratio, tipo=${view.javaClass.simpleName}, fg=${Integer.toHexString(view.currentTextColor)}, bg=${Integer.toHexString(opaco)}", ratio >= minimo)
            }
        }
    }

    private fun captura(nombre: String) {
        instrument.waitForIdleSync()
        SystemClock.sleep(300)
        val bitmap = instrument.uiAutomation.takeScreenshot() ?: error("Sin captura")
        FileOutputStream(File(context.cacheDir, "qa-$nombre.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun accesibilidadFuenteAmpliadaYContrasteEnClaroYOscuro() {
        for ((modo, etiqueta) in listOf(AppCompatDelegate.MODE_NIGHT_NO to "claro", AppCompatDelegate.MODE_NIGHT_YES to "oscuro")) {
            instrument.runOnMainSync { AppCompatDelegate.setDefaultNightMode(modo) }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                esperar(scenario) { it.findViewById<Button>(R.id.btnIngresar).height > 0 }
                scenario.onActivity { assertTrue(it.resources.configuration.fontScale >= 1.49f); auditar(it) }
                captura("login-$etiqueta")
            }
            ActivityScenario.launch(RegistroActivity::class.java).use { scenario ->
                esperar(scenario) { it.findViewById<Button>(R.id.btnRegistrarse).height > 0 }
                scenario.onActivity { auditar(it) }
                captura("registro-$etiqueta")
            }
            ActivityScenario.launch<FormularioGastoActivity>(intent(FormularioGastoActivity::class.java)).use { scenario ->
                esperar(scenario) { it.findViewById<Button>(R.id.btnGuardarGasto).height > 0 }
                scenario.onActivity { auditar(it); it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                esperar(scenario) { it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
                ocultarTeclado()

                try {
                    esperar(scenario) {
                        val scroll = it.findViewById<androidx.core.widget.NestedScrollView>(R.id.main)
                        scroll.isSmoothScrollingEnabled = false
                        scroll.fullScroll(View.FOCUS_DOWN)
                        val v = it.findViewById<Button>(R.id.btnCancelarGasto)
                        val rect = Rect()
                        v.getGlobalVisibleRect(rect) && rect.height() >= v.height - 1
                    }
                } finally { captura("formulario-horizontal-$etiqueta") }
                scenario.onActivity { auditar(it) }
                captura("formulario-horizontal-$etiqueta")
                scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            }
            ActivityScenario.launch<DetalleGastoActivity>(intent(DetalleGastoActivity::class.java, true)).use { scenario ->
                esperar(scenario) { it.findViewById<Button>(R.id.btnEditarGasto).isEnabled }
                scenario.onActivity { auditar(it) }
                captura("detalle-$etiqueta")
            }
            ActivityScenario.launch<GastosActivity>(intent(GastosActivity::class.java)).use { scenario ->
                esperar(scenario) { it.findViewById<RecyclerView>(R.id.rvGastos).findViewHolderForAdapterPosition(0) != null }
                scenario.onActivity { auditar(it) }
                captura("lista-$etiqueta")
            }
        }
    }
}
