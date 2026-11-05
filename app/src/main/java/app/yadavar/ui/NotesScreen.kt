package app.yadavar.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.yadavar.data.DEFAULT_PACK_ID
import app.yadavar.data.NoteEntity
import app.yadavar.time.Fmt

@Composable
fun NotesScreen(vm: MainViewModel) {
    val notes by vm.notes.collectAsState()
    val settings by vm.settings.collectAsState()
    val packs by vm.packs.collectAsState()
    val packFilter by vm.packFilter.collectAsState()
    val packNames = packs.associate { it.id to it.name }
    val counts = notes.groupingBy { it.packId }.eachCount()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) } // 0 همه، 1 یادداشت ساده، 2 یادآوری
    var toDelete by remember { mutableStateOf<NoteEntity?>(null) }

    val shown = notes.filter { n ->
        val okFilter = when (filter) { 1 -> n.remindAt == null; 2 -> n.remindAt != null; else -> true }
        val okPack = packFilter == null || n.packId == packFilter
        okFilter && okPack && (query.isBlank() || n.text.contains(query.trim()) || n.title.contains(query.trim()))
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NeoTopBar("همه یادداشت‌ها", onBack = { vm.back() }) {
            NeoIconButton(AppIcons.Folder, "مدیریت پک‌ها", { vm.go(Screen.PACKS) })
        }
        NeoTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, label = "جست‌وجو", leadingIcon = Icons.Default.Search,
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeoChip("همه", selected = filter == 0, onClick = { filter = 0 })
            NeoChip("یادداشت ساده", selected = filter == 1, onClick = { filter = 1 })
            NeoChip("یادآوری زمان‌دار", selected = filter == 2, onClick = { filter = 2 })
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeoChip("همه پک‌ها", selected = packFilter == null, onClick = { vm.setPackFilter(null) })
            for (p in packs) {
                NeoChip(
                    Fmt.digits("${p.name} (${counts[p.id] ?: 0})"),
                    selected = packFilter == p.id, onClick = { vm.setPackFilter(p.id) },
                    icon = packIconFor(p.name),
                )
            }
        }
        if (shown.isEmpty()) {
            Text("موردی پیدا نشد.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(
            Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(shown, key = { it.id }) { n ->
                NoteCard(
                    n, settings.use24h, onClick = { vm.editNote(n) }, onToggleDone = { vm.toggleDone(n) },
                    onDelete = { toDelete = n },
                    packName = if (packFilter == null) packNames[n.packId]?.takeIf { n.packId != DEFAULT_PACK_ID } else null,
                )
            }
        }
    }

    toDelete?.let { n ->
        DeleteNoteDialog(n, onConfirm = { vm.delete(n); toDelete = null }, onDismiss = { toDelete = null })
    }
}
