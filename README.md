# Glickotlin

**Glickotlin** is a Kotlin implementation of the Glicko2 algorithm.

Glicko2 as a public domain rating-system algorithm used in a wide variety of game leagues (chess, go, video games...).
It is described by its inventor Mark Glickman on his website : [http://glicko.net/glicko.html](http://glicko.net/glicko.html)

## Usage

```
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

```
