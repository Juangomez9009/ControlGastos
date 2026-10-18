package com.example.controlgastos.data

import android.database.sqlite.SQLiteConstraintException
import android.util.Base64
import com.example.controlgastos.data.local.Usuario
import com.example.controlgastos.data.local.UsuarioDao
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CorreoRegistradoException : Exception()

class UsuarioRepository(private val dao: UsuarioDao) {
    suspend fun registrar(nombre: String, correo: String, contrasena: String): Long =
        withContext(Dispatchers.IO) {
            val correoNormalizado = correo.trim().lowercase(Locale.ROOT)
            if (dao.buscarPorCorreo(correoNormalizado) != null) {
                throw CorreoRegistradoException()
            }
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val clave = PBEKeySpec(contrasena.toCharArray(), salt, 210_000, 256)
            val hash = try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(clave).encoded
            } finally {
                clave.clearPassword()
            }
            try {
                dao.insertar(
                    Usuario(
                        nombre = nombre.trim(),
                        correo = correoNormalizado,
                        contrasenaHash = Base64.encodeToString(hash, Base64.NO_WRAP),
                        contrasenaSalt = Base64.encodeToString(salt, Base64.NO_WRAP)
                    )
                )
            } catch (error: SQLiteConstraintException) {
                if (dao.buscarPorCorreo(correoNormalizado) != null) {
                    throw CorreoRegistradoException()
                }
                throw error
            }
        }

    suspend fun iniciarSesion(correo: String, contrasena: String): Usuario? =
        withContext(Dispatchers.IO) {
            val usuario = dao.buscarPorCorreo(correo.trim().lowercase(Locale.ROOT))
                ?: return@withContext null
            val salt = Base64.decode(usuario.contrasenaSalt, Base64.NO_WRAP)
            val clave = PBEKeySpec(contrasena.toCharArray(), salt, 210_000, 256)
            val hash = try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(clave).encoded
            } finally {
                clave.clearPassword()
            }
            val esperado = Base64.decode(usuario.contrasenaHash, Base64.NO_WRAP)
            if (java.security.MessageDigest.isEqual(hash, esperado)) usuario else null
        }
}
