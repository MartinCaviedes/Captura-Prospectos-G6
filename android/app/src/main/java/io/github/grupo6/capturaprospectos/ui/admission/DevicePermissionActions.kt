package io.github.grupo6.capturaprospectos.ui.admission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/** Solicitud sólo tras una acción; denegar conserva el formulario y permite ingreso manual. */
@Composable internal fun rememberDevicePermissionAction(
    camera: Boolean,
    onGranted: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onGranted)
    val permissions = remember(camera) { if (camera) arrayOf(Manifest.permission.CAMERA)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) }
    fun granted() = permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (granted()) callback() else denied = true
    }
    if (denied) {
        val activity = context.activity()
        val canRequestAgain = activity != null && permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        AlertDialog(onDismissRequest = { denied = false }, title = { Text("Permiso ${if (camera) "de cámara" else "de ubicación"} denegado") },
            text = { Text(if (camera) "La cámara se usa para leer un QR ficticio, sin guardar fotos. Puedes completar el RUT manualmente."
                else "La ubicación necesita tu permiso y confirmación. Puedes continuar sin ubicación o confirmar explícitamente las coordenadas del evento. La ubicación aproximada también se comprueba contra los límites de precisión.") },
            confirmButton = { TextButton(onClick = {
                denied = false
                if (canRequestAgain) launcher.launch(permissions)
                else context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }) { Text(if (canRequestAgain) "Solicitar de nuevo" else "Abrir ajustes") } },
            dismissButton = { TextButton(onClick = { denied = false }) { Text("Continuar manualmente") } })
    }
    return { if (granted()) callback() else launcher.launch(permissions) }
}

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.takeUnless { it === this }?.activity()
    else -> null
}
