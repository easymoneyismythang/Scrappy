package com.scrappy.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.scrappy.app.data.AiIdentification
import com.scrappy.app.data.AiHttpClient
import com.scrappy.app.data.RequestCancellation
import java.io.ByteArrayOutputStream
import java.io.File

object AiPhotoClient {
    /** Decode and re-encode pixels; no original EXIF/location metadata is sent. */
    fun jpeg(file: File, cancellation: RequestCancellation = RequestCancellation()): ByteArray {
        cancellation.check()
        var image=reviewBitmap(file,cancellation)
        try {
            cancellation.check()
            val stream=ByteArrayOutputStream()
            var quality=80
            while(true) {
                cancellation.check()
                stream.reset()
                check(image.compress(Bitmap.CompressFormat.JPEG,quality,stream)) { "Photo could not be prepared" }
                cancellation.check()
                require(stream.size()>=4) { "Photo could not be prepared" }
                if(stream.size()<=1_048_576)return stream.toByteArray()
                // Detailed/noisy photos can exceed 1 MiB even at 1600 pixels.
                // Keep the original local photo, and reduce only the upload copy.
                if(quality>50) {
                    quality-=15
                } else {
                    val width=maxOf(1,image.width*3/4)
                    val height=maxOf(1,image.height*3/4)
                    check(width<image.width || height<image.height) { "Photo could not be prepared within the upload limit" }
                    val smaller=Bitmap.createScaledBitmap(image,width,height,true)
                    image.recycle();image=smaller;quality=80
                }
            }
        } finally { image.recycle() }
    }
    /** Bounded, orientation-correct pixels for both local review and AI upload. */
    fun reviewBitmap(file: File, cancellation: RequestCancellation = RequestCancellation()): Bitmap {
        cancellation.check()
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeFile(file.absolutePath,bounds)
        cancellation.check()
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Photo could not be read" }
        val options=BitmapFactory.Options().apply { inSampleSize=1 }
        // Decoders can round a sampled odd dimension up. Integer division
        // rounds down (3201 / 2 == 1600), so compare the ceiling instead.
        val longest=maxOf(bounds.outWidth,bounds.outHeight).toLong()
        while((longest+options.inSampleSize-1)/options.inSampleSize > 1600) options.inSampleSize *= 2
        cancellation.check()
        val decoded=BitmapFactory.decodeFile(file.absolutePath,options) ?: error("Photo could not be read")
        try {
            // Decoding itself is not interruptible, so check immediately after it and
            // recycle the decoded bitmap through this try/finally-style catch path.
            cancellation.check()
            val orientation=ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)
            cancellation.check()
            val transform=Matrix()
            when(orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> transform.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> transform.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> transform.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> transform.postScale(-1f,1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> transform.postScale(1f,-1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { transform.postRotate(90f);transform.postScale(-1f,1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { transform.postRotate(270f);transform.postScale(-1f,1f) }
            }
            val image=Bitmap.createBitmap(decoded,0,0,decoded.width,decoded.height,transform,true)
            if(image !== decoded) decoded.recycle()
            return image
        } catch(error: Throwable) { decoded.recycle();throw error }
    }
    fun identify(jpeg: ByteArray, endpoint: String, cancellation: RequestCancellation = RequestCancellation()): AiIdentification =
        AiHttpClient().identify(jpeg, endpoint, cancellation)
}
