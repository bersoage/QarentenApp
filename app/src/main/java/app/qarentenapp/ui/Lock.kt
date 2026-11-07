package app.qarentenapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.qarentenapp.R
import app.qarentenapp.core.Repo
import app.qarentenapp.core.Security

private fun checkPassword(p: String): Boolean {
    val s = Repo.config.value.settings
    return Security.check(p, s.passwordSalt, s.passwordHash)
}

/** Pantalla de bloqueo al abrir la app. */
@Composable
fun LockScreen(onUnlock: (String) -> Boolean) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Navy).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.logo_candado), null, Modifier.height(140.dp))
        Spacer(Modifier.height(24.dp))
        Text("Ingresá la contraseña para desbloquear", color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = text, onValueChange = { text = it; error = false },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { error = !onUnlock(text) }),
            isError = error,
            supportingText = { if (error) Text("Contraseña incorrecta") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Ice, unfocusedBorderColor = Color(0xFF7F95C2), cursorColor = Ice,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { error = !onUnlock(text) }, modifier = Modifier.fillMaxWidth()) { Text("Desbloquear") }
    }
}

/** Diálogo de contraseña para accesos directos, widgets y ventanas flotantes. */
@Composable
fun PasswordDialog(onCancel: () -> Unit, onOk: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    if (done) return
    val submit: () -> Unit = {
        if (checkPassword(text)) {
            done = true
            onOk()
        } else {
            error = true
        }
    }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Contraseña") },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it; error = false },
                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                isError = error,
                supportingText = { if (error) Text("Contraseña incorrecta", color = MaterialTheme.colorScheme.error) },
            )
        },
        confirmButton = { TextButton(onClick = { submit() }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } },
    )
}

/** Diálogo para crear o cambiar la contraseña. */
@Composable
fun SetPasswordDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(a, { a = it; err = null }, label = { Text("Contraseña") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                OutlinedTextField(b, { b = it; err = null }, label = { Text("Confirmar contraseña") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                err = when {
                    a.length < 4 -> "Tiene que tener al menos 4 caracteres"
                    a != b -> "Las contraseñas no son iguales"
                    else -> null
                }
                if (err == null) onSave(a)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
