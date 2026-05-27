package com.noobexon.xposedfakelocation.manager.ui.targetapps

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.noobexon.xposedfakelocation.manager.ui.targetapps.components.ProfileEditorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetAppsScreen(
    navController: NavController,
    viewModel: TargetAppsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val editingProfile = uiState.editingPackageName?.let { uiState.profiles[it] }

    if (editingProfile != null) {
        ProfileEditorDialog(
            profile = editingProfile,
            appLabel = uiState.apps.firstOrNull { it.packageName == editingProfile.packageName }?.label
                ?: editingProfile.packageName,
            locationTemplates = uiState.locationTemplates,
            templates = uiState.templates,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::saveProfile
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Target Apps") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))


            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                label = { Text("Search apps or package names") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${uiState.selectedPackages.size} selected",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.filteredApps, key = { it.packageName }) { app ->
                        TargetAppRow(
                            app = app,
                            onToggle = { viewModel.toggleApp(app.packageName) },
                            onEdit = { viewModel.editApp(app.packageName) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetAppRow(
    app: TargetAppItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(
                packageName = app.packageName,
                label = app.label
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Checkbox(
                checked = app.isSelected,
                onCheckedChange = { onToggle() }
            )

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${app.label} profile")
            }
        }
    }
}

@Composable
private fun AppIcon(
    packageName: String,
    label: String
) {
    val context = LocalContext.current
    val iconBitmap = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap()
        }.getOrNull()
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap.asImageBitmap(),
            contentDescription = "$label icon",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
    } else {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
