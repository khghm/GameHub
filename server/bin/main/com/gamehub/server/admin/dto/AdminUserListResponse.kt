// server/src/main/kotlin/com/gamehub/server/admin/dto/AdminUserListResponse.kt
package com.gamehub.server.admin.dto

import com.gamehub.server.repository.UserDto
import kotlinx.serialization.Serializable

@Serializable
data class AdminUserListResponse(
    val users: List<UserDto>,
    val total: Int
)