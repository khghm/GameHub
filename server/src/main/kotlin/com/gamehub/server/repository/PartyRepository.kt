package com.gamehub.server.repository

import com.gamehub.server.domain.PartiesTable
import com.gamehub.server.domain.PartyMembersTable
import com.gamehub.server.domain.UsersTable
import com.gamehub.server.modules.Party
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.util.UUID

data class PartyMember( // This is the repository's internal PartyMember
    val userId: String,
    val username: String
)

class PartyRepository {

    fun createParty(party: Party): Party = transaction {
        PartiesTable.insert {
            it[id] = party.id
            it[leaderId] = UUID.fromString(party.leaderId)
            it[state] = party.state
            it[createdAt] = OffsetDateTime.now()
            it[maxMembers] = party.maxMembers
        }

        party.members.forEach { member ->
            PartyMembersTable.insert {
                it[partyId] = party.id
                it[userId] = UUID.fromString(member.userId)
                it[username] = member.username
            }
        }
        party
    }

    fun getParty(partyId: String): Party? = transaction {
        val partyRow = PartiesTable.selectAll().where { PartiesTable.id eq partyId }.singleOrNull() ?: return@transaction null

        val members = PartyMembersTable.selectAll().where { PartyMembersTable.partyId eq partyId }.map {
            com.gamehub.server.modules.PartyMember( // Use PartyModule's PartyMember
                userId = it[PartyMembersTable.userId].toString(),
                username = it[PartyMembersTable.username]
            )
        }.toMutableList()

        partyRow.toParty(members)
    }

    fun addMember(partyId: String, member: com.gamehub.server.modules.PartyMember): Boolean = transaction {
        // member is PartyModule.PartyMember, but we only store userId and username
        val party = getParty(partyId) ?: return@transaction false
        if (party.members.size >= party.maxMembers) return@transaction false

        val insertedRows = PartyMembersTable.insert {
            it[PartyMembersTable.partyId] = partyId
            it[PartyMembersTable.userId] = UUID.fromString(member.userId)
            it[PartyMembersTable.username] = member.username
        }.insertedCount

        insertedRows > 0
    }

    fun removeMember(partyId: String, userId: String): Boolean = transaction {
        val deletedRows = PartyMembersTable.deleteWhere {
            (PartyMembersTable.partyId eq partyId) and (PartyMembersTable.userId eq UUID.fromString(userId))
        }
        deletedRows > 0
    }

    fun updatePartyState(partyId: String, state: String): Boolean = transaction {
        val updatedRows = PartiesTable.update({ PartiesTable.id eq partyId }) {
            it[PartiesTable.state] = state
        }
        updatedRows > 0
    }

    fun updatePartyLeader(partyId: String, newLeaderId: String): Boolean = transaction {
        val updatedRows = PartiesTable.update({ PartiesTable.id eq partyId }) {
            it[PartiesTable.leaderId] = UUID.fromString(newLeaderId)
        }
        updatedRows > 0
    }

    fun deleteParty(partyId: String): Boolean = transaction {
        PartyMembersTable.deleteWhere { PartyMembersTable.partyId eq partyId }
        val deletedRows = PartiesTable.deleteWhere { PartiesTable.id eq partyId }
        deletedRows > 0
    }

    private fun ResultRow.toParty(members: MutableList<com.gamehub.server.modules.PartyMember>): Party {
        return Party(
            id = this[PartiesTable.id],
            leaderId = this[PartiesTable.leaderId].toString(),
            members = members,
            state = this[PartiesTable.state],
            maxMembers = this[PartiesTable.maxMembers],
            createdAt = this[PartiesTable.createdAt].toInstant().toEpochMilli()
        )
    }

    fun findPartyByUserId(userId: String): Party? = transaction {
        PartyMembersTable.selectAll().where { PartyMembersTable.userId eq UUID.fromString(userId) }.singleOrNull()?.let {
            getParty(it[PartyMembersTable.partyId])
        }
    }
}
