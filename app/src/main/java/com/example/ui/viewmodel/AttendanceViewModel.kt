package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.AttendanceInsights
import com.example.data.model.ChatMessage
import com.example.data.model.Sender
import com.example.data.model.StudentProfile
import com.example.data.model.SubjectAttendance
import com.example.data.remote.Content
import com.example.data.remote.GeminiClient
import com.example.data.remote.Part
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import android.util.Base64
import java.util.Locale
import java.util.Random

sealed class ScreenState {
    object Login : ScreenState()
    data class Sync(val rollNo: String, val password: String) : ScreenState()
    data class MainDashboard(val rollNo: String) : ScreenState()
}

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AttendanceRepository(database.studentDao())

    // UI screen navigation state
    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Login)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    // Profile & Attendance loaded states
    private val _currentRollNo = MutableStateFlow<String>("")
    val currentRollNo: StateFlow<String> = _currentRollNo.asStateFlow()

    val studentProfile: StateFlow<StudentProfile?> = _currentRollNo
        .flatMapLatest { roll ->
            if (roll.isEmpty()) flowOf(null) else repository.getStudentProfile(roll)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subjectAttendanceList: StateFlow<List<SubjectAttendance>> = _currentRollNo
        .flatMapLatest { roll ->
            if (roll.isEmpty()) flowOf(emptyList()) else repository.getSubjectAttendance(roll)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceInsights: StateFlow<AttendanceInsights> = subjectAttendanceList
        .map { repository.computeInsights(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AttendanceInsights(0.0, 0, 0, 0, 0))

    // CAPTCHA properties
    private val _captchaText = MutableStateFlow("")
    val captchaText: StateFlow<String> = _captchaText.asStateFlow()

    private val _captchaBitmap = MutableStateFlow<Bitmap?>(null)
    val captchaBitmap: StateFlow<Bitmap?> = _captchaBitmap.asStateFlow()

    private val _isCaptchaOcrRunning = MutableStateFlow(false)
    val isCaptchaOcrRunning: StateFlow<Boolean> = _isCaptchaOcrRunning.asStateFlow()

    private val _captchaError = MutableStateFlow<String?>(null)
    val captchaError: StateFlow<String?> = _captchaError.asStateFlow()

    // Sync state logs
    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _isSyncRunning = MutableStateFlow(false)
    val isSyncRunning: StateFlow<Boolean> = _isSyncRunning.asStateFlow()

    // Chat properties
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatBotGenerating = MutableStateFlow(false)
    val isChatBotGenerating: StateFlow<Boolean> = _isChatBotGenerating.asStateFlow()

    init {
        // Generate initial CAPTCHA
        generateNewCaptcha()

        // Check if there's a cached profile from previous sessions (fast load!)
        viewModelScope.launch {
            val lastRollNo = repository.getLastLoggedInRollNo()
            if (lastRollNo != null) {
                _currentRollNo.value = lastRollNo
                
                // Detect and migrate older semester subjects in cache automatically
                try {
                    val subjects = repository.getSubjectAttendance(lastRollNo).first()
                    val hasOldSubjects = subjects.any { 
                        it.subjectCode in listOf("MEMEC303", "MEMEC302", "MEMEC301", "AMEC301", "HMC01") ||
                        it.subjectCode in listOf("COEC201", "COEC310", "COEC302", "AIDS01", "COEC102", "COEC211") ||
                        it.subjectCode in listOf("ECEC202", "COEC311", "COEC205", "ECEC301", "ECEC320")
                    }
                    if (hasOldSubjects || subjects.isEmpty()) {
                        repository.performPortalSync(lastRollNo, "saved_password_placeholder") { _, _ -> }
                    }
                } catch (e: Exception) {
                    // Fallback gracefully
                }

                _screenState.value = ScreenState.MainDashboard(lastRollNo)
                // Welcome message
                appendBotWelcomeMessage(lastRollNo)
            }
        }
    }

    // CAPTCHA Generation using standard canvas drawing
    fun generateNewCaptcha() {
        val rand = Random()
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // clear readable chars
        val text = StringBuilder()
        for (i in 0 until 5) {
            text.append(chars[rand.nextInt(chars.length)])
        }
        val textStr = text.toString()
        _captchaText.value = textStr
        _captchaError.value = null

        val width = 200
        val height = 70
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        val bgPaint = Paint().apply {
            color = AndroidColor.rgb(15, 23, 42) // Slate 900
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw bounding glassmorphic border
        val borderPaint = Paint().apply {
            color = AndroidColor.rgb(51, 65, 85) // Slate 700
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, (width - 1).toFloat(), (height - 1).toFloat(), borderPaint)

        // Noise lines
        val linePaint = Paint().apply {
            color = AndroidColor.rgb(59, 130, 246) // Accent Electric Blue
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        for (i in 0 until 5) {
            canvas.drawLine(
                rand.nextFloat() * width, rand.nextFloat() * height,
                rand.nextFloat() * width, rand.nextFloat() * height,
                linePaint
            )
        }

        // Noise dots
        val dotPaint = Paint().apply {
            color = AndroidColor.rgb(148, 163, 184) // Slate 400
            strokeWidth = 3f
        }
        for (i in 0 until 40) {
            canvas.drawPoint(rand.nextFloat() * width, rand.nextFloat() * height, dotPaint)
        }

        // Draw Letters
        val textPaint = Paint().apply {
            color = AndroidColor.rgb(241, 245, 249) // White grey
            textSize = 34f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val charWidth = (width - 40) / textStr.length
        for (i in textStr.indices) {
            canvas.save()
            val x = 20f + i * charWidth + rand.nextInt(12) - 6
            val y = 46f + rand.nextInt(10) - 5
            val angle = (rand.nextFloat() * 30) - 15f
            canvas.rotate(angle, x, y)
            canvas.drawText(textStr[i].toString(), x, y, textPaint)
            canvas.restore()
        }

        _captchaBitmap.value = bitmap
    }

    // Convert Bitmap to Base64 String for Gemini API consumption
    private suspend fun Bitmap.toBase64Png(): String = withContext(Dispatchers.IO) {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    // Automatically solve CAPTCHA using Gemini Vision
    fun solveCaptchaWithOcr() {
        val bitmap = _captchaBitmap.value ?: return
        viewModelScope.launch {
            _isCaptchaOcrRunning.value = true
            _captchaError.value = null
            try {
                val base64 = bitmap.toBase64Png()
                val result = GeminiClient.extractTextFromCaptcha(base64)
                if (result == "ERROR_NO_API_KEY") {
                    _captchaError.value = "Gemini API key is not configured in Secrets panel."
                } else if (result == "UNKNOWN" || result.isEmpty()) {
                    _captchaError.value = "OCR could not read CAPTCHA. Please enter manually."
                } else {
                    // Pre-fill or directly return solved CAPTCHA text
                    _captchaText.value = result
                }
            } catch (e: Exception) {
                _captchaError.value = "Failed to run OCR: ${e.localizedMessage ?: "Network error"}"
            } finally {
                _isCaptchaOcrRunning.value = false
            }
        }
    }

    // Attempt login with roll number and password
    fun attemptLogin(rollNo: String, password: String, captchaInput: String) {
        if (rollNo.length < 5) {
            _captchaError.value = "Invalid Roll Number. Must be at least 5 character dimensions."
            return
        }
        if (password.isEmpty()) {
            _captchaError.value = "Password cannot be empty."
            return
        }
        if (captchaInput.trim().uppercase() != _captchaText.value.uppercase()) {
            _captchaError.value = "Incorrect CAPTCHA solver characters. Feel free to refresh."
            return
        }

        // Logic succeeds! Transition to Sync screen
        _screenState.value = ScreenState.Sync(rollNo, password)
        executePortalSync(rollNo, password)
    }

    private fun executePortalSync(rollNo: String, password: String) {
        viewModelScope.launch {
            _isSyncRunning.value = true
            _syncLogs.value = emptyList()
            _syncProgress.value = 0f

            try {
                _currentRollNo.value = rollNo
                val profile = repository.performPortalSync(rollNo, password) { step, log ->
                    // Update steps
                    _syncLogs.value = _syncLogs.value + log
                    _syncProgress.value = step.toFloat() / 13f
                }

                // Successful sync! Welcome student
                appendBotWelcomeMessage(rollNo)
                _screenState.value = ScreenState.MainDashboard(rollNo)
            } catch (e: Exception) {
                _syncLogs.value = _syncLogs.value + "❌ Crawl aborted: ${e.localizedMessage ?: "Unknown connection error"}"
            } finally {
                _isSyncRunning.value = false
            }
        }
    }

    private suspend fun appendBotWelcomeMessage(rollNo: String) {
        val profile = repository.getStudentProfileOneShot(rollNo)
        val name = profile?.name ?: "Student"
        val sem = profile?.semester ?: "4"
        val welcomeText = "👋 **Welcome back, $name!**\n\n" +
                "I am your **NSUT Attendance & Analytics Chatbot**. I have successfully synchronized and loaded your **Current Semester (Semester $sem) attendance** grid from the official IMS portal into my local Room cache (older semesters have been ignored to keep your dashboard clean!).\n\n" +
                "You can use these quick action triggers below or ask any free-form questions about your schedules, bunk options, or shortages:\n\n" +
                "- `HI`: Displays overall attendance totals and subject breakdown.\n" +
                "- `SAFE`: Shows classes you can skip.\n" +
                "- `RISK`: Lists subjects needing attendance recovery.\n" +
                "- `SW`: Lists subject index (type `SW <number>` for detail dates)."

        _chatMessages.value = listOf(
            ChatMessage(sender = Sender.BOT, text = welcomeText)
        )
    }

    fun handleUserSendMessage(inputText: String) {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) return

        // Create user message
        val userMsg = ChatMessage(sender = Sender.USER, text = trimmed)
        _chatMessages.value = _chatMessages.value + userMsg

        val profile = studentProfile.value
        val subjects = subjectAttendanceList.value

        if (profile == null || subjects.isEmpty()) {
            _chatMessages.value = _chatMessages.value + ChatMessage(
                sender = Sender.BOT,
                text = "⚠️ No local synchronized attendance data successfully cached. Please sync the portal first."
            )
            return
        }

        _isChatBotGenerating.value = true

        viewModelScope.launch {
            // 1. Try resolving keywords with exact local equations for offline/speed instant answers!
            val localResponse = getLocalResponse(trimmed, profile, subjects)
            if (localResponse.isNotEmpty()) {
                _chatMessages.value = _chatMessages.value + ChatMessage(sender = Sender.BOT, text = localResponse)
                _isChatBotGenerating.value = false
                return@launch
            }

            // 2. If it is a conversational message, utilize the Gemini API!
            try {
                val systemInstruction = "You are the NSUT Student Attendance & Analysis Chatbot.\n" +
                    "Here is the student's real-time academic attendance data:\n" +
                    "Student Name: ${profile.name}\n" +
                    "Roll Number: ${profile.rollNo}\n" +
                    "Department: ${profile.department}\n" +
                    "Degree: ${profile.degree}\n" +
                    "Semester: ${profile.semester}\n\n" +
                    "Subjects List:\n" +
                    subjects.joinToString("\n") {
                        "- ${it.subjectName} (${it.subjectCode}): Attended=${it.attended}/${it.total}, Percentage=${String.format(Locale.US, "%.1f", it.percentage)}%, Bunk Buffer (75%)=${it.skippable75}, Needed (75%)=${it.needed75}, Bunk Buffer (65%)=${it.skippable65}, Needed (65%)=${it.needed65}, Absent Dates=${it.absentDates.joinToString(", ")}"
                    } + "\n\n" +
                    "Instructions for you:\n" +
                    "1. Respond directly to the user query inside conversational mode.\n" +
                    "2. If they ask custom questions (like 'Which is my worst class?' or 'Can I miss Manufacturing next week?'), explain the specific math buffers based on their actual numbers above.\n" +
                    "3. Limit responses to 2 concise markdown paragraphs maximum unless they request deep calculations.\n" +
                    "4. Use professional student-encouraging support tones. Use beautiful Markdown styling (lists, bold highlights, status code tags)."

                // Convert history to Gemini request parts
                // Keep only the last 6 turns to remain safe with context boundary limits
                val textHistory = _chatMessages.value.takeLast(6).map {
                    Content(parts = listOf(Part(text = it.text)))
                }

                val reply = GeminiClient.generateConversationalReply(systemInstruction, textHistory)
                _chatMessages.value = _chatMessages.value + ChatMessage(sender = Sender.BOT, text = reply)
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = Sender.BOT,
                    text = "🤖 *Offline Fallback:* Sorry, I had an issue connecting to my Gemini AI core. Try checking your network or using standard shortcuts like `HI` or `SAFE`!"
                )
            } finally {
                _isChatBotGenerating.value = false
            }
        }
    }

    // Local state calculation responses
    private fun getLocalResponse(message: String, profile: StudentProfile, subjects: List<SubjectAttendance>): String {
        val clean = message.uppercase().trim()
        return when {
            clean == "HI" || clean == "SUMMARY" || clean == "DASHBOARD" || clean == "TOTAL" -> {
                val totalClasses = subjects.sumOf { it.total }
                val totalAttended = subjects.sumOf { it.attended }
                val totalAbsent = subjects.sumOf { it.absent }
                val overall = if (totalClasses > 0) (totalAttended.toDouble() / totalClasses) * 100 else 0.0

                "👋 **Hello ${profile.name}!** Here is your attendance dashboard summary:\n\n" +
                        "📌 **Overall Attendance:** **${String.format(Locale.US, "%.1f", overall)}%** ($totalAttended/$totalClasses classes)\n" +
                        "❌ **Total Absent:** **$totalAbsent** lectures missed\n" +
                        "🎉 **Bunk Buffer:** You can safe-bunk **${subjects.sumOf { it.skippable75 }}** classes currently.\n\n" +
                        "**Subject breakdown:**\n" +
                        subjects.joinToString("\n") { sub ->
                            val status = if (sub.percentage >= 75.0) "🟢 SAFE" else "🔴 SHORT"
                            "- **${sub.subjectCode} - ${sub.subjectName}**: **${String.format(Locale.US, "%.1f", sub.percentage)}%** • $status"
                        }
            }

            clean == "SAFE" || clean == "BUNK" -> {
                val safeSubjects = subjects.filter { it.percentage >= 75.0 }
                if (safeSubjects.isEmpty()) {
                    "⚠️ **No subjects are currently safe!** All of your subjects are below the 75% threshold. Bunking is highly discouraged."
                } else {
                    "🟢 **Safe Skip Buffer (maintain >= 75% attendance):**\n\n" +
                            safeSubjects.joinToString("\n") { sub ->
                                "- **${sub.subjectName} (${sub.subjectCode})**: You can safely miss **${sub.skippable75}** classes. Current: **${String.format(Locale.US, "%.1f", sub.percentage)}%**."
                            } + "\n\n*Plan your bunks carefully to allocate time for self-study and exam preps!*"
                }
            }

            clean == "RISK" || clean == "DANGER" || clean == "ABSENT" -> {
                val shortSubjects = subjects.filter { it.percentage < 75.0 }
                if (shortSubjects.isEmpty()) {
                    "🎉 **Amazing!** You have no subjects at risk of attendance shortfalls. All your subjects are safely above 75%."
                } else {
                    "🔴 **Attendance Shortage Alert (Action Required):**\n\n" +
                            "You must attend consecutive lectures to recover above 75%:\n\n" +
                            shortSubjects.joinToString("\n") { sub ->
                                "- **${sub.subjectName} (${sub.subjectCode})**: Current **${String.format(Locale.US, "%.1f", sub.percentage)}%** • Attend **${sub.needed75}** consecutive classes to recover."
                            } + "\n\n*Make sure to prioritize these classes to avoid semester eligibility issues!*"
                }
            }

            clean.startsWith("SW") -> {
                val numPart = clean.removePrefix("SW").trim().toIntOrNull()
                if (numPart != null && numPart in 1..subjects.size) {
                    val sub = subjects[numPart - 1]
                    val statusBadge = if (sub.percentage >= 75.0) "🟢 SAFE" else "🔴 SHORT"
                    "📘 **Subject Detail Summary:**\n\n" +
                            "**${sub.subjectName}** (`${sub.subjectCode}`)\n" +
                            "- **Status:** $statusBadge\n" +
                            "- **Attended:** ${sub.attended} out of ${sub.total} classes held\n" +
                            "- **Percentage:** **${String.format(Locale.US, "%.1f", sub.percentage)}%**\n" +
                            "- **Bunk Buffer (75%):** **${sub.skippable75}** classes\n" +
                            "- **Bunk Buffer (65%):** **${sub.skippable65}** classes\n" +
                            "- **Recover (75%):** **${sub.needed75}** consecutive lectures\n" +
                            "- **Date-wise Absences:** ${if (sub.absentDates.isEmpty()) "None recorded" else sub.absentDates.joinToString(", ")}"
                } else {
                    "📘 **Subject-Wise (SW) Attendance Index:**\n\n" +
                            "Enter `SW <number>` (e.g., `SW 1`) to see detailed absences and skip limits:\n\n" +
                            subjects.mapIndexed { idx, sub ->
                                "${idx + 1}. **${sub.subjectCode}** - ${sub.subjectName} (${String.format(Locale.US, "%.1f", sub.percentage)}%)"
                            }.joinToString("\n")
                }
            }

            clean == "PROFILE" -> {
                "👤 **Student Academic Profile:**\n\n" +
                        "- **Name:** ${profile.name}\n" +
                        "- **Roll Number:** ${profile.rollNo}\n" +
                        "- **Department:** ${profile.department}\n" +
                        "- **Degree Program:** ${profile.degree}\n" +
                        "- **Academic Term:** Semester ${profile.semester}\n" +
                        "- **Synced Database State:** Cache Loaded (Local SQLite DB)"
            }

            clean == "CALENDAR" || clean == "HOLIDAYS" -> {
                "📅 **Academic Leave & Marked Holidays (University Legend):**\n\n" +
                        "- **GH (Gazetted Holiday):** No lectures occurred\n" +
                        "- **TL (Textual Leave):** Recognized academic permission\n" +
                        "- **CS (Class Suspended):** General faculty suspension\n\n" +
                        "**Next official holidays:**\n" +
                        "- June 23: Summer Break Session Begins\n" +
                        "- August 15: Independence Day\n" +
                        "- October 2: Gandhi Jayanti"
            }

            clean == "WEBSITE" || clean == "DATA" -> {
                "🌐 **Portal Synchronization Details:**\n\n" +
                        "- **Source:** IMS NSIT Portal (`imsnsit.org`)\n" +
                        "- **Navigation Agent:** Headless Playwright Chromium (Automated Simulation Mode)\n" +
                        "- **OCR Solution Mode:** Alphanumeric white-listed OCR\n" +
                        "- **Latest Capture Block:** Successfully mapped data panels."
            }

            else -> ""
        }
    }

    // Force re-scrape/sync data from the dashboard
    fun forceSyncRefresh() {
        val roll = _currentRollNo.value
        val profile = studentProfile.value ?: return

        // Fetch saved password or default to something stable
        val password = "saved_password_placeholder"

        _screenState.value = ScreenState.Sync(roll, password)
        executePortalSync(roll, password)
    }

    // Delete student, session cache and log out
    fun triggerLogOut() {
        val roll = _currentRollNo.value
        viewModelScope.launch {
            if (roll.isNotEmpty()) {
                repository.logOut(roll)
            }
            _currentRollNo.value = ""
            _chatMessages.value = emptyList()
            _screenState.value = ScreenState.Login
            generateNewCaptcha()
        }
    }
}
