package com.avioiva.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avioiva.player.ui.PlayerViewModel
import java.util.Locale

/**
 * "Equalizer" tab: multi-band EQ with device presets, Bass Boost and a 3D
 * Surround (Virtualizer) effect. All effects attach to the player's audio
 * session and apply live, even mid-song.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(vm: PlayerViewModel) {
    val state by vm.equalizer.state.collectAsStateWithLifecycle()
    var presetMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(title = { Text("Equalizer") })

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Equalizer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Enable equalizer", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.attached) "Active on the current audio session"
                    else "Applies as soon as playback starts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = state.enabled, onCheckedChange = { vm.equalizer.setEnabled(it) })
        }

        // -------- preset selector --------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                OutlinedButton(onClick = { presetMenuOpen = true }) {
                    Text(
                        if (state.activePreset in state.presets.indices)
                            state.presets[state.activePreset]
                        else "Custom"
                    )
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = presetMenuOpen,
                    onDismissRequest = { presetMenuOpen = false },
                ) {
                    state.presets.forEachIndexed { index, name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                vm.equalizer.applyPreset(index)
                                presetMenuOpen = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (state.activePreset in state.presets.indices) "Preset" else "Drag a band to customise",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // -------- band sliders --------
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            state.bandLabels.forEachIndexed { band, label ->
                BandRow(
                    label = label,
                    level = state.bandLevels.getOrElse(band) { 0 },
                    range = state.bandRange,
                    enabled = state.enabled,
                    onChange = { vm.equalizer.setBandLevel(band, it) },
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // -------- bass boost --------
        EffectSection(
            icon = { Icon(Icons.Rounded.Speaker, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            title = "Bass Boost",
            enabled = state.bassEnabled,
            strength = state.bassStrength,
            onToggle = { vm.equalizer.setBassEnabled(it) },
            onStrength = { vm.equalizer.setBassStrength(it) },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // -------- 3D surround --------
        EffectSection(
            icon = { Icon(Icons.Rounded.SpatialAudio, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
            title = "3D Surround",
            enabled = state.surroundEnabled,
            strength = state.surroundStrength,
            onToggle = { vm.equalizer.setSurroundEnabled(it) },
            onStrength = { vm.equalizer.setSurroundStrength(it) },
        )

        Spacer(Modifier.padding(bottom = 24.dp))
    }
}

/** One EQ band: frequency label, level slider, dB value. */
@Composable
private fun BandRow(
    label: String,
    level: Int,
    range: IntRange,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    var value by remember(label, level) { mutableStateOf(level.toFloat()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
        )
        Slider(
            value = value,
            onValueChange = {
                value = it
                onChange(it.toInt())
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "%+.1f dB".format(Locale.US, value / 100f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun EffectSection(
    icon: @Composable () -> Unit,
    title: String,
    enabled: Boolean,
    strength: Int,
    onToggle: (Boolean) -> Unit,
    onStrength: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1.6f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        Column(
            Modifier.weight(2.4f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Slider(
                value = strength.toFloat(),
                onValueChange = { onStrength(it.toInt()) },
                valueRange = 0f..1000f,
                enabled = enabled,
            )
            Text(
                text = "${(strength / 10)}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
