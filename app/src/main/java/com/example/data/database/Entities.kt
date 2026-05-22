package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_profiles")
data class StudentProfileEntity(
    @PrimaryKey val rollNo: String,
    val name: String,
    val department: String,
    val degree: String,
    val semester: String,
    val password: String,
    val photoUrl: String = ""
)

@Entity(tableName = "subject_attendance")
data class SubjectAttendanceEntity(
    @PrimaryKey val id: String,
    val rollNo: String,
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
    val absentDates: String
)
