# Scrappy 4.9.6 — bounded network responses

Weather response bodies were read without a size bound. A very large reply could exhaust memory. Weather JSON is now capped at 256 KiB before parsing. AI JSON uses the same bounded reader with its existing 16 KiB cap, stopping immediately after the first excess byte. Connections and input streams are still closed on failure.

Four regression tests cover empty/exact-limit responses, oversized endless streams, short/zero-length reads, and invalid limits. They use local synthetic streams, not live providers.

Verification: standalone Kotlin suite 141 passed, zero failures; Node gateway suite 19 passed, zero failures; release configuration and document consistency passed. Android unit tests/lint/preview APK packaging attempted, but the Gradle 8.11.1 download failed with Network is unreachable before compilation. Kotlin verification excludes Android activities and SharedPreferences adapters; live Gemini, weather and physical phone acceptance are still pending.

No installable APK/AAB was produced. Included GitHub/GitLab pipelines remain the route to a full Android build in a networked runner. Production gateway hosting, public privacy policy, signing and Play Console acceptance remain outstanding.
