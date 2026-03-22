package com.stockmarket.tutorials.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stockmarket.tutorials.data.model.AccessType
import com.stockmarket.tutorials.data.model.VideoCategory
import com.stockmarket.tutorials.ui.theme.*
import com.stockmarket.tutorials.ui.viewmodel.AdminViewModel
import com.stockmarket.tutorials.ui.viewmodel.OperationStatus

/**
 * Admin screen for adding a new video tutorial.
 * Supports setting metadata, category, access type, and assignments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddVideoScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var videoStoragePath by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(VideoCategory.BASICS) }
    var selectedAccessType by remember { mutableStateOf(AccessType.ASSIGNED) }
    var assignedUserIds by remember { mutableStateOf("") }
    var assignedGroups by remember { mutableStateOf("") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var accessTypeExpanded by remember { mutableStateOf(false) }

    val operationStatus by adminViewModel.operationStatus.collectAsState()

    LaunchedEffect(operationStatus) {
        if (operationStatus is OperationStatus.Success) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Video",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = BlueInfo.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = BlueInfo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Upload the video file to Firebase Storage first, then enter the storage path here to register the video.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BlueInfo
                    )
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Video Title") },
                leadingIcon = {
                    Icon(Icons.Default.Title, contentDescription = null, tint = TextMuted)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = videoTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                leadingIcon = {
                    Icon(Icons.Default.Description, contentDescription = null, tint = TextMuted)
                },
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = videoTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Video Storage Path
            OutlinedTextField(
                value = videoStoragePath,
                onValueChange = { videoStoragePath = it },
                label = { Text("Firebase Storage Path") },
                leadingIcon = {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = TextMuted)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = videoTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        "e.g., videos/options_trading_basics.mp4",
                        color = TextMuted
                    )
                },
                placeholder = {
                    Text("videos/filename.mp4", color = TextMuted.copy(alpha = 0.5f))
                }
            )

            // Category Selector
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = {
                        Icon(
                            getCategoryIcon(selectedCategory),
                            contentDescription = null,
                            tint = Green400
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    colors = videoTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    VideoCategory.all().forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                selectedCategory = category
                                categoryExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    getCategoryIcon(category),
                                    contentDescription = null,
                                    tint = Green400
                                )
                            }
                        )
                    }
                }
            }

            // Access Type Selector
            ExposedDropdownMenuBox(
                expanded = accessTypeExpanded,
                onExpandedChange = { accessTypeExpanded = !accessTypeExpanded }
            ) {
                OutlinedTextField(
                    value = selectedAccessType.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Access Type") },
                    leadingIcon = {
                        Icon(
                            when (selectedAccessType) {
                                AccessType.PUBLIC -> Icons.Default.Public
                                AccessType.ASSIGNED -> Icons.Default.Lock
                                AccessType.ADMIN_ONLY -> Icons.Default.AdminPanelSettings
                            },
                            contentDescription = null,
                            tint = when (selectedAccessType) {
                                AccessType.PUBLIC -> BlueInfo
                                AccessType.ASSIGNED -> OrangeWarning
                                AccessType.ADMIN_ONLY -> GoldAccent
                            }
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = accessTypeExpanded)
                    },
                    colors = videoTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = accessTypeExpanded,
                    onDismissRequest = { accessTypeExpanded = false }
                ) {
                    AccessType.entries.forEach { accessType ->
                        DropdownMenuItem(
                            text = { Text(accessType.name) },
                            onClick = {
                                selectedAccessType = accessType
                                accessTypeExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    when (accessType) {
                                        AccessType.PUBLIC -> Icons.Default.Public
                                        AccessType.ASSIGNED -> Icons.Default.Lock
                                        AccessType.ADMIN_ONLY -> Icons.Default.AdminPanelSettings
                                    },
                                    contentDescription = null,
                                    tint = when (accessType) {
                                        AccessType.PUBLIC -> BlueInfo
                                        AccessType.ASSIGNED -> OrangeWarning
                                        AccessType.ADMIN_ONLY -> GoldAccent
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Assigned User IDs (only for ASSIGNED access type)
            if (selectedAccessType == AccessType.ASSIGNED) {
                OutlinedTextField(
                    value = assignedUserIds,
                    onValueChange = { assignedUserIds = it },
                    label = { Text("Assigned User IDs (comma-separated)") },
                    leadingIcon = {
                        Icon(Icons.Default.People, contentDescription = null, tint = TextMuted)
                    },
                    singleLine = true,
                    colors = videoTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Enter Firebase UIDs of users who should access this video", color = TextMuted)
                    }
                )

                OutlinedTextField(
                    value = assignedGroups,
                    onValueChange = { assignedGroups = it },
                    label = { Text("Assigned Groups (comma-separated)") },
                    leadingIcon = {
                        Icon(Icons.Default.Group, contentDescription = null, tint = TextMuted)
                    },
                    singleLine = true,
                    colors = videoTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("e.g., premium, options_group", color = TextMuted)
                    },
                    placeholder = {
                        Text("premium, advanced", color = TextMuted.copy(alpha = 0.5f))
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error
            if (operationStatus is OperationStatus.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RedBearish.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = RedBearish,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (operationStatus as OperationStatus.Error).message,
                            color = RedBearish,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Add Video Button
            Button(
                onClick = {
                    val userIds = assignedUserIds.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    val groups = assignedGroups.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    adminViewModel.addVideo(
                        title = title,
                        description = description,
                        category = selectedCategory,
                        videoStoragePath = videoStoragePath,
                        accessType = selectedAccessType,
                        assignedUserIds = userIds,
                        assignedGroups = groups
                    )
                },
                enabled = title.isNotBlank() && videoStoragePath.isNotBlank() &&
                          operationStatus !is OperationStatus.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green600,
                    disabledContainerColor = Green900.copy(alpha = 0.5f)
                )
            ) {
                if (operationStatus is OperationStatus.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TextPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.VideoCall, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Add Video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun videoTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Green500,
    unfocusedBorderColor = DarkBorder,
    focusedLabelColor = Green500,
    unfocusedLabelColor = TextMuted,
    cursorColor = Green500,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
