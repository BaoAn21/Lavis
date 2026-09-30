package com.sim.lavis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sim.lavis.data.db.SingerEntity
import com.sim.lavis.data.db.SongWithSingers
import com.sim.lavis.ui.LibraryViewModel
import com.sim.lavis.ui.components.LavisIcons
import com.sim.lavis.ui.components.SongCover
import com.sim.lavis.ui.theme.TextFaint
import com.sim.lavis.ui.theme.TextMuted

/**
 * Rename the song, toggle its singers, or create a brand new singer and assign it in one go.
 * Singer toggles apply immediately; the title is saved with the Save button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditSongDialog(
    songWithSingers: SongWithSingers,
    allSingers: List<SingerEntity>,
    viewModel: LibraryViewModel,
    onDismiss: () -> Unit
) {
    val song = songWithSingers.song
    var title by remember { mutableStateOf(song.title) }
    var newSinger by remember { mutableStateOf("") }
    val assignedIds = songWithSingers.singers.map { it.id }.toSet()

    fun addSinger() {
        if (newSinger.isNotBlank()) {
            viewModel.createSingerAndAssign(newSinger, song.id)
            newSinger = ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SongCover(contentUri = song.contentUri, key = song.title, size = 56.dp)
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text("Edit song", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = song.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = lavisFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                )

                Text(
                    text = "Singers",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                if (allSingers.isEmpty()) {
                    Text(
                        "No singers yet — create one below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                FlowRow(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allSingers.forEach { singer ->
                        val assigned = singer.id in assignedIds
                        FilterChip(
                            selected = assigned,
                            onClick = { viewModel.toggleSinger(song.id, singer.id, assigned) },
                            label = { Text(singer.name) },
                            leadingIcon = if (assigned) {
                                { Icon(LavisIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = newSinger,
                    onValueChange = { newSinger = it },
                    placeholder = { Text("New singer", color = TextFaint) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = lavisFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { addSinger() }),
                    trailingIcon = {
                        IconButton(onClick = ::addSinger, enabled = newSinger.isNotBlank()) {
                            Icon(LavisIcons.Add, contentDescription = "Create and assign singer")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        viewModel.setSongTitle(song.id, title)
                        onDismiss()
                    }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun lavisFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary
)
