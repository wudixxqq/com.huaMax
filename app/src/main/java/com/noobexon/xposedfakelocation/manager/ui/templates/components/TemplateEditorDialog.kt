package com.noobexon.xposedfakelocation.manager.ui.templates.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noobexon.xposedfakelocation.R
import com.noobexon.xposedfakelocation.data.model.GpsNoiseLevel
import com.noobexon.xposedfakelocation.data.model.LocationTemplate
import com.noobexon.xposedfakelocation.data.model.OverrideState
import com.noobexon.xposedfakelocation.manager.ui.settings.CategoryHeader
import com.noobexon.xposedfakelocation.manager.ui.settings.DoubleSettingItem
import com.noobexon.xposedfakelocation.manager.ui.settings.FloatSettingItem
import com.noobexon.xposedfakelocation.manager.ui.settings.OverrideStateSelector
import com.noobexon.xposedfakelocation.manager.ui.settings.gpsNoiseLevelLabel

@Composable
fun TemplateEditorDialog(
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
        title = { Text(stringResource(R.string.template_title)) },
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
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text(stringResource(R.string.field_latitude)) },
                    singleLine = true,
                    isError = parsedLatitude == null || parsedLatitude !in -90.0..90.0,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text(stringResource(R.string.field_longitude)) },
                    singleLine = true,
                    isError = parsedLongitude == null || parsedLongitude !in -180.0..180.0,
                    modifier = Modifier.fillMaxWidth()
                )

                CategoryHeader(stringResource(R.string.category_location))
                OverrideStateSelector(stringResource(R.string.setting_randomize_title), randomizeOverride) {
                    randomizeOverride = it
                    useRandomize = it == OverrideState.ENABLED
                }
                if (randomizeOverride == OverrideState.ENABLED) {
                    DoubleSettingItem(
                        title = stringResource(R.string.setting_randomize_title),
                        description = stringResource(R.string.setting_randomize_description),
                        useValue = true,
                        onUseValueChange = { useRandomize = it },
                        value = randomizeRadius,
                        onValueChange = { randomizeRadius = it },
                        label = stringResource(R.string.setting_randomize_radius_label),
                        unit = "m",
                        minValue = 0f,
                        maxValue = 2000f,
                        step = 0.1f
                    )
                }

                OverrideStateSelector(stringResource(R.string.setting_gps_noise_title), gpsNoiseOverride) {
                    gpsNoiseOverride = it
                    useGpsNoise = it == OverrideState.ENABLED
                }
                if (gpsNoiseOverride == OverrideState.ENABLED) {
                    GpsNoiseLevel.entries.forEach { level ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = gpsNoiseLevel == level, onClick = { gpsNoiseLevel = level })
                            Text(gpsNoiseLevelLabel(level))
                        }
                    }
                }

                OverrideStateSelector(stringResource(R.string.setting_horizontal_accuracy_title), accuracyOverride) {
                    accuracyOverride = it
                    useAccuracy = it == OverrideState.ENABLED
                }
                if (accuracyOverride == OverrideState.ENABLED) {
                    DoubleSettingItem(
                        title = stringResource(R.string.setting_horizontal_accuracy_title),
                        description = stringResource(R.string.setting_horizontal_accuracy_description),
                        useValue = true,
                        onUseValueChange = { useAccuracy = it },
                        value = accuracy,
                        onValueChange = { accuracy = it },
                        label = stringResource(R.string.setting_horizontal_accuracy_label),
                        unit = "m",
                        minValue = 0f,
                        maxValue = 100f,
                        step = 1f
                    )
                }

                OverrideStateSelector(stringResource(R.string.setting_vertical_accuracy_title), verticalAccuracyOverride) {
                    verticalAccuracyOverride = it
                    useVerticalAccuracy = it == OverrideState.ENABLED
                }
                if (verticalAccuracyOverride == OverrideState.ENABLED) {
                    FloatSettingItem(
                        title = stringResource(R.string.setting_vertical_accuracy_title),
                        description = stringResource(R.string.setting_vertical_accuracy_description),
                        useValue = true,
                        onUseValueChange = { useVerticalAccuracy = it },
                        value = verticalAccuracy,
                        onValueChange = { verticalAccuracy = it },
                        label = stringResource(R.string.setting_vertical_accuracy_label),
                        unit = "m",
                        minValue = 0f,
                        maxValue = 100f,
                        step = 1f
                    )
                }

                CategoryHeader(stringResource(R.string.category_altitude))
                OverrideStateSelector(stringResource(R.string.setting_altitude_title), altitudeOverride) {
                    altitudeOverride = it
                    useAltitude = it == OverrideState.ENABLED
                }
                if (altitudeOverride == OverrideState.ENABLED) {
                    DoubleSettingItem(
                        title = stringResource(R.string.setting_altitude_title),
                        description = stringResource(R.string.setting_altitude_description),
                        useValue = true,
                        onUseValueChange = { useAltitude = it },
                        value = altitude,
                        onValueChange = { altitude = it },
                        label = stringResource(R.string.setting_altitude_label),
                        unit = "m",
                        minValue = 0f,
                        maxValue = 2000f,
                        step = 0.5f
                    )
                }

                OverrideStateSelector(stringResource(R.string.setting_msl_title), meanSeaLevelOverride) {
                    meanSeaLevelOverride = it
                    useMeanSeaLevel = it == OverrideState.ENABLED
                }
                if (meanSeaLevelOverride == OverrideState.ENABLED) {
                    DoubleSettingItem(
                        title = stringResource(R.string.setting_msl_title),
                        description = stringResource(R.string.setting_msl_description),
                        useValue = true,
                        onUseValueChange = { useMeanSeaLevel = it },
                        value = meanSeaLevel,
                        onValueChange = { meanSeaLevel = it },
                        label = stringResource(R.string.setting_msl_label),
                        unit = "m",
                        minValue = -400f,
                        maxValue = 2000f,
                        step = 0.5f
                    )
                }

                OverrideStateSelector(stringResource(R.string.setting_msl_accuracy_title), meanSeaLevelAccuracyOverride) {
                    meanSeaLevelAccuracyOverride = it
                    useMeanSeaLevelAccuracy = it == OverrideState.ENABLED
                }
                if (meanSeaLevelAccuracyOverride == OverrideState.ENABLED) {
                    FloatSettingItem(
                        title = stringResource(R.string.setting_msl_accuracy_title),
                        description = stringResource(R.string.setting_msl_accuracy_description),
                        useValue = true,
                        onUseValueChange = { useMeanSeaLevelAccuracy = it },
                        value = meanSeaLevelAccuracy,
                        onValueChange = { meanSeaLevelAccuracy = it },
                        label = stringResource(R.string.setting_msl_accuracy_label),
                        unit = "m",
                        minValue = 0f,
                        maxValue = 100f,
                        step = 1f
                    )
                }

                CategoryHeader(stringResource(R.string.category_movement))
                OverrideStateSelector(stringResource(R.string.setting_speed_title), speedOverride) {
                    speedOverride = it
                    useSpeed = it == OverrideState.ENABLED
                }
                if (speedOverride == OverrideState.ENABLED) {
                    FloatSettingItem(
                        title = stringResource(R.string.setting_speed_title),
                        description = stringResource(R.string.setting_speed_description),
                        useValue = true,
                        onUseValueChange = { useSpeed = it },
                        value = speed,
                        onValueChange = { speed = it },
                        label = stringResource(R.string.setting_speed_label),
                        unit = "m/s",
                        minValue = 0f,
                        maxValue = 30f,
                        step = 0.1f
                    )
                }

                OverrideStateSelector(stringResource(R.string.setting_speed_accuracy_title), speedAccuracyOverride) {
                    speedAccuracyOverride = it
                    useSpeedAccuracy = it == OverrideState.ENABLED
                }
                if (speedAccuracyOverride == OverrideState.ENABLED) {
                    FloatSettingItem(
                        title = stringResource(R.string.setting_speed_accuracy_title),
                        description = stringResource(R.string.setting_speed_accuracy_description),
                        useValue = true,
                        onUseValueChange = { useSpeedAccuracy = it },
                        value = speedAccuracy,
                        onValueChange = { speedAccuracy = it },
                        label = stringResource(R.string.setting_speed_accuracy_label),
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
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
