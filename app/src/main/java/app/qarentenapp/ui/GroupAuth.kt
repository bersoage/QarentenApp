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
class AuthRequest(val title: String, val onDenied: (() -> Unit)? = null, val onOk: () -> Unit)

/** Contenedor del pedido pendiente; lo dibuja [AuthGate]. */
class AuthState {
    val req = MutableStateFlow<AuthRequest?>(null)
    fun ask(title: String, onDenied: (() -> Unit)? = null, onOk: () -> Unit) { req.value = AuthRequest(title, onDenied, onOk) }
    fun close() { req.value = null }
}

object Biometrics {
    private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun available(ctx: Context): Boolean =
        BiometricManager.from(ctx).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    fun activity(ctx: Context): FragmentActivity? {
        var c = ctx
        while (c is ContextWrapper) { if (c is FragmentActivity) return c; c = c.baseContext }
        return null
    }

    fun prompt(a: FragmentActivity, title: String, canUsePassword: Boolean, onOk: () -> Unit, onPassword: () -> Unit, onCancel: () -> Unit) {
        val cb = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOk()
            override fun onAuthenticationError(code: Int, msg: CharSequence) {
                if (code == BiometricPrompt.ERROR_NEGATIVE_BUTTON && canUsePassword) onPassword() else onCancel()
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("Q'App")
            .setAllowedAuthenticators(AUTH)
            .setNegativeButtonText(if (canUsePassword) "Usar contraseña" else "Cancelar")
            .build()
        BiometricPrompt(a, ContextCompat.getMainExecutor(a), cb).authenticate(info)
    }
}

/**
 * Pide huella o contraseña según el método elegido en Ajustes y, si sale bien, ejecuta el pedido.
 * Si no hay ni huella ni contraseña configuradas no hay con qué proteger: deja pasar.
 */
@Composable
fun AuthGate(state: AuthState) {
    val req by state.req.collectAsState()
    val r = req ?: return
    val ctx = LocalContext.current
    val s = Repo.config.value.settings
    val hasPw = s.passwordHash != null
    var showPw by remember(r) { mutableStateOf(false) }
    fun ok() { state.close(); r.onOk() }
    fun denied() { state.close(); r.onDenied?.invoke() }
    LaunchedEffect(r) {
        val act = Biometrics.activity(ctx)
        val bio = s.lockMethod != "PASSWORD" && act != null && Biometrics.available(ctx)
        when {
            bio -> Biometrics.prompt(act!!, r.title, canUsePassword = hasPw && s.lockMethod == "BOTH",
                onOk = { ok() }, onPassword = { showPw = true }, onCancel = { denied() })
            hasPw && s.lockMethod != "BIOMETRIC" -> showPw = true
            hasPw -> showPw = true   // solo huella pero el celular ya no tiene: se cae a la contraseña
            else -> ok()
        }
    }
    if (showPw) PasswordDialog(onCancel = { denied() }, onOk = { ok() })
}
