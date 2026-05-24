//SettingsScreen.kt
package com.noobexon.xposedfakelocation.manager.ui.settings

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.noobexon.xposedfakelocation.data.model.GpsNoiseLevel
import com.noobexon.xposedfakelocation.manager.control.ControlReceiver

// Dimension constants
private object Dimensions {
    val SPACING_EXTRA_SMALL = 4.dp
    val SPACING_SMALL = 8.dp
    val SPACING_MEDIUM = 16.dp
    val SPACING_LARGE = 24.dp
    val CARD_CORNER_RADIUS = 12.dp
    val CARD_ELEVATION = 2.dp
    val CATEGORY_SPACING = 32.dp
}

// Setting definitions to reduce duplication
private object SettingDefinitions {
    // Define setting categories
    val CATEGORIES = mapOf(
        "Location" to listOf("Randomize Nearby Location", "GPS Noise", "Custom Horizontal Accuracy", "Custom Vertical Accuracy"),
        "Altitude" to listOf("Custom Altitude", "Custom MSL", "Custom MSL Accuracy"),
        "Movement" to listOf("Custom Speed", "Custom Speed Accuracy")
    )
    
    // Define all settings with their parameters
    @Composable
    fun getSettings(viewModel: SettingsViewModel): List<SettingData> = listOf(
        // Randomize Nearby Location
        DoubleSettingData(
            title = "Randomize Nearby Location",
            description = "Randomly places your location within the specified radius",
            useValueState = viewModel.useRandomize.collectAsState(),
            valueState = viewModel.randomizeRadius.collectAsState(),
            setUseValue = viewModel::setUseRandomize,
            setValue = viewModel::setRandomizeRadius,
            label = "Randomization Radius",
            unit = "m",
            minValue = 0f,
            maxValue = 2000f,
            step = 0.1f
        ),
        // Custom Horizontal Accuracy
        DoubleSettingData(
            title = "Custom Horizontal Accuracy",
            description = "Sets the horizontal accuracy of your location",
            useValueState = viewModel.useAccuracy.collectAsState(),
            valueState = viewModel.accuracy.collectAsState(),
            setUseValue = viewModel::setUseAccuracy,
            setValue = viewModel::setAccuracy,
            label = "Horizontal Accuracy",
            unit = "m",
            minValue = 0f,
            maxValue = 100f,
            step = 1f
        ),
        // Custom Vertical Accuracy
        FloatSettingData(
            title = "Custom Vertical Accuracy",
            description = "Sets the vertical accuracy of your location",
            useValueState = viewModel.useVerticalAccuracy.collectAsState(),
            valueState = viewModel.verticalAccuracy.collectAsState(),
            setUseValue = viewModel::setUseVerticalAccuracy,
            setValue = viewModel::setVerticalAccuracy,
            label = "Vertical Accuracy",
            unit = "m",
            minValue = 0f,
            maxValue = 100f,
            step = 1f
        ),
        // Custom Altitude
        DoubleSettingData(
            title = "Custom Altitude",
            description = "Sets a custom altitude for your location",
            useValueState = viewModel.useAltitude.collectAsState(),
            valueState = viewModel.altitude.collectAsState(),
            setUseValue = viewModel::setUseAltitude,
            setValue = viewModel::setAltitude,
            label = "Altitude",
            unit = "m",
            minValue = 0f,
            maxValue = 2000f,
            step = 0.5f
        ),
        // Custom MSL
        DoubleSettingData(
            title = "Custom MSL",
            description = "Sets a custom mean sea level value",
            useValueState = viewModel.useMeanSeaLevel.collectAsState(),
            valueState = viewModel.meanSeaLevel.collectAsState(),
            setUseValue = viewModel::setUseMeanSeaLevel,
            setValue = viewModel::setMeanSeaLevel,
            label = "MSL",
            unit = "m",
            minValue = -400f,
            maxValue = 2000f,
            step = 0.5f
        ),
        // Custom MSL Accuracy
        FloatSettingData(
            title = "Custom MSL Accuracy",
            description = "Sets the accuracy of the mean sea level value",
            useValueState = viewModel.useMeanSeaLevelAccuracy.collectAsState(),
            valueState = viewModel.meanSeaLevelAccuracy.collectAsState(),
            setUseValue = viewModel::setUseMeanSeaLevelAccuracy,
            setValue = viewModel::setMeanSeaLevelAccuracy,
            label = "MSL Accuracy",
            unit = "m",
            minValue = 0f,
            maxValue = 100f,
            step = 1f
        ),
        // Custom Speed
        FloatSettingData(
            title = "Custom Speed",
            description = "Sets a custom speed for your location",
            useValueState = viewModel.useSpeed.collectAsState(),
            valueState = viewModel.speed.collectAsState(),
            setUseValue = viewModel::setUseSpeed,
            setValue = viewModel::setSpeed,
            label = "Speed",
            unit = "m/s",
            minValue = 0f,
            maxValue = 30f,
            step = 0.1f
        ),
        // Custom Speed Accuracy
        FloatSettingData(
            title = "Custom Speed Accuracy",
            description = "Sets the accuracy of your speed value",
            useValueState = viewModel.useSpeedAccuracy.collectAsState(),
            valueState = viewModel.speedAccuracy.collectAsState(),
            setUseValue = viewModel::setUseSpeedAccuracy,
            setValue = viewModel::setSpeedAccuracy,
            label = "Speed Accuracy",
            unit = "m/s",
            minValue = 0f,
            maxValue = 100f,
            step = 1f
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = viewModel ()
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showRebootDialog by remember { mutableStateOf(false) }

    // Get settings from the definition object
    val allSettings = SettingDefinitions.getSettings(settingsViewModel)

    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = { Text("Reboot required") },
            text = {
                Text(
                    "You just enabled 'Use built-in target app selection'. The system_server and com.android.phone hooks can only be installed at boot, so a full device reboot is required for this change to take effect. Per-app hooks are unaffected and pick up changes on the next app launch."
                )
            },
            confirmButton = {
                TextButton(onClick = { showRebootDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { focusManager.clearFocus() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimensions.SPACING_MEDIUM)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(Dimensions.SPACING_MEDIUM))

                CategoryHeader("Notifications")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.SPACING_SMALL),
                    shape = RoundedCornerShape(Dimensions.CARD_CORNER_RADIUS),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.CARD_ELEVATION)
                ) {
                    Column(modifier = Modifier.padding(Dimensions.SPACING_SMALL)) {
                        BooleanSettingItem(
                            title = "Hide 'Fake location active' toast",
                            description = "Suppresses the toast shown when the module activates in a target app",
                            checked = settingsViewModel.hideFakeLocationToast.collectAsState().value,
                            onCheckedChange = settingsViewModel::setHideFakeLocationToast
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Dimensions.SPACING_MEDIUM))

                CategoryHeader("Target Apps")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.SPACING_SMALL),
                    shape = RoundedCornerShape(Dimensions.CARD_CORNER_RADIUS),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.CARD_ELEVATION)
                ) {
                    Column(modifier = Modifier.padding(Dimensions.SPACING_SMALL)) {
                        BooleanSettingItem(
                            title = "Use built-in target app selection",
                            description = "On (default): target apps are picked in the in-app 'Target Apps' screen; LSPosed scope only needs 'android' and 'com.android.phone'. Off: the in-app list is ignored and the system-server / phone hooks are skipped — pick target apps directly in LSPosed scope (pre-v0.0.7 behavior). A reboot is only required when enabling this option, so the system_server and com.android.phone hooks can be installed at boot. Switching back to the LSPosed-scope-only method, or any per-app target-list change, takes effect at the next app launch — no reboot needed.",
                            checked = settingsViewModel.useInAppTargetApps.collectAsState().value,
                            onCheckedChange = { newValue ->
                                settingsViewModel.setUseInAppTargetApps(newValue)
                                if (newValue) {
                                    showRebootDialog = true
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Dimensions.SPACING_MEDIUM))

                CategoryHeader("External Control")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.SPACING_SMALL),
                    shape = RoundedCornerShape(Dimensions.CARD_CORNER_RADIUS),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.CARD_ELEVATION)
                ) {
                    Column(modifier = Modifier.padding(Dimensions.SPACING_SMALL)) {
                        BooleanSettingItem(
                            title = "Allow external broadcast control",
                            description = "Off (default): blocks the BroadcastReceiver — no app or ADB shell can start/stop spoofing or set coordinates via intents. On: any installed app and 'adb shell am broadcast' can drive the module (no permission check; coordinates are clamped to valid lat/lon ranges). Only enable this if you trust every app on the device or are running on a controlled (e.g., automation) device. See docs/EXTERNAL_CONTROL.md for actions and lock-down options.",
                            checked = settingsViewModel.enableBroadcastControl.collectAsState().value,
                            onCheckedChange = { newValue ->
                                settingsViewModel.setEnableBroadcastControl(newValue)
                                setControlReceiverEnabled(context, newValue)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Dimensions.SPACING_MEDIUM))

                // Display settings by category
                SettingDefinitions.CATEGORIES.forEach { (category, settingsInCategory) ->
                    CategoryHeader(category)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimensions.SPACING_SMALL),
                        shape = RoundedCornerShape(Dimensions.CARD_CORNER_RADIUS),
                        elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.CARD_ELEVATION)
                    ) {
                        Column(modifier = Modifier.padding(Dimensions.SPACING_SMALL)) {
                            settingsInCategory.forEach { settingTitle ->
                                val setting = allSettings.find { it.title == settingTitle }
                                if (settingTitle == "GPS Noise") {
                                    GpsNoiseSetting(
                                        useGpsNoise = settingsViewModel.useGpsNoise.collectAsState().value,
                                        gpsNoiseLevel = settingsViewModel.gpsNoiseLevel.collectAsState().value,
                                        onUseGpsNoiseChange = settingsViewModel::setUseGpsNoise,
                                        onGpsNoiseLevelChange = settingsViewModel::setGpsNoiseLevel
                                    )
                                } else {
                                    setting?.let {
                                    when (setting) {
                                        is DoubleSettingData -> {
                                            DoubleSettingComposable(setting)
                                        }
                                        is FloatSettingData -> {
                                            FloatSettingComposable(setting)
                                        }
                                    }
                                    }
                                }
                                    if (settingTitle != settingsInCategory.last()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = Dimensions.SPACING_SMALL),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Dimensions.SPACING_MEDIUM))
                }
                
                // Add space at the bottom of the list
                Spacer(modifier = Modifier.height(Dimensions.SPACING_LARGE))
            }
        }
    }
}

@Composable
private fun GpsNoiseSetting(
    useGpsNoise: Boolean,
    gpsNoiseLevel: GpsNoiseLevel,
    onUseGpsNoiseChange: (Boolean) -> Unit,
    onGpsNoiseLevelChange: (GpsNoiseLevel) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BooleanSettingItem(
            title = "GPS Noise",
            description = "Adds stationary GPS noise around the selected location",
            checked = useGpsNoise,
            onCheckedChange = onUseGpsNoiseChange
        )
        if (useGpsNoise) {
            GpsNoiseLevel.entries.forEach { level ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.SPACING_SMALL),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = gpsNoiseLevel == level,
                        onClick = { onGpsNoiseLevelChange(level) }
                    )
                    Text(level.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }
    }
}

private fun setControlReceiverEnabled(context: android.content.Context, enabled: Boolean) {
    val component = ComponentName(context, ControlReceiver::class.java)
    val newState = if (enabled) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } else {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
    context.packageManager.setComponentEnabledSetting(
        component,
        newState,
        PackageManager.DONT_KILL_APP
    )
}

@Composable
fun CategoryHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimensions.SPACING_SMALL)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(
            modifier = Modifier
                .weight(2f)
                .padding(start = Dimensions.SPACING_MEDIUM),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun BooleanSettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(Dimensions.SPACING_SMALL)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(
                        onClick = { showTooltip = !showTooltip },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "More information about $title",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (showTooltip) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Dimensions.SPACING_EXTRA_SMALL)
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.semantics {
                    contentDescription = if (checked) "Disable $title" else "Enable $title"
                }
            )
        }
    }
}

@Composable
fun DoubleSettingItem(
    title: String,
    description: String,
    useValue: Boolean,
    onUseValueChange: (Boolean) -> Unit,
    value: Double,
    onValueChange: (Double) -> Unit,
    label: String,
    unit: String,
    minValue: Float,
    maxValue: Float,
    step: Float
) {
    SettingItem(
        title = title,
        description = description,
        useValue = useValue,
        onUseValueChange = onUseValueChange,
        value = value,
        onValueChange = onValueChange,
        label = label,
        unit = unit,
        minValue = minValue,
        maxValue = maxValue,
        step = step,
        valueFormatter = { "%.2f".format(it) },
        parseValue = { it.toDouble() }
    )
}

@Composable
fun FloatSettingItem(
    title: String,
    description: String,
    useValue: Boolean,
    onUseValueChange: (Boolean) -> Unit,
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    unit: String,
    minValue: Float,
    maxValue: Float,
    step: Float
) {
    SettingItem(
        title = title,
        description = description,
        useValue = useValue,
        onUseValueChange = onUseValueChange,
        value = value,
        onValueChange = onValueChange,
        label = label,
        unit = unit,
        minValue = minValue,
        maxValue = maxValue,
        step = step,
        valueFormatter = { "%.2f".format(it) },
        parseValue = { it }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Number> SettingItem(
    title: String,
    description: String,
    useValue: Boolean,
    onUseValueChange: (Boolean) -> Unit,
    value: T,
    onValueChange: (T) -> Unit,
    label: String,
    unit: String,
    minValue: Float,
    maxValue: Float,
    step: Float,
    valueFormatter: (T) -> String,
    parseValue: (Float) -> T
) {
    var showTooltip by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(Dimensions.SPACING_SMALL)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    IconButton(
                        onClick = { showTooltip = !showTooltip },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "More information about $title",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                if (showTooltip) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Dimensions.SPACING_EXTRA_SMALL)
                    )
                }
            }
            
            Switch(
                checked = useValue,
                onCheckedChange = onUseValueChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.semantics { 
                    contentDescription = if (useValue) "Disable $title" else "Enable $title" 
                }
            )
        }

        if (useValue) {
            Spacer(modifier = Modifier.height(Dimensions.SPACING_MEDIUM))
            
            var sliderValue by remember { mutableFloatStateOf(value.toFloat()) }
            var showExactValue by remember { mutableStateOf(false) }
            
            LaunchedEffect(value) {
                if (sliderValue != value.toFloat()) {
                    sliderValue = value.toFloat()
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SPACING_SMALL),
                modifier = Modifier.fillMaxWidth()
            ) {
                val displayText = "$label: ${valueFormatter(parseValue(sliderValue))} $unit"
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showExactValue = !showExactValue }
                )
                
                // Add +/- buttons for precise adjustment
                OutlinedIconButton(
                    onClick = { 
                        val newValue = (sliderValue - step).coerceAtLeast(minValue)
                        sliderValue = newValue
                        onValueChange(parseValue(newValue))
                    },
                    enabled = sliderValue > minValue,
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "−",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                OutlinedIconButton(
                    onClick = { 
                        val newValue = (sliderValue + step).coerceAtMost(maxValue)
                        sliderValue = newValue
                        onValueChange(parseValue(newValue))
                    },
                    enabled = sliderValue < maxValue,
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "+",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            
            // Min and max value labels
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.SPACING_SMALL)
            ) {
                Text(
                    text = "${minValue.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${maxValue.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    sliderValue = newValue
                },
                onValueChangeFinished = {
                    onValueChange(parseValue(sliderValue))
                },
                valueRange = minValue..maxValue,
                steps = ((maxValue - minValue) / step).toInt() - 1,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Adjust $title value"
                    }
            )
        }
    }
}

sealed class SettingData {
    abstract val title: String
    abstract val description: String
    abstract val useValueState: State<Boolean>
    abstract val setUseValue: (Boolean) -> Unit
    abstract val label: String
    abstract val unit: String
    abstract val minValue: Float
    abstract val maxValue: Float
    abstract val step: Float
}

data class DoubleSettingData(
    override val title: String,
    override val description: String,
    override val useValueState: State<Boolean>,
    val valueState: State<Double>,
    override val setUseValue: (Boolean) -> Unit,
    val setValue: (Double) -> Unit,
    override val label: String,
    override val unit: String,
    override val minValue: Float,
    override val maxValue: Float,
    override val step: Float
) : SettingData()

data class FloatSettingData(
    override val title: String,
    override val description: String,
    override val useValueState: State<Boolean>,
    val valueState: State<Float>,
    override val setUseValue: (Boolean) -> Unit,
    val setValue: (Float) -> Unit,
    override val label: String,
    override val unit: String,
    override val minValue: Float,
    override val maxValue: Float,
    override val step: Float
) : SettingData()

@Composable
fun DoubleSettingComposable(
    setting: DoubleSettingData
) {
    DoubleSettingItem(
        title = setting.title,
        description = setting.description,
        useValue = setting.useValueState.value,
        onUseValueChange = setting.setUseValue,
        value = setting.valueState.value,
        onValueChange = setting.setValue,
        label = setting.label,
        unit = setting.unit,
        minValue = setting.minValue,
        maxValue = setting.maxValue,
        step = setting.step
    )
}

@Composable
fun FloatSettingComposable(
    setting: FloatSettingData
) {
    FloatSettingItem(
        title = setting.title,
        description = setting.description,
        useValue = setting.useValueState.value,
        onUseValueChange = setting.setUseValue,
        value = setting.valueState.value,
        onValueChange = setting.setValue,
        label = setting.label,
        unit = setting.unit,
        minValue = setting.minValue,
        maxValue = setting.maxValue,
        step = setting.step
    )
}
