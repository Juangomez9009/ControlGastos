package com.example.controlgastos.ui.gastos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.controlgastos.R
import com.example.controlgastos.data.local.Gasto
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class GastoAdapter(private val abrirDetalle: (Gasto) -> Unit) : ListAdapter<Gasto, GastoAdapter.Holder>(Diferencias) {
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val descripcion: TextView = view.findViewById(R.id.tvDescripcionGasto)
        val monto: TextView = view.findViewById(R.id.tvMontoGasto)
        val detalle: TextView = view.findViewById(R.id.tvDatosGasto)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_gasto, parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val gasto = getItem(position)
        val formato = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO")).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }
        holder.itemView.setOnClickListener { abrirDetalle(gasto) }
        holder.descripcion.text = gasto.descripcion
        holder.monto.text = "$ " + formato.format(BigDecimal.valueOf(gasto.montoCentavos, 2))
        holder.detalle.text = "${gasto.categoria} · ${gasto.fecha}"
    }
    private object Diferencias : DiffUtil.ItemCallback<Gasto>() {
        override fun areItemsTheSame(oldItem: Gasto, newItem: Gasto) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Gasto, newItem: Gasto) = oldItem == newItem
    }
}
