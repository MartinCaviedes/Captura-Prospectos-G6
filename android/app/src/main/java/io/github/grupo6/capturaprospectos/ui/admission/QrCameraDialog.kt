package io.github.grupo6.capturaprospectos.ui.admission

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.grupo6.capturaprospectos.domain.capture.RutValidator
import java.util.concurrent.atomic.AtomicBoolean

/** Sólo QR sintético; modelo en la APK, sin fotos, archivos ni payload crudo persistido. */
@Composable internal fun QrCameraDialog(
    state: AdmissionFormState,
    onPayload: (String) -> Unit,
    onUnavailable: () -> Unit,
    onApply: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    var replaceRut by remember(state.qrCandidate) { mutableStateOf(false) }
    val candidate = state.qrCandidate
    val needsReplacement = candidate != null && state.input.rut.isNotBlank() && RutValidator.normalize(state.input.rut) != candidate.rut
    AlertDialog(onDismissRequest = onClose, title = { Text("Leer QR ficticio con cámara") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Enfoca un QR G6DEMO:1. No se guardan fotos; puedes cerrar y completar los datos manualmente.")
            if (candidate == null && state.qrError == null) QrCameraPreview(onPayload, onUnavailable,
                Modifier.fillMaxWidth().height(220.dp))
            candidate?.let {
                Text("Nombre completo reconocido: ${it.fullName}")
                Text("RUT candidato: ${it.rut}")
                Text("Sólo se puede aplicar el RUT. Completa nombres y apellidos manualmente; el nombre completo no se divide ni se copia a Nombres.")
                if (needsReplacement) {
                    Row(Modifier.fillMaxWidth().toggleable(replaceRut, role = Role.Checkbox, onValueChange = { replaceRut = it })) {
                        Checkbox(replaceRut, onCheckedChange = null)
                        Text("Confirmo reemplazar el RUT escrito por el candidato.", Modifier.weight(1f))
                    }
                }
            }
            state.qrError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = {
            if (candidate != null) TextButton(onClick = { onApply(replaceRut) }, enabled = !needsReplacement || replaceRut) { Text("Confirmar RUT candidato") }
            else if (state.qrError != null) TextButton(onClick = onRetry) { Text("Volver a enfocar") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Continuar manualmente") } })
}

@SuppressLint("MissingPermission") // El consumidor solicita CAMERA antes de abrir el diálogo.
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable private fun QrCameraPreview(onPayload: (String) -> Unit, onUnavailable: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val payload by rememberUpdatedState(onPayload)
    val unavailable by rememberUpdatedState(onUnavailable)
    val view = remember(context) { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    AndroidView(factory = { view }, modifier = modifier.semantics { contentDescription = "Vista previa de cámara para QR ficticio" })
    DisposableEffect(context, lifecycle, view) {
        val active = AtomicBoolean(true)
        val delivered = AtomicBoolean(false)
        val executor = ContextCompat.getMainExecutor(context)
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
        val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        var provider: ProcessCameraProvider? = null
        var cameraState: LiveData<CameraState>? = null
        val observer = Observer<CameraState> { camera ->
            if (camera.error != null && active.get() && delivered.compareAndSet(false, true)) unavailable()
        }
        analysis.setAnalyzer(executor) { frame ->
            val image = frame.image
            if (!active.get() || delivered.get() || image == null) frame.close()
            else try {
                scanner.process(InputImage.fromMediaImage(image, frame.imageInfo.rotationDegrees))
                    .addOnSuccessListener(executor) { results ->
                        val raw = results.firstOrNull { it.format == Barcode.FORMAT_QR_CODE && it.rawValue != null }?.rawValue
                        if (active.get() && raw != null && delivered.compareAndSet(false, true)) payload(raw)
                    }
                    .addOnFailureListener(executor) { if (active.get() && delivered.compareAndSet(false, true)) unavailable() }
                    .addOnCompleteListener(executor) { frame.close() }
            } catch (_: Exception) { frame.close(); if (active.get() && delivered.compareAndSet(false, true)) unavailable() }
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (active.get()) try {
                provider = future.get()
                if (provider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    cameraState = provider!!.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis).cameraInfo.cameraState
                    cameraState!!.observe(lifecycle, observer)
                }
                else unavailable()
            } catch (_: Exception) { if (active.get()) unavailable() }
        }, executor)
        onDispose {
            active.set(false)
            cameraState?.removeObserver(observer)
            analysis.clearAnalyzer()
            provider?.unbind(preview, analysis)
            scanner.close()
        }
    }
}
