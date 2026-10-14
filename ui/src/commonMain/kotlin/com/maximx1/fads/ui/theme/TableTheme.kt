package com.maximx1.fads.ui.theme

/** The looks the player can choose between. The names are what gets saved, so renaming one forgets the choice. */
enum class TableTheme(
    val label: String,
) {
    FLAT("Flat"),
    VISTA("Vista"),
    CYBERPUNK("Cyberpunk"),
    BRUSHED_METAL("Brushed metal"),
    BACKROOM("Backroom"),
    ;

    /** True if this theme's table is pale, so whatever is drawn over its edges needs to be dark to show. */
    val isPale: Boolean get() = look.paleTable
}

internal val TableTheme.look: Look
    get() =
        when (this) {
            TableTheme.FLAT -> FlatLook
            TableTheme.VISTA -> VistaLook
            TableTheme.CYBERPUNK -> CyberpunkLook
            TableTheme.BRUSHED_METAL -> BrushedMetalLook
            TableTheme.BACKROOM -> BackroomLook
        }
