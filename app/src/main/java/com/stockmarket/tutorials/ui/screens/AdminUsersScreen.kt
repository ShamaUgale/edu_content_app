package com.stockmarket.tutorials.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stockmarket.tutorials.data.model.User
import com.stockmarket.tutorials.data.model.UserRole
import com.stockmarket.tutorials.ui.theme.*
import com.stockmarket.tutorials.ui.viewmodel.AdminViewModel
import com.stockmarket.tutorials.ui.viewmodel.OperationStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Admin screen for managing users.
 * Lists all users with options to edit, deactivate, and unbind devices.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    adminViewModel: AdminViewModel,
    onAddUser: () -> Unit,
    onBack: () -> Unit
) {
    val users by adminViewModel.users.collectAsState()
    val isLoading by adminViewModel.isLoadingUsers.collectAsState()
    val operationStatus by adminViewModel.operationStatus.collectAsState()

    var showDeactivateDialog by remember { mutableStateOf<User?>(null) }
    var showUnbindDialog by remember { mutableStateOf<User?>(null) }

    // Show snackbar on operation status changes
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(operationStatus) {
        when (operationStatus) {
            is OperationStatus.Success -> {
                snackbarHostState.showSnackbar(
                    (operationStatus as OperationStatus.Success).message
                )
                adminViewModel.clearOperationStatus()
            }
            is OperationStatus.Error -> {
                snackbarHostState.showSnackbar(
                    (operationStatus as OperationStatus.Error).message
                )
                adminViewModel.clearOperationStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Manage Users",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${users.size} users",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddUser) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Add User",
                            tint = Green400
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddUser,
                containerColor = Green600,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add User")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Green500)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(users, key = { it.uid }) { user ->
                    UserCard(
                        user = user,
                        onDeactivate = { showDeactivateDialog = user },
                        onUnbindDevice = { showUnbindDialog = user }
                    )
                }
            }
        }

        // Deactivate User Dialog
        showDeactivateDialog?.let { user ->
            AlertDialog(
                onDismissRequest = { showDeactivateDialog = null },
                title = {
                    Text(
                        "Deactivate User",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("Are you sure you want to deactivate ${user.displayName} (${user.email})?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            adminViewModel.deactivateUser(user.uid)
                            showDeactivateDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = RedBearish)
                    ) {
                        Text("Deactivate")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeactivateDialog = null }) {
                        Text("Cancel")
                    }
                },
                containerColor = DarkCard,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary
            )
        }

        // Unbind Device Dialog
        showUnbindDialog?.let { user ->
            AlertDialog(
                onDismissRequest = { showUnbindDialog = null },
                title = {
                    Text(
                        "Unbind Device",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "This will allow ${user.displayName} to log in from a new device. " +
                        "The next device they log in from will become their bound device."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            adminViewModel.unbindDevice(user.uid)
                            showUnbindDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = OrangeWarning)
                    ) {
                        Text("Unbind")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUnbindDialog = null }) {
                        Text("Cancel")
                    }
                },
                containerColor = DarkCard,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary
            )
        }
    }
}

@Composable
fun UserCard(
    user: User,
    onDeactivate: () -> Unit,
    onUnbindDevice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isActive) DarkCard else DarkCard.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (user.role == UserRole.ADMIN) GoldAccent.copy(alpha = 0.2f)
                            else Green800
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (user.role == UserRole.ADMIN) GoldAccent else Green300
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (user.isActive) TextPrimary
                                    else TextMuted
                        )
                        if (user.role == UserRole.ADMIN) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = GoldAccent.copy(alpha = 0.2f),
                                contentColor = GoldAccent
                            ) {
                                Text(
                                    "ADMIN",
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (user.isActive) GreenBullish.copy(alpha = 0.15f)
                            else RedBearish.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (user.isActive) "Active" else "Inactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (user.isActive) GreenBullish else RedBearish,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Last login
                Column {
                    Text(
                        text = "Last Login",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = if (user.lastLogin > 0) {
                            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                .format(Date(user.lastLogin))
                        } else "Never",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Device bound
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Device",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = if (user.boundDeviceId != null) "Bound" else "Unbound",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (user.boundDeviceId != null) BlueInfo else TextMuted
                    )
                }

                // Groups
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Groups",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = if (user.assignedGroups.isNotEmpty())
                            user.assignedGroups.joinToString(", ")
                        else "None",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (user.boundDeviceId != null) {
                    TextButton(
                        onClick = onUnbindDevice,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = OrangeWarning
                        )
                    ) {
                        Icon(
                            Icons.Default.PhonelinkErase,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unbind Device", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (user.isActive && user.role != UserRole.ADMIN) {
                    TextButton(
                        onClick = onDeactivate,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = RedBearish
                        )
                    ) {
                        Icon(
                            Icons.Default.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deactivate", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
