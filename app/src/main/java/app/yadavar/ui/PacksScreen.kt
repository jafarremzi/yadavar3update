package app.yadavar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.yadavar.data.DEFAULT_PACK_ID
import app.yadavar.data.DEFAULT_PACK_NAME
import app.yadavar.data.PackEntity
import app.yadavar.time.Fmt

/** مدیریت پک‌ها: ساخت، تغییر نام، حذف و نمایش کارهای هر پک؛ آیکن هر پک از روی نامش انتخاب می‌شود. */
@Composable
fun PacksScreen(vm: MainViewModel) {
    val packs by vm.packs.collectAsState()
    val notes by vm.notes.collectAsState()
    val counts = notes.groupingBy { it.packId }.eachCount()
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<PackEntity?>(null) }
    var deleting by remember { mutableStateOf<PackEntity?>(null) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NeoTopBar("پک‌ها", onBack = { vm.back() }) {
            NeoButton("پک جدید", { creating = true }, icon = AppIcons.Add, primary = true)
        }
        NeoSurface(Modifier.fillMaxWidth(), corner = 26.dp, depth = 6.dp) {
            Row(
                Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                NeoIconTile(AppIcons.Inventory2, tileSize = 64.dp, iconSize = 34.dp)
                Text(
                    "کارها را در پک‌ها گروه‌بندی کنید؛ مثلاً «دانشگاه» یا «سایت». پک «$DEFAULT_PACK_NAME» همیشه هست و کارهای بدون پک در آن قرار می‌گیرند.",
                    Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyColumn(
            Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(packs, key = { it.id }) { p ->
                val count = counts[p.id] ?: 0
                NeoSurface(Modifier.fillMaxWidth(), corner = 26.dp, onClick = { vm.showPack(p.id) }, depth = 7.dp) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NeoIconTile(packIconFor(p.name), tileSize = 58.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    Modifier.size(7.dp).clip(CircleShape)
                                        .background(if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                )
                                Text(
                                    Fmt.digits("$count کار"), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (p.id != DEFAULT_PACK_ID) {
                            NeoIconButton(
                                Icons.Default.Edit, "تغییر نام", { renaming = p }, buttonSize = 38.dp, iconSize = 18.dp,
                                tint = if (LocalNeo.current) Neo.Cyan else null,
                            )
                            Spacer(Modifier.width(8.dp))
                            NeoIconButton(
                                Icons.Default.Delete, "حذف پک", { deleting = p }, buttonSize = 38.dp, iconSize = 18.dp,
                                tint = if (LocalNeo.current) Neo.TextSecondary else null,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        NeoIconButton(
                            AppIcons.ChevronLeft, "نمایش کارهای این پک", { vm.showPack(p.id) },
                            buttonSize = 40.dp, iconSize = 22.dp,
                            tint = if (LocalNeo.current) Neo.Teal else null,
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        PackNameDialog(
            title = "پک جدید", initial = "", confirmLabel = "ساخت",
            onConfirm = { vm.createPack(it); creating = false }, onDismiss = { creating = false },
        )
    }
    renaming?.let { p ->
        PackNameDialog(
            title = "تغییر نام پک", initial = p.name, confirmLabel = "ذخیره",
            onConfirm = { vm.renamePack(p.id, it); renaming = null }, onDismiss = { renaming = null },
        )
    }
    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("پک «${p.name}» حذف شود؟") },
            text = { Text("کارهای داخل این پک حذف نمی‌شوند و به «$DEFAULT_PACK_NAME» منتقل می‌شوند.") },
            confirmButton = { TextButton(onClick = { vm.deletePack(p.id); deleting = null }) { Text("حذف پک") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("انصراف") } },
        )
    }
}
