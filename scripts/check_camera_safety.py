#!/usr/bin/env python3
"""Offline source guard for Scrappy's photo-consent and cancellation flow.

This is a fast regression check, not a substitute for compiling or running Android tests.
"""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def check_sources(camera: str, photo: str, http: str) -> list[str]:
    errors: list[str] = []
    camera_checks = {
        "AI upload requires explicit consent": 'setPositiveButton("Send and analyze")' in camera,
        "local-only choice is available": 'setNegativeButton("Keep photo local"' in camera,
        "privacy policy is offered before upload": 'setNeutralButton("Privacy policy")' in camera,
        "camera worker checks cancellation before photo preparation": 'cancellation.check()\n                        val jpeg=AiPhotoClient.jpeg(photo,cancellation)' in camera,
        "camera worker checks cancellation before network identification": 'cancellation.check()\n                        AiPhotoClient.identify(jpeg,BuildConfig.AI_ENDPOINT,cancellation)' in camera,
        "late worker results are generation-guarded": 'generation==request' in camera,
        "cancel invalidates prior worker generation": 'generation++;analysis=null\n        activeRequest?.cancel()' in camera,
        "screen destruction cancels active request": 'activeRequest?.cancel();activeRequest=null' in camera,
    }
    photo_checks = {
        "photo prep passes cancellation into image decoding": 'reviewBitmap(file,cancellation)' in photo,
        "image decoder checks cancellation after bounds decode": 'BitmapFactory.decodeFile(file.absolutePath,bounds)\n        cancellation.check()' in photo,
        "image decoder checks cancellation after pixel decode": 'BitmapFactory.decodeFile(file.absolutePath,options) ?: error("Photo could not be read")\n        try {\n            // Decoding itself is not interruptible, so check immediately after it and\n            // recycle the decoded bitmap through this try/finally-style catch path.\n            cancellation.check()' in photo,
        "photo prep checks cancellation during compression": 'while(true) {\n                cancellation.check()' in photo,
        "photo prep checks cancellation after compression": 'image.compress(Bitmap.CompressFormat.JPEG,quality,stream)) { "Photo could not be prepared" }\n                cancellation.check()' in photo,
        "photo bitmap is recycled in finally": '} finally { image.recycle() }' in photo,
        "upload image has a strict size cap": 'stream.size()<=1_048_576' in photo,
        "original photo is not overwritten by preparation": 'file.writeBytes' not in photo and 'file.outputStream' not in photo,
        "EXIF orientation is read for pixel correction": 'ExifInterface.TAG_ORIENTATION' in photo,
    }
    http_checks = {
        "HTTP worker checks cancellation before request": 'cancellation.check()' in http,
        "HTTP connection can be disconnected on cancellation": 'cancellation.attach { connection.disconnect() }' in http,
        "HTTP cleanup detaches cancellation callback": 'cancellation.detach()' in http,
    }
    for group in (camera_checks, photo_checks, http_checks):
        errors.extend(label for label, passed in group.items() if not passed)
    return errors


def main() -> int:
    camera = (ROOT / "app/src/main/java/com/scrappy/app/CameraActivity.kt").read_text()
    photo = (ROOT / "app/src/main/java/com/scrappy/app/AiPhotoClient.kt").read_text()
    http = (ROOT / "app/src/main/java/com/scrappy/app/data/AiHttpClient.kt").read_text()
    errors = check_sources(camera, photo, http)
    if errors:
        print("CAMERA SAFETY SOURCE CHECK: FAIL")
        for error in errors:
            print(f"- {error}")
        return 1
    print("CAMERA SAFETY SOURCE CHECK: PASS — 20 consent, cancellation, privacy and cleanup invariants")
    print("Boundary: source-level guard only; Android compilation and runtime behavior remain unverified.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
