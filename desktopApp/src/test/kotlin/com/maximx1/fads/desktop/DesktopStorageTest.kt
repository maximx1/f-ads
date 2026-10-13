package com.maximx1.fads.desktop

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopStorageTest {
    private val folder: Path = Files.createTempDirectory("fads-test")

    @Test
    fun whatIsWrittenIsReadBackEvenByAnotherStorageAndReplacedByTheNextWrite() {
        // A folder that is not there yet, as on the very first run.
        val saves = folder.resolve("Fuck Ads")
        assertNull(DesktopStorage(saves).read("game.json"))

        DesktopStorage(saves).write("game.json", "first")
        DesktopStorage(saves).write("settings.txt", "theme=FLAT")
        DesktopStorage(saves).write("game.json", "second")
        assertEquals("second", DesktopStorage(saves).read("game.json"))
        assertEquals("theme=FLAT", DesktopStorage(saves).read("settings.txt"))
        assertEquals(
            setOf("game.json", "settings.txt"),
            Files.list(saves).use { files ->
                files.map { it.fileName.toString() }.toList().toSet()
            },
        )
    }

    @Test
    fun aFolderThatCannotBeWrittenLosesTheSaveButDoesNotThrow() {
        // A folder cannot be made inside something that is a file.
        val blocker = Files.writeString(folder.resolve("blocker"), "in the way")
        val storage = DesktopStorage(blocker.resolve("saves"))
        storage.write("game.json", "lost")
        assertNull(storage.read("game.json"))
    }

    @Test
    fun eachOperatingSystemKeepsTheFilesWhereItsAppsDo() {
        val home = Path.of("/home/justin")
        val nothingSet = { _: String -> null }
        assertEquals(
            Path.of("/home/justin/Library/Application Support/Fuck Ads"),
            DesktopStorage.directoryFor("Fuck Ads", os = "Mac OS X", home = home, environment = nothingSet),
        )
        assertEquals(
            Path.of("/home/justin/.local/share/Fuck Ads"),
            DesktopStorage.directoryFor("Fuck Ads", os = "Linux", home = home, environment = nothingSet),
        )
        assertEquals(
            Path.of("/data/Fuck Ads"),
            DesktopStorage.directoryFor("Fuck Ads", os = "Linux", home = home) {
                "/data".takeIf { _ ->
                    it ==
                        "XDG_DATA_HOME"
                }
            },
        )
        assertEquals(
            Path.of("/roaming/Fuck Ads"),
            DesktopStorage.directoryFor("Fuck Ads", os = "Windows 11", home = home) {
                "/roaming".takeIf { _ ->
                    it ==
                        "APPDATA"
                }
            },
        )
        assertEquals(
            Path.of("/home/justin/Fuck Ads"),
            DesktopStorage.directoryFor("Fuck Ads", os = "Windows 11", home = home, environment = nothingSet),
        )
    }
}
