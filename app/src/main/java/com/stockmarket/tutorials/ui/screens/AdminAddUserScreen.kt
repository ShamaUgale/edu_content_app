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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.stockmarket.tutorials.data.model.UserRole
import com.stockmarket.tutorials.ui.theme.*
import com.stockmarket.tutorials.ui.viewmodel.AdminViewModel
import com.stockmarket.tutorials.ui.viewmodel.OperationStatus

/**
 * Admin screen for adding a new user.
 * Only admins can create user accounts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddUserScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.USER) }
    var groups by remember { mutableStateOf("") }
    var roleExpanded by remember { mutableStateOf(false) }

    val operationStatus by adminViewModel.operationStatus.collectAsState()

    // Navigate back on success
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
                        text = "Add New User",
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
                        text = "Create credentials for a new user. Share the login details securely with the user.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BlueInfo
                    )
                }
            }

            // Display Name
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = adminTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = adminTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                colors = adminTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        "Minimum 8 characters. Share this securely with the user.",
                        color = TextMuted
                    )
                }
            )

            // Role Selector
            ExposedDropdownMenuBox(
                expanded = roleExpanded,
                onExpandedChange = { roleExpanded = !roleExpanded }
            ) {
                OutlinedTextField(
                    value = selectedRole.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Role") },
                    leadingIcon = {
                        Icon(
                            if (selectedRole == UserRole.ADMIN)
                                Icons.Default.AdminPanelSettings
                            else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (selectedRole == UserRole.ADMIN)
                                GoldAccent else TextMuted
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded)
                    },
                    colors = adminTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = roleExpanded,
                    onDismissRequest = { roleExpanded = false }
                ) {
                    UserRole.entries.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.name) },
                            onClick = {
                                selectedRole = role
                                roleExpanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (role == UserRole.ADMIN)
                                        Icons.Default.AdminPanelSettings
                                    else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (role == UserRole.ADMIN)
                                        GoldAccent else TextMuted
                                )
                            }
                        )
                    }
                }
            }

            // Groups
            OutlinedTextField(
                value = groups,
                onValueChange = { groups = it },
                label = { Text("Groups (comma-separated)") },
                leadingIcon = {
                    Icon(Icons.Default.Group, contentDescription = null, tint = TextMuted)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = adminTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        "e.g., premium, options_group, beginners",
                        color = TextMuted
                    )
                },
                placeholder = { Text("premium, advanced", color = TextMuted.copy(alpha = 0.5f)) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
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

            // Create Button
            Button(
                onClick = {
                    val groupsList = groups.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    adminViewModel.createUser(
                        email = email,
                        password = password,
                        displayName = displayName,
                        role = selectedRole,
                        groups = groupsList
                    )
                },
                enabled = email.isNotBlank() && password.length >= 8 &&
                          displayName.isNotBlank() &&
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
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Create User",
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
private fun adminTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Green500,
    unfocusedBorderColor = DarkBorder,
    focusedLabelColor = Green500,
    unfocusedLabelColor = TextMuted,
    cursorColor = Green500,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
