package io.github.grupo6.capturaprospectos.data.profile

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import io.github.grupo6.capturaprospectos.domain.profile.MAX_AVATAR_BYTES
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

internal object AvatarImages {
    const val MAX_INPUT_BYTES=10*1024*1024
    fun selected(context:Context,uri:Uri):ByteArray {
        require(uri.scheme=="content")
        val bytes=context.contentResolver.openInputStream(uri)?.use {stream ->
            val output=ByteArrayOutputStream();val buffer=ByteArray(8192)
            var total=0
            while(true) {val n=stream.read(buffer);if(n<0) break;total+=n;require(total<=MAX_INPUT_BYTES);output.write(buffer,0,n)}
            output.toByteArray()
        } ?: throw java.io.IOException("Selected image unavailable")
        return encoded(bytes)
    }
    fun encoded(bytes:ByteArray,api:Int=Build.VERSION.SDK_INT):ByteArray {
        require(bytes.isNotEmpty() && bytes.size<=MAX_INPUT_BYTES)
        val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
        require(bounds.outWidth in 1..32768 && bounds.outHeight in 1..32768)
        var sample=1
        while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1024) sample*=2
        val bitmap=if(Build.VERSION.SDK_INT>=28 && api>=28) ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) {decoder,_,_->
            decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE;decoder.setTargetSampleSize(sample)
        } else {
            val raw=requireNotNull(BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply {inSampleSize=sample}))
            val orientation=try {ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)} catch(_:java.io.IOException) {ExifInterface.ORIENTATION_NORMAL}
            val matrix=Matrix().apply {when(orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f,1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f,-1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {setRotate(90f);postScale(-1f,1f)}
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {setRotate(-90f);postScale(-1f,1f)}
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }}
            Bitmap.createBitmap(raw,0,0,raw.width,raw.height,matrix,true).also {if(it!==raw) raw.recycle()}
        }
        return try {thumbnail(bitmap)} finally {bitmap.recycle()}
    }
    fun thumbnail(bitmap:Bitmap):ByteArray {
        require(!bitmap.isRecycled && bitmap.width>0 && bitmap.height>0)
        val source=if(bitmap.config==Bitmap.Config.HARDWARE) requireNotNull(bitmap.copy(Bitmap.Config.ARGB_8888,false)) else bitmap
        val output=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
        try {
            val side=minOf(source.width,source.height)
            val left=(source.width-side)/2;val top=(source.height-side)/2
            val canvas=Canvas(output);canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(source,Rect(left,top,left+side,top+side),Rect(0,0,256,256),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            val bytes=ByteArrayOutputStream().also {check(output.compress(Bitmap.CompressFormat.JPEG,85,it))}.toByteArray()
            require(bytes.size<=MAX_AVATAR_BYTES);return bytes
        } finally {output.recycle();if(source!==bitmap) source.recycle()}
    }
}
