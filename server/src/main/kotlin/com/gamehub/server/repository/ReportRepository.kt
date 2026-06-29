package com.gamehub.server.repository

import com.gamehub.server.domain.ReportsTable
import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import com.gamehub.shared.report.Report
import com.gamehub.shared.report.ReportDecision
import com.gamehub.shared.report.ReportStatus
import com.gamehub.shared.report.ViolationType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class ReportRepository {

    suspend fun createReport(
        reporterId: String,
        reportedUserId: String,
        type: String,
        reason: String,
        details: String?,
        evidenceUrl: String?,
        reporterScoreSnapshot: Int,
        violationType: String
    ): Long = dbQuery {
        ReportsTable.insert {
            it[this.reporterId] = reporterId
            it[this.reportedUserId] = reportedUserId
            it[this.type] = type
            it[this.reason] = reason
            it[this.details] = details
            it[this.evidenceUrl] = evidenceUrl
            it[this.reporterScoreSnapshot] = reporterScoreSnapshot
            it[this.status] = ReportStatus.pending.name
            it[this.violationType] = violationType
            it[this.createdAt] = OffsetDateTime.now()
        }.resultedValues?.single()?.get(ReportsTable.id) ?: throw IllegalStateException("Failed to create report")
    }

    suspend fun getReports(
        status: String?,
        violationType: String?,
        fromDate: Long?,
        toDate: Long?,
        reporterId: String?,
        reportedUserId: String?,
        minReporterScore: Int?,
        offset: Int,
        limit: Int
    ): List<Report> = dbQuery {
        var query = ReportsTable.selectAll()
        status?.let { query = query.where { ReportsTable.status eq it } }
        violationType?.let { query = query.where { ReportsTable.violationType eq it } }
        fromDate?.let {
            val instant = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toOffsetDateTime()
            query = query.where { ReportsTable.createdAt greaterEq instant }
        }
        toDate?.let {
            val instant = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toOffsetDateTime()
            query = query.where { ReportsTable.createdAt lessEq instant }
        }
        reporterId?.let { query = query.where { ReportsTable.reporterId eq it } }
        reportedUserId?.let { query = query.where { ReportsTable.reportedUserId eq it } }
        minReporterScore?.let { query = query.where { ReportsTable.reporterScoreSnapshot greaterEq minReporterScore } }
        query
            .orderBy(ReportsTable.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { rowToReport(it) }
    }

    suspend fun getReportsCount(
        status: String?,
        violationType: String?,
        fromDate: Long?,
        toDate: Long?,
        reporterId: String?,
        reportedUserId: String?,
        minReporterScore: Int?
    ): Int = dbQuery {
        var query = ReportsTable.selectAll()
        status?.let { query = query.where { ReportsTable.status eq it } }
        violationType?.let { query = query.where { ReportsTable.violationType eq it } }
        fromDate?.let {
            val instant = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toOffsetDateTime()
            query = query.where { ReportsTable.createdAt greaterEq instant }
        }
        toDate?.let {
            val instant = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toOffsetDateTime()
            query = query.where { ReportsTable.createdAt lessEq instant }
        }
        reporterId?.let { query = query.where { ReportsTable.reporterId eq it } }
        reportedUserId?.let { query = query.where { ReportsTable.reportedUserId eq it } }
        minReporterScore?.let { query = query.where { ReportsTable.reporterScoreSnapshot greaterEq minReporterScore } }
        query.count().toInt()
    }

    suspend fun updateReportStatus(reportId: Long, status: ReportStatus, decision: String, moderatorId: String): Boolean = dbQuery {
        val updatedRows = ReportsTable.update({ ReportsTable.id eq reportId }) {
            it[this.status] = status.name
            it[this.decision] = decision
            it[this.moderatorId] = moderatorId
            it[this.reviewedAt] = OffsetDateTime.now()
        }
        updatedRows > 0
    }

    private fun rowToReport(row: ResultRow): Report = Report(
        id = row[ReportsTable.id],
        reporterId = row[ReportsTable.reporterId],
        reportedUserId = row[ReportsTable.reportedUserId],
        type = row[ReportsTable.type],
        reason = row[ReportsTable.reason],
        details = row[ReportsTable.details],
        evidenceUrl = row[ReportsTable.evidenceUrl],
        reporterScoreSnapshot = row[ReportsTable.reporterScoreSnapshot],
        status = ReportStatus.valueOf(row[ReportsTable.status]),
        violationType = ViolationType.valueOf(row[ReportsTable.violationType]),
        decision = try { ReportDecision.valueOf(row[ReportsTable.decision] ?: "none") } catch (e: Exception) { ReportDecision.none },
        moderatorId = row[ReportsTable.moderatorId],
        createdAt = row[ReportsTable.createdAt].toInstant().toEpochMilli(),
        reviewedAt = row[ReportsTable.reviewedAt]?.toInstant()?.toEpochMilli()
    )
}