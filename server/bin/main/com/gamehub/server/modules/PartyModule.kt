package com.gamehub.server.modules

import com.gamehub.server.cache.PresenceCache
import com.gamehub.server.repository.PartyRepository
import com.gamehub.shared.cache.CacheProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class PartyMember(
    val userId: String,
    val username: String,
    @Transient var isOnline: Boolean = false
)

@Serializable
data class Party(
    val id: String = UUID.randomUUID().toString(),
    var leaderId: String,
    val members: MutableList<PartyMember> = mutableListOf(),
    var state: String = "idle",
    val maxMembers: Int = 4, // Default max members to 4 (Ludo example)
    val createdAt: Long = System.currentTimeMillis()
)

class PartyModule(private val cache: CacheProvider, private val partyRepository: PartyRepository) {
    private val json = Json { ignoreUnknownKeys = true }

    // استفاده از runBlocking موقتاً برای سازگاری با کد موجود؛
    // در آینده تمام Handlerها باید suspend باشند。
    fun createParty(leaderId: String, leaderName: String): Party = runBlocking {
        val party = Party(leaderId = leaderId, members = mutableListOf(PartyMember(leaderId, leaderName, PresenceCache.isUserOnline(leaderId))))
        partyRepository.createParty(party) // Save to DB
        saveParty(party) // Save to Cache
        party
    }

    fun getParty(partyId: String): Party? = runBlocking {
        // Try to get from cache first
        var party = cache.get("party:$partyId")?.let { json.decodeFromString<Party>(it) }

        if (party == null) {
            // If not in cache or deserialization fails, get from DB
            party = partyRepository.getParty(partyId)
        }

        party?.members?.forEach { it.isOnline = PresenceCache.isUserOnline(it.userId) }
        if (party != null) saveParty(party) // Cache the party after retrieving from DB
        party
    }

    fun addMember(partyId: String, userId: String, userName: String): Boolean = runBlocking {
        val party = getParty(partyId) ?: return@runBlocking false
        if (party.members.size >= party.maxMembers) return@runBlocking false
        if (party.members.any { it.userId == userId }) return@runBlocking false

        val member = PartyMember(userId, userName, PresenceCache.isUserOnline(userId))
        partyRepository.addMember(partyId, PartyMember(userId, userName)) // Add to DB (without isOnline)
        party.members.add(member)
        saveParty(party) // Update Cache
        true
    }

    fun removeMember(partyId: String, userId: String): Boolean = runBlocking {
        val party = getParty(partyId) ?: return@runBlocking false
        val memberToRemove = party.members.find { it.userId == userId } ?: return@runBlocking false

        partyRepository.removeMember(partyId, userId) // Remove from DB
        party.members.remove(memberToRemove)

        if (party.members.isEmpty()) {
            partyRepository.deleteParty(partyId) // Delete from DB
            cache.delete("party:$partyId") // Delete from Cache
            return@runBlocking true
        }

        if (party.leaderId == userId) {
            party.leaderId = party.members.first().userId
            partyRepository.updatePartyLeader(partyId, party.leaderId) // Update leader in DB
        }
        saveParty(party) // Update Cache
        true
    }

    fun setState(partyId: String, state: String) = runBlocking {
        val party = getParty(partyId) ?: return@runBlocking
        party.state = state
        partyRepository.updatePartyState(partyId, state) // Update DB
        saveParty(party) // Update Cache
    }

    private suspend fun saveParty(party: Party) {
        cache.set("party:${party.id}", json.encodeToString(party), 3600)
    }

    // Helper function to prepare Party object for client (populating transient fields)
    suspend fun toClientParty(party: Party): Party {
        val clientMembers = party.members.map { member ->
            // Create a copy of PartyMember and set isOnline dynamically
            member.copy(isOnline = PresenceCache.isUserOnline(member.userId))
        }.toMutableList()
        return party.copy(members = clientMembers)
    }
}