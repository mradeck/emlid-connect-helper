package de.emlidhelper.app.shizuku

import android.content.Context
import androidx.annotation.Keep
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

@Keep
class PrivilegedShellService : IPrivilegedShellService.Stub {

    @Suppress("UNUSED_PARAMETER")
    @Keep
    constructor(context: Context) : super()

    @Keep
    constructor() : super()

    override fun exec(command: String): String {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()

            val output = StringBuilder()
            val readerThread = Thread {
                try {
                    process.inputStream.bufferedReader().use { reader ->
                        val buffer = CharArray(4096)
                        var read: Int
                        while (reader.read(buffer).also { read = it } != -1) {
                            output.append(buffer, 0, read)
                        }
                    }
                } catch (_: Exception) {
                }
            }
            readerThread.start()

            val finished = process.waitFor(12, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return "EXIT:-1\nTIMEOUT"
            }
            readerThread.join(1000)
            val exitCode = process.exitValue()
            "EXIT:$exitCode\n$output"
        } catch (e: Exception) {
            "EXIT:-1\n${e.javaClass.simpleName}: ${e.message}"
        }
    }

    override fun destroy() {
        exitProcess(0)
    }
}
