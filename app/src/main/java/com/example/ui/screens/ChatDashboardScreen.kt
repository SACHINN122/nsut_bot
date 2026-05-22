package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AttendanceFilterStatus
import com.example.data.model.AttendanceInsights
import com.example.data.model.ChatMessage
import com.example.data.model.DashboardFilters
import com.example.data.model.Sender
import com.example.data.model.StudentProfile
import com.example.data.model.SubjectAttendance
import java.util.Locale

@Composable
fun ChatDashboardScreen(
    profile: StudentProfile?,
    subjects: List<SubjectAttendance>,
    filteredSubjects: List<SubjectAttendance>,
    insights: AttendanceInsights,
    filters: DashboardFilters,
    availableSemesters: List<String>,
    messages: List<ChatMessage>,
    isBotGenerating: Boolean,
    onSendMessage: (String) -> Unit,
    onRefreshData: () -> Unit,
    onLogOut: () -> Unit,
    onFilterSemester: (String) -> Unit,
    onFilterStatus: (AttendanceFilterStatus) -> Unit,
    onFilterSearch: (String) -> Unit,
    onResetFilters: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // HEADER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Photo
                if (profile?.photoUrl != null && profile.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = profile.photoUrl,
                        contentDescription = "Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    )
                } else {
                    val initials = if (profile != null && profile.name.isNotEmpty()) {
                        profile.name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase()
                    } else "SP"
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF3B82F6), CircleShape)
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    ) {
                        Text(
                            text = initials,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.name ?: "Sachin Prajapati",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Roll: ${profile?.rollNo ?: "2024UME4116"} • Sem ${profile?.semester ?: "4"}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefreshData,
                        modifier = Modifier.testTag("refresh_dashboard_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data", tint = Color.White)
                    }
                    IconButton(
                        onClick = onLogOut,
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = Color(0xFFFCA5A5))
                    }
                }
            }

            HorizontalDivider(color = Color(0x11FFFFFF), thickness = 1.dp)

            // TAB SELECTOR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(Color(0x1F1E293B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                TabButton(
                    label = "Analytics Dashboard",
                    icon = Icons.Default.Home,
                    isSelected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 }
                )
                TabButton(
                    label = "AI Chat Assistant",
                    icon = Icons.Default.Send,
                    isSelected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedTab == 0) {
                    DashboardTabContent(
                        subjects = filteredSubjects,
                        totalSubjects = subjects.size,
                        insights = insights,
                        filters = filters,
                        availableSemesters = availableSemesters,
                        onFilterSemester = onFilterSemester,
                        onFilterStatus = onFilterStatus,
                        onFilterSearch = onFilterSearch,
                        onResetFilters = onResetFilters,
                        onChatShortcut = { shortcut ->
                            onSendMessage(shortcut)
                            selectedTab = 1
                        }
                    )
                } else {
                    ChatbotTabContent(
                        messages = messages,
                        isGenerating = isBotGenerating,
                        onSendMessage = onSendMessage
                    )
                }
            }
        }
    }
}

@Composable
fun TabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .background(
                if (isSelected) Color(0xFF3B82F6) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color.White else Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color(0xFF94A3B8)
        )
    }
}

@Composable
fun DashboardTabContent(
    subjects: List<SubjectAttendance>,
    totalSubjects: Int,
    insights: AttendanceInsights,
    filters: DashboardFilters,
    availableSemesters: List<String>,
    onFilterSemester: (String) -> Unit,
    onFilterStatus: (AttendanceFilterStatus) -> Unit,
    onFilterSearch: (String) -> Unit,
    onResetFilters: () -> Unit,
    onChatShortcut: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_scroll_area"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // OVERALL METRIC CARD
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0x331E293B), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = "OVERALL ATTENDANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", insights.overallPercentage)}%",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total classes: ${insights.totalAttended} attended / ${insights.totalClasses} held",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Showing ${subjects.size} of $totalSubjects subjects",
                            fontSize = 10.sp,
                            color = if (subjects.size < totalSubjects) Color(0xFFF59E0B) else Color(0xFF6EE7B7)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(72.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { (insights.overallPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                            color = if (insights.overallPercentage >= 75.0) Color(0xFF10B981) else Color(0xFFEF4444),
                            strokeWidth = 6.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "${insights.totalClasses - insights.totalAttended} Abs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // FILTER BAR
        item {
            FilterBar(
                filters = filters,
                availableSemesters = availableSemesters,
                onFilterSemester = onFilterSemester,
                onFilterStatus = onFilterStatus,
                onFilterSearch = onFilterSearch,
                onResetFilters = onResetFilters
            )
        }

        // STATS BUBBLES
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBubble(
                    label = "Total Absent",
                    value = insights.totalAbsent.toString(),
                    color = Color(0xFFD1D5DB),
                    modifier = Modifier.weight(1f)
                )
                StatBubble(
                    label = "Safe Bunks (75%)",
                    value = "${insights.totalSkippable75} Class",
                    color = Color(0xFFA7F3D0),
                    modifier = Modifier.weight(1f)
                )
                StatBubble(
                    label = "Active Shortages",
                    value = subjects.count { it.percentage < 75.0 }.toString(),
                    color = if (subjects.count { it.percentage < 75.0 } > 0) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // QUICK SHORTCUTS
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "QUICK ACTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item { ShortcutChip(label = "Check Bunk Limits", icon = Icons.Default.List, onClick = { onChatShortcut("SAFE") }) }
                    item { ShortcutChip(label = "Show Shorts/Danger", icon = Icons.Default.Warning, onClick = { onChatShortcut("RISK") }) }
                    item { ShortcutChip(label = "Check Holidays", icon = Icons.Default.Info, onClick = { onChatShortcut("CALENDAR") }) }
                }
            }
        }

        // SUBJECTS HEADER
        item {
            Text(
                text = "SUBJECTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        // SUBJECT CARDS
        items(subjects.size) { index ->
            val sub = subjects[index]
            SubjectAttCard(sub, index + 1, onDetailClick = { onChatShortcut("SW ${index + 1}") })
        }

        if (subjects.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No subjects match the current filters.\nTry changing or resetting filters.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun FilterBar(
    filters: DashboardFilters,
    availableSemesters: List<String>,
    onFilterSemester: (String) -> Unit,
    onFilterStatus: (AttendanceFilterStatus) -> Unit,
    onFilterSearch: (String) -> Unit,
    onResetFilters: () -> Unit
) {
    var searchText by remember { mutableStateOf(filters.searchQuery) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color(0x1A1E293B), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "FILTERS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 1.sp
            )
            TextButton(
                onClick = {
                    onResetFilters()
                    searchText = ""
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Reset", fontSize = 11.sp, color = Color(0xFF60A5FA))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search field
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                onFilterSearch(it)
            },
            placeholder = { Text("Search subject name or code...", color = Color(0xFF475569), fontSize = 12.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0x1EFFFFFF),
                cursorColor = Color(0xFF3B82F6)
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = {
                        searchText = ""
                        onFilterSearch("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Semester chips
        val semOptions = listOf("All") + availableSemesters
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(semOptions.size) { idx ->
                val sem = semOptions[idx]
                val isSelected = filters.semester == sem
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSemester(sem) },
                    label = {
                        Text(
                            text = if (sem == "All") "All Semesters" else "Sem $sem",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3B82F6),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0x1A1E293B),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0x0DFFFFFF),
                        selectedBorderColor = Color(0xFF3B82F6),
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Status chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val statusOptions = listOf(
                "All" to AttendanceFilterStatus.ALL,
                "Safe" to AttendanceFilterStatus.SAFE,
                "Borderline" to AttendanceFilterStatus.BORDERLINE,
                "Shortage" to AttendanceFilterStatus.SHORTAGE
            )
            items(statusOptions.size) { idx ->
                val (label, status) = statusOptions[idx]
                val isSelected = filters.status == status
                val chipColor = when (status) {
                    AttendanceFilterStatus.SAFE -> Color(0xFF10B981)
                    AttendanceFilterStatus.BORDERLINE -> Color(0xFFF59E0B)
                    AttendanceFilterStatus.SHORTAGE -> Color(0xFFEF4444)
                    else -> Color(0xFF3B82F6)
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterStatus(status) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0x1A1E293B),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0x0DFFFFFF),
                        selectedBorderColor = chipColor,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
    }
}

@Composable
fun StatBubble(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0x141E293B), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 9.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
    }
}

@Composable
fun ShortcutChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(Color(0xFF1E293B), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0x16FFFFFF), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun SubjectAttCard(sub: SubjectAttendance, index: Int, onDetailClick: () -> Unit) {
    val statusColor = when {
        sub.percentage >= 75.0 -> Color(0xFF10B981)
        sub.percentage >= 65.0 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    val statusBadgeText = when {
        sub.percentage >= 75.0 -> "SAFE"
        sub.percentage >= 65.0 -> "BORDERLINE"
        else -> "SHORTAGE"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(Color(0x261E293B), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
            .clickable { onDetailClick() }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.3f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${sub.subjectCode} • Subject #$index",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Sem ${sub.semester}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF60A5FA)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sub.subjectName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusBadgeText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "LECTURE STATS", fontSize = 9.sp, color = Color(0xFF64748B))
                    Text(
                        text = "${sub.attended} Attended / ${sub.total} Held",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "${String.format(Locale.US, "%.1f", sub.percentage)}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (sub.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                color = statusColor,
                trackColor = Color(0x0DFFFFFF),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.Transparent, RoundedCornerShape(100.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x0A000000), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (sub.percentage >= 75.0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bunkable: Safe to miss ${sub.skippable75} more classes",
                                fontSize = 11.sp,
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Shortfall: Attend ${sub.needed75} consecutive classes to recover (75%)",
                                fontSize = 11.sp,
                                color = Color(0xFFFCA5A5),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Navigate detail",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun ChatbotTabContent(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    onSendMessage: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val attemptSubmit = {
        if (textInput.trim().isNotEmpty()) {
            val toSend = textInput
            textInput = ""
            onSendMessage(toSend)
            keyboardController?.hide()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .testTag("chat_messages_board"),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages.size) { index ->
                val msg = messages[index]
                ChatBubble(msg)
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF3B82F6),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Consulting smart core analyser...",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0x0AFFFFFF), thickness = 1.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x1A0F172A))
                .padding(vertical = 8.dp)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tags = listOf(
                    Pair("SUMMARY 📁", "HI"),
                    Pair("BUNK LIMITS 🟢", "SAFE"),
                    Pair("RISK MATRIX 🔴", "RISK"),
                    Pair("SUBJECT CODE 📘", "SW"),
                    Pair("ACADEMIC CALENDAR 📅", "CALENDAR"),
                    Pair("SYSTEM STATE 🌐", "WEBSITE"),
                    Pair("PROFILE 👤", "PROFILE")
                )
                items(tags.size) { index ->
                    val (label, cmd) = tags[index]
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(100.dp))
                            .border(1.dp, Color(0x1F3B82F6), RoundedCornerShape(100.dp))
                            .clickable { onSendMessage(cmd) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0x0AFFFFFF), thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask about binks, limits or index details...", color = Color(0xFF475569), fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0x1EFFFFFF)
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { attemptSubmit() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_textfield"),
                trailingIcon = {
                    if (textInput.isNotEmpty()) {
                        IconButton(
                            onClick = { attemptSubmit() }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.sender == Sender.USER
    val bubbleBg = if (isUser) Color(0xFF3B82F6) else Color(0xFF1E293B)
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleBg, shape)
                .border(1.dp, if (isUser) Color(0x1F000000) else Color(0x0AFFFFFF), shape)
                .padding(14.dp)
                .testTag(if (isUser) "user_message_bubble" else "bot_message_bubble")
        ) {
            MarkdownText(
                rawText = msg.text,
                textColor = if (isUser) Color.White else Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
fun MarkdownText(rawText: String, textColor: Color) {
    Column {
        val lines = rawText.split("\n")
        lines.forEach { line ->
            val trimmedLine = line.trim()
            when {
                trimmedLine.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                trimmedLine.startsWith("###") -> {
                    val header = trimmedLine.removePrefix("###").trim()
                    Text(
                        text = header,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                trimmedLine.startsWith("##") -> {
                    val header = trimmedLine.removePrefix("##").trim()
                    Text(
                        text = header,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                trimmedLine.startsWith("#") -> {
                    val header = trimmedLine.removePrefix("#").trim()
                    Text(
                        text = header,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                trimmedLine.startsWith("-") || trimmedLine.startsWith("•") || trimmedLine.startsWith("* ") -> {
                    val content = trimmedLine.substring(1).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "• ", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF60A5FA))
                        Text(
                            text = parseInlineMarkdown(content),
                            fontSize = 13.sp,
                            color = textColor,
                            lineHeight = 18.sp
                        )
                    }
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        fontSize = 13.sp,
                        color = textColor,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return remember(text) {
        androidx.compose.ui.text.buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                when {
                    text.startsWith("**", i) -> {
                        val end = text.indexOf("**", i + 2)
                        if (end != -1) {
                            pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color.White))
                            append(text.substring(i + 2, end))
                            pop()
                            i = end + 2
                        } else {
                            append("*")
                            i++
                        }
                    }
                    text.startsWith("`", i) -> {
                        val end = text.indexOf("`", i + 1)
                        if (end != -1) {
                            pushStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF60A5FA),
                                    background = Color(0x1F000000)
                                )
                            )
                            append(text.substring(i + 1, end))
                            pop()
                            i = end + 1
                        } else {
                            append("`")
                            i++
                        }
                    }
                    else -> {
                        append(text[i])
                        i++
                    }
                }
            }
        }
    }
}
