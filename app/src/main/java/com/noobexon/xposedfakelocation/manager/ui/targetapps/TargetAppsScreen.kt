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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.noobexon.xposedfakelocation.data.model.AppLocationProfile
import com.noobexon.xposedfakelocation.data.model.FavoriteLocation
import com.noobexon.xposedfakelocation.data.model.GpsNoiseLevel
import com.noobexon.xposedfakelocation.data.model.LocationTemplate
import com.noobexon.xposedfakelocation.data.model.OverrideState
import com.noobexon.xposedfakelocation.manager.ui.settings.BooleanSettingItem
import com.noobexon.xposedfakelocation.manager.ui.settings.CategoryHeader
import com.noobexon.xposedfakelocation.manager.ui.settings.DoubleSettingItem
import com.noobexon.xposedfakelocation.manager.ui.settings.FloatSettingItem
import com.noobexon.xposedfakelocation.manager.ui.settings.OverrideStateSelector

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditorDialog(
    profile: AppLocationProfile,
    appLabel: String,
    locationTemplates: List<FavoriteLocation>,
    templates: List<LocationTemplate>,
    onDismiss: () -> Unit,
    onSave: (AppLocationProfile) -> Unit
) {
    var enabled by remember(profile.packageName) { mutableStateOf(profile.enabled) }
    var latitude by remember(profile.packageName) { mutableStateOf(profile.latitude.toString()) }
    var longitude by remember(profile.packageName) { mutableStateOf(profile.longitude.toString()) }
    var useCustomAdvancedSettings by remember(profile.packageName) { mutableStateOf(profile.useCustomAdvancedSettings) }
    var randomizeOverride by remember(profile.packageName) { mutableStateOf(profile.randomizeOverride) }
    var useRandomize by remember(profile.packageName) { mutableStateOf(profile.useRandomize) }
    var randomizeRadius by remember(profile.packageName) { mutableStateOf(profile.randomizeRadius) }
    var accuracyOverride by remember(profile.packageName) { mutableStateOf(profile.accuracyOverride) }
    var useAccuracy by remember(profile.packageName) { mutableStateOf(profile.useAccuracy) }
    var accuracy by remember(profile.packageName) { mutableStateOf(profile.accuracy) }
    var altitudeOverride by remember(profile.packageName) { mutableStateOf(profile.altitudeOverride) }
    var useAltitude by remember(profile.packageName) { mutableStateOf(profile.useAltitude) }
    var altitude by remember(profile.packageName) { mutableStateOf(profile.altitude) }
    var verticalAccuracyOverride by remember(profile.packageName) { mutableStateOf(profile.verticalAccuracyOverride) }
    var useVerticalAccuracy by remember(profile.packageName) { mutableStateOf(profile.useVerticalAccuracy) }
    var verticalAccuracy by remember(profile.packageName) { mutableStateOf(profile.verticalAccuracy) }
    var meanSeaLevelOverride by remember(profile.packageName) { mutableStateOf(profile.meanSeaLevelOverride) }
    var useMeanSeaLevel by remember(profile.packageName) { mutableStateOf(profile.useMeanSeaLevel) }
    var meanSeaLevel by remember(profile.packageName) { mutableStateOf(profile.meanSeaLevel) }
    var meanSeaLevelAccuracyOverride by remember(profile.packageName) { mutableStateOf(profile.meanSeaLevelAccuracyOverride) }
    var useMeanSeaLevelAccuracy by remember(profile.packageName) { mutableStateOf(profile.useMeanSeaLevelAccuracy) }
    var meanSeaLevelAccuracy by remember(profile.packageName) { mutableStateOf(profile.meanSeaLevelAccuracy) }
    var speedOverride by remember(profile.packageName) { mutableStateOf(profile.speedOverride) }
    var useSpeed by remember(profile.packageName) { mutableStateOf(profile.useSpeed) }
    var speed by remember(profile.packageName) { mutableStateOf(profile.speed) }
    var speedAccuracyOverride by remember(profile.packageName) { mutableStateOf(profile.speedAccuracyOverride) }
    var useSpeedAccuracy by remember(profile.packageName) { mutableStateOf(profile.useSpeedAccuracy) }
    var speedAccuracy by remember(profile.packageName) { mutableStateOf(profile.speedAccuracy) }
    var gpsNoiseOverride by remember(profile.packageName) { mutableStateOf(profile.gpsNoiseOverride) }
    var useGpsNoise by remember(profile.packageName) { mutableStateOf(profile.useGpsNoise) }
    var gpsNoiseLevel by remember(profile.packageName) { mutableStateOf(profile.gpsNoiseLevel) }
    var selectedTemplateId by remember(profile.packageName) { mutableStateOf(profile.templateId) }
    var useCustom by remember(profile.packageName) {
        mutableStateOf(profile.useCustomLocation)
    }
    var templateMenuExpanded by remember(profile.packageName) { mutableStateOf(false) }

    val parsedLatitude = latitude.toDoubleOrNull()
    val parsedLongitude = longitude.toDoubleOrNull()
    val canSave = !useCustom ||
        (parsedLatitude != null &&
            parsedLatitude in -90.0..90.0 &&
            parsedLongitude != null &&
            parsedLongitude in -180.0..180.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit $appLabel") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleRow("Enabled", enabled) { enabled = it }
                ExposedDropdownMenuBox(
                    expanded = templateMenuExpanded,
                    onExpandedChange = { templateMenuExpanded = !templateMenuExpanded }
                ) {
                    val selectedTemplate = templates.firstOrNull { it.id == selectedTemplateId }
                    OutlinedTextField(
                        value = when {
                            useCustom -> "Custom"
                            selectedTemplate != null -> selectedTemplate.name
                            else -> "Global"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Template") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = templateMenuExpanded,
                        onDismissRequest = { templateMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Global") },
                            onClick = {
                                useCustom = false
                                selectedTemplateId = null
                                templateMenuExpanded = false
                            }
                        )
                        templates.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.name) },
                                onClick = {
                                    selectedTemplateId = template.id
                                    useCustom = false
                                    latitude = template.latitude.toString()
                                    longitude = template.longitude.toString()
                                    useRandomize = template.useRandomize
                                    randomizeRadius = template.randomizeRadius
                                    useAccuracy = template.useAccuracy
                                    accuracy = template.accuracy
                                    useAltitude = template.useAltitude
                                    altitude = template.altitude
                                    useVerticalAccuracy = template.useVerticalAccuracy
                                    verticalAccuracy = template.verticalAccuracy
                                    useMeanSeaLevel = template.useMeanSeaLevel
                                    meanSeaLevel = template.meanSeaLevel
                                    useMeanSeaLevelAccuracy = template.useMeanSeaLevelAccuracy
                                    meanSeaLevelAccuracy = template.meanSeaLevelAccuracy
                                    useSpeed = template.useSpeed
                                    speed = template.speed
                                    useSpeedAccuracy = template.useSpeedAccuracy
                                    speedAccuracy = template.speedAccuracy
                                    useGpsNoise = template.useGpsNoise
                                    gpsNoiseLevel = template.gpsNoiseLevel
                                    templateMenuExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Custom") },
                            onClick = {
                                useCustom = true
                                selectedTemplateId = null
                                templateMenuExpanded = false
                            }
                        )
                    }
                }
                if (useCustom) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        isError = parsedLatitude == null || parsedLatitude !in -90.0..90.0,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        isError = parsedLongitude == null || parsedLongitude !in -180.0..180.0,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                BooleanSettingItem(
                    title = "Advanced",
                    description = "Override advanced settings for this app",
                    checked = useCustomAdvancedSettings,
                    onCheckedChange = { useCustomAdvancedSettings = it }
                )
                if (useCustomAdvancedSettings) {
                        CategoryHeader("Location")
                        OverrideStateSelector("Randomize Nearby Location", randomizeOverride) { randomizeOverride = it; useRandomize = it == OverrideState.ENABLED }
                        if (randomizeOverride == OverrideState.ENABLED) {
                        DoubleSettingItem(
                            title = "Randomize Nearby Location",
                            description = "Randomly places your location within the specified radius",
                            useValue = true,
                            onUseValueChange = { useRandomize = it },
                            value = randomizeRadius,
                            onValueChange = { randomizeRadius = it },
                            label = "Randomization Radius",
                            unit = "m",
                            minValue = 0f,
                            maxValue = 2000f,
                            step = 0.1f
                        )
                        }
                        OverrideStateSelector("GPS noise", gpsNoiseOverride) { gpsNoiseOverride = it; useGpsNoise = it == OverrideState.ENABLED }
                        if (gpsNoiseOverride == OverrideState.ENABLED) {
                            GpsNoiseLevel.entries.forEach { level ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = gpsNoiseLevel == level,
                                        onClick = { gpsNoiseLevel = level }
                                    )
                                    Text(level.name.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            }
                        }
                        OverrideStateSelector("Custom Horizontal Accuracy", accuracyOverride) { accuracyOverride = it; useAccuracy = it == OverrideState.ENABLED }
                        if (accuracyOverride == OverrideState.ENABLED) {
                        DoubleSettingItem(
                            title = "Custom Horizontal Accuracy",
                            description = "Sets the horizontal accuracy of your location",
                            useValue = true,
                            onUseValueChange = { useAccuracy = it },
                            value = accuracy,
                            onValueChange = { accuracy = it },
                            label = "Horizontal Accuracy",
                            unit = "m",
                            minValue = 0f,
                            maxValue = 100f,
                            step = 1f
                        )
                        }
                        OverrideStateSelector("Custom Vertical Accuracy", verticalAccuracyOverride) { verticalAccuracyOverride = it; useVerticalAccuracy = it == OverrideState.ENABLED }
                        if (verticalAccuracyOverride == OverrideState.ENABLED) {
                        FloatSettingItem(
                            title = "Custom Vertical Accuracy",
                            description = "Sets the vertical accuracy of your location",
                            useValue = true,
                            onUseValueChange = { useVerticalAccuracy = it },
                            value = verticalAccuracy,
                            onValueChange = { verticalAccuracy = it },
                            label = "Vertical Accuracy",
                            unit = "m",
                            minValue = 0f,
                            maxValue = 100f,
                            step = 1f
                        )
                        }

                        CategoryHeader("Altitude")
                        OverrideStateSelector("Custom Altitude", altitudeOverride) { altitudeOverride = it; useAltitude = it == OverrideState.ENABLED }
                        if (altitudeOverride == OverrideState.ENABLED) {
                        DoubleSettingItem(
                            title = "Custom Altitude",
                            description = "Sets a custom altitude for your location",
                            useValue = true,
                            onUseValueChange = { useAltitude = it },
                            value = altitude,
                            onValueChange = { altitude = it },
                            label = "Altitude",
                            unit = "m",
                            minValue = 0f,
                            maxValue = 2000f,
                            step = 0.5f
                        )
                        }
                        OverrideStateSelector("Custom MSL", meanSeaLevelOverride) { meanSeaLevelOverride = it; useMeanSeaLevel = it == OverrideState.ENABLED }
                        if (meanSeaLevelOverride == OverrideState.ENABLED) {
                        DoubleSettingItem(
                            title = "Custom MSL",
                            description = "Sets a custom mean sea level value",
                            useValue = true,
                            onUseValueChange = { useMeanSeaLevel = it },
                            value = meanSeaLevel,
                            onValueChange = { meanSeaLevel = it },
                            label = "MSL",
                            unit = "m",
                            minValue = -400f,
                            maxValue = 2000f,
                            step = 0.5f
                        )
                        }
                        OverrideStateSelector("Custom MSL Accuracy", meanSeaLevelAccuracyOverride) { meanSeaLevelAccuracyOverride = it; useMeanSeaLevelAccuracy = it == OverrideState.ENABLED }
                        if (meanSeaLevelAccuracyOverride == OverrideState.ENABLED) {
                        FloatSettingItem(
                            title = "Custom MSL Accuracy",
                            description = "Sets the accuracy of the mean sea level value",
                            useValue = true,
                            onUseValueChange = { useMeanSeaLevelAccuracy = it },
                            value = meanSeaLevelAccuracy,
                            onValueChange = { meanSeaLevelAccuracy = it },
                            label = "MSL Accuracy",
                            unit = "m",
                            minValue = 0f,
                            maxValue = 100f,
                            step = 1f
                        )
                        }

                        CategoryHeader("Movement")
                        OverrideStateSelector("Custom Speed", speedOverride) { speedOverride = it; useSpeed = it == OverrideState.ENABLED }
                        if (speedOverride == OverrideState.ENABLED) {
                        FloatSettingItem(
                            title = "Custom Speed",
                            description = "Sets a custom speed for your location",
                            useValue = true,
                            onUseValueChange = { useSpeed = it },
                            value = speed,
                            onValueChange = { speed = it },
                            label = "Speed",
                            unit = "m/s",
                            minValue = 0f,
                            maxValue = 30f,
                            step = 0.1f
                        )
                        }
                        OverrideStateSelector("Custom Speed Accuracy", speedAccuracyOverride) { speedAccuracyOverride = it; useSpeedAccuracy = it == OverrideState.ENABLED }
                        if (speedAccuracyOverride == OverrideState.ENABLED) {
                        FloatSettingItem(
                            title = "Custom Speed Accuracy",
                            description = "Sets the accuracy of your speed value",
                            useValue = true,
                            onUseValueChange = { useSpeedAccuracy = it },
                            value = speedAccuracy,
                            onValueChange = { speedAccuracy = it },
                            label = "Speed Accuracy",
                            unit = "m/s",
                            minValue = 0f,
                            maxValue = 100f,
                            step = 1f
                        )
                        }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        profile.copy(
                            enabled = enabled,
                            templateId = selectedTemplateId,
                            useCustomLocation = useCustom,
                            useCustomAdvancedSettings = useCustomAdvancedSettings,
                            latitude = parsedLatitude ?: profile.latitude,
                            longitude = parsedLongitude ?: profile.longitude,
                            randomizeOverride = randomizeOverride,
                            useRandomize = useRandomize,
                            randomizeRadius = randomizeRadius,
                            accuracyOverride = accuracyOverride,
                            useAccuracy = useAccuracy,
                            accuracy = accuracy,
                            altitudeOverride = altitudeOverride,
                            useAltitude = useAltitude,
                            altitude = altitude,
                            verticalAccuracyOverride = verticalAccuracyOverride,
                            useVerticalAccuracy = useVerticalAccuracy,
                            verticalAccuracy = verticalAccuracy,
                            meanSeaLevelOverride = meanSeaLevelOverride,
                            useMeanSeaLevel = useMeanSeaLevel,
                            meanSeaLevel = meanSeaLevel,
                            meanSeaLevelAccuracyOverride = meanSeaLevelAccuracyOverride,
                            useMeanSeaLevelAccuracy = useMeanSeaLevelAccuracy,
                            meanSeaLevelAccuracy = meanSeaLevelAccuracy,
                            speedOverride = speedOverride,
                            useSpeed = useSpeed,
                            speed = speed,
                            speedAccuracyOverride = speedAccuracyOverride,
                            useSpeedAccuracy = useSpeedAccuracy,
                            speedAccuracy = speedAccuracy,
                            gpsNoiseOverride = gpsNoiseOverride,
                            useGpsNoise = useGpsNoise,
                            gpsNoiseLevel = gpsNoiseLevel
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ProfileNumberSetting(
    label: String,
    unit: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column {
        ToggleRow(label, enabled, onEnabledChange)
        if (enabled) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("$label ($unit)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
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
