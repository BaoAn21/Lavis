package com.sim.lavis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sim.lavis.data.db.SingerEntity
import com.sim.lavis.data.db.SongWithSingers
import com.sim.lavis.ui.LibraryViewModel
import com.sim.lavis.ui.components.DimText
import com.sim.lavis.ui.theme.TermAmber
import com.sim.lavis.ui.theme.TermBlack
import com.sim.lavis.ui.theme.TermCyan
import com.sim.lavis.ui.theme.TermGray
import com.sim.lavis.ui.theme.TermGreenDim

/**
 * Terminal-style metadata editor: rename the song, toggle singers on/off,
 * or create a brand new singer and assign it in one go.
 */
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

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TermBlack)
                .border(1.dp, TermGreenDim)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "$ edit ${song.fileName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            DimText("title:", modifier = Modifier.padding(top = 16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = terminalFieldColors()
            )

            DimText("singers: [tap to toggle]", modifier = Modifier.padding(top = 16.dp))
            Column(modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                if (allSingers.isEmpty()) {
                    DimText("(no singers yet — create one below)", Modifier.padding(vertical = 8.dp))
                }
                allSingers.forEach { singer ->
                    val assigned = singer.id in assignedIds
                    Text(
                        text = (if (assigned) "[x] " else "[ ] ") + singer.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (assigned) TermAmber else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleSinger(song.id, singer.id, assigned) }
                            .padding(vertical = 6.dp)
                    )
                }
            }

            DimText("new singer:", modifier = Modifier.padding(top = 12.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newSinger,
                    onValueChange = { newSinger = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = terminalFieldColors()
                )
                Text(
                    text = " [+add]",
                    style = MaterialTheme.typography.labelLarge,
                    color = TermCyan,
                    modifier = Modifier.clickable {
                        if (newSinger.isNotBlank()) {
                            viewModel.createSingerAndAssign(newSinger, song.id)
                            newSinger = ""
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "[cancel]",
                    style = MaterialTheme.typography.labelLarge,
                    color = TermGray,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
                Text(
                    text = "  [save]",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        viewModel.setSongTitle(song.id, title)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun terminalFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = TermGreenDim,
    focusedTextColor = MaterialTheme.colorScheme.primary,
    unfocusedTextColor = MaterialTheme.colorScheme.primary,
    cursorColor = MaterialTheme.colorScheme.primary
)
