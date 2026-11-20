import unittest
from check_camera_safety import check_sources

CAMERA = '''
setPositiveButton("Send and analyze")
setNegativeButton("Keep photo local")
setNeutralButton("Privacy policy")
cancellation.check()
                        val jpeg=AiPhotoClient.jpeg(photo,cancellation)
cancellation.check()
                        AiPhotoClient.identify(jpeg,BuildConfig.AI_ENDPOINT,cancellation)
generation==request
generation++;analysis=null
        activeRequest?.cancel()
activeRequest?.cancel();activeRequest=null
'''
PHOTO = '''
reviewBitmap(file,cancellation)
BitmapFactory.decodeFile(file.absolutePath,bounds)
        cancellation.check()
BitmapFactory.decodeFile(file.absolutePath,options) ?: error("Photo could not be read")
        try {
            // Decoding itself is not interruptible, so check immediately after it and
            // recycle the decoded bitmap through this try/finally-style catch path.
            cancellation.check()
while(true) {
                cancellation.check()
image.compress(Bitmap.CompressFormat.JPEG,quality,stream)) { "Photo could not be prepared" }
                cancellation.check()
} finally { image.recycle() }
stream.size()<=1_048_576
ExifInterface.TAG_ORIENTATION
'''
HTTP = '''
cancellation.check()
cancellation.attach { connection.disconnect() }
cancellation.detach()
'''

class CameraSafetySourceTests(unittest.TestCase):
    def test_complete_flow_passes_all_source_guards(self):
        self.assertEqual([], check_sources(CAMERA, PHOTO, HTTP))

    def test_missing_upload_consent_is_detected(self):
        errors = check_sources(CAMERA.replace('setPositiveButton("Send and analyze")', ''), PHOTO, HTTP)
        self.assertIn("AI upload requires explicit consent", errors)

    def test_missing_stale_result_guard_is_detected(self):
        errors = check_sources(CAMERA.replace('generation==request', ''), PHOTO, HTTP)
        self.assertIn("late worker results are generation-guarded", errors)

    def test_missing_bitmap_cleanup_is_detected(self):
        errors = check_sources(CAMERA, PHOTO.replace('} finally { image.recycle() }', ''), HTTP)
        self.assertIn("photo bitmap is recycled in finally", errors)

    def test_missing_decode_cancellation_is_detected(self):
        errors = check_sources(CAMERA, PHOTO.replace('reviewBitmap(file,cancellation)', 'reviewBitmap(file)'), HTTP)
        self.assertIn("photo prep passes cancellation into image decoding", errors)

    def test_missing_upload_size_limit_is_detected(self):
        errors = check_sources(CAMERA, PHOTO.replace('stream.size()<=1_048_576', ''), HTTP)
        self.assertIn("upload image has a strict size cap", errors)

if __name__ == "__main__":
    unittest.main()
