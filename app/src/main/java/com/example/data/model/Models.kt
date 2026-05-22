package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StudentProfile(
    val name: String,
    val rollNo: String,
    val department: String,
    val degree: String,
    val semester: String,
    val photoUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class SubjectAttendance(
    val subjectName: String,
    val subjectCode: String,
    val semester: String,
    val attended: Int,
    val total: Int,
    val absent: Int,
    val percentage: Double,
    val skippable75: Int,
    val needed75: Int,
    val skippable65: Int,
    val needed65: Int,
    val absentDates: List<String>
)

@JsonClass(generateAdapter = true)
data class AttendanceInsights(
    val overallPercentage: Double,
    val totalClasses: Int,
    val totalAttended: Int,
    val totalAbsent: Int,
    val totalSkippable75: Int
)

enum class Sender {
    USER, BOT
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: Sender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SyncStepInfo(
    val id: Int,
    val label: String,
    val durationMs: Long
)

enum class AttendanceFilterStatus {
    ALL, SAFE, BORDERLINE, SHORTAGE
}

data class DashboardFilters(
    val semester: String = "All",
    val status: AttendanceFilterStatus = AttendanceFilterStatus.ALL,
    val searchQuery: String = ""
)
