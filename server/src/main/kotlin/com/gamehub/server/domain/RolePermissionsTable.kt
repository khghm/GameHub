package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table

object RolePermissionsTable : Table("role_permissions") {
    val roleId = long("role_id")
    val permissionId = long("permission_id")
    override val primaryKey = PrimaryKey(roleId, permissionId)
}