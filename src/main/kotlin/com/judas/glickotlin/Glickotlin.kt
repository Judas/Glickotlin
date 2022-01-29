package com.judas.glickotlin

import kotlin.math.*

const val SCALE_FACTOR: Double = 173.7178
const val SCALE_OFFSET: Double = 1500.0

data class Config(
    /**
     * Initial rating for new player. Recommended value is 1500.
     */
    val initialRating: Double = 1500.0,

    /**
     * Initial rating deviation for new player. Recommended value is 350.
     */
    val initialDeviation: Double = 350.0,

    /**
     * Initial rating volatility for new player. Recommended value is 0.06.
     */
    val initialVolatility: Double = 0.06,

    /**
     * Tau constant, it basically powers up the volatility.
     * High values means the game is less "random" so a win/loss is more impactful.
     * Should be between 0.3 & 1.2
     */
    val systemConstantTau: Double = 0.5
)

class Player(rating: Double, deviation: Double, volatility: Double) {
    // Step 2 : convert to Glicko2 scale
    internal var scaledRating = (rating - SCALE_OFFSET) / SCALE_FACTOR // µ
    internal var scaledDeviation = deviation / SCALE_FACTOR // φ
    internal var scaledVolatility = volatility

    // Step 8 : convert back to original scale
    fun rating(): Double = SCALE_FACTOR * scaledRating + SCALE_OFFSET
    fun deviation(): Double = scaledDeviation * SCALE_FACTOR
    fun volatility(): Double = scaledVolatility
}

data class Game(val opponent: Player, val result: GameResult)

enum class GameResult(val value: Double) { VICTORY(1.0), DEFEAT(0.0), DRAW(0.5) }

class Glickotlin(private val config: Config = Config()) {
    fun createPlayer(
        rating: Double = config.initialRating,
        deviation: Double = config.initialDeviation,
        volatility: Double = config.initialVolatility
    ): Player = Player(rating, deviation, volatility)

    /**
     * Computes the new rating/deviation/volatility of a player after the given games.
     */
    fun updateRating(player: Player, games: List<Game>) {
        // If player does not compete, only the deviation is modified
        if (games.isEmpty()) {
            // Step 6 : Compute new deviation
            player.scaledDeviation = sqrt(player.scaledDeviation.pow(2) + player.scaledVolatility.pow(2))
            return
        }

        // Step 3 : Compute estimated variance (v)
        val variance = games.map { it.opponent }.map {
            val e = e(player, it)
            g(it).pow(2) * e * (1 - e)
        }.reduce { a, b -> a + b }.pow(-1)

        // Step 4 : Compute estimated improvement (delta)
        val improvement = variance * games.map { g(it.opponent) * (it.result.value - e(player, it.opponent)) }
            .reduce { a, b -> a + b }

        // Step 5-1 : Define tolerance
        val tolerance = 0.000001 // epsilon

        // Step 5-2 : Define initial limits
        var limitA = ln(player.scaledVolatility.pow(2))
        var limitB = if (improvement.pow(2) > player.scaledVolatility.pow(2) + variance) {
            ln(improvement.pow(2) - player.scaledVolatility.pow(2) - variance)
        } else {
            var k = 0
            var fk: Double
            do {
                k++
                fk = f(
                    ln(player.scaledVolatility.pow(2)) - k * config.systemConstantTau,
                    player,
                    variance,
                    improvement,
                    config.systemConstantTau
                )
            } while (fk < 0)

            ln(player.scaledVolatility.pow(2)) - k * config.systemConstantTau
        }

        // Step 5-3 : Define fa & fb
        var fa = f(
            limitA, player, variance, improvement, config.systemConstantTau
        )
        var fb = f(
            limitB, player, variance, improvement, config.systemConstantTau
        )

        // Step 5-4 : Narrow the bracket down
        while (abs(limitB - limitA) > tolerance) {
            val limitC = limitA + (limitA - limitB) * fa / (fb - fa)
            val fc = f(
                limitC, player, variance, improvement, config.systemConstantTau
            )

            if (fc * fb < 0) {
                limitA = limitB
                fa = fb
            } else {
                fa /= 2
            }
            limitB = limitC
            fb = fc
        }

        // Step 5-5 : Compute new volatility
        player.scaledVolatility = exp(limitA / 2)

        // Step 6-7 : Compute the new rating & deviation
        val newScaledDeviation =
            1 / sqrt(1 / sqrt(player.scaledDeviation.pow(2) + player.scaledVolatility.pow(2)).pow(2) + 1 / variance)
        val newScaledRating = player.scaledRating + newScaledDeviation.pow(2) * games.map {
            g(it.opponent) * (it.result.value - e(
                player, it.opponent
            ))
        }.reduce { a, b -> a + b }

        player.scaledRating = newScaledRating
        player.scaledDeviation = newScaledDeviation
    }

    /**
     * G function of the Glicko algorithm. Used in steps 3, 4 & 7
     */
    private fun g(player: Player): Double = 1 / (sqrt(1 + 3 * player.scaledDeviation.pow(2) / PI.pow(2)))

    /**
     * E function of the Glicko algorithm. Used in steps 3, 4 & 7
     */
    private fun e(player: Player, opponent: Player): Double =
        1 / (1 + exp((opponent.scaledRating - player.scaledRating) * g(opponent)))

    /**
     * f function of the Glicko algorithm. Used in step 5
     */
    private fun f(x: Double, player: Player, variance: Double, improvement: Double, tau: Double): Double {
        val m = exp(x) * (improvement.pow(2) - player.scaledDeviation.pow(2) - variance - exp(x))
        val n = 2 * (player.scaledDeviation.pow(2) + variance + exp(x)).pow(2)
        val o = x - ln(player.scaledVolatility.pow(2))
        val p = tau.pow(2)
        return m / n - o / p
    }
}
