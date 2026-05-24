package com.noobexon.xposedfakelocation.manager.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
fun TemplatesScreen(
    navController: NavController,
    viewModel: TemplatesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    uiState.editingTemplate?.let { template ->
        TemplateEditorDialog(
            template = template,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::saveTemplate
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Templates") },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::addTemplate) {
                Icon(Icons.Default.Add, contentDescription = "Add template")
            }
        }
    ) { innerPadding ->
        if (uiState.templates.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No templates yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(uiState.templates, key = { it.id }) { template ->
                    TemplateRow(
                        template = template,
                        onEdit = { viewModel.editTemplate(template) },
                        onDelete = { viewModel.deleteTemplate(template) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun TemplateRow(
    template: LocationTemplate,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Lat: ${template.latitude}, Lon: ${template.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${template.name}")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${template.name}")
            }
        }
    }
}

@Composable
private fun TemplateEditorDialog(
    template: LocationTemplate,
    onDismiss: () -> Unit,
    onSave: (LocationTemplate) -> Unit
) {
    var name by remember(template.id) { mutableStateOf(template.name) }
    var latitude by remember(template.id) { mutableStateOf(template.latitude.toString()) }
    var longitude by remember(template.id) { mutableStateOf(template.longitude.toString()) }
    var randomizeOverride by remember(template.id) { mutableStateOf(template.randomizeOverride) }
    var useRandomize by remember(template.id) { mutableStateOf(template.useRandomize) }
    var randomizeRadius by remember(template.id) { mutableStateOf(template.randomizeRadius) }
    var accuracyOverride by remember(template.id) { mutableStateOf(template.accuracyOverride) }
    var useAccuracy by remember(template.id) { mutableStateOf(template.useAccuracy) }
    var accuracy by remember(template.id) { mutableStateOf(template.accuracy) }
    var altitudeOverride by remember(template.id) { mutableStateOf(template.altitudeOverride) }
    var useAltitude by remember(template.id) { mutableStateOf(template.useAltitude) }
    var altitude by remember(template.id) { mutableStateOf(template.altitude) }
    var verticalAccuracyOverride by remember(template.id) { mutableStateOf(template.verticalAccuracyOverride) }
    var useVerticalAccuracy by remember(template.id) { mutableStateOf(template.useVerticalAccuracy) }
    var verticalAccuracy by remember(template.id) { mutableStateOf(template.verticalAccuracy) }
    var meanSeaLevelOverride by remember(template.id) { mutableStateOf(template.meanSeaLevelOverride) }
    var useMeanSeaLevel by remember(template.id) { mutableStateOf(template.useMeanSeaLevel) }
    var meanSeaLevel by remember(template.id) { mutableStateOf(template.meanSeaLevel) }
    var meanSeaLevelAccuracyOverride by remember(template.id) { mutableStateOf(template.meanSeaLevelAccuracyOverride) }
    var useMeanSeaLevelAccuracy by remember(template.id) { mutableStateOf(template.useMeanSeaLevelAccuracy) }
    var meanSeaLevelAccuracy by remember(template.id) { mutableStateOf(template.meanSeaLevelAccuracy) }
    var speedOverride by remember(template.id) { mutableStateOf(template.speedOverride) }
    var useSpeed by remember(template.id) { mutableStateOf(template.useSpeed) }
    var speed by remember(template.id) { mutableStateOf(template.speed) }
    var speedAccuracyOverride by remember(template.id) { mutableStateOf(template.speedAccuracyOverride) }
    var useSpeedAccuracy by remember(template.id) { mutableStateOf(template.useSpeedAccuracy) }
    var speedAccuracy by remember(template.id) { mutableStateOf(template.speedAccuracy) }
    var gpsNoiseOverride by remember(template.id) { mutableStateOf(template.gpsNoiseOverride) }
    var useGpsNoise by remember(template.id) { mutableStateOf(template.useGpsNoise) }
    var gpsNoiseLevel by remember(template.id) { mutableStateOf(template.gpsNoiseLevel) }

    val parsedLatitude = latitude.toDoubleOrNull()
    val parsedLongitude = longitude.toDoubleOrNull()
    val canSave = name.isNotBlank() &&
        parsedLatitude != null &&
        parsedLatitude in -90.0..90.0 &&
        parsedLongitude != null &&
        parsedLongitude in -180.0..180.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Template") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = latitude, onValueChange = { latitude = it }, label = { Text("Latitude") }, singleLine = true, isError = parsedLatitude == null || parsedLatitude !in -90.0..90.0, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = longitude, onValueChange = { longitude = it }, label = { Text("Longitude") }, singleLine = true, isError = parsedLongitude == null || parsedLongitude !in -180.0..180.0, modifier = Modifier.fillMaxWidth())
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
                            RadioButton(selected = gpsNoiseLevel == level, onClick = { gpsNoiseLevel = level })
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
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        template.copy(
                            name = name.trim(),
                            latitude = parsedLatitude ?: template.latitude,
                            longitude = parsedLongitude ?: template.longitude,
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
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun TemplateNumberSetting(
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
            OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text("$label ($unit)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
    }
}
