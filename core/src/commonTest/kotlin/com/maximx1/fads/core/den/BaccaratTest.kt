package com.maximx1.fads.core.den

import com.maximx1.fads.core.model.Card
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BaccaratTest {
    private fun cards(codes: String) = codes.split(' ').map(Card::fromCode)

    /** A shoe dealt in order: player, banker, player, banker, then the third cards. */
    private fun shoe(codes: String) = cards(codes) + Card.deck.filter { it !in cards(codes) }

    private fun coup(codes: String) = coupOf(shoe(codes)).let { (player, banker) -> player.size to banker.size }

    @Test
    fun cardsCountTheirLastDigitAndCourtCardsNothing() {
        assertEquals(0, cards("KS QH JD 10C").sumOf { it.baccaratValue })
        assertEquals(1, cards("AS").single().baccaratValue)
        assertEquals(5, baccaratTotal(cards("7S 8H")))
        assertEquals(9, baccaratTotal(cards("9S KH")))
    }

    @Test
    fun aThirdCardIsDrawnByTheRules() {
        // A natural on either side: nobody draws.
        assertEquals(2 to 2, coup("8S 2H KD 3C"))
        assertEquals(2 to 2, coup("2S 9H 3D KC"))
        // The player stands on six or seven; the banker then draws to five or less.
        assertEquals(2 to 3, coup("6S 2H KD 3C 4D"))
        assertEquals(2 to 2, coup("6S 3H KD 3C"))
        // The player draws on five or less; the banker's draw hangs on the player's third card.
        assertEquals(3 to 3, coup("2S 2H 3D KC 9D 4S"), "banker on two always draws")
        assertEquals(3 to 2, coup("2S 3H 3D KC 8D"), "banker on three stands against an eight")
        assertEquals(3 to 3, coup("2S 4H 3D KC 2D 4S"), "banker on four draws against a two")
        assertEquals(3 to 2, coup("2S 5H 3D KC 3D"), "banker on five stands against a three")
        assertEquals(3 to 3, coup("2S 6H 3D KC 7D 4S"), "banker on six draws against a seven")
        assertEquals(3 to 2, coup("2S 7H 3D KC 6D"), "banker on seven stands")
    }

    @Test
    fun eachSidePaysItsOddsAndATieHandsThePlayerAndBankerBetsBack() {
        val bets =
            Den(purse = Purse(500))
                .betBaccarat(BaccaratSide.PLAYER, 100)
                .betBaccarat(BaccaratSide.BANKER, 100)
                .betBaccarat(BaccaratSide.TIE, 20)
        assertEquals(Purse(280), bets.purse)

        // Player 9 to banker 5: evens on the player.
        val player = bets.playBaccaratFrom(shoe("9S 2H KD 3C"))
        assertEquals(BaccaratSide.PLAYER, player.baccarat().last?.won)
        assertEquals(Purse(280 + 200), player.purse)

        // Banker 8 to player 5, no draw: evens less five in a hundred on the banker.
        val banker = bets.playBaccaratFrom(shoe("2S 5H 3D 3C"))
        assertEquals(BaccaratSide.BANKER, banker.baccarat().last?.won)
        assertEquals(Purse(280 + 195), banker.purse)

        // A tie at nine: eight to one on the tie, and the other two handed back.
        val tie = bets.playBaccaratFrom(shoe("9S 9H KD KC"))
        assertEquals(BaccaratSide.TIE, tie.baccarat().last?.won)
        assertEquals(Purse(280 + 180 + 200), tie.purse)
        assertEquals(1, tie.baccarat().coups)
        assertEquals(emptyMap(), tie.baccarat().bets)
        assertEquals(tie, Den.fromJson(tie.toJson()))
    }

    @Test
    fun betsTakeTheRoomsLeastAndComeBackWhenCleared() {
        val den = Den(purse = Purse(50)).betBaccarat(BaccaratSide.TIE, 5).clearBaccarat()
        assertEquals(Purse(50), den.purse)
        assertFailsWith<IllegalArgumentException> { den.playBaccarat(1) }
        assertFailsWith<IllegalArgumentException> {
            Den(purse = Purse(5_000)).betBaccarat(BaccaratSide.TIE, 999, Room.HIGH_ROLLERS)
        }
        assertEquals(
            1,
            Den(purse = Purse(50))
                .betBaccarat(BaccaratSide.PLAYER, 5)
                .playBaccarat(3)
                .baccarat()
                .coups,
        )
    }
}
