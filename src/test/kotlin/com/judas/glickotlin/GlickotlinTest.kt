package com.judas.glickotlin

import org.junit.jupiter.api.Test
import kotlin.math.pow
import kotlin.test.assertEquals

class GlickotlinTest {
    @Test
    fun `glicko pdf file default sample`() {
        // Step 1 : Determine players and their initial values
        val glickotlin = Glickotlin()
        val articuno = glickotlin.createPlayer(1500.0, 200.0, 0.06)
        val zapdos = glickotlin.createPlayer(1400.0, 30.0)
        val moltres = glickotlin.createPlayer(1550.0, 100.0)
        val mewtwo = glickotlin.createPlayer(1700.0, 300.0)
        glickotlin.updateRating(
            articuno, listOf(
                Game(zapdos, GameResult.VICTORY),
                Game(moltres, GameResult.DEFEAT),
                Game(mewtwo, GameResult.DEFEAT)
            )
        )

        // Values are different than the ones from the PDF document, this is because the document rounds the numbers
        assertEquals(1464.05, articuno.rating().decimalCap(2), "Wrong rating.")
        assertEquals(151.51, articuno.deviation().decimalCap(2), "Wrong deviation.")
        assertEquals(0.05999, articuno.volatility().decimalCap(5), "Wrong volatility.")
    }

    @Test
    fun `empty game list should only update deviation`() {
        // Step 1 : Determine players and their initial values
        val glickotlin = Glickotlin()
        val snorlax = glickotlin.createPlayer(1500.0, 200.0, 0.06)
        glickotlin.updateRating(snorlax, listOf())

        assertEquals(1500.0, snorlax.rating().decimalCap(2), "Rating should not have changed.")
        assertEquals(200.27, snorlax.deviation().decimalCap(2), "Wrong deviation.")
        assertEquals(0.06, snorlax.volatility().decimalCap(2), "Volatility should not have changed.")
    }
}

fun Double.decimalCap(decimalCount: Int): Double = (this * 10.0.pow(decimalCount)).toInt() / 10.0.pow(decimalCount)
