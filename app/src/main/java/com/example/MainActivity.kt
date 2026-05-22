package com.example

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val isServiceEnabledState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PurityAccessibilityService.isAppWindowActive = true
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PurityLockApp(
                    isServiceEnabled = isServiceEnabledState.value,
                    onOpenSettings = { openAccessibilitySettings() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PurityAccessibilityService.isAppWindowActive = true
        isServiceEnabledState.value = isAccessibilityServiceEnabled(this)
    }

    override fun onPause() {
        super.onPause()
        PurityAccessibilityService.isAppWindowActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        PurityAccessibilityService.isAppWindowActive = false
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Toast.makeText(this, "Select 'Purity Lock Active Protection' from the list and enable it", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open Accessibility Settings. Please open them manually.", Toast.LENGTH_LONG).show()
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, PurityAccessibilityService::class.java).flattenToString()
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return enabledServicesSetting.contains(expectedComponentName)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurityLockApp(
    isServiceEnabled: Boolean,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    
    // Core Preferences States
    var isEnabled by remember { mutableStateOf(PreferencesManager.isProtectionEnabled(context)) }
    var caseInsensitive by remember { mutableStateOf(PreferencesManager.isCaseInsensitive(context)) }
    var replacementText by remember { mutableStateOf(PreferencesManager.getReplacementText(context)) }
    var blockedWords by remember { mutableStateOf(PreferencesManager.getBlockedWords(context).toList()) }
    
    var newWordInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Status Determination
    val statusTitle: String
    val statusSubtitle: String
    val statusColor: Color
    val statusPulseColor: Color

    if (!isServiceEnabled) {
        statusTitle = "Service Deactivated"
        statusSubtitle = "System accessibility helper is inactive. Click to connect shielding."
        statusColor = Color(0xFFE26D5C) // Modern Crimson
        statusPulseColor = Color(0x33E26D5C)
    } else if (!isEnabled) {
        statusTitle = "Shielding Paused"
        statusSubtitle = "Purity Lock is configured but paused. Toggle active protection below."
        statusColor = Color(0xFFFFB236) // Soft Amber
        statusPulseColor = Color(0x33FFB236)
    } else {
        statusTitle = "Protection Active"
        statusSubtitle = "Silently monitoring typing to keep your experience clean and safe."
        statusColor = Color(0xFFD0BCFF) // Elegant Dark Purple Highlight
        statusPulseColor = Color(0x1AD0BCFF)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFD0BCFF), shape = RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Shield Logo",
                                    tint = Color(0xFF381E72),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Purity Lock",
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = (-0.5).sp,
                                fontSize = 20.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            
            // STATUS BANNER CARD
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("status_card"),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Elegant Dark Shield Visual Indicator (glowing layout from design specifications)
                    Box(
                        modifier = Modifier
                            .size(192.dp)
                            .border(BorderStroke(2.dp, statusColor.copy(alpha = 0.2f)), shape = CircleShape)
                            .background(statusColor.copy(alpha = 0.05f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(128.dp)
                                .background(statusColor, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isServiceEnabled && isEnabled) Icons.Default.CheckCircle else Icons.Default.Lock,
                                contentDescription = "Shield Status",
                                tint = if (statusColor == Color(0xFFD0BCFF)) Color(0xFF381E72) else Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Light,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = statusSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFC6C6CD),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    // Action Button when accessibility is inactive
                    if (!isServiceEnabled) {
                        Button(
                            onClick = onOpenSettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = statusColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("activate_service_button"),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Configure System", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Open Device Accessibility",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // MASTER ON/OFF PROTECTION TOGGLE
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2E2F33)
                ),
                border = BorderStroke(1.dp, Color(0xFF45464F).copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("protection_toggle_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Enable Protection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E2E6)
                        )
                        Text(
                            text = if (isServiceEnabled) "Accessibility Service is Running" else "Accessibility helper is inactive",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF90909A)
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            isEnabled = checked
                            PreferencesManager.setProtectionEnabled(context, checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF381E72),
                            checkedTrackColor = Color(0xFFD0BCFF),
                            uncheckedThumbColor = Color(0xFF90909A),
                            uncheckedTrackColor = Color(0xFF1A1C1E)
                        ),
                        modifier = Modifier.testTag("protection_switch")
                    )
                }
            }

            // BLOCKED WORDS CUSTOMIZER
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF232429)
                ),
                border = BorderStroke(1.dp, Color(0xFF45464F).copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Blocked Words",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E2E6)
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF45464F), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${blockedWords.size} Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2E2E6),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Word input field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newWordInput,
                            onValueChange = { newWordInput = it },
                            placeholder = { Text("Add custom blocked word", color = Color(0xFF90909A)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_word_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1A1C1E),
                                unfocusedContainerColor = Color(0xFF1A1C1E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF45464F)
                            )
                        )
                        IconButton(
                            onClick = {
                                if (newWordInput.trim().isNotEmpty()) {
                                    val added = PreferencesManager.addBlockedWord(context, newWordInput)
                                    if (added) {
                                        blockedWords = PreferencesManager.getBlockedWords(context).toList()
                                        newWordInput = ""
                                        Toast.makeText(context, "Added custom filter", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Already exists/Invalid word", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .background(Color(0xFFD0BCFF), shape = RoundedCornerShape(12.dp))
                                .size(48.dp)
                                .testTag("add_word_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Word",
                                tint = Color(0xFF381E72)
                            )
                        }
                    }

                    // Horizontal scrolling list of blocked words
                    if (blockedWords.isEmpty()) {
                        Text(
                            text = "No words blocked. Purity Lock will check nothing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF90909A),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            items(blockedWords) { word ->
                                Card(
                                    shape = RoundedCornerShape(99.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF1A1C1E)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF45464F))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = word,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Normal,
                                            color = Color(0xFFC6C6CD)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Remove Word",
                                            tint = Color(0xFF90909A),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    PreferencesManager.removeBlockedWord(context, word)
                                                    blockedWords = PreferencesManager.getBlockedWords(context).toList()
                                                    Toast.makeText(context, "Removed '$word'", Toast.LENGTH_SHORT).show()
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // EXTRA FEATURE OPTIONS
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF232429)
                ),
                border = BorderStroke(1.dp, Color(0xFF45464F).copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Extraction & Filter Tuning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E2E6)
                    )

                    // Case Sensitiveness Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Case-Insensitive Checking",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE2E2E6)
                            )
                            Text(
                                text = "Detects words regardless of upper or lower case structure.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF90909A)
                            )
                        }
                        Switch(
                            checked = caseInsensitive,
                            onCheckedChange = { checked ->
                                caseInsensitive = checked
                                PreferencesManager.setCaseInsensitive(context, checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF381E72),
                                checkedTrackColor = Color(0xFFD0BCFF),
                                uncheckedThumbColor = Color(0xFF90909A),
                                uncheckedTrackColor = Color(0xFF1A1C1E)
                            ),
                            modifier = Modifier.testTag("case_switch")
                        )
                    }

                    HorizontalDivider(color = Color(0xFF45464F).copy(alpha = 0.3f))

                    // Custom Replacement Text field
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Filter Mode Replacement Character(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E2E6)
                        )
                        Text(
                            text = "By default, the word is completely erased (blank text). You can specify alternative masking characters below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF90909A)
                        )
                        OutlinedTextField(
                            value = replacementText,
                            onValueChange = { 
                                replacementText = it
                                PreferencesManager.setReplacementText(context, it)
                            },
                            placeholder = { Text("E.g., **** or [Filtered]", color = Color(0xFF90909A)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("replacement_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1A1C1E),
                                unfocusedContainerColor = Color(0xFF1A1C1E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF45464F)
                            )
                        )
                    }
                    
                    HorizontalDivider(color = Color(0xFF45464F).copy(alpha = 0.3f))

                    // Reset Button
                    Button(
                        onClick = {
                            PreferencesManager.resetToDefaults(context)
                            isEnabled = PreferencesManager.isProtectionEnabled(context)
                            caseInsensitive = PreferencesManager.isCaseInsensitive(context)
                            replacementText = PreferencesManager.getReplacementText(context)
                            blockedWords = PreferencesManager.getBlockedWords(context).toList()
                            Toast.makeText(context, "Filters restored to clean pristine state", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("reset_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Reset Settings", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Filters to Default", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // INSTRUCTION FOOTER
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF232429).copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, Color(0xFF45464F).copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🔒 Security, Speed and Client Purity Commitment",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E2E6)
                    )
                    Text(
                        text = "Purity Lock operates 100% locally on your device. It does not demand, support, or use internet connections or remote caches, meaning no typed input is ever stored, leaked, or analyzed externally. Instant, lightning-fast correction runs under 15ms safely offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF90909A),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
