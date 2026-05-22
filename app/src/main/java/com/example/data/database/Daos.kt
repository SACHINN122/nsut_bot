package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Query("SELECT * FROM student_profiles WHERE rollNo = :rollNo")
    fun getStudentProfile(rollNo: String): Flow<StudentProfileEntity?>

    @Query("SELECT * FROM student_profiles WHERE rollNo = :rollNo")
    suspend fun getStudentProfileOneShot(rollNo: String): StudentProfileEntity?

    @Query("SELECT * FROM student_profiles LIMIT 1")
    suspend fun getLastLoggedInStudent(): StudentProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentProfile(student: StudentProfileEntity)

    @Query("SELECT * FROM subject_attendance WHERE rollNo = :rollNo")
    fun getSubjectAttendance(rollNo: String): Flow<List<SubjectAttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjectAttendance(attendance: List<SubjectAttendanceEntity>)

    @Query("DELETE FROM subject_attendance WHERE rollNo = :rollNo")
    suspend fun deleteSubjectAttendanceForStudent(rollNo: String)

    @Query("DELETE FROM student_profiles WHERE rollNo = :rollNo")
    suspend fun deleteStudentProfile(rollNo: String)
}
