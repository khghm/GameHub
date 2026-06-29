package com.gamehub.server.admin

import com.gamehub.server.behavior.BehaviorService
import com.gamehub.server.repository.ReportRepository
import com.gamehub.server.repository.UserRepository
import com.gamehub.shared.report.ReportDecision
import com.gamehub.shared.report.ReportStatus
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.UUID

class ReportService(
    private val reportRepository: ReportRepository,
    private val behaviorService: BehaviorService,
    private val userRepository: UserRepository
) {

    suspend fun applyDecision(
        reportId: Long,
        decision: ReportDecision,
        reason: String,
        moderatorId: String,
        durationHours: Int? = null
    ): Boolean {
        // 1. دریافت گزارش
        val reports = reportRepository.getReports(null, null, null, null, null, null, null, 0, 1)
        val report = reports.firstOrNull { it.id == reportId } ?: return false

        // 2. اعمال جریمه بر اساس تصمیم
        when (decision) {
            ReportDecision.warning -> {
                // فقط لاگ در رفتار (با یک نوع جریمه سبک)
                behaviorService.applyPenalty(report.reportedUserId, "report_warning", reportId.toString())
            }
            ReportDecision.mute -> {
                // میوت چت به مدت ۲۴ ساعت (می‌توان قابل تنظیم کرد)
                // در سیستم رفتاری یک جریمه ثبت می‌کنیم
                behaviorService.applyPenalty(report.reportedUserId, "report_mute", reportId.toString())
                // همچنین می‌توان در Redis یک کلید برای میوت ذخیره کرد
            }
            ReportDecision.temp_ban -> {
                // بن موقت
                val expiresAt = if (durationHours != null) System.currentTimeMillis() + durationHours * 3600_000L else null
                // ذخیره در Redis و دیتابیس
                behaviorService.applyPenalty(report.reportedUserId, "report_temp_ban", reportId.toString())
            }
            ReportDecision.perma_ban -> {
                // بن دائم
                behaviorService.applyPenalty(report.reportedUserId, "report_perma_ban", reportId.toString())
            }
            else -> {} // none یا ignore
        }

        // 3. به‌روزرسانی وضعیت گزارش
        val status = if (decision == ReportDecision.none) ReportStatus.auto_closed else ReportStatus.reviewed
        val decisionStr = decision.name
        val success = reportRepository.updateReportStatus(reportId, status, decisionStr, moderatorId)

        // 4. (اختیاری) به‌روزرسانی امتیاز گزارش‌دهنده: اگر گزارش معتبر بود، +۲، اگر نامعتبر بود -۱
        // این کار باید توسط یک job جداگانه یا در زمان تصمیم انجام شود. فعلاً فقط لاگ می‌کنیم.

        return success
    }
}