package com.maximx1.fads.ui.den

import androidx.compose.ui.graphics.Color
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.ui.den.FeltPattern.DIAMONDS
import com.maximx1.fads.ui.den.FeltPattern.DOTS
import com.maximx1.fads.ui.den.FeltPattern.HEXES
import com.maximx1.fads.ui.den.FeltPattern.LATTICE
import com.maximx1.fads.ui.den.FeltPattern.PINSTRIPE
import com.maximx1.fads.ui.den.FeltPattern.PLAIN
import com.maximx1.fads.ui.den.FeltPattern.SCALES
import com.maximx1.fads.ui.den.FeltPattern.STARS
import com.maximx1.fads.ui.den.FeltPattern.SUNBURST
import com.maximx1.fads.ui.den.FeltPattern.WAVES

/**
 * Every look the den can have: the house's own first, which every player has, then the fifty to buy. Each is its
 * felt, rim, card back and accent colours, the pattern in its felt and the emblem at the heart of its tables. The
 * keys are saved, so they never change.
 */
val DenThemes: List<DenTheme> =
    listOf(
        theme(Den.HOUSE_THEME, "House", PLAIN, "♠", 0xFF1F7A45, 0xFF6B4423, 0xFF1E40AF, 0xFFFFE082),
        theme("secret-agent", "Secret Agent", PINSTRIPE, "007", 0xFF1C1C1E, 0xFF0A0A0A, 0xFF8A6D1F, 0xFFD4AF37),
        theme("imperial", "Imperial Dynasty", SCALES, "龍", 0xFF8B1A1A, 0xFF3D2410, 0xFFB8860B, 0xFFFFD54F),
        theme("samurai", "Samurai", WAVES, "侍", 0xFF1F2F57, 0xFF1A1A1A, 0xFF9E1B1B, 0xFFE8D5B5),
        theme("pirate", "Pirate Ship", LATTICE, "☠", 0xFF2E4A3F, 0xFF5A3A1E, 0xFF2B2B2B, 0xFFE0C068),
        theme("art-deco", "Art Deco", SUNBURST, "◆", 0xFF102A2E, 0xFFB08D3A, 0xFF0F3D3E, 0xFFE6C36A),
        theme("neon-vegas", "Neon Vegas", STARS, "★", 0xFF2A0F4A, 0xFFE91E8C, 0xFF00B8D4, 0xFFFF4FD8),
        theme("monte-carlo", "Monte Carlo", DIAMONDS, "♛", 0xFF0E4D3A, 0xFFC9B37E, 0xFF6E1423, 0xFFF2E3B3),
        theme("saloon", "Wild West Saloon", PLAIN, "☆", 0xFF6E4B2A, 0xFF3B2414, 0xFF7A2E1A, 0xFFF0C987),
        theme("riverboat", "Riverboat", WAVES, "⚓", 0xFF23503F, 0xFF7B5530, 0xFF1F3A5F, 0xFFE5D3A1),
        theme("pharaoh", "Pharaoh's Tomb", HEXES, "☥", 0xFFB38B3D, 0xFF2F2A1F, 0xFF1F5E8C, 0xFF40C4FF),
        theme("viking", "Viking Hall", LATTICE, "ᚱ", 0xFF3E4A52, 0xFF5B3A1E, 0xFF7A1F1F, 0xFFB0BEC5),
        theme("arabian", "Arabian Nights", STARS, "☾", 0xFF1A2A6C, 0xFFB8860B, 0xFF5B1A6E, 0xFFFFD27F),
        theme("mardi-gras", "Mardi Gras", DIAMONDS, "⚜", 0xFF4A1A6B, 0xFF2E7D32, 0xFFC9A227, 0xFFFFEB3B),
        theme("havana", "Havana Nights", PINSTRIPE, "♣", 0xFF5D3A22, 0xFF2B1A10, 0xFF0F6B5E, 0xFFF4B860),
        theme("speakeasy", "Speakeasy", PINSTRIPE, "♦", 0xFF3B1F1F, 0xFF151515, 0xFF4E342E, 0xFFD7B377),
        theme("moulin-rouge", "Moulin Rouge", DOTS, "♥", 0xFF8E1C2E, 0xFF1A0A0A, 0xFF111111, 0xFFFF8FA3),
        theme("royal-court", "Royal Court", DIAMONDS, "♚", 0xFF2C1A5C, 0xFFC5A253, 0xFF8B0000, 0xFFFFD700),
        theme("jungle", "Lost Jungle", SCALES, "✿", 0xFF1E5631, 0xFF4E3B1F, 0xFF33691E, 0xFFC5E1A5),
        theme("arctic", "Arctic Lodge", HEXES, "❄", 0xFF5B7C99, 0xFFECEFF1, 0xFF37474F, 0xFFE1F5FE),
        theme("deep-sea", "Deep Sea", WAVES, "≈", 0xFF0B3C5D, 0xFF1F2A33, 0xFF00695C, 0xFF64FFDA),
        theme("outer-space", "Outer Space", STARS, "✦", 0xFF0B0F2B, 0xFF5C6BC0, 0xFF311B92, 0xFFB388FF),
        theme("cyber-grid", "Cyber Grid", LATTICE, "◈", 0xFF06161F, 0xFF00E5FF, 0xFFFF00A8, 0xFF18FFFF),
        theme("steampunk", "Steampunk", HEXES, "⚙", 0xFF4A3B2A, 0xFFB87333, 0xFF3E2723, 0xFFFFB74D),
        theme("gothic", "Gothic Castle", DIAMONDS, "♜", 0xFF2A2A35, 0xFF111116, 0xFF4A0E1E, 0xFFB0A4C8),
        theme("haunted", "Haunted Manor", SCALES, "☽", 0xFF262C2A, 0xFF3A2E3F, 0xFF4A148C, 0xFFB2FF59),
        theme("candy", "Candy Shop", DOTS, "✧", 0xFFF06292, 0xFFFFFFFF, 0xFF7E57C2, 0xFFFFF59D),
        theme("tiki", "Tiki Bar", WAVES, "✺", 0xFF00796B, 0xFF8D6E63, 0xFFE65100, 0xFFFFCC80),
        theme("sakura", "Cherry Blossom", DOTS, "桜", 0xFFF3C6D3, 0xFF5D4037, 0xFFAD1457, 0xFFC2185B),
        theme("zen", "Zen Garden", WAVES, "禅", 0xFF8C8C6A, 0xFF3E3A2F, 0xFF2E4A3F, 0xFFDCE775),
        theme("mayan", "Mayan Temple", HEXES, "◬", 0xFF3E6B48, 0xFF6D5C3E, 0xFFB5651D, 0xFFFFC107),
        theme("olympus", "Mount Olympus", SUNBURST, "Ω", 0xFFE8E2D0, 0xFFB8A15A, 0xFF1565C0, 0xFF1565C0),
        theme("colosseum", "Colosseum", PLAIN, "Ⅻ", 0xFF9C6B3C, 0xFF5E4630, 0xFF8B1A1A, 0xFFFFE0B2),
        theme("celtic", "Celtic Knot", LATTICE, "☘", 0xFF1B5E20, 0xFF3E2723, 0xFF2E7D32, 0xFFA5D6A7),
        theme("bavarian", "Bavarian Hall", DIAMONDS, "✠", 0xFF1E3A6E, 0xFFE0E0E0, 0xFF1565C0, 0xFF90CAF9),
        theme("paris", "Paris Café", PINSTRIPE, "⚜", 0xFF243B55, 0xFF2B2B2B, 0xFF8E1C2E, 0xFFF8BBD0),
        theme("venice", "Venetian Masquerade", DIAMONDS, "♢", 0xFF3A0D4A, 0xFFC9A227, 0xFF00695C, 0xFFE1BEE7),
        theme("bollywood", "Bollywood", SUNBURST, "✷", 0xFFC2185B, 0xFFFFB300, 0xFF4527A0, 0xFFFFE082),
        theme("safari", "Safari", SCALES, "❋", 0xFFB8935A, 0xFF4E3B1F, 0xFF5D4037, 0xFFFFCC80),
        theme("oasis", "Desert Oasis", WAVES, "☼", 0xFFD9A35F, 0xFF8D6E63, 0xFF00838F, 0xFF80DEEA),
        theme("aurora", "Northern Lights", WAVES, "✶", 0xFF0D2B3E, 0xFF263238, 0xFF00BFA5, 0xFFA7FFEB),
        theme("volcano", "Volcano", HEXES, "▲", 0xFF2B0F0A, 0xFF4E1C0F, 0xFFBF360C, 0xFFFF6E40),
        theme("emerald", "Emerald City", DIAMONDS, "◇", 0xFF0B6E4F, 0xFFC0CA33, 0xFF1B5E20, 0xFFB9F6CA),
        theme("velvet", "Midnight Velvet", PLAIN, "☾", 0xFF1A1033, 0xFF0D0D0D, 0xFF4A148C, 0xFFCE93D8),
        theme("hollywood", "Golden Age Hollywood", SUNBURST, "✪", 0xFF2B2B2B, 0xFFD4AF37, 0xFF8B0000, 0xFFFFE57F),
        theme("disco", "Disco Inferno", DOTS, "✺", 0xFF4A148C, 0xFFC0C0C0, 0xFFFF6F00, 0xFFFFD740),
        theme("diner", "Retro Diner", PINSTRIPE, "♨", 0xFF00838F, 0xFFE53935, 0xFFE53935, 0xFFFFF176),
        theme("clover", "Lucky Clover", SCALES, "♣", 0xFF2E7D32, 0xFFC9A227, 0xFF1B5E20, 0xFFFFD54F),
        theme("ruby", "Ruby Room", DIAMONDS, "♦", 0xFF7B1020, 0xFF2B0A0F, 0xFF4A0E1E, 0xFFFF8A80),
        theme("hearts", "Hearts Club", DOTS, "♥", 0xFF5C1A33, 0xFF1A0A10, 0xFFAD1457, 0xFFFF80AB),
        theme("black-tie", "Black Tie", PINSTRIPE, "♠", 0xFF151515, 0xFF3A3A3A, 0xFF212121, 0xFFE0E0E0),
    )

/** The theme saved as [key], or the house's if no theme is called that any more. */
fun denTheme(key: String): DenTheme = DenThemes.find { it.key == key } ?: DenThemes.first()

@Suppress("LongParameterList") // A theme and each of its colours.
private fun theme(
    key: String,
    name: String,
    pattern: FeltPattern,
    emblem: String,
    felt: Long,
    rim: Long,
    back: Long,
    accent: Long,
) = DenTheme(key, name, Color(felt), pattern, Color(rim), Color(back), Color(accent), emblem)
