package com.noobexon.xposedfakelocation.manager.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noobexon.xposedfakelocation.data.model.OverrideState

@Composable
fun OverrideStateSelector(
    title: String,
    state: OverrideState,
    onStateChange: (OverrideState) -> Unit
) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        OverrideState.entries.forEach { option ->
            FilterChip(
                selected = state == option,
                onClick = { onStateChange(option) },
                label = {
                    Text(
                        when (option) {
                            OverrideState.INHERIT -> "Inherit"
                            OverrideState.ENABLED -> "On"
                            OverrideState.DISABLED -> "Off"
                        }
                    )
                },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}
