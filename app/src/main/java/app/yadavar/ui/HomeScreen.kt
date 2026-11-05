package app.yadavar.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.yadavar.data.DEFAULT_PACK_ID
import app.yadavar.data.NoteEntity
import app.yadavar.time.Fmt

@Composable
private fun PulseRing() {
    val t = rememberInfiniteTransition(label = "pulse")
    val s by t.animateFloat(
        initialValue = 0.85f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "scale",
    )
    Box(
        Modifier.size(190.dp).scale(s).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
    )
}

/** قهرمان صفحه: میکروفون بزرگ با موج‌های کناری. */
@Composable
private fun MicHero(listening: Boolean, onClick: () -> Unit) {
    val neo = LocalNeo.current
    val desc = if (listening) "توقف شنیدن" else "شروع ضبط صدا"
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Waveform(active = listening, mirrored = false, modifier = Modifier.weight(1f).height(64.dp))
        Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
            if (listening) PulseRing()
            if (neo) {
                NeoSurface(
                    Modifier.size(156.dp).semantics { contentDescription = desc },
                    corner = 999.dp, onClick = onClick, depth = 10.dp, glow = Neo.Teal.copy(alpha = 0.35f),
                ) {
                    NeoSurface(Modifier.align(Alignment.Center).size(116.dp), corner = 999.dp, inset = true) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (listening) {
                                Box(Modifier.size(34.dp).background(Neo.Teal, RoundedCornerShape(8.dp)))
                            } else {
                                Icon(AppIcons.Mic, null, Modifier.size(62.dp), tint = Neo.Teal)
                            }
                        }
                    }
                }
            } else {
                Box(
                    Modifier.size(130.dp).clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                        .clickable { onClick() }
                        .semantics { contentDescription = desc },
                    contentAlignment = Alignment.Center,
                ) {
                    if (listening) {
                        Box(Modifier.size(34.dp).background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(8.dp)))
                    } else {
                        Icon(AppIcons.Mic, null, Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        Waveform(active = listening, mirrored = true, modifier = Modifier.weight(1f).height(64.dp))
    }
}

@Composable
fun HomeScreen(vm: MainViewModel) {
    val voice by vm.voice.collectAsState()
    val notes by vm.notes.collectAsState()
    val settings by vm.settings.collectAsState()
    val packs by vm.packs.collectAsState()
    val packNames = packs.associate { it.id to it.name }
    var toDelete by remember { mutableStateOf<NoteEntity?>(null) }
    val ctx = LocalContext.current

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startListening() else vm.onMicPermissionDenied()
    }

    fun onMicClick() {
        when (voice.phase) {
            VoicePhase.LISTENING -> vm.stopListening()
            VoicePhase.PROCESSING -> Unit
            else -> {
                val ok = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                if (ok) vm.startListening() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val now = System.currentTimeMillis()
    val upcoming = notes.filter { val r = it.remindAt; r != null && !it.done && r > now }
        .sortedBy { it.remindAt }.take(3)
    val recent = notes.take(5)
    val listening = voice.phase == VoicePhase.LISTENING

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "یادآور من", Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                NeoIconButton(Icons.Default.Settings, "تنظیمات", { vm.go(Screen.SETTINGS) })
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                MicHero(listening = listening, onClick = { onMicClick() })
                val label = when (voice.phase) {
                    VoicePhase.READY -> "آماده شنیدن؛ دکمه را لمس کنید"
                    VoicePhase.LISTENING -> "در حال گوش دادن… " + Fmt.timer(voice.seconds)
                    VoicePhase.PROCESSING -> "در حال پردازش…"
                    VoicePhase.SAVED -> "ثبت شد ✓"
                    VoicePhase.ERROR -> voice.error ?: "خطا"
                }
                Text(
                    label, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (voice.phase == VoicePhase.ERROR) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (listening && voice.partial.isNotBlank()) {
                    Text(
                        voice.partial, Modifier.padding(top = 8.dp), textAlign = TextAlign.Center,
                        maxLines = 3, color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                if (listening) TextButton(onClick = { vm.cancelListening() }) { Text("لغو") }
                if (voice.phase == VoicePhase.ERROR && voice.needsAppSettings) {
                    TextButton(onClick = { PermissionHelper.openAppSettings(ctx) }) { Text("باز کردن تنظیمات برنامه") }
                }
            }
        }
        if (upcoming.isNotEmpty()) {
            item { SectionTitle("یادآوری‌های نزدیک", AppIcons.Schedule) }
            items(upcoming, key = { it.id }) { n ->
                NoteCard(
                    n, settings.use24h, onClick = { vm.editNote(n) }, onDelete = { toDelete = n },
                    packName = packNames[n.packId]?.takeIf { n.packId != DEFAULT_PACK_ID },
                )
            }
        }
        item { SectionTitle("یادداشت‌های اخیر", AppIcons.Description) }
        if (recent.isEmpty()) {
            item {
                Text(
                    "هنوز یادداشتی ندارید. میکروفون را لمس کنید و مثلاً بگویید «فردا ساعت ۹ صبح یادآوری کن جلسه دارم».",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(recent, key = { "n${it.id}" }) { n ->
                NoteCard(
                    n, settings.use24h, onClick = { vm.editNote(n) }, onDelete = { toDelete = n },
                    packName = packNames[n.packId]?.takeIf { n.packId != DEFAULT_PACK_ID },
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                NeoButton(
                    "همه یادداشت‌ها", { vm.go(Screen.NOTES) }, Modifier.weight(1f),
                    icon = AppIcons.GraphicEq, primary = true,
                )
                NeoButton("یادداشت متنی", { vm.newTextNote() }, Modifier.weight(1f), icon = AppIcons.NoteAdd)
            }
        }
        item {
            NeoButton("پک‌های کارها", { vm.go(Screen.PACKS) }, Modifier.fillMaxWidth(), icon = AppIcons.Folder)
        }
        item {
            NeoSurface(Modifier.fillMaxWidth(), corner = 22.dp, depth = 5.dp) {
                Row(
                    Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NeoIconTile(AppIcons.Lightbulb, tileSize = 44.dp, iconSize = 24.dp)
                    Text(
                        "با صدا هم می‌توانید: «یک پک با عنوان دانشگاه بساز» یا «فردا تحویل پروژه، به کارهای دانشگاه اضافه کن».",
                        Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    toDelete?.let { n ->
        DeleteNoteDialog(n, onConfirm = { vm.delete(n); toDelete = null }, onDismiss = { toDelete = null })
    }
}
