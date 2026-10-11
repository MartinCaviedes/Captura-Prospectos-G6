package io.github.grupo6.capturaprospectos.data.device

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.grupo6.capturaprospectos.domain.device.*
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

/** Decodificación real de bitmaps ficticios; no prueba enfoque físico ni concede CAMERA. */
class OfflineQrDecoderTest {
    private val target get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun decode(asset: String): String {
        val bitmap = InstrumentationRegistry.getInstrumentation().context.assets.open(asset).use(BitmapFactory::decodeStream)
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
        try {
            val results = Tasks.await(scanner.process(InputImage.fromBitmap(bitmap, 0)), 15, TimeUnit.SECONDS)
            assertEquals(1, results.size)
            assertEquals(Barcode.FORMAT_QR_CODE, results.single().format)
            return requireNotNull(results.single().rawValue)
        } finally { scanner.close(); bitmap.recycle() }
    }
    @Test fun bundledModelDecodesSyntheticQrWithoutInternetPermission() {
        assertEquals(PackageManager.PERMISSION_DENIED, target.packageManager.checkPermission(Manifest.permission.INTERNET, target.packageName))
        val result = DemoQrParser().parse(decode("qr-synthetic.png")) as QrResult.Candidate
        assertEquals("12345678-5", result.value.rut)
        assertEquals("Persona Ficticia Alfa", result.value.fullName)
    }
    @Test fun decodedUrlRemainsUnsupportedWithoutOpeningOrApplyingIt() {
        assertEquals(QrResult.Unsupported, DemoQrParser().parse(decode("qr-unsupported.png")))
    }
}
