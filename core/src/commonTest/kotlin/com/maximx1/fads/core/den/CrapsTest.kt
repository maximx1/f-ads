package com.maximx1.fads.core.den

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrapsTest {
    private fun table(vararg bets: Pair<CrapsBet, Long>): Den =
        bets.fold(Den(purse = Purse(1_000))) { den, (bet, amount) -> den.betCraps(bet, amount) }

    @Test
    fun theComeOutRollWinsOnSevenOrElevenLosesOnCrapsAndSetsAPointOtherwise() {
        val lines = table(CrapsBet.PASS to 10, CrapsBet.DONT_PASS to 10)
        assertEquals(Purse(980), lines.purse)
        val seven = lines.rollDiceAs(3, 4)
        assertEquals(20, seven.craps().last?.paid, "pass wins, don't pass loses")
        assertNull(seven.craps().point)
        assertTrue(seven.craps().bets.isEmpty())

        val three = lines.rollDiceAs(1, 2)
        assertEquals(20, three.craps().last?.paid, "don't pass wins, pass loses")

        val twelve = lines.rollDiceAs(6, 6)
        assertEquals(0, twelve.craps().last?.paid)
        assertEquals(mapOf(CrapsBet.DONT_PASS to 10L), twelve.craps().bets, "a twelve stands the don't pass off")

        val six = lines.rollDiceAs(2, 4)
        assertEquals(6, six.craps().point)
        assertEquals(lines.craps().bets, six.craps().bets)
        assertFalse(six.canBetCraps(CrapsBet.PASS), "the lines are closed once a point is on")
        assertFailsWith<IllegalArgumentException> { six.betCraps(CrapsBet.PASS, 5) }
    }

    @Test
    fun withAPointOnMakingItWinsThePassAndASevenTheDontPass() {
        val on = table(CrapsBet.PASS to 10, CrapsBet.DONT_PASS to 10).rollDiceAs(4, 4)
        val nothing = on.rollDiceAs(2, 3)
        assertEquals(8, nothing.craps().point)
        assertEquals(0, nothing.craps().last?.paid)
        val made = on.rollDiceAs(5, 3)
        assertEquals(20, made.craps().last?.paid)
        assertNull(made.craps().point)
        val sevenOut = on.rollDiceAs(6, 1)
        assertEquals(20, sevenOut.craps().last?.paid)
        assertNull(sevenOut.craps().point)
    }

    @Test
    fun oneRollBetsSettleOnTheNextThrow() {
        val bets = table(CrapsBet.FIELD to 10, CrapsBet.ANY_SEVEN to 10, CrapsBet.ANY_CRAPS to 10)
        // Field double on 2 is 30 back, any craps 7 to 1 is 80, and any seven loses.
        assertEquals(
            110,
            bets
                .rollDiceAs(1, 1)
                .craps()
                .last
                ?.paid,
        )
        assertEquals(
            50,
            bets
                .rollDiceAs(3, 4)
                .craps()
                .last
                ?.paid,
            "any seven 4 to 1, field loses on 7",
        )
        assertEquals(
            20,
            bets
                .rollDiceAs(4, 5)
                .craps()
                .last
                ?.paid,
            "field evens on 9",
        )
        assertTrue(
            bets
                .rollDiceAs(4, 5)
                .craps()
                .bets
                .isEmpty(),
        )
    }

    @Test
    fun placeBetsWorkOnlyWithAPointOnPayTheirOddsAndStayUp() {
        val placed = table(CrapsBet.PLACE_6 to 12, CrapsBet.PLACE_4 to 5)
        val comeOut = placed.rollDiceAs(3, 3)
        assertEquals(0, comeOut.craps().last?.paid, "off on the come-out")
        assertEquals(6, comeOut.craps().point)

        val six = comeOut.rollDiceAs(2, 4)
        assertEquals(14, six.craps().last?.paid, "7 to 6 on twelve")
        assertEquals(placed.craps().bets, six.craps().bets, "and the bet stays up")

        val withPass = comeOut.betCraps(CrapsBet.PLACE_4, 5)
        assertEquals(
            18,
            withPass
                .rollDiceAs(2, 2)
                .craps()
                .last
                ?.paid,
            "9 to 5 on ten",
        )
        assertTrue(
            comeOut
                .rollDiceAs(3, 4)
                .craps()
                .bets
                .isEmpty(),
            "a seven takes them down",
        )
    }

    @Test
    fun clearingTakesBackAllButTheLinesOnceAPointIsOn() {
        val on = table(CrapsBet.PASS to 10, CrapsBet.PLACE_8 to 6).rollDiceAs(5, 5)
        val cleared = on.clearCraps()
        assertEquals(mapOf(CrapsBet.PASS to 10L), cleared.craps().bets)
        assertEquals(on.purse + 6, cleared.purse)
        assertEquals(Purse(1_000), table(CrapsBet.FIELD to 7).clearCraps().purse)
        assertFailsWith<IllegalArgumentException> { Den().rollDice(1) }
        assertFailsWith<IllegalArgumentException> {
            Den(
                purse = Purse(5_000),
            ).betCraps(CrapsBet.FIELD, 999, Room.HIGH_ROLLERS)
        }
        val rolled = table(CrapsBet.FIELD to 7).rollDice(4)
        assertEquals(1, rolled.craps().throws)
        assertEquals(rolled, Den.fromJson(rolled.toJson()))
    }
}
