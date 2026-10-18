package com.example.controlgastos.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gastos",
    foreignKeys = [ForeignKey(entity = Usuario::class, parentColumns = ["id"],
        childColumns = ["usuarioId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["usuarioId"])]
)
data class Gasto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val descripcion: String,
    val montoCentavos: Long,
    val categoria: String,
    val fecha: String
)
