package app.qarentenapp.core

import java.security.MessageDigest
import java.security.SecureRandom

/** Contraseña de protección: se guarda solo un hash con sal, nunca el texto. */
object Security {
    fun newSalt(): String {
        val b = ByteArray(16)
        SecureRandom().nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    fun hash(password: String, salt: String): String {
        var d = (salt + password).toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        repeat(10_000) { d = md.digest(d) }
        return d.joinToString("") { "%02x".format(it) }
    }

    /** ¿La app se bloquea al abrirla? Con método «contraseña» hace falta tenerla creada. */
    fun appLocked(s: app.qarentenapp.data.AppSettings) =
        s.protectionEnabled && (s.lockMethod != "PASSWORD" || s.passwordHash != null)

    fun check(password: String, salt: String?, hash: String?): Boolean =
        salt != null && hash != null && hash(password, salt) == hash
}
