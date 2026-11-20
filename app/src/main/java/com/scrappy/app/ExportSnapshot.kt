package com.scrappy.app

import android.content.Context
import java.io.File
import java.util.UUID

/** Keep export bytes off the saved-state Bundle (Android has a small Binder limit). */
class ExportSnapshot(context: Context) {
    private val directory = File(context.cacheDir,"document-exports").apply { mkdirs() }
    private fun file(token: String): File {
        require(token.matches(Regex("[a-f0-9-]{36}\\.txt"))) { "Invalid export snapshot" }
        return File(directory,token)
    }
    fun save(text: String): String {
        // Abandoned snapshots expire after seven days; active picker state is retained.
        val cutoff=System.currentTimeMillis()-7*86_400_000L
        directory.listFiles()?.filter { it.lastModified()<cutoff }?.forEach { it.delete() }
        val token="${UUID.randomUUID()}.txt"
        file(token).writeText(text,Charsets.UTF_8)
        return token
    }
    fun read(token: String) = file(token).readText(Charsets.UTF_8)
    fun remove(token: String) { file(token).delete() }
}
