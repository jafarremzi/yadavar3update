package app.yadavar.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.yadavar.data.NoteEntity
import app.yadavar.data.NoteRepository
import app.yadavar.data.Repeat
import app.yadavar.time.Fmt
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.delay

fun repeatLabel(r: Int) = when (r) {
    Repeat.DAILY -> "روزانه"
    Repeat.WEEKLY -> "هفتگی"
    else -> "بدون تکرار"
}

/**
 * کارت یادداشت. کاشی آیکن: یادآوری ← ساعت زنگ‌دار، یادداشت ساده ← آیکن پک (یا «سند» برای پک عمومی).
 * [packName] فقط برای پک‌های غیرپیش‌فرض داده می‌شود و هم برچسب و هم آیکن کاشی از آن می‌آید.
 */
@Composable
fun NoteCard(
    note: NoteEntity,
    use24h: Boolean,
    onClick: () -> Unit,
    onToggleDone: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    packName: String? = null,
) {
    val whenText = remember(note.remindAt, use24h, note.repeat) {
        note.remindAt?.let {
            val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
            Fmt.dateTime(dt, use24h) + if (note.repeat != Repeat.NONE) " · " + repeatLabel(note.repeat) else ""
        }
    }
    val tileIcon = when {
        note.remindAt != null -> AppIcons.Alarm
        packName != null -> packIconFor(packName)
        else -> AppIcons.Description
    }
    NeoSurface(
        Modifier.fillMaxWidth().alpha(if (note.done) 0.65f else 1f),
        corner = 26.dp, onClick = onClick, depth = 7.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeoIconTile(tileIcon, tileSize = 54.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    note.displayTitle,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (note.done) TextDecoration.LineThrough else null,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (packName != null) {
                    Text(
                        "پک: $packName",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (whenText != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(AppIcons.Event, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            whenText, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                val end = note.remindAt
                if (end != null && !note.done) ReminderProgress(note, end)
                if (onToggleDone != null) {
                    NeoChip(
                        label = if (note.done) "بازگردانی" else "انجام شد",
                        selected = false, onClick = onToggleDone,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            if (onDelete != null) {
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    NeoIconButton(
                        Icons.Default.Edit, "ویرایش", onClick, buttonSize = 40.dp, iconSize = 19.dp,
                        tint = if (LocalNeo.current) Neo.Cyan else null,
                    )
                    NeoIconButton(
                        Icons.Default.Delete, "حذف", onDelete, buttonSize = 40.dp, iconSize = 19.dp,
                        tint = if (LocalNeo.current) Neo.TextSecondary else null,
                    )
                }
            }
        }
    }
}

/** تأیید حذف یک یادداشت؛ هم در صفحهٔ اصلی و هم در «همه یادداشت‌ها» استفاده می‌شود. */
@Composable
fun DeleteNoteDialog(note: NoteEntity, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حذف شود؟") },
        text = { Text(note.displayTitle, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("حذف") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

/** دیالوگ نام پک (ساخت یا تغییر نام) با پیش‌نمایش زندهٔ آیکن خودکار. */
@Composable
fun PackNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NeoIconTile(packIconFor(name), tileSize = 52.dp)
                    OutlinedTextField(
                        value = name, onValueChange = { name = it.take(NoteRepository.MAX_PACK_NAME) },
                        modifier = Modifier.weight(1f), singleLine = true, label = { Text("نام پک") },
                    )
                }
                Text(
                    "آیکن به‌صورت خودکار از روی نام انتخاب می‌شود.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
fun StepperRow(label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeoStepButton("−", onMinus)
            NeoStepButton("+", onPlus)
        }
    }
}

/** نوار پیشرفت یک یادداشت: از زمان ساخت (یا نوبت قبلی در تکرارشونده‌ها) تا لحظه‌ی یادآوری. */
@Composable
fun ReminderProgress(note: NoteEntity, end: Long) {
    val start = when (note.repeat) {
        Repeat.DAILY -> maxOf(note.createdAt, end - 86_400_000L)
        Repeat.WEEKLY -> maxOf(note.createdAt, end - 7 * 86_400_000L)
        else -> note.createdAt
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(end) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= end) break
            delay(if (end - now <= 3_600_000L) 1_000L else 15_000L)
        }
    }
    val span = (end - start).coerceAtLeast(1L)
    val target = ((now - start).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    val shown by animateFloatAsState(target, tween(500), label = "reminderProgress")
    val left = end - now
    val label = when {
        left <= 0L -> "زمان یادآوری رسید"
        left < 60_000L -> Fmt.digits("${left / 1000 + 1} ثانیه دیگر")
        else -> Fmt.remaining(
            LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault()),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault()),
        )
    }
    Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NeoProgress(shown)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(AppIcons.Schedule, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
