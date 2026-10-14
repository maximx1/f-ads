package com.maximx1.fads.game

/** Keeps a few small pieces of text between runs, each under a name. Every platform brings its own. */
interface Storage {
    /** The text saved under [name], or `null` if there is none or it cannot be read. Blocks while it reads. */
    fun read(name: String): String?

    /**
     * Saves [text] under [name] in place of what was there. Blocks while it writes. If the write fails the old
     * text is kept and nothing is thrown: losing a save should never stop the game.
     */
    fun write(
        name: String,
        text: String,
    )
}
