package com.scrappy.app

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import com.scrappy.app.data.RequestCancellation
import java.util.concurrent.CancellationException

/** Exercises Android bitmap/EXIF APIs; no provider requests are made. */
@RunWith(AndroidJUnit4::class)
class AiPhotoClientTest {
    private val cache get() = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir

    @Test
    fun canceledPreparationDoesNotReadPhoto() {
        val missing = File(cache, "scrappy_missing_${java.util.UUID.randomUUID()}.jpg")
        val cancellation = RequestCancellation().apply { cancel() }
        // Cancellation must win over the decode error for this missing file.
        assertThrows(CancellationException::class.java) {
            AiPhotoClient.jpeg(missing, cancellation)
        }
        assertFalse(missing.exists())
    }

    @Test
    fun canceledPreparationPreservesOriginalPhoto() {
        val file = quadrantPhoto()
        try {
            val original = file.readBytes()
            val cancellation = RequestCancellation().apply { cancel() }
            assertThrows(CancellationException::class.java) {
                AiPhotoClient.jpeg(file, cancellation)
            }
            assertArrayEquals(original, file.readBytes())
            // A fresh request can still prepare the same retained photo.
            assertTrue(AiPhotoClient.jpeg(file, RequestCancellation()).size in 4..1_048_576)
        } finally { file.delete() }
    }

    @Test
    fun reviewCorrectsAllEightExifOrientations() {
        val red = Color.RED
        val green = Color.GREEN
        val blue = Color.BLUE
        val yellow = Color.YELLOW
        // Expected output quadrants: top-left, top-right, bottom-left, bottom-right.
        val cases = listOf(
            ExifInterface.ORIENTATION_NORMAL to listOf(red, green, blue, yellow),
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL to listOf(green, red, yellow, blue),
            ExifInterface.ORIENTATION_ROTATE_180 to listOf(yellow, blue, green, red),
            ExifInterface.ORIENTATION_FLIP_VERTICAL to listOf(blue, yellow, red, green),
            ExifInterface.ORIENTATION_TRANSPOSE to listOf(red, blue, green, yellow),
            ExifInterface.ORIENTATION_ROTATE_90 to listOf(blue, red, yellow, green),
            ExifInterface.ORIENTATION_TRANSVERSE to listOf(yellow, green, blue, red),
            ExifInterface.ORIENTATION_ROTATE_270 to listOf(green, yellow, red, blue)
        )
        for ((orientation, expected) in cases) {
            val file = quadrantPhoto()
            try {
                ExifInterface(file.absolutePath).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                    saveAttributes()
                }
                val image = AiPhotoClient.reviewBitmap(file)
                try {
                    val swapped = orientation in 5..8
                    assertEquals("Width for EXIF $orientation", if (swapped) 120 else 80, image.width)
                    assertEquals("Height for EXIF $orientation", if (swapped) 80 else 120, image.height)
                    val pixels = listOf(
                        image.getPixel(image.width / 4, image.height / 4),
                        image.getPixel(3 * image.width / 4, image.height / 4),
                        image.getPixel(image.width / 4, 3 * image.height / 4),
                        image.getPixel(3 * image.width / 4, 3 * image.height / 4)
                    )
                    for (index in pixels.indices) {
                        assertColor("EXIF $orientation quadrant $index", expected[index], pixels[index])
                    }
                } finally { image.recycle() }
            } finally { file.delete() }
        }
    }

    @Test
    fun uploadReencodingStripsLocationAndPhotoMetadata() {
        val original = quadrantPhoto()
        val encoded = File.createTempFile("scrappy_reencoded_", ".jpg", cache)
        try {
            ExifInterface(original.absolutePath).apply {
                setAttribute(ExifInterface.TAG_GPS_LATITUDE, "40/1,0/1,0/1")
                setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
                setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "75/1,0/1,0/1")
                setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
                setAttribute(ExifInterface.TAG_USER_COMMENT, "private-test-metadata")
                saveAttributes()
            }
            // Prove that the source fixture actually contains metadata first.
            assertNotNull(ExifInterface(original.absolutePath).getAttribute(ExifInterface.TAG_GPS_LATITUDE))
            val bytes = AiPhotoClient.jpeg(original)
            assertTrue(bytes.size in 4..1_048_576)
            encoded.writeBytes(bytes)
            val exif = ExifInterface(encoded.absolutePath)
            for (tag in listOf(ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_USER_COMMENT)) {
                assertNull("Upload retained $tag", exif.getAttribute(tag))
            }
        } finally { original.delete(); encoded.delete() }
    }

    @Test
    fun largePhotoReviewIsBounded() {
        val file = File.createTempFile("scrappy_large_", ".jpg", cache)
        try {
            val original = Bitmap.createBitmap(3200, 2400, Bitmap.Config.ARGB_8888)
            try {
                original.eraseColor(Color.GRAY)
                file.outputStream().use { assertTrue(original.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            } finally { original.recycle() }
            val review = AiPhotoClient.reviewBitmap(file)
            try {
                assertTrue(maxOf(review.width, review.height) <= 1600)
                assertEquals(1600, review.width)
                assertEquals(1200, review.height)
            } finally { review.recycle() }
        } finally { file.delete() }
    }

    @Test
    fun oddSizedPhotosStayBoundedInBothOrientations() {
        for ((width, height) in listOf(3201 to 2401, 2401 to 3201, 1601 to 1201)) {
            val file = File.createTempFile("scrappy_odd_", ".jpg", cache)
            try {
                val original = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                try {
                    original.eraseColor(Color.GRAY)
                    file.outputStream().use { assertTrue(original.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
                } finally { original.recycle() }
                val review = AiPhotoClient.reviewBitmap(file)
                try {
                    assertTrue("$width x $height exceeded the review limit",
                        maxOf(review.width, review.height) <= 1600)
                    assertTrue(review.width > 0 && review.height > 0)
                } finally { review.recycle() }
            } finally { file.delete() }
        }
    }

    @Test
    fun unreadablePhotoIsRejectedBeforeUpload() {
        val file = File.createTempFile("scrappy_corrupt_", ".jpg", cache)
        try {
            file.writeText("This is not an image")
            assertThrows(IllegalArgumentException::class.java) { AiPhotoClient.jpeg(file) }
        } finally { file.delete() }
    }

    @Test
    fun detailedPhotoIsPreparedWithinUploadLimitWithoutChangingOriginal() {
        val file = File.createTempFile("scrappy_detail_", ".jpg", cache)
        val bitmap = Bitmap.createBitmap(1600, 1600, Bitmap.Config.ARGB_8888)
        try {
            val random = java.util.Random(17)
            val row = IntArray(bitmap.width)
            for (y in 0 until bitmap.height) {
                for (x in row.indices) row[x] = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
                bitmap.setPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            }
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)) }
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_USER_COMMENT, "private-detailed-photo")
                saveAttributes()
            }
            assertEquals("private-detailed-photo",
                ExifInterface(file.absolutePath).getAttribute(ExifInterface.TAG_USER_COMMENT))
            val original = file.readBytes()
            val review = AiPhotoClient.reviewBitmap(file)
            try {
                val fixedQuality = java.io.ByteArrayOutputStream()
                assertTrue(review.compress(Bitmap.CompressFormat.JPEG, 80, fixedQuality))
                assertTrue("Fixture must exceed the old fixed-quality upload limit", fixedQuality.size() > 1_048_576)
            } finally { review.recycle() }
            val jpeg = AiPhotoClient.jpeg(file)
            assertTrue(jpeg.size in 4..1_048_576)
            jpeg.inputStream().use {
                assertNull("Adaptive compression retained photo metadata",
                    ExifInterface(it).getAttribute(ExifInterface.TAG_USER_COMMENT))
            }
            assertArrayEquals("Preparing an upload must not rewrite the local photo", original, file.readBytes())
            val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
            assertNotNull("Upload must still decode as an image", decoded)
            try {
                assertTrue(maxOf(decoded.width, decoded.height) <= 1600)
                assertTrue(decoded.width > 0 && decoded.height > 0)
            } finally { decoded.recycle() }
        } finally { bitmap.recycle(); file.delete() }
    }

    @Test
    fun smallPhotoUploadPreservesDimensions() {
        val file = quadrantPhoto()
        try {
            val jpeg = AiPhotoClient.jpeg(file)
            val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
            assertNotNull(decoded)
            try {
                assertEquals(80, decoded.width)
                assertEquals(120, decoded.height)
            } finally { decoded.recycle() }
        } finally { file.delete() }
    }

    private fun quadrantPhoto(): File {
        val file = File.createTempFile("scrappy_orientation_", ".jpg", cache)
        val bitmap = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888)
        try {
            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                bitmap.setPixel(x, y, if (y < 60) {
                    if (x < 40) Color.RED else Color.GREEN
                } else {
                    if (x < 40) Color.BLUE else Color.YELLOW
                })
            }
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)) }
            return file
        } catch (error: Throwable) {
            file.delete()
            throw error
        } finally { bitmap.recycle() }
    }

    private fun assertColor(message: String, expected: Int, actual: Int) {
        // JPEG compression may shift channels slightly even in solid quadrants.
        assertTrue(message, kotlin.math.abs(Color.red(expected) - Color.red(actual)) < 25 &&
            kotlin.math.abs(Color.green(expected) - Color.green(actual)) < 25 &&
            kotlin.math.abs(Color.blue(expected) - Color.blue(actual)) < 25)
    }
}
