package com.gamehub.server.admin

import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object AlertRulesTable : Table("alert_rules") {
    val id = long("ID").autoIncrement()
    val name = varchar("NAME", 100)
    val metric = varchar("METRIC", 100)
    val metricFilter = varchar("METRIC_FILTER", 255).nullable()
    val condition = varchar("CONDITION", 10)
    val threshold = double("THRESHOLD")
    val durationSeconds = integer("DURATION_SECONDS").default(60)
    val channels = varchar("CHANNELS", 255)
    val severity = varchar("SEVERITY", 20).default("warning")
    val enabled = bool("ENABLED").default(true)
    val createdAt = timestamp("CREATED_AT")
    val updatedAt = timestamp("UPDATED_AT")
    override val primaryKey = PrimaryKey(id)
}

object AlertHistoryTable : Table("alert_history") {
    val id = long("ID").autoIncrement()
    val ruleId = long("RULE_ID")
    val metricValue = double("METRIC_VALUE")
    val message = text("MESSAGE")
    val channelsSent = varchar("CHANNELS_SENT", 255)
    val createdAt = timestamp("CREATED_AT")
    override val primaryKey = PrimaryKey(id)
}

// بقیه کلاس AlertService بدون تغییر باقی می‌ماند (فقط نام جدول‌ها تغییر کرده)
class AlertService(
    private val metricsService: MetricsService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lastAlertCache = mutableMapOf<Long, Long>()

    fun start() {
        scope.launch {
            while (true) {
                delay(30_000)
                checkAllRules()
            }
        }
    }

    private suspend fun checkAllRules() {
        val rules = dbQuery {
            AlertRulesTable.select { AlertRulesTable.enabled eq true }.map { rowToRule(it) }
        }
        val metrics = metricsService.getCurrentMetrics()

        for (rule in rules) {
            val currentValue = getMetricValue(metrics, rule.metric, rule.metricFilter) ?: continue
            if (isConditionMet(currentValue, rule.condition, rule.threshold)) {
                val lastAlert = lastAlertCache[rule.id] ?: 0
                if (System.currentTimeMillis() - lastAlert > rule.durationSeconds * 1000L) {
                    sendAlert(rule, currentValue, metrics.timestamp)
                    lastAlertCache[rule.id] = System.currentTimeMillis()
                    dbQuery {
                        AlertHistoryTable.insert {
                            it[AlertHistoryTable.ruleId] = rule.id
                            it[AlertHistoryTable.metricValue] = currentValue
                            it[AlertHistoryTable.message] = "Rule '${rule.name}' triggered: ${rule.metric}=$currentValue"
                            it[AlertHistoryTable.channelsSent] = rule.channels
                            it[AlertHistoryTable.createdAt] = Instant.now()
                        }
                    }
                }
            }
        }
    }

    private fun getMetricValue(metrics: MetricsSnapshot, metricName: String, filter: String?): Double? {
        return when (metricName) {
            "onlineHubUsers" -> metrics.onlineHubUsers.toDouble()
            "inGameUsers" -> metrics.inGameUsers.toDouble()
            "activeGames" -> metrics.activeGames.toDouble()
            "matchmaking_queue_size" -> {
                if (filter != null) {
                    val parts = filter.split(":")
                    val gameId = parts.getOrNull(1)
                    val mode = parts.getOrNull(3)
                    val key = if (gameId != null && mode != null) "$gameId:$mode" else null
                    key?.let { metrics.queueSizes[it]?.toDouble() }
                } else {
                    metrics.queueSizes.values.sum().toDouble()
                }
            }
            else -> null
        }
    }

    private fun isConditionMet(value: Double, condition: String, threshold: Double): Boolean {
        return when (condition) {
            ">" -> value > threshold
            ">=" -> value >= threshold
            "<" -> value < threshold
            "<=" -> value <= threshold
            "==" -> value == threshold
            else -> false
        }
    }

    private suspend fun sendAlert(rule: AlertRule, value: Double, timestamp: Long) {
        val message = "⚠️ Alert: ${rule.name}\nMetric: ${rule.metric} = $value (threshold: ${rule.condition} ${rule.threshold})\nTime: ${Instant.ofEpochMilli(timestamp)}"
        println("📢 ALERT: $message")
    }

    private fun rowToRule(row: ResultRow): AlertRule = AlertRule(
        id = row[AlertRulesTable.id],
        name = row[AlertRulesTable.name],
        metric = row[AlertRulesTable.metric],
        metricFilter = row[AlertRulesTable.metricFilter],
        condition = row[AlertRulesTable.condition],
        threshold = row[AlertRulesTable.threshold],
        durationSeconds = row[AlertRulesTable.durationSeconds],
        channels = row[AlertRulesTable.channels],
        severity = row[AlertRulesTable.severity],
        enabled = row[AlertRulesTable.enabled]
    )
}

data class AlertRule(
    val id: Long,
    val name: String,
    val metric: String,
    val metricFilter: String?,
    val condition: String,
    val threshold: Double,
    val durationSeconds: Int,
    val channels: String,
    val severity: String,
    val enabled: Boolean
)
@Serializable
data class AlertRuleCreateRequest(
    val name: String,
    val metric: String,
    val metricFilter: String?,
    val condition: String,
    val threshold: Double,
    val durationSeconds: Int,
    val channels: String,
    val severity: String,
    val enabled: Boolean
)

@Serializable
data class AlertRuleUpdateRequest(
    val name: String,
    val metric: String,
    val metricFilter: String?,
    val condition: String,
    val threshold: Double,
    val durationSeconds: Int,
    val channels: String,
    val severity: String,
    val enabled: Boolean
)
