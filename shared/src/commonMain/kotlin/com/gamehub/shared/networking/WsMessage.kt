// shared/src/commonMain/kotlin/com/gamehub/shared/networking/WsMessage.kt
package com.gamehub.shared.networking

import com.gamehub.shared.core.GameMode
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.util.UUID

@Serializable
sealed class WsMessage {
    abstract val msgId: String?
    abstract val type: String
}

// ---------- Client → Server ----------

@Serializable
@SerialName("matchmaking_request")
data class MatchmakingRequestMsg(
    override val msgId: String? = null,
    val request: MatchmakingRequest,
    val nonce: String = UUID.randomUUID().toString(),   // جدید
    val timestamp: Long = System.currentTimeMillis()    // جدید
) : WsMessage() {
    override val type: String = "matchmaking_request"
}

@Serializable
sealed class MatchmakingRequest

@Serializable
@SerialName("solo")
data class SoloRequest(
    val userId: String,
    val gameId: String,
    val mode: GameMode
) : MatchmakingRequest()

@Serializable
@SerialName("party")
data class PartyRequest(
    val partyId: String,
    val leaderId: String,
    val members: List<String>,
    val gameId: String,
    val mode: GameMode
) : MatchmakingRequest()

@Serializable
@SerialName("submit_move")
data class SubmitMoveMsg(
    override val msgId: String? = null,
    val gameId: String,
    val movePayload: JsonElement,
    val clientMoveId: String? = null,
    val nonce: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val reactionMs: Long? = null
) : WsMessage() {
    override val type: String = "submit_move"
}

@Serializable
@SerialName("resume_game")
data class ResumeGameMsg(
    override val msgId: String? = null,
    val gameId: String,
    val lastProcessedEventId: Long? = null,
    val reconnectToken: String? = null
) : WsMessage() {
    override val type: String = "resume_game"
}

@Serializable
@SerialName("surrender")
data class SurrenderMsg(
    override val msgId: String? = null,
    val gameId: String
) : WsMessage() {
    override val type: String = "surrender"
}

@Serializable
@SerialName("chat_message")
data class ChatMessageMsg(
    override val msgId: String? = null,
    val channelType: String,
    val channelId: String,
    val senderId: String,
    val content: ChatContent,
    val nonce: String = UUID.randomUUID().toString(),   // جدید
    val timestamp: Long = System.currentTimeMillis()    // جدید
) : WsMessage() {
    override val type: String = "chat_message"
}

@Serializable
data class ChatContent(
    val type: String,
    val body: String,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
@SerialName("time_sync_req")
data class TimeSyncRequest(
    override val msgId: String? = null,
    val clientSendTime: Long,
    val nonce: Long
) : WsMessage() {
    override val type: String = "time_sync_req"
}

@Serializable
@SerialName("time_sync_poll_resp")
data class TimeSyncPollResp(
    override val msgId: String? = null,
    val serverPollSendTime: Long,
    val clientRecvTime: Long,
    val clientSendTime: Long,
    val nonce: Long
) : WsMessage() {
    override val type: String = "time_sync_poll_resp"
}

// ---------- Server → Client ----------

@Serializable
@SerialName("game_event")
data class GameEventMsg(
    override val msgId: String? = null,
    val gameId: String,
    val eventType: String,
    val eventData: JsonElement
) : WsMessage() {
    override val type: String = "game_event"
}

@Serializable
@SerialName("move_result")
data class MoveResultMsg(
    override val msgId: String? = null,
    val gameId: String,
    val pieceId: String? = null,
    val playerId: String,
    val from: JsonElement? = null,
    val to: JsonElement? = null,
    val capturedPieceId: String? = null,
    val moveSpecificData: JsonElement? = null,
    val clientMoveId: String? = null,
    val accepted: Boolean = true
) : WsMessage() {
    override val type: String = "move_result"
}

@Serializable
@SerialName("game_state_update")
data class GameStateUpdateMsg(
    override val msgId: String? = null,
    val gameId: String,
    val statePayload: JsonElement,
    val currentTurnPlayerId: String? = null,
    val turnPhase: String? = null,
    val timers: Map<String, Int> = emptyMap(),
    val playerStatuses: Map<String, String> = emptyMap()
) : WsMessage() {
    override val type: String = "game_state_update"
}

@Serializable
@SerialName("game_over")
data class GameOverMsg(
    override val msgId: String? = null,
    val gameId: String,
    val winnerId: String? = null,
    val results: Map<String, String> = emptyMap()
) : WsMessage() {
    override val type: String = "game_over"
}

@Serializable
@SerialName("match_proposal")
data class MatchProposalMsg(
    override val msgId: String? = null,
    val gameId: String,
    val mode: GameMode,
    val players: List<String>,
    val timeoutSeconds: Int = 10
) : WsMessage() {
    override val type: String = "match_proposal"
}

@Serializable
@SerialName("presence_update")
data class PresenceUpdateMsg(
    override val msgId: String? = null,
    val userId: String,
    val status: String,
    val gameId: String? = null
) : WsMessage() {
    override val type: String = "presence_update"
}

@Serializable
@SerialName("time_sync_resp")
data class TimeSyncResponse(
    override val msgId: String? = null,
    val clientSendTime: Long,
    val serverRecvTime: Long,
    val serverSendTime: Long,
    val nonce: Long,
    val signature: String
) : WsMessage() {
    override val type: String = "time_sync_resp"
}

@Serializable
@SerialName("time_sync_poll")
data class TimeSyncPoll(
    override val msgId: String? = null,
    val serverPollSendTime: Long,
    val nonce: Long
) : WsMessage() {
    override val type: String = "time_sync_poll"
}

@Serializable
@SerialName("error")
data class ErrorMsg(
    override val msgId: String? = null,
    val code: String,
    val description: String,
    val category: String = "system"
) : WsMessage() {
    override val type: String = "error"
}