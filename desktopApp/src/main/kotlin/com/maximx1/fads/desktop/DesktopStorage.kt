package com.maximx1.fads.desktop

import com.maximx1.fads.game.Storage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Storage as files in [directory], one for each name. A file is replaced whole or not at all. */
class DesktopStorage(
    private val directory: Path,
) : Storage {
    override fun read(name: String): String? =
        try {
            Files.readString(directory.resolve(name))
        } catch (ignored: IOException) {
            null
        }

    override fun write(
        name: String,
        text: String,
    ) {
        try {
            Files.createDirectories(directory)
            // Written beside the real file and then moved over it, so a crash half way leaves the old one.
            val draft = Files.createTempFile(directory, name, ".tmp")
            Files.writeString(draft, text)
            Files.move(draft, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            System.err.println("Could not save $name: $e")
        }
    }

    companion object {
        /** Where this operating system keeps an application's own files, in a folder named [app]. */
        fun directoryFor(
            app: String,
            os: String = System.getProperty("os.name"),
            home: Path = Path.of(System.getProperty("user.home")),
            environment: (String) -> String? = System::getenv,
        ): Path =
            when {
                os.startsWith("Mac") -> home.resolve("Library/Application Support").resolve(app)
                os.startsWith("Windows") -> (environment("APPDATA")?.let(Path::of) ?: home).resolve(app)
                else -> (environment("XDG_DATA_HOME")?.let(Path::of) ?: home.resolve(".local/share")).resolve(app)
            }
    }
}
