# Scrappy 4.9.6 preview build

Built from the November 8 verification checkpoint with Android Gradle Plugin 8.10.1, Gradle 8.11.1, Java 17, compile SDK 36, and Android Build Tools 35.0.0. The Android Gradle Plugin update aligns the project with API 36.

- Preview and camera-test APKs assembled. The Android build tools verified both signatures and checked 16 KiB APK alignment.
- Preview and camera-test variants passed 154 JVM tests each. The shared HTTP transport suite passed 19 tests.
- The gateway passed 32 tests using a mock provider. The build helpers passed 39 tests, and the camera source guard passed 20 checks.
- Android lint completed with 0 errors and 75 warnings.
- Phone installation and camera behavior require a Samsung acceptance test. Live Gemini requires a deployed gateway and configured HTTPS endpoint and privacy URLs.

SHA-256 for the verified preview APK: `3a95fddaff1786928154395711fa5e8ab34446b7e444aed1d72fd4e5a7dd434a`.

SHA-256 for the verified camera-test APK: `4c7a2127ee78b3b7f101f98c7ede6dc650f1d6a1f4d3d5aba66f007e4710e2c8`.
