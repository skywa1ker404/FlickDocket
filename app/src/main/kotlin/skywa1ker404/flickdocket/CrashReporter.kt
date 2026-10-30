package skywa1ker404.flickdocket

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Keeps the last uncaught exception in a private file so a crash can be diagnosed without any telemetry. Nothing is sent
 * anywhere; the user decides whether to share it from Settings. Keys and tokens are redacted before writing.
 */
object CrashReporter {
    private const val FILE = "last-crash.txt"
    private val SECRETS = listOf(
        Regex("(?i)(api_key|apikey|access_token|refresh_token|client_secret|code_verifier|authorization)([=:\\s\"]+)[^&\\s\"]+") to "$1$2[redacted]",
        Regex("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+") to "Bearer [redacted]",
    )

    fun redact(text: String): String = SECRETS.fold(text) { acc, (re, to) -> re.replace(acc, to) }

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
                File(app.filesDir, FILE).writeText(redact("${java.util.Date()} thread=${thread.name}\n$trace").take(20_000))
            } catch (_: Throwable) { /* never mask the original crash */ }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? = try { File(context.filesDir, FILE).takeIf { it.exists() }?.readText() } catch (_: Exception) { null }
    fun clear(context: Context) { try { File(context.filesDir, FILE).delete() } catch (_: Exception) {} }
}
