package com.gamehub.host.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamehub.host.network.PartyClient
import com.gamehub.host.network.PartyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PartyMemberUI(
    val userId: String,
    val username: String,
    val isOnline: Boolean
)

data class PartyUI(
    val id: String = "",
    val leaderId: String = "",
    val members: List<PartyMemberUI> = emptyList(),
    val state: String = ""
)

class PartyViewModel : ViewModel() {
    private val client = PartyClient()
    private val _party = MutableStateFlow<PartyUI?>(null)
    val party: StateFlow<PartyUI?> = _party
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    var currentUserId: String = ""
    var authToken: String = ""

    init {
        // Connect to hub for party messages
        connectHub()
    }

    fun connectHub() {
        if (authToken.isEmpty()) return
        viewModelScope.launch {
            try {
                val serverIp = com.gamehub.host.BuildConfig.SERVER_IP
                client.connect("ws://$serverIp:8080/ws/hub?token=$authToken")
                client.events.collect { event ->
                    when (event) {
                        is PartyEvent.Connected -> {
                            _isLoading.value = false
                        }
                        is PartyEvent.PartyCreated -> {
                            _party.value = PartyUI(
                                id = event.partyId,
                                leaderId = event.leaderId,
                                members = event.members
                            )
                            _isLoading.value = false
                        }
                        is PartyEvent.PartyUpdated -> {
                            _party.value = _party.value?.copy(
                                members = event.members,
                                state = event.state
                            )
                        }
                        is PartyEvent.PartyDeleted -> {
                            _party.value = null
                        }
                        is PartyEvent.Error -> {
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun createParty() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                client.send("""{"type":"party.create"}""")
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun inviteToParty(username: String) {
        viewModelScope.launch {
            // Need to get userId by username first (implement a lookup), for now skip
        }
    }

    fun kickMember(memberId: String) {
        // TODO: implement party.kick via server
    }

    fun leaveParty() {
        viewModelScope.launch {
            val partyId = _party.value?.id ?: return@launch
            client.send("""{"type":"party.leave","partyId":"$partyId"}""")
            _party.value = null
        }
    }

    fun startGame(gameType: String = "ludo") {
        viewModelScope.launch {
            val partyId = _party.value?.id ?: return@launch
            client.send("""{"type":"party.start","partyId":"$partyId","gameType":"$gameType"}""")
        }
    }
}