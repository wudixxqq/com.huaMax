package com.noobexon.xposedfakelocation.manager.ui.settings

import android.app.Activity
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.noobexon.xposedfakelocation.R
import com.noobexon.xposedfakelocation.manager.control.ControlReceiver
import com.noobexon.xposedfakelocation.manager.localization.LanguageOption
import com.noobexon.xposedfakelocation.manager.localization.LocaleController

private object Dimensions {
    val SPACING_EXTRA_SMALL = 4.dp
    val SPACING_SMALL = 8.dp
    val SPACING_MEDIUM = 16.dp
    val SPACING_LARGE = 24.dp
    val CARD_CORNER_RADIUS = 12.dp
    val CARD_ELEVATION = 2.dp
}

private object SettingDefinitions {
    @Composable
    fun getCategories(): Map<String, List<String>> {
        val randomizeTitle = stringResource(R.string.setting_randomize_title)
        val horizontalAccuracyTitle = stringResource(R.string.setting_horizontal_accuracy_title)
        val verticalAccuracyTitle = stringResource(R.string.setting_vertical_accuracy_title)
        val altitudeTitle = stringResource(R.string.setting_altitude_title)
        val mslTitle = stringResource(R.string.setting_msl_title)
        val mslAccuracyTitle = stringResource(R.string.setting_msl_accuracy_title)
        val speedTitle = stringResource(R.string.setting_speed_title)
        val speedAccuracyTitle = stringResource(R.string.setting_speed_accuracy_title)

        return mapOf(
            stringResource(R.string.category_location) to listOf(
                randomizeTitle,
                horizontalAccuracyTitle,
                verticalAccuracyTitle
            ),
            stringResource(R.string.category_altitude) to listOf(
                altitudeTitle,
                mslTitle,
                mslAccuracyTitle
            ),
            stringResource(R.string.category_movement) to listOf(
                speedTitle,
                speedAccuracyTitle
            )
        )
    }

    @Composable
    fun getSettings(viewModel: SettingsViewModel): List<SettingData> = listOf(
        DoubleSettingData(
            title = stringResource(R.string.setting_randomize_title),
            description = stringResource(R.string.setting_randomize_description),
            useValueState = viewModel.useRandomize.collectAsState(),
            valueState = viewModel.randomizeRadius.collectAsState(),
            setUseValue = viewModel::setUseRandomize,
            setValue = viewModel::setRandomizeRadius,
            label = stringResource(R.string.setting_randomize_radius_label),
            unit = "m",
            minValue = 0f,
            maxValue = 2000f,
            step = 0.1f
        ),
        DoubleSettingData(
            title = stringResource(R.string.setting_horizontal_accuracy_title),
            description = stringResource(R.string.setting_horizontal_accuracy_description),
            useValueState = viewModel.useAccuracy.collectAsState(),
            valueState = viewModel.accuracy.collectAsState(),
            setUseValue = viewModel::setUseAccuracy,
            setValue = viewModel::setAccuracy,
            label = stringResource(R.string.setting_horizontal_accuracy_label),
            unit = "m",
            minValue = 0f,
            maxValue = 100f,
            step = 1f
        ),
        FloatSettingData(
            title = stringResource(R.string.setting_vertical_accuracy_title),
            description = stringResource(R.string.setting_vertical_accuracy_description),
            useValueState = viewModel.useVerticalAccuracy.collectAsState(),
            valueState = viewModel.verticalAccuracy.collectAsState(),
            setUseValue = viewModel::setUseVerticalAccuracy,
            setValue = viewModel::setVerticalAccuracy,
            label = stringResource(R.string.setting_vertical_accuracy_label),
            unit = "m",
            minValue = 0f,
            maxValue = 100f,
            step = 1f
        ),
        DoubleSettingData(
            title = stringResource(R.string.setting_altitude_title),
            description = stringResource(R.string.setting_altitude_description),
            useValueState = viewModel.useAltitude.collectAsState(),
            valueState = viewModel.altitude.collectAsState(),
            setUseValue = viewModel::setUseAltitude,
            setValue = viewModel::setAltitude,
            label = stringResource(R.string.setting_altitude_label),
            unit = "m",
            minValue = 0f,
            maxValue = 2000f,
            step = 0.5f
        ),
        DoubleSettingData(
            title = stringResource(R.string.setting_msl_title),
            description = stringResource(R.string.setting_msl_description),
            useValueState = viewModel.useMeanSeaLevel.collectAsState(),
            valueState = viewModel.meanSeaLevel.collectAsState(),
            setUseValue = viewModel::setUseMeanSeaLevel,
            setValue = viewModel::setMeanSeaLevel,
            label = stringResource(R.string.setting_msl_label),
            unit = "m",
            minValue = -400f,
            maxValue = 2000f,
            step = 0.5f
        ),
        FloatSettingData(
            title = stringResource(R.string.setting_msl_accuracy_title),
            description = stringResource(R.string.setting_msl_accuracy_description),
            useValueState = viewModel.useMeanSeaLevelAccuracy.collectAsState(),
            valueState = viewModel.meanSeaLevelAccuracy.collectAsState(),
            setUseValue = viewModel::setUseMeanSeaLevelAccuracy,
            setValue = viewModel::setMeanSeaLevelAccuracy,
            label = stringResource(R.string.setting_msl_accuracy_label),
            unit = "m",
            minValue = 0f,
            maxValue = 100f,
            step = 1f
        ),
        FloatSettingData(
            title = stringResource(R.string.setting_speed_title),
            description = stringResource(R.string.setting_speed_description),
            useValueState = viewModel.useSpeed.collectAsState(),
            valueState = viewModel.speed.collectAsState(),
            setUseValue = viewModel::setUseSpeed,
            setValue = viewModel::setSpeed,
            label = stringResource(R.string.setting_speed_label),
            unit = "m/s",
            minValue = 0f,
            maxValue = 30f,
            step = 0.1f
        ),
        FloatSettingData(
            title = stringResource(R.string.setting_speed_accuracy_title),
            description = stringResource(R.string.setting_speed_accuracy_description),
            useValueState = viewModel.useSpeedAccuracy.collectAsState(),
            valueState = viewModel.speedAccuracy.collectAsState(),
            setUseValue = viewModel::setUseSpeedAccuracy,
            setValue = viewModel::setSpeedAccuracy,
            label = stringResource(R.string.setting_speed_accuracy_label),
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
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val allSettings = SettingDefinitions.getSettings(settingsViewModel)
    val categories = SettingDefinitions.getCategories()
    val selectedLanguage = LanguageOption.fromTag(settingsViewModel.languageTag.collectAsState().value)

    val snackbarHostState = remember { SnackbarHostState() }
    // null = hidden; true = hooks were enabled; false = hooks were disabled
    var restartDialogEnabled by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        settingsViewModel.systemHooksEvents.collect { event ->
            when (event) {
                is SystemHooksEvent.RestartRequired -> restartDialogEnabled = event.enabled
                is SystemHooksEvent.ModuleNotActive ->
                    snackbarHostState.showSnackbar(context.getString(R.string.system_hooks_module_inactive))
                is SystemHooksEvent.ScopeRequestFailed ->
                    snackbarHostState.showSnackbar(context.getString(R.string.system_hooks_scope_failed, event.message))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_settings)) },
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
                            contentDescription = stringResource(R.string.cd_navigate_back)
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

                CategoryHeader(stringResource(R.string.category_language))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.SPACING_SMALL),
                    shape = RoundedCornerShape(Dimensions.CARD_CORNER_RADIUS),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.CARD_ELEVATION)
                ) {
                    LanguageSettingItem(
                        selectedLanguage = selectedLanguage,
                        onLanguageSelected = { option ->
                            settingsViewModel.setLanguageTag(option.tag)
                            LocaleController.persistLanguageTag(context, option.tag)
                            (context as? Activity)?.recreate()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(Dimensions.SPACING_MEDIUM))

                CategoryHeader(stringResource(R.string.category_notifications))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.SPACING_SMALL),
                    shape = RoundedCornerShape(Dimensions.CARD_CORNER_RADIUS),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.CARD_ELEVATION)
                ) {
                    Column(modifier = Modifier.padding(Dimensions.SPACING_SMALL)) {
                        BooleanSettingItem(
                            title = stringResource(R.string.setting_hide_toast_title),
                            description = stringResource(R.string.setting_hide_toast_description),
                            checked = settingsViewModel.hideFakeLocationToast.collectAsState().value,
                            onCheckedChange = settingsViewModel::setHideFakeLocationToast
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Dimensions.SPACING_MEDIUM))

                CategoryHeader(stringResource(R.string.category_external_control))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.SPACING_SMALL),
                    shape = RoundedCornerShape(Dimensions.CARD_CORNER_RADIUS),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.CARD_ELEVATION)
                ) {
                    Column(modifier = Modifier.padding(Dimensions.SPACING_SMALL)) {
                        BooleanSettingItem(
                            title = stringResource(R.string.setting_external_broadcast_title),
                            description = stringResource(R.string.setting_external_broadcast_description),
                            checked = settingsViewModel.enableBroadcastControl.collectAsState().value,
                            onCheckedChange = { newValue ->
                                settingsViewModel.setEnableBroadcastControl(newValue)
                                setControlReceiverEnabled(context, newValue)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Dimensions.SPACING_MEDIUM))

                CategoryHeader(stringResource(R.string.category_system_hooks))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.SPACING_SMALL),
                    shape = RoundedCornerShape(Dimensions.CARD_CORNER_RADIUS),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.CARD_ELEVATION)
                ) {
                    Column(modifier = Modifier.padding(Dimensions.SPACING_SMALL)) {
                        BooleanSettingItem(
                            title = stringResource(R.string.setting_system_hooks_title),
                            description = stringResource(R.string.setting_system_hooks_description),
                            checked = settingsViewModel.enableSystemHooks.collectAsState().value,
                            onCheckedChange = settingsViewModel::setEnableSystemHooks
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Dimensions.SPACING_MEDIUM))

                categories.forEach { (category, settingsInCategory) ->
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
                                setting?.let {
                                        when (setting) {
                                            is DoubleSettingData -> DoubleSettingComposable(setting)
                                            is FloatSettingData -> FloatSettingComposable(setting)
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

                Spacer(modifier = Modifier.height(Dimensions.SPACING_LARGE))
            }

            restartDialogEnabled?.let { enabled ->
                AlertDialog(
                    onDismissRequest = { restartDialogEnabled = null },
                    title = { Text(stringResource(R.string.dialog_restart_required_title)) },
                    text = {
                        Text(
                            stringResource(
                                if (enabled) R.string.dialog_restart_required_enable_message
                                else R.string.dialog_restart_required_disable_message
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { restartDialogEnabled = null }) {
                            Text(stringResource(R.string.action_ok))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSettingItem(
    selectedLanguage: LanguageOption,
    onLanguageSelected: (LanguageOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimensions.SPACING_SMALL)
    ) {
        Text(
            text = stringResource(R.string.setting_language_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(R.string.setting_language_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Dimensions.SPACING_EXTRA_SMALL)
        )
        Spacer(modifier = Modifier.height(Dimensions.SPACING_SMALL))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = stringResource(selectedLanguage.labelRes),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.setting_language_title)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                LanguageOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes)) },
                        onClick = {
                            expanded = false
                            onLanguageSelected(option)
                        }
                    )
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
    val moreInfoDescription = stringResource(R.string.setting_more_info, title)
    val disableDescription = stringResource(R.string.setting_disable, title)
    val enableDescription = stringResource(R.string.setting_enable, title)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimensions.SPACING_SMALL)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            contentDescription = moreInfoDescription,
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
                    contentDescription = if (checked) disableDescription else enableDescription
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
    val moreInfoDescription = stringResource(R.string.setting_more_info, title)
    val disableDescription = stringResource(R.string.setting_disable, title)
    val enableDescription = stringResource(R.string.setting_enable, title)
    val adjustDescription = stringResource(R.string.setting_adjust_value, title)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimensions.SPACING_SMALL)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            contentDescription = moreInfoDescription,
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
                    contentDescription = if (useValue) disableDescription else enableDescription
                }
            )
        }

        if (useValue) {
            Spacer(modifier = Modifier.height(Dimensions.SPACING_MEDIUM))

            var sliderValue by remember { mutableFloatStateOf(value.toFloat()) }
            var showExactValue by remember { mutableStateOf(false) }

            LaunchedEffect(value) {
                if (sliderValue != value.toFloat()) sliderValue = value.toFloat()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SPACING_SMALL),
                modifier = Modifier.fillMaxWidth()
            ) {
                val displayText = stringResource(
                    R.string.setting_value_display,
                    label,
                    valueFormatter(parseValue(sliderValue)),
                    unit
                )
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showExactValue = !showExactValue }
                )

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
                onValueChange = { newValue -> sliderValue = newValue },
                onValueChangeFinished = { onValueChange(parseValue(sliderValue)) },
                valueRange = minValue..maxValue,
                steps = ((maxValue - minValue) / step).toInt() - 1,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = adjustDescription }
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
fun DoubleSettingComposable(setting: DoubleSettingData) {
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
fun FloatSettingComposable(setting: FloatSettingData) {
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
