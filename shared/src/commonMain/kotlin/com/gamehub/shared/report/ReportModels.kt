package com.gamehub.shared.report

import kotlinx.serialization.Serializable

@Serializable
enum class ViolationType {
    harassment,
    cheating,
    inappropriate_content,
    spam,
    offensive_username,
    other
}

@Serializable
enum class ReportStatus {
    pending,
    reviewed,
    auto_closed,
    appealed
}

@Serializable
enum class ReportDecision {
    none,
    warning,
    mute,
    temp_ban,
    perma_ban
}

@Serializable
data class Report(
    val id: Long,
    val reporterId: String,
    val reportedUserId: String,
    val type: String,                // "user", "chat", "game"
    val reason: String,
    val details: String?,
    val evidenceUrl: String?,
    val reporterScoreSnapshot: Int,
    val status: ReportStatus,
    val violationType: ViolationType,
    val decision: ReportDecision,
    val moderatorId: String?,
    val createdAt: Long,
    val reviewedAt: Long?
)

@Serializable
data class ReportAction(
    val id: Long,
    val reportId: Long,
    val adminId: String,
    val action: String,
    val details: String?,
    val createdAt: Long
)

@Serializable
data class ReportFilter(
    val status: ReportStatus? = null,
    val violationType: ViolationType? = null,
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val reporterId: String? = null,
    val reportedUserId: String? = null,
    val minReporterScore: Int? = null,
    val page: Int = 1,
    val pageSize: Int = 20
)

@Serializable
data class ReportActionRequest(
    val reportId: Long,
    val decision: ReportDecision,
    val reason: String,
    val durationHours: Int? = null  // برای بن موقت
)