package io.github.grupo6.capturaprospectos.data.profile

import android.content.ContentValues
import android.graphics.*
import android.media.ExifInterface
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import java.io.*
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*

/** Solo cuentas UUID nuevas y fotos sintéticas; no abre datos personales del dispositivo. */
class PersonalProfileStorageTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val a=UUID.randomUUID().toString();private val b=UUID.randomUUID().toString()
    private fun file(id:String)=File(context.noBackupFilesDir,"personal-profile-$id.bin")
    private fun avatar():ByteArray {
        val image=Bitmap.createBitmap(400,600,Bitmap.Config.ARGB_8888)
        image.eraseColor(Color.GREEN)
        return try {AvatarImages.thumbnail(image)} finally {image.recycle()}
    }
    @After fun cleanup() {
        for(id in listOf(a,b)) {
            listOf("", ".new", ".bak").forEach {File(file(id).path+it).delete()}
            KeyStore.getInstance("AndroidKeyStore").apply {load(null);deleteEntry("g6.personal.profile.$id")}
        }
    }
    @Test fun encryptedProfileSurvivesReopenAndFieldsDoNotOverwriteEachOther()=runBlocking {
        val store=AndroidPersonalProfileStore(context);val jpeg=avatar()
        store.nickname(a,"Alias ficticio");store.photo(a,jpeg)
        val cipher=file(a).readBytes().toString(Charsets.ISO_8859_1);assertFalse(cipher.contains("Alias ficticio"))
        val fresh=AndroidPersonalProfileStore(context).load(a)
        assertEquals("Alias ficticio",fresh.nickname);assertArrayEquals(jpeg,fresh.photoJpeg)
        store.nickname(a,"Otro alias");assertArrayEquals(jpeg,store.load(a).photoJpeg)
        store.photo(a,null);assertEquals("Otro alias",store.load(a).nickname);assertNull(store.load(a).photoJpeg)
    }
    @Test fun separateAccountsCannotDecryptCopiedCiphertextAndFailureDoesNotDeleteFile()=runBlocking {
        val store=AndroidPersonalProfileStore(context);store.nickname(a,"Cuenta A")
        assertEquals("",store.load(b).nickname)
        file(b).writeBytes(file(a).readBytes());val before=file(b).readBytes()
        assertTrue(runCatching {store.load(b)}.isFailure);assertArrayEquals(before,file(b).readBytes())
        assertEquals("Cuenta A",store.load(a).nickname)
    }
    @Test fun invalidInputOrTamperingCannotSilentlyReplaceSavedPersonalization()=runBlocking {
        val store=AndroidPersonalProfileStore(context);store.nickname(a,"Conservado");val before=file(a).readBytes()
        assertTrue(runCatching {store.nickname(a,"A\nB")}.isFailure)
        assertTrue(runCatching {store.photo(a,byteArrayOf(1,2,3,4))}.isFailure)
        assertArrayEquals(before,file(a).readBytes())
        val changed=before.copyOf();changed[changed.lastIndex]=(changed.last().toInt() xor 1).toByte();file(a).writeBytes(changed)
        assertTrue(runCatching {store.load(a)}.isFailure);assertArrayEquals(changed,file(a).readBytes())
    }
    @Test fun concurrentNicknameAndPhotoMergeOnLatestStoredValue()=runBlocking {
        val store=AndroidPersonalProfileStore(context);val jpeg=avatar()
        listOf(async {store.nickname(a,"Alias concurrente")},async {store.photo(a,jpeg)}).awaitAll()
        assertEquals("Alias concurrente",store.load(a).nickname);assertArrayEquals(jpeg,store.load(a).photoJpeg)
    }
    @Test fun imageNormalizationStripsExifAndSupportsLegacyDecoder() {
        val image=Bitmap.createBitmap(600,400,Bitmap.Config.ARGB_8888)
        Canvas(image).apply {
            drawColor(Color.RED);drawRect(300f,0f,600f,400f,Paint().apply {color=Color.GREEN})
        }
        val temp=File(context.cacheDir,"test-avatar-${UUID.randomUUID()}.jpg")
        try {
            temp.outputStream().use {image.compress(Bitmap.CompressFormat.JPEG,95,it)}
            ExifInterface(temp.path).apply {
                setAttribute(ExifInterface.TAG_MAKE,"G6 synthetic fixture")
                setAttribute(ExifInterface.TAG_GPS_LATITUDE,"33/1,0/1,0/1")
                setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,"S")
                setAttribute(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_ROTATE_90.toString());saveAttributes()
            }
            for(api in listOf(26,34)) {
                val bytes=AvatarImages.encoded(temp.readBytes(),api)
                val decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.size)
                assertEquals(256,decoded.width);assertEquals(256,decoded.height)
                assertTrue(Color.red(decoded.getPixel(128,32))>220)
                assertTrue(Color.green(decoded.getPixel(128,224))>220);decoded.recycle()
                val exif=ExifInterface(ByteArrayInputStream(bytes))
                assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));assertNull(exif.getAttribute(ExifInterface.TAG_MAKE))
                assertTrue(bytes.size<=128*1024)
            }
        } finally {image.recycle();temp.delete()}
    }
    @Test fun selectedContentUriCanBeCopiedWithoutPersistingAccessToOriginal() {
        val resolver=context.contentResolver
        val uri=requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,"g6-test-avatar-${UUID.randomUUID()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
        }))
        try {
            resolver.openOutputStream(uri)!!.use {it.write(avatar())}
            val bytes=AvatarImages.selected(context,uri);val decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.size)
            assertEquals(256,decoded.width);assertEquals(256,decoded.height);decoded.recycle()
        } finally {resolver.delete(uri,null,null)}
    }
    @Test fun malformedOversizedImagesAndInvalidAccountIdsAreRejected()=runBlocking {
        assertTrue(runCatching {AvatarImages.encoded(byteArrayOf(1,2,3))}.isFailure)
        assertTrue(runCatching {AvatarImages.encoded(ByteArray(AvatarImages.MAX_INPUT_BYTES+1))}.isFailure)
        assertTrue(runCatching {AndroidPersonalProfileStore(context).nickname("../../outside","Alias")}.isFailure)
    }
}
