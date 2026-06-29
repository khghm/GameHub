package com.gamehub.host.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamehub.host.network.TournamentClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

data class TournamentUI(
    val id: String,
    val gameId: String,
    val name: String,
    val format: String,
    val maxParticipants: Int,
    val currentParticipants: Int,
    val entryFeeCoins: Long,
    val prizePoolCoins: Long,
    val status: String,
    val winnerId: String? = null,
    val bracketData: String? = null
)

class TournamentViewModel : ViewModel() {
    private val client = TournamentClient()
    private val _tournament = MutableStateFlow<TournamentUI?>(null)
    val tournament: StateFlow<TournamentUI?> = _tournament
    private val _tournaments = MutableStateFlow<List<TournamentUI>>(emptyList())
    val tournaments: StateFlow<List<TournamentUI>> = _tournaments
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    var playerName: String = ""

    init {
        connect()
    }

    fun connect() {
        viewModelScope.launch {
            try {
                val serverIp = com.gamehub.host.BuildConfig.SERVER_IP
                client.connect("ws://$serverIp:8080/tournament")
                _isConnected.value = true
                client.events.collect { event ->
                    val json = Json.parseToJsonElement(event).jsonObject
                    val type = json["type"]?.jsonPrimitive?.content
                    when (type) {
                        "list" -> {
                            val arr = json["tournaments"]?.jsonArray ?: return@collect
                            val list = arr.mapNotNull { parseTournamentFromJson(it.toString()) }
                            _tournaments.value = list
                        }
                        else -> {
                            val tournament = parseTournamentFromJson(event)
                            if (tournament != null) {
                                _tournament.value = tournament
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _isConnected.value = false
            }
        }
    }

    fun createTournament(
        gameType: String,
        name: String,
        format: String = "single_elimination",
        maxPlayers: Int = 8,
        entryFee: Long = 0L,
        prizeDistribution: Map<String, Double> = mapOf("1" to 1.0),
        allowedBands: List<String> = listOf("A", "B"),
        minLevel: Int = 0,
        minElo: Int = 0,
        registrationStart: Long = System.currentTimeMillis(),
        registrationEnd: Long = System.currentTimeMillis() + 86400000L,
        startTime: Long = System.currentTimeMillis() + 86400000L
    ) {
        val prizeDistJson = prizeDistribution.entries.joinToString(",", "{", "}") { "\"${it.key}\":${it.value}" }
        val bandsJson = allowedBands.joinToString(",", "[", "]") { "\"$it\"" }
        val msg = """{
            "action":"create",
            "gameType":"$gameType",
            "name":"$name",
            "format":"$format",
            "maxPlayers":$maxPlayers,
            "entryFee":$entryFee,
            "prizeDistribution":$prizeDistJson,
            "allowedBands":$bandsJson,
            "minLevel":$minLevel,
            "minElo":$minElo,
            "registrationStart":$registrationStart,
            "registrationEnd":$registrationEnd,
            "startTime":$startTime,
            "createdBy":"$playerName"
        }""".trimIndent()
        send(msg)
        listTournaments()
    }

    fun joinTournament(tournamentId: String) {
        send("""{"action":"join","tournamentId":"$tournamentId","playerName":"$playerName"}""")
    }

    fun listTournaments() {
        send("""{"action":"list"}""")
    }

    fun startTournament(tournamentId: String) {
        send("""{"action":"start","tournamentId":"$tournamentId"}""")
    }

    fun subscribe(tournamentId: String) {
        send("""{"action":"subscribe","tournamentId":"$tournamentId"}""")
    }

    fun unsubscribe() {
        send("""{"action":"unsubscribe"}""")
    }

    fun startMatch(tournamentId: String, matchId: String) {
        send("""{"action":"start_match","tournamentId":"$tournamentId","matchId":"$matchId"}""")
    }

    fun reportResult(tournamentId: String, matchId: String, winnerId: String) {
        send("""{"action":"report_result","tournamentId":"$tournamentId","matchId":"$matchId","winnerId":"$winnerId"}""")
    }

    fun getMyMatch(tournamentId: String, userId: String) {
        send("""{"action":"my_match","tournamentId":"$tournamentId","userId":"$userId"}""")
    }

    private fun send(msg: String) {
        viewModelScope.launch { client.send(msg) }
    }

    private fun parseTournamentFromJson(json: String): TournamentUI? {
        return try {
            val obj = Json.parseToJsonElement(json).jsonObject
            TournamentUI(
                id = obj["id"]?.jsonPrimitive?.content ?: "",
                gameId = obj["gameId"]?.jsonPrimitive?.content ?: "",
                name = obj["name"]?.jsonPrimitive?.content ?: "",
                format = obj["format"]?.jsonPrimitive?.content ?: "single_elimination",
                maxParticipants = obj["maxParticipants"]?.jsonPrimitive?.int ?: 0,
                currentParticipants = obj["currentParticipants"]?.jsonPrimitive?.int ?: 0,
                entryFeeCoins = obj["entryFeeCoins"]?.jsonPrimitive?.long ?: 0L,
                prizePoolCoins = obj["prizePoolCoins"]?.jsonPrimitive?.long ?: 0L,
                status = obj["status"]?.jsonPrimitive?.content ?: "waiting",
                winnerId = obj["winnerId"]?.jsonPrimitive?.contentOrNull,
                bracketData = obj["bracketData"]?.toString()
            )
        } catch (e: Exception) {
            null
        }
    }
}