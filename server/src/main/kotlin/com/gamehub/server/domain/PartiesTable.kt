package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object PartiesTable : Table("parties") {
    val id = varchar("id", 36).uniqueIndex()
    val leaderId = uuid("leader_id")
    val state = varchar("state", 20).default("idle")
    val createdAt = timestampWithTimeZone("created_at")
    val maxMembers = integer("max_members").default(4)

    override val primaryKey = PrimaryKey(id)
}

object PartyMembersTable : Table("party_members") {
    val partyId = varchar("party_id", 36).references(PartiesTable.id)
    val userId = uuid("user_id").references(UsersTable.id)
    val username = varchar("username", 30)

    override val primaryKey = PrimaryKey(partyId, userId)
}