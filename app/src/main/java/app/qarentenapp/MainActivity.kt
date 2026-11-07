package app.qarentenapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import app.qarentenapp.ui.QRoot
import app.qarentenapp.ui.MainViewModel
import app.qarentenapp.ui.QTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { _, _ -> vm.redetect() }
    private val binderListener = Shizuku.OnBinderReceivedListener { vm.redetect() }

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshAsync() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Shizuku.addRequestPermissionResultListener(shizukuListener)
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { QTheme { QRoot(vm) } }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    override fun onStop() {
        super.onStop()
        vm.onStop()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuListener)
        Shizuku.removeBinderReceivedListener(binderListener)
        super.onDestroy()
    }
}
