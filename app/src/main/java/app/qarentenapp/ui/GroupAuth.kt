package app.qarentenapp.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.qarentenapp.core.Repo
import kotlinx.coroutines.flow.MutableStateFlow

/** Un pedido de autenticación: [onOk] corre si se desbloquea, [onDenied] si se cancela. */
class AuthRequest(val title: String, val method: String? = null, val onDenied: (() -> Unit)? = null, val onOk: () -> Unit)

/** Contenedor del pedido pendiente; lo dibuja [AuthGate]. */
class AuthState {
    val req = MutableStateFlow<AuthRequest?>(null)
    /** [method]: DEVICE, BIOMETRIC o PASSWORD; null = el método general de Ajustes. */
    fun ask(title: String, method: String? = null, onDenied: (() -> Unit)? = null, onOk: () -> Unit) { req.value = AuthRequest(title, method, onDenied, onOk) }
    fun close() { req.value = null }
}

object Biometrics {
    private const val BIO = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
    // Huella o el bloqueo del propio celular (patrón, PIN o contraseña). WEAK porque STRONG + credencial no se puede combinar antes de Android 11.
    private const val DEVICE = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun available(ctx: Context): Boolean =
        BiometricManager.from(ctx).canAuthenticate(BIO) == BiometricManager.BIOMETRIC_SUCCESS

    /** El celular tiene huella o algún bloqueo de pantalla puesto. */
    fun deviceAvailable(ctx: Context): Boolean =
        BiometricManager.from(ctx).canAuthenticate(DEVICE) == BiometricManager.BIOMETRIC_SUCCESS

    fun activity(ctx: Context): FragmentActivity? {
        var c = ctx
        while (c is ContextWrapper) { if (c is FragmentActivity) return c; c = c.baseContext }
        return null
    }

    /** Método efectivo: el de la tarea o el general; el viejo "BOTH" equivale a DEVICE. */
    fun effective(method: String?): String {
        val m = if (method.isNullOrEmpty()) Repo.config.value.settings.lockMethod else method
        return if (m == "BIOMETRIC" || m == "PASSWORD") m else "DEVICE"
    }

    fun prompt(a: FragmentActivity, title: String, device: Boolean, onOk: () -> Unit, onCancel: () -> Unit) {
        val cb = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOk()
            override fun onAuthenticationError(code: Int, msg: CharSequence) = onCancel()
        }
        val b = BiometricPrompt.PromptInfo.Builder().setTitle(title).setSubtitle("Q'App")
        if (device) b.setAllowedAuthenticators(DEVICE) else b.setAllowedAuthenticators(BIO).setNegativeButtonText("Cancelar")
        BiometricPrompt(a, ContextCompat.getMainExecutor(a), cb).authenticate(b.build())
    }
}

/**
 * Pide huella, bloqueo del celular (patrón/PIN) o contraseña de Q'App según el método y, si sale bien, ejecuta el pedido.
 * Si el celular no tiene nada configurado se cae a la contraseña de Q'App; sin contraseña tampoco hay con qué proteger y deja pasar.
 */
@Composable
fun AuthGate(state: AuthState) {
    val req by state.req.collectAsState()
    val r = req ?: return
    val ctx = LocalContext.current
    val hasPw = Repo.config.value.settings.passwordHash != null
    var showPw by remember(r) { mutableStateOf(false) }
    fun ok() { state.close(); r.onOk() }
    fun denied() { state.close(); r.onDenied?.invoke() }
    LaunchedEffect(r) {
        val act = Biometrics.activity(ctx)
        val m = Biometrics.effective(r.method)
        val device = m == "DEVICE" && Biometrics.deviceAvailable(ctx)
        val bio = m == "BIOMETRIC" && Biometrics.available(ctx)
        when {
            act != null && (device || bio) -> Biometrics.prompt(act, r.title, device, onOk = { ok() }, onCancel = { denied() })
            hasPw -> showPw = true
            else -> ok()
        }
    }
    if (showPw) PasswordDialog(onCancel = { denied() }, onOk = { ok() })
}
