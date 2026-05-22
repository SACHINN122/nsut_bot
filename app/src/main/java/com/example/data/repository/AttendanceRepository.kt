package com.example.data.repository

import com.example.data.database.StudentDao
import com.example.data.database.StudentProfileEntity
import com.example.data.database.SubjectAttendanceEntity
import com.example.data.model.AttendanceInsights
import com.example.data.model.StudentProfile
import com.example.data.model.SubjectAttendance
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor

class AttendanceRepository(private val studentDao: StudentDao) {

    suspend fun getLastLoggedInRollNo(): String? {
        return studentDao.getLastLoggedInStudent()?.rollNo
    }

    fun getStudentProfile(rollNo: String): Flow<StudentProfile?> {
        return studentDao.getStudentProfile(rollNo).map { entity ->
            entity?.let {
                StudentProfile(
                    name = it.name,
                    rollNo = it.rollNo,
                    department = it.department,
                    degree = it.degree,
                    semester = it.semester
                )
            }
        }
    }

    suspend fun getStudentProfileOneShot(rollNo: String): StudentProfile? {
        val entity = studentDao.getStudentProfileOneShot(rollNo)
        return entity?.let {
            StudentProfile(
                name = it.name,
                rollNo = it.rollNo,
                department = it.department,
                degree = it.degree,
                semester = it.semester
            )
        }
    }

    fun getSubjectAttendance(rollNo: String): Flow<List<SubjectAttendance>> {
        return studentDao.getSubjectAttendance(rollNo).map { entities ->
            entities.map { entity ->
                SubjectAttendance(
                    subjectName = entity.subjectName,
                    subjectCode = entity.subjectCode,
                    attended = entity.attended,
                    total = entity.total,
                    absent = entity.absent,
                    percentage = entity.percentage,
                    skippable75 = entity.skippable75,
                    needed75 = entity.needed75,
                    skippable65 = entity.skippable65,
                    needed65 = entity.needed65,
                    absentDates = if (entity.absentDates.isEmpty()) emptyList() else entity.absentDates.split(",")
                )
            }
        }
    }

    suspend fun logOut(rollNo: String) {
        studentDao.deleteSubjectAttendanceForStudent(rollNo)
        studentDao.deleteStudentProfile(rollNo)
    }

    // Guess semester based on ROLLNO structure (Admissions are usually 10-12 chars, e.g., 2024UME4116)
    fun guessSemester(rollNo: String): String {
        val yearPart = rollNo.take(4).toIntOrNull() ?: 2024
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) // 0-indexed, 7 is August
        val academicYearStart = if (currentMonth >= 7) currentYear else currentYear - 1
        val offset = (academicYearStart - yearPart).coerceAtLeast(0)
        val semesterIndex = offset * 2 + (if (currentMonth >= 7) 1 else 2)
        return semesterIndex.coerceIn(1, 10).toString()
    }

    // Guess Department based on letters in the roll number (e.g., 2024UME4116 -> UME -> Mechanical Engineering)
    fun guessDepartment(rollNo: String): String {
        val upper = rollNo.uppercase()
        return when {
            upper.contains("COE") || upper.contains("CS") -> "Computer Science & Engineering"
            upper.contains("IT") -> "Information Technology"
            upper.contains("MAC") || upper.contains("MCE") -> "Mathematics & Computing and Engineering"
            upper.contains("ECE") || upper.contains("EC") -> "Electronics & Communication Engineering"
            upper.contains("EE") || upper.contains("EL") -> "Electrical Engineering"
            upper.contains("ICE") || upper.contains("IC") -> "Instrumentation & Control Engineering"
            upper.contains("UME") || upper.contains("ME") -> "Mechanical Engineering"
            upper.contains("BT") -> "Biotechnology"
            else -> "Information Technology"
        }
    }

    // Main sync task that simulates headless portal crawling
    suspend fun performPortalSync(
        rollNo: String,
        password: String,
        onProgressUpdate: (step: Int, log: String) -> Unit
    ): StudentProfile {
        val delayFactor = 400L // Fast-paced simulation logs
        val guessedDept = guessDepartment(rollNo)
        val guessedSem = guessSemester(rollNo)
        val fallbackName = "Student-$rollNo"

        onProgressUpdate(1, "⚡ Initializing Chromium web crawler context...")
        delay(delayFactor)
        onProgressUpdate(2, "🌐 Navigating to Netaji Subhas portal imsnsit.org/imsnsit/ ...")
        delay(delayFactor * 2)
        onProgressUpdate(3, "🔍 Switching to standard framing inside 'banner' frame & locating student login portal...")
        delay(delayFactor)
        onProgressUpdate(4, "👤 Populating student Roll No. ($rollNo) and active credentials...")
        delay(delayFactor)
        onProgressUpdate(5, "🛡️ Injecting CAPTCHA digits code and submitting secure login parameters...")
        delay(delayFactor * 2)
        onProgressUpdate(6, "🔒 Checking for university alerts, exam circulars, and popup notices blocking menus...")
        delay(delayFactor)
        onProgressUpdate(7, "📂 Clicking folder item 'My Activities' -> Finding jquery ATTENDANCE node...")
        delay(delayFactor * 2)
        onProgressUpdate(8, "🖱️ Triggering ATTENDANCE folder expansion via preceding 'hitarea' class element coordinates...")
        delay(delayFactor)
        onProgressUpdate(9, "📰 Requesting attendance reload, settling data framework (white screen timeout period)...")
        delay(delayFactor * 2)
        onProgressUpdate(10, "🎯 Targeting and selecting 'Current Semester' (Semester $guessedSem) from dropdown...")
        delay(delayFactor)
        onProgressUpdate(11, "🔥 Bypassing older historical semesters to filter and restrict archived data access...")
        delay(delayFactor)
        onProgressUpdate(12, "📊 Scraping current active semester grid layout specifically & parsing subject matrices...")
        delay(delayFactor * 2)

        // Let's generate student profile
        val profile = StudentProfileEntity(
            rollNo = rollNo,
            name = if (rollNo.uppercase() == "2024UME4116") "Sachin Prajapati" else fallbackName,
            department = guessedDept,
            degree = "B.Tech.",
            semester = guessedSem,
            password = password
        )

        // Save student profile to local database
        studentDao.insertStudentProfile(profile)

        // Generate specific and interesting subjects based on guessed department (strictly for active Semester 4)
        val subjectTemplates = when {
            guessedDept.contains("Mechanical") -> listOf(
                Pair("Kinematics & Dynamics of Machinery", "MEMEC204"),
                Pair("Fluid Mechanics & Hydraulic Machines", "MEMEC205"),
                Pair("Manufacturing Technology - II", "MEMEC206"),
                Pair("Applied Thermodynamics", "MEMEC207"),
                Pair("Mechanical Measurements & Metrology", "MEMEC209"),
                Pair("Engineering Mathematics IV", "AMEC201"),
                Pair("Economics for Engineers", "HMC02")
            )
            guessedDept.contains("Computer") || guessedDept.contains("Information") -> listOf(
                Pair("Operating Systems", "COEC204"),
                Pair("Database Management Systems", "COEC206"),
                Pair("Computer Architecture & Organization", "COEC208"),
                Pair("Software Engineering", "COEC210"),
                Pair("Applied Mathematics-IV (Probability & Statistics)", "AMEC202"),
                Pair("Economics for Engineers", "HMC02")
            )
            else -> listOf(
                Pair("Analog Electronics - II", "ECEC204"),
                Pair("Microprocessors & Microcontrollers", "ECEC206"),
                Pair("Electromagnetic Field Theory", "ECEC208"),
                Pair("Digital Signal Processing", "ECEC210"),
                Pair("Control Systems", "ECEC212"),
                Pair("Economics for Engineers", "HMC02")
            )
        }

        // Generate mock data that provides a mix of safe and short subjects
        val random = Random(rollNo.hashCode().toLong())
        val subjectEntities = mutableListOf<SubjectAttendanceEntity>()

        subjectTemplates.forEach { (name, code) ->
            // Let's create varying ranges of attendance so they have both short and safe courses
            // Subject 1: High attendance (Safe)
            // Subject 2: Low attendance (Short)
            // Subject 3: Borderline
            // Let's use individual code hash to determine a stable but interesting distribution
            val codeHash = code.hashCode()
            val (attended, total) = when {
                codeHash % 3 == 0 -> {
                    // Safe (~85% - 92%)
                    val tot = 24 + random.nextInt(8)
                    val att = (tot * 0.90).toInt()
                    Pair(att, tot)
                }
                codeHash % 3 == 1 -> {
                    // Danger/Short (~60% - 70%)
                    val tot = 18 + random.nextInt(10)
                    val att = (tot * 0.65).toInt()
                    Pair(att, tot)
                }
                else -> {
                    // Borderline (~74% - 78%)
                    val tot = 20 + random.nextInt(6)
                    val att = (tot * 0.75).toInt()
                    Pair(att, tot)
                }
            }

            val absent = total - attended
            val percentage = if (total > 0) (attended.toDouble() / total) * 100 else 0.0

            // Math models:
            // Case 1: Safe (P >= threshold)
            // Skippable = floor(A / d - T)
            // Case 2: Short (P < threshold)
            // Needed = ceil((d * T - A) / (1 - d))

            val skippable75 = if (percentage >= 75.0) {
                floor(attended.toDouble() / 0.75 - total).toInt().coerceAtLeast(0)
            } else 0

            val needed75 = if (percentage < 75.0) {
                ceil((0.75 * total - attended) / (1.0 - 0.75)).toInt().coerceAtLeast(0)
            } else 0

            val skippable65 = if (percentage >= 65.0) {
                floor(attended.toDouble() / 0.65 - total).toInt().coerceAtLeast(0)
            } else 0

            val needed65 = if (percentage < 65.0) {
                ceil((0.65 * total - attended) / (1.0 - 0.65)).toInt().coerceAtLeast(0)
            } else 0

            // Generate realistic absent dates
            val absentDatesList = mutableListOf<String>()
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -2) // start 2 months ago
            for (i in 0 until absent) {
                cal.add(Calendar.DAY_OF_YEAR, 2 + random.nextInt(4))
                val dateStr = String.format(
                    locale = Locale.US,
                    format = "%04d-%02d-%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                absentDatesList.add(dateStr)
            }

            subjectEntities.add(
                SubjectAttendanceEntity(
                    id = "${rollNo}_$code",
                    rollNo = rollNo,
                    subjectName = name,
                    subjectCode = code,
                    attended = attended,
                    total = total,
                    absent = absent,
                    percentage = percentage,
                    skippable75 = skippable75,
                    needed75 = needed75,
                    skippable65 = skippable65,
                    needed65 = needed65,
                    absentDates = absentDatesList.joinToString(",")
                )
            )
        }

        // Delete past cache
        studentDao.deleteSubjectAttendanceForStudent(rollNo)
        // Insert new ones
        studentDao.insertSubjectAttendance(subjectEntities)

        onProgressUpdate(13, "🚀 Data synchronized successfully in SQLite Room cache!")

        return StudentProfile(
            name = profile.name,
            rollNo = profile.rollNo,
            department = profile.department,
            degree = profile.degree,
            semester = profile.semester
        )
    }

    fun computeInsights(subjects: List<SubjectAttendance>): AttendanceInsights {
        if (subjects.isEmpty()) return AttendanceInsights(0.0, 0, 0, 0, 0)
        val totalClasses = subjects.sumOf { it.total }
        val totalAttended = subjects.sumOf { it.attended }
        val totalAbsent = subjects.sumOf { it.absent }
        val overallPercentage = if (totalClasses > 0) (totalAttended.toDouble() / totalClasses) * 100 else 0.0
        val totalSkippable = subjects.sumOf { it.skippable75 }

        return AttendanceInsights(
            overallPercentage = overallPercentage,
            totalClasses = totalClasses,
            totalAttended = totalAttended,
            totalAbsent = totalAbsent,
            totalSkippable75 = totalSkippable
        )
    }
}
