package com.noobexon.xposedfakelocation.manager.ui.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noobexon.xposedfakelocation.data.model.GpsNoiseLevel
import com.noobexon.xposedfakelocation.data.model.LocationTemplate
import com.noobexon.xposedfakelocation.data.model.OverrideState
import com.noobexon.xposedfakelocation.manager.ui.map.MapViewModel
import com.noobexon.xposedfakelocation.manager.ui.settings.BooleanSettingItem
import com.noobexon.xposedfakelocation.manager.ui.settings.CategoryHeader
import com.noobexon.xposedfakelocation.manager.ui.settings.DoubleSettingItem
import com.noobexon.xposedfakelocation.manager.ui.settings.FloatSettingItem
import com.noobexon.xposedfakelocation.manager.ui.settings.OverrideStateSelector

@Composable
fun AddToTemplateDialog(
    mapViewModel: MapViewModel,
    onDismissRequest: () -> Unit,
    onAddTemplate: (LocationTemplate) -> Unit
) {
    val uiState by mapViewModel.uiState.collectAsStateWithLifecycle()
    val draft = uiState.addToTemplateDraft ?: return

    var advanced by remember(draft.id) { mutableStateOf(false) }
    var name by remember(draft.id) { mutableStateOf("") }
    var randomizeOverride by remember(draft.id) { mutableStateOf(draft.randomizeOverride) }
    var useRandomize by remember(draft.id) { mutableStateOf(draft.useRandomize) }
    var randomizeRadius by remember(draft.id) { mutableStateOf(draft.randomizeRadius) }
    var gpsNoiseOverride by remember(draft.id) { mutableStateOf(draft.gpsNoiseOverride) }
    var useGpsNoise by remember(draft.id) { mutableStateOf(draft.useGpsNoise) }
    var gpsNoiseLevel by remember(draft.id) { mutableStateOf(draft.gpsNoiseLevel) }
    var accuracyOverride by remember(draft.id) { mutableStateOf(draft.accuracyOverride) }
    var useAccuracy by remember(draft.id) { mutableStateOf(draft.useAccuracy) }
    var accuracy by remember(draft.id) { mutableStateOf(draft.accuracy) }
    var verticalAccuracyOverride by remember(draft.id) { mutableStateOf(draft.verticalAccuracyOverride) }
    var useVerticalAccuracy by remember(draft.id) { mutableStateOf(draft.useVerticalAccuracy) }
    var verticalAccuracy by remember(draft.id) { mutableStateOf(draft.verticalAccuracy) }
    var altitudeOverride by remember(draft.id) { mutableStateOf(draft.altitudeOverride) }
    var useAltitude by remember(draft.id) { mutableStateOf(draft.useAltitude) }
    var altitude by remember(draft.id) { mutableStateOf(draft.altitude) }
    var meanSeaLevelOverride by remember(draft.id) { mutableStateOf(draft.meanSeaLevelOverride) }
    var useMeanSeaLevel by remember(draft.id) { mutableStateOf(draft.useMeanSeaLevel) }
    var meanSeaLevel by remember(draft.id) { mutableStateOf(draft.meanSeaLevel) }
    var meanSeaLevelAccuracyOverride by remember(draft.id) { mutableStateOf(draft.meanSeaLevelAccuracyOverride) }
    var useMeanSeaLevelAccuracy by remember(draft.id) { mutableStateOf(draft.useMeanSeaLevelAccuracy) }
    var meanSeaLevelAccuracy by remember(draft.id) { mutableStateOf(draft.meanSeaLevelAccuracy) }
    var speedOverride by remember(draft.id) { mutableStateOf(draft.speedOverride) }
    var useSpeed by remember(draft.id) { mutableStateOf(draft.useSpeed) }
    var speed by remember(draft.id) { mutableStateOf(draft.speed) }
    var speedAccuracyOverride by remember(draft.id) { mutableStateOf(draft.speedAccuracyOverride) }
    var useSpeedAccuracy by remember(draft.id) { mutableStateOf(draft.useSpeedAccuracy) }
    var speedAccuracy by remember(draft.id) { mutableStateOf(draft.speedAccuracy) }

    val nameError = name.isBlank()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add to Templates") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError
                )
                if (nameError) {
                    Text(
                        text = "Please provide a name",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                Text(
                    text = "Lat: ${draft.latitude}, Lon: ${draft.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                BooleanSettingItem(
                    title = "Advanced",
                    description = "Override the current global settings before saving this template",
                    checked = advanced,
                    onCheckedChange = { advanced = it }
                )
                if (advanced) {
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
            }
        },
        confirmButton = {
            TextButton(
                enabled = !nameError,
                onClick = {
                    val template = if (advanced) {
                        draft.copy(
                            name = name.trim(),
                            randomizeOverride = randomizeOverride,
                            useRandomize = useRandomize,
                            randomizeRadius = randomizeRadius,
                            gpsNoiseOverride = gpsNoiseOverride,
                            useGpsNoise = useGpsNoise,
                            gpsNoiseLevel = gpsNoiseLevel,
                            accuracyOverride = accuracyOverride,
                            useAccuracy = useAccuracy,
                            accuracy = accuracy,
                            verticalAccuracyOverride = verticalAccuracyOverride,
                            useVerticalAccuracy = useVerticalAccuracy,
                            verticalAccuracy = verticalAccuracy,
                            altitudeOverride = altitudeOverride,
                            useAltitude = useAltitude,
                            altitude = altitude,
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
                            speedAccuracy = speedAccuracy
                        )
                    } else {
                        draft.copy(name = name.trim())
                    }
                    onAddTemplate(template)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
