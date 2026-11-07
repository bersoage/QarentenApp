package app.qarentenapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.qarentenapp.R

@Composable
fun QRoot(vm: MainViewModel) {
    val loaded by vm.loaded.collectAsState()
    if (!loaded) { Splash(); return }

    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }

    val pendingImport by vm.pendingImport.collectAsState()
    if (pendingImport != null) {
        val n = pendingImport!!.quarantined.size
        AlertDialog(
            onDismissRequest = { vm.cancelImport() },
            title = { Text("Restaurar backup") },
            text = {
                Text("Se van a reemplazar tus grupos y ajustes por los del backup (${pendingImport!!.groups.size} grupos).\n\n¿Querés además volver a congelar las $n apps que tenías en cuarentena?")
            },
            confirmButton = { TextButton(onClick = { vm.confirmImport(true) }) { Text("Restaurar y congelar") } },
            dismissButton = { TextButton(onClick = { vm.confirmImport(false) }) { Text("Solo restaurar") } },
        )
    }

    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "groups"
    val tabs = listOf("groups", "apps", "settings")

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (route in tabs) NavigationBar {
                NavigationBarItem(route == "groups", { nav.tab("groups") }, { Icon(Icons.Outlined.GridView, null) }, label = { Text("Grupos") })
                NavigationBarItem(route == "apps", { nav.tab("apps") }, { Icon(Icons.AutoMirrored.Outlined.List, null) }, label = { Text("Todas las apps") })
                NavigationBarItem(route == "settings", { nav.tab("settings") }, { Icon(Icons.Outlined.Settings, null) }, label = { Text("Ajustes") })
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "groups", modifier = Modifier.padding(pad)) {
            composable("groups") { GroupsScreen(vm, open = { nav.navigate("group/$it") }) }
            composable("apps") { AppsScreen(vm) }
            composable("settings") { SettingsScreen(vm) }
            composable("group/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                val id = it.arguments?.getString("id") ?: return@composable
                GroupScreen(vm, id, back = { nav.popBackStack() }, pick = { nav.navigate("pick/$id") })
            }
            composable("pick/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                val id = it.arguments?.getString("id") ?: return@composable
                PickAppsScreen(vm, id, done = { nav.popBackStack() })
            }
        }
    }
}

private fun androidx.navigation.NavController.tab(route: String) = navigate(route) {
    popUpTo("groups") { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun Splash() {
    Column(
        Modifier.fillMaxSize().background(Navy).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.logo_candado), contentDescription = null, modifier = Modifier.height(220.dp))
        Spacer(Modifier.height(32.dp))
        Image(
            painterResource(R.drawable.logo_texto), contentDescription = "QarentenApp",
            modifier = Modifier.width(300.dp).clip(RoundedCornerShape(20.dp)),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            "Congelá lo que no usás.\nActivalo cuando lo necesitás.",
            color = Color(0xFFBFD3F2), fontSize = 16.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Ice, strokeWidth = 2.5.dp)
            Spacer(Modifier.width(10.dp))
            Text("Buscando root o Shizuku…", color = Color(0xFFBFD3F2), fontSize = 13.sp)
        }
    }
}

/** Encabezado con el logo, usado en la pantalla de grupos. */
@Composable
fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Image(painterResource(R.drawable.logo_candado), contentDescription = null, modifier = Modifier.height(28.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            buildBrand(), fontSize = 16.sp,
        )
    }
}
