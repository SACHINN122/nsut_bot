package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.PurpleAccent

@Composable
fun LoginScreen(
    captchaBitmap: Bitmap?,
    isOcrRunning: Boolean,
    errorMessage: String?,
    captchaText: String, // OCR auto-filled value
    onRefreshCaptcha: () -> Unit,
    onSolveOcr: () -> Unit,
    onLoginClick: (rollNo: String, pass: String, captcha: String) -> Unit
) {
    var rollNo by remember { mutableStateOf("2024UME4116") }
    var password by remember { mutableStateOf("nsut_secret_pass") }
    var captchaInput by remember { mutableStateOf("") }

    // Sync input once OCR pre-fills the value
    LaunchedEffect(captchaText) {
        if (captchaText.isNotEmpty()) {
            captchaInput = captchaText
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E1B4B)  // Indigo 950
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual Logo & Accent Heading
            Text(
                text = "IMS NSUT",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricBlue,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Smart Attendance",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "AI-Driven Conversational Wrapper for IMS NSIT Portal",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Glassmorphic Login Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x331E293B), RoundedCornerShape(24.dp)) // Glassmorphic background
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SECURE STUDENT SIGN-IN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Roll Number TextField
                    OutlinedTextField(
                        value = rollNo,
                        onValueChange = { rollNo = it },
                        label = { Text("Roll Number", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            cursorColor = ElectricBlue
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Roll Number Icon",
                                tint = ElectricBlue
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rollno_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password TextField
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("IMS Portal Password", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            cursorColor = ElectricBlue
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Password Icon",
                                tint = ElectricBlue
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordTransformationInstance,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // CAPTCHA Block
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x1A000000), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(Color.Black, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (captchaBitmap != null) {
                                Image(
                                    bitmap = captchaBitmap.asImageBitmap(),
                                    contentDescription = "IMS CAPTCHA",
                                    modifier = Modifier.fillMaxHeight().testTag("captcha_image")
                                )
                            } else {
                                CircularProgressIndicator(color = ElectricBlue, strokeWidth = 2.dp)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // OCR Auto Solver Button
                        Button(
                            onClick = onSolveOcr,
                            enabled = !isOcrRunning && captchaBitmap != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x263B82F6),
                                contentColor = ElectricBlue
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("ocr_button")
                        ) {
                            if (isOcrRunning) {
                                CircularProgressIndicator(
                                    color = ElectricBlue,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("🤖 OCR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Refresh Button
                        IconButton(
                            onClick = onRefreshCaptcha,
                            modifier = Modifier.testTag("refresh_captcha_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Captcha", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Captcha characters Input field
                    OutlinedTextField(
                        value = captchaInput,
                        onValueChange = { captchaInput = it },
                        label = { Text("CAPTCHA Characters", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            cursorColor = ElectricBlue
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("captcha_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Authenticate & Scrape Sync button
                    Button(
                        onClick = { onLoginClick(rollNo, password, captchaInput) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button")
                    ) {
                        Text(
                            text = "LOGIN & SYNC PORTAL",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer disclaimers
            Text(
                text = "🔒 Credentials are saved strictly within local Room AppDatabase. Captchas older than 45s will auto-verify on refresh.",
                fontSize = 10.sp,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// Single instance transform cache to avoid needless allocations
private val PasswordTransformationInstance = PasswordVisualTransformation()
