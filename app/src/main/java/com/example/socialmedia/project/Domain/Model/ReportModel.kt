package com.example.socialmedia.project.Domain.Model

import com.example.socialmedia.project.Domain.Enum.ReportReason
import com.example.socialmedia.project.Domain.Enum.ReportStatus
import com.example.socialmedia.project.Domain.Enum.ReportedType
import java.util.UUID

data class ReportModel(
    val reportId: String = UUID.randomUUID().toString(),
    val reporterId: String = "",
    val reportedType: ReportedType = ReportedType.POST,
    val reportedId: String = "",
    val reason: ReportReason = ReportReason.SPAM,
    val description: String? = null,
    val status: ReportStatus = ReportStatus.PENDING,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
