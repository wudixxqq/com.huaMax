package com.noobexon.xposedfakelocation.manager.ui.map.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noobexon.xposedfakelocation.data.model.LocationTemplate
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateTemplateLocationDialog(
    templates: List<LocationTemplate>,
    marker: GeoPoint?,
    onDismissRequest: () -> Unit,
    onUpdateTemplate: (LocationTemplate, latitude: Double, longitude: Double) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selectedTemplate by remember(templates) { mutableStateOf<LocationTemplate?>(null) }

    val filteredTemplates = remember(templates, query) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            templates
        } else {
            templates.filter {
                it.name.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Update Template Location") },
        text = {
            Column {
                if (marker != null) {
                    Text(
                        text = "New location: ${marker.latitude}, ${marker.longitude}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (templates.isEmpty()) {
                    Text("No templates available.")
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                selectedTemplate = templates.firstOrNull { template ->
                                    template.name.equals(it, ignoreCase = true)
                                }
                                expanded = true
                            },
                            label = { Text("Template") },
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            filteredTemplates.forEach { template ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = template.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Current: ${template.latitude}, ${template.longitude}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedTemplate = template
                                        query = template.name
                                        expanded = false
                                    }
                                )
                            }
                            if (filteredTemplates.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No matching templates") },
                                    onClick = {}
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedTemplate != null && marker != null,
                onClick = {
                    val template = selectedTemplate
                    val point = marker
                    if (template != null && point != null) {
                        onUpdateTemplate(template, point.latitude, point.longitude)
                    }
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
