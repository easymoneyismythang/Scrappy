plugins { id("org.jetbrains.kotlin.jvm") }

kotlin {
    jvmToolchain(17)
    sourceSets.main {
        kotlin.srcDir("../app/src/main/java")
        kotlin.include(
            "com/scrappy/app/data/AiHttpClient.kt",
            "com/scrappy/app/data/AiIdentification.kt",
            "com/scrappy/app/data/BoundedResponse.kt",
            "com/scrappy/app/data/RequestCancellation.kt",
            "com/scrappy/app/data/RequestDeadline.kt",
            "com/scrappy/app/data/ResponseDeadline.kt"
        )
    }
}

dependencies {
    implementation("org.json:json:20240303")
    testImplementation("junit:junit:4.13.2")
}

// These loopback socket tests need the JDK's HTTP server module. They execute
// on the host JVM and never add a server dependency to the Android app.
tasks.test {
    jvmArgs("--add-modules=jdk.httpserver")
    systemProperty("scrappy.projectRoot",rootProject.projectDir.absolutePath)
    inputs.files(rootProject.file("backend/server.mjs"),file("fixtures/gateway.mjs"))
}
