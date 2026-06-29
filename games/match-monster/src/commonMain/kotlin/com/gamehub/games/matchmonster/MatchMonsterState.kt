package com.gamehub.games.matchmonster

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

enum class MonsterType(val displayName: String) {
    FIRE("آتش"),
    WATER("آب"),
    EARTH("زمین"),
    AIR("باد"),
    DARK("تاریکی"),
    LIGHT("نور")
}

enum class TileState {
    NORMAL,
    FROZEN,
    STONE,
    THICK
}

enum class SpecialTileType {
    NONE,
    BOMB,
    LIGHTNING,
    RAINBOW
}

@Serializable
data class Tile(
    val type: MonsterType,
    val state: TileState = TileState.NORMAL,
    val special: SpecialTileType = SpecialTileType.NONE,
    val thickLevel: Int = 0
)

@Serializable
data class GarbageRow(
    val eyePosition: Int, // column index (0-5)
    val isCleared: Boolean = false
)

@Serializable
data class PlayerGameData(
    val board: List<List<Tile?>> = List(8) { List(6) { null } }, // 8 rows, 6 columns
    val hp: Int = 100,
    val garbageQueue: Int = 0,
    val garbageRows: List<GarbageRow> = emptyList()
)

@Serializable
data class PlayerDataEntry(
    val playerId: PlayerId,
    val data: PlayerGameData
)

@Serializable
data class MatchMonsterState(
    val playerData: List<PlayerDataEntry>,
    val currentPlayer: PlayerId?,
    val players: List<PlayerId>,
    val isGameOver: Boolean = false
) : GameState() {
    // Helper function to get player data as a map (for ease of use in existing code)
    fun getPlayerDataMap(): Map<PlayerId, PlayerGameData> =
        playerData.associate { it.playerId to it.data }
}
