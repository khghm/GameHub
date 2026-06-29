package com.gamehub.games.blokus

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

@Serializable
enum class BlokusColor {
    EMPTY,
    RED,
    BLUE,
    GREEN,
    YELLOW
}

@Serializable
data class BlokusPieceData(
    val id: Int,
    val size: Int,
    val name: String,
    val shapes: List<List<Pair<Int, Int>>> // 8 possible shapes (rotations/flips)
)

@Serializable
data class BlokusPlayerData(
    val playerId: PlayerId,
    val color: BlokusColor,
    val remainingPieces: Set<Int>, // set of piece ids remaining
    val hasMadeFirstMove: Boolean = false
)

@Serializable
data class BlokusState(
    val players: List<PlayerId>,
    val currentPlayerIndex: Int,
    val board: List<List<BlokusColor>>, // 20x20
    val playerData: Map<PlayerId, BlokusPlayerData>,
    val consecutivePasses: Int = 0,
    val gameOver: Boolean = false,
    val lastPieceWasMonomino: Boolean = false // for bonus
) : GameState() {
    val currentPlayer: PlayerId?
        get() = if (gameOver) null else players.getOrNull(currentPlayerIndex)

    companion object {
        fun initial(players: List<PlayerId>): BlokusState {
            // Use exactly the players passed in (GameSession handles adding bots)
            val fullPlayers = players.take(4)

            val colors = listOf(BlokusColor.RED, BlokusColor.BLUE, BlokusColor.GREEN, BlokusColor.YELLOW)
            val playerDataMap = fullPlayers.mapIndexed { idx, player ->
                player to BlokusPlayerData(
                    playerId = player,
                    color = colors[idx],
                    remainingPieces = (1..21).toSet(),
                    hasMadeFirstMove = false
                )
            }.toMap()

            val board = List(20) { List(20) { BlokusColor.EMPTY } }

            return BlokusState(
                players = fullPlayers,
                currentPlayerIndex = 0,
                board = board,
                playerData = playerDataMap,
                consecutivePasses = 0,
                gameOver = false,
                lastPieceWasMonomino = false
            )
        }
    }
}

// Define all 21 Blokus pieces with all 8 possible orientations!
// Each shape is translated so that min offset row and column are 0!
val BlokusPieces = listOf(
    // 1: Monomino (I1)
    BlokusPieceData(1, 1, "تک‌خانه", listOf(listOf(0 to 0))),

    // 2: Domino (I2)
    BlokusPieceData(
        2,
        2,
        "دومینو",
        listOf(
            listOf(0 to 0, 0 to 1),
            listOf(0 to 0, 1 to 0)
        )
    ),

    // 3: I3
    BlokusPieceData(
        3,
        3,
        "سه‌تایی مستقیم",
        listOf(
            listOf(0 to 0, 0 to 1, 0 to 2),
            listOf(0 to 0, 1 to 0, 2 to 0)
        )
    ),

    // 4: L3 - adjusted to remove negative offsets!
    BlokusPieceData(
        4,
        3,
        "سه‌تایی گوشه",
        listOf(
            listOf(0 to 0, 0 to 1, 1 to 0),
            listOf(1 to 0, 1 to 1, 0 to 1),
            listOf(1 to 0, 1 to 1, 0 to 0),
            listOf(1 to 0, 0 to 0, 0 to 1),
            listOf(0 to 1, 0 to 0, 1 to 0),
            listOf(0 to 0, 1 to 0, 1 to 1),
            listOf(0 to 0, 1 to 0, 0 to 1),
            listOf(1 to 1, 0 to 1, 0 to 0)
        )
    ),

    //5: I4
    BlokusPieceData(
        5,
        4,
        "چهارتایی مستقیم",
        listOf(
            listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3),
            listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0)
        )
    ),

    // 6: L4 - adjusted!
    BlokusPieceData(
        6,
        4,
        "چهارتایی L",
        listOf(
            listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0),
            listOf(2 to 0, 1 to 0, 0 to 0, 2 to 1),
            listOf(0 to 2, 0 to 1, 1 to 1, 2 to 1),
            listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2),
            listOf(1 to 3, 1 to 2, 1 to 1, 0 to 1),
            listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1),
            listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1),
            listOf(1 to 0, 0 to 0, 0 to 1, 0 to 2)
        )
    ),

    // 7: T4 - adjusted!
    BlokusPieceData(
        7,
        4,
        "چهارتایی T",
        listOf(
            listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1),
            listOf(0 to 1, 1 to 1, 2 to 1, 1 to 2),
            listOf(0 to 1, 0 to 0, 0 to 2, 1 to 1),
            listOf(1 to 1, 0 to 1, 2 to 1, 1 to 0)
        )
    ),

    // 8: S4 - adjusted!
    BlokusPieceData(
        8,
        4,
        "چهارتایی اریب",
        listOf(
            listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2),
            listOf(0 to 1, 1 to 1, 1 to 0, 2 to 0),
            listOf(0 to 2, 0 to 1, 1 to 1, 1 to 0),
            listOf(2 to 1, 1 to 1, 1 to 2, 0 to 2),
            listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2),
            listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1),
            listOf(0 to 2, 0 to 1, 1 to 1, 1 to 0),
            listOf(2 to 1, 1 to 1, 1 to 0, 0 to 0)
        )
    ),

    // 9: O4
    BlokusPieceData(
        9,
        4,
        "چهارتایی مربع",
        listOf(listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1))
    ),

    // 10: I5
    BlokusPieceData(
        10,
        5,
        "پنج‌تایی مستقیم",
        listOf(
            listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4),
            listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0)
        )
    ),

    // 11: L5 - adjusted!
    BlokusPieceData(
        11,
        5,
        "پنج‌تایی L",
        listOf(
            listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 1 to 0),
            listOf(3 to 0, 2 to 0, 1 to 0, 0 to 0, 3 to 1),
            listOf(0 to 3, 0 to 2, 1 to 2, 2 to 2, 3 to 2),
            listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2, 1 to 3),
            listOf(1 to 4, 1 to 3, 1 to 2, 1 to 1, 0 to 1),
            listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 3 to 1),
            listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1, 3 to 1),
            listOf(1 to 0, 0 to 0, 0 to 1, 0 to 2, 0 to 3)
        )
    ),

    // 12: Y5 - adjusted!
    BlokusPieceData(
        12,
        5,
        "پنج‌تایی Y",
        listOf(
            listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 2 to 1),
            listOf(2 to 0, 2 to 1, 2 to 2, 2 to 3, 1 to 1),
            listOf(0 to 1, 1 to 1, 2 to 1, 3 to 1, 2 to 0),
            listOf(1 to 3, 1 to 2, 1 to 1, 1 to 0, 2 to 1),
            listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 2 to 1),
            listOf(2 to 0, 2 to 1, 2 to 2, 2 to 3, 1 to 1),
            listOf(0 to 1, 1 to 1, 2 to 1, 3 to 1, 2 to 0),
            listOf(1 to 3, 1 to 2, 1 to 1, 1 to 0, 2 to 1)
        )
    ),

    // 13: N5 - adjusted!
    BlokusPieceData(
        13,
        5,
        "پنج‌تایی N",
        listOf(
            listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2, 1 to 3),
            listOf(0 to 1, 1 to 1, 1 to 0, 2 to 0, 3 to 0),
            listOf(0 to 3, 0 to 2, 1 to 2, 1 to 1, 1 to 0),
            listOf(3 to 1, 2 to 1, 2 to 2, 1 to 2, 0 to 2),
            listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2, 1 to 3),
            listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1, 3 to 1),
            listOf(0 to 3, 0 to 2, 1 to 2, 1 to 1, 1 to 0),
            listOf(3 to 1, 2 to 1, 2 to 0, 1 to 0, 0 to 0)
        )
    ),

    // 14: V5
    BlokusPieceData(
        14,
        5,
        "پنج‌تایی V",
        listOf(
            listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 2 to 0),
            listOf(2 to 0, 1 to 0, 0 to 0, 2 to 1, 2 to 2),
            listOf(0 to 2, 0 to 1, 0 to 0, 1 to 2, 2 to 2),
            listOf(0 to 0, 1 to 0, 2 to 0, 0 to 1, 0 to 2),
            listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 2 to 0),
            listOf(0 to 0, 1 to 0, 2 to 0, 0 to 1, 0 to 2),
            listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 2 to 0),
            listOf(0 to 0, 1 to 0, 2 to 0, 0 to 1, 0 to 2)
        )
    ),

    // 15: T5 - adjusted!
    BlokusPieceData(
        15,
        5,
        "پنج‌تایی T",
        listOf(
            listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1, 2 to 1),
            listOf(0 to 1, 1 to 1, 2 to 1, 1 to 2, 1 to 3),
            listOf(0 to 1, 0 to 0, 0 to 2, 1 to 1, 2 to 1),
            listOf(1 to 3, 1 to 2, 1 to 1, 0 to 1, 2 to 1),
            listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1, 2 to 1),
            listOf(0 to 1, 1 to 1, 2 to 1, 1 to 2, 1 to 3),
            listOf(0 to 1, 0 to 0, 0 to 2, 1 to 1, 2 to 1),
            listOf(1 to 3, 1 to 2, 1 to 1, 0 to 1, 2 to 1)
        )
    ),

    // 16: U5 - adjusted!
    BlokusPieceData(
        16,
        5,
        "پنج‌تایی U",
        listOf(
            listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 2),
            listOf(0 to 1, 1 to 1, 2 to 1, 0 to 2, 2 to 2),
            listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 2),
            listOf(0 to 1, 1 to 1, 2 to 1, 0 to 0, 2 to 0)
        )
    ),

    // 17: W5 - adjusted!
    BlokusPieceData(
        17,
        5,
        "پنج‌تایی W",
        listOf(
            listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2, 2 to 2),
            listOf(0 to 2, 1 to 2, 1 to 1, 2 to 1, 2 to 0),
            listOf(0 to 2, 0 to 1, 1 to 1, 1 to 0, 2 to 0),
            listOf(2 to 1, 1 to 1, 1 to 2, 0 to 2, 0 to 3),
            listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2, 2 to 2),
            listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1, 2 to 2),
            listOf(0 to 2, 0 to 1, 1 to 1, 1 to 0, 2 to 0),
            listOf(2 to 2, 1 to 2, 1 to 1, 0 to 1, 0 to 0)
        )
    ),

    // 18: Z5 - adjusted!
    BlokusPieceData(
        18,
        5,
        "پنج‌تایی Z",
        listOf(
            listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2, 1 to 3),
            listOf(0 to 1, 1 to 1, 2 to 1, 2 to 0, 3 to 0),
            listOf(0 to 3, 0 to 2, 0 to 1, 1 to 1, 1 to 0),
            listOf(3 to 1, 2 to 1, 1 to 1, 1 to 2, 0 to 2),
            listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2, 1 to 3),
            listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1, 3 to 1),
            listOf(0 to 3, 0 to 2, 0 to 1, 1 to 1, 1 to 0),
            listOf(3 to 1, 2 to 1, 1 to 1, 1 to 0, 0 to 0)
        )
    ),

    // 19: P5 - adjusted!
    BlokusPieceData(
        19,
        5,
        "پنج‌تایی P",
        listOf(
            listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1, 0 to 2),
            listOf(1 to 0, 2 to 0, 1 to 1, 2 to 1, 0 to 0),
            listOf(0 to 2, 0 to 1, 1 to 1, 1 to 0, 0 to 0),
            listOf(1 to 0, 0 to 0, 1 to 1, 0 to 1, 2 to 0),
            listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1, 1 to 2),
            listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1, 2 to 1),
            listOf(1 to 2, 1 to 1, 0 to 1, 0 to 0, 1 to 0),
            listOf(2 to 1, 1 to 1, 1 to 0, 2 to 0, 0 to 0)
        )
    ),

    // 20: X5 - adjusted!
    BlokusPieceData(
        20,
        5,
        "پنج‌تایی X",
        listOf(listOf(1 to 1, 0 to 1, 2 to 1, 1 to 0, 1 to 2))
    ),

    // 21: F5 - adjusted!
    BlokusPieceData(
        21,
        5,
        "پنج‌تایی F",
        listOf(
            listOf(1 to 1, 1 to 2, 0 to 2, 2 to 1, 1 to 0),
            listOf(1 to 1, 2 to 1, 2 to 2, 1 to 0, 0 to 1),
            listOf(1 to 1, 1 to 0, 2 to 0, 0 to 1, 1 to 2),
            listOf(1 to 1, 0 to 1, 0 to 0, 1 to 2, 2 to 1),
            listOf(1 to 1, 1 to 2, 2 to 2, 0 to 1, 1 to 0),
            listOf(1 to 1, 2 to 1, 2 to 0, 1 to 2, 0 to 1),
            listOf(1 to 1, 1 to 0, 0 to 0, 2 to 1, 1 to 2),
            listOf(1 to 1, 0 to 1, 0 to 2, 1 to 0, 2 to 1)
        )
    )
)

// Starting corners for each color!
val BlokusStartingCorners = mapOf(
    BlokusColor.RED to (0 to 0),
    BlokusColor.BLUE to (0 to 19),
    BlokusColor.GREEN to (19 to 0),
    BlokusColor.YELLOW to (19 to 19)
)
