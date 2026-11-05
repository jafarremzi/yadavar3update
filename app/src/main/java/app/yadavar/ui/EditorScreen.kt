package app.yadavar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.yadavar.data.Repeat
import app.yadavar.time.Fmt
import app.yadavar.time.ParseStatus
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(vm: MainViewModel) {
    val s by vm.editor.collectAsState()
    val settings by vm.settings.collectAsState()
    val packs by vm.packs.collectAsState()
    var newPackDialog by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    var askedNotif by remember { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> vm.save() }

    fun trySave() {
        val needAsk = s.reminderEnabled && Build.VERSION.SDK_INT >= 33 && !askedNotif &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needAsk) {
            askedNotif = true
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.save()
        }
    }

    val dt = s.dateTime
    // ساعت زنده: اگر ویرایشگر باز بماند و زمان از دست برود، همان لحظه «گذشته» نشان داده می‌شود
    var nowTick by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            nowTick = LocalDateTime.now()
        }
    }
    // زمان نسبی («یک دقیقه دیگر») بدون دست‌کاری: هنگام تأیید دوباره از همان لحظه حساب می‌شود
    val relativeStale = s.relativeMinutes != null && !s.timeTouched && dt != null && !dt.isAfter(nowTick)
    val pastNow = dt != null && !dt.isAfter(nowTick) && !relativeStale
    val blocked = s.text.isBlank() || (s.reminderEnabled && (dt == null || pastNow ||
        (s.status == ParseStatus.IN_PAST && !relativeStale) || s.status == ParseStatus.INVALID))

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NeoTopBar(
            if (s.existing != null) "ویرایش" else if (s.fromVoice) "تأیید گفتار" else "یادداشت جدید",
            onBack = { vm.back() },
        )

        NeoTextField(
            value = s.text, onValueChange = vm::onTextChange, modifier = Modifier.fillMaxWidth(),
            label = if (s.fromVoice) "متن تشخیص‌داده‌شده (قابل ویرایش)" else "متن", minLines = 3,
        )

        SectionTitle("پک", AppIcons.Folder)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (p in packs) {
                NeoChip(p.name, selected = s.packId == p.id, onClick = { vm.setEditorPack(p.id) }, icon = packIconFor(p.name))
            }
            NeoChip("+ پک جدید", selected = false, onClick = { newPackDialog = true })
        }

        NeoSurface(Modifier.fillMaxWidth(), corner = 22.dp, depth = 5.dp) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "یادآوری زمان‌دار", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(checked = s.reminderEnabled, onCheckedChange = vm::setReminderEnabled)
            }
        }

        if (s.reminderEnabled && dt != null) {
            NeoTextField(
                value = s.title, onValueChange = vm::onTitleChange, modifier = Modifier.fillMaxWidth(),
                label = "عنوان کار", singleLine = true,
            )

            s.message?.let { msg ->
                val bad = s.status == ParseStatus.IN_PAST || s.status == ParseStatus.INVALID
                Text(
                    msg,
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(if (bad) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(12.dp),
                    color = if (bad) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            if (relativeStale) {
                Text("با تأیید، مدت گفته‌شده دوباره از همین لحظه حساب می‌شود.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if ((s.status == ParseStatus.IN_PAST || pastNow) && s.message == null) {
                Text("این زمان گذشته است؛ زمان را تغییر دهید.", color = MaterialTheme.colorScheme.error)
            }

            NeoSurface(Modifier.fillMaxWidth(), corner = 26.dp, depth = 7.dp) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(Fmt.date(dt.toLocalDate()), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        Fmt.time(dt, settings.use24h), style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                    )
                    Text(Fmt.remaining(nowTick, dt), color = MaterialTheme.colorScheme.onSurfaceVariant)

                    s.alternative?.let { alt ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NeoChip(Fmt.time(dt, settings.use24h), selected = true, onClick = {})
                            NeoChip(Fmt.time(alt, settings.use24h), selected = false, onClick = { vm.useAlternative() })
                        }
                    }
                    StepperRow("روز", { vm.adjust(ChronoUnit.DAYS, -1) }, { vm.adjust(ChronoUnit.DAYS, 1) })
                    StepperRow("ساعت", { vm.adjust(ChronoUnit.HOURS, -1) }, { vm.adjust(ChronoUnit.HOURS, 1) })
                    StepperRow("دقیقه (۵ تا ۵ تا)", { vm.adjust(ChronoUnit.MINUTES, -5) }, { vm.adjust(ChronoUnit.MINUTES, 5) })
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NeoChip("۱ دقیقه دیگر", selected = false, onClick = { vm.setQuick(3) })
                        NeoChip("۱۰ دقیقه دیگر", selected = false, onClick = { vm.setQuick(0) })
                        NeoChip("۱ ساعت دیگر", selected = false, onClick = { vm.setQuick(1) })
                        NeoChip("فردا ۹ صبح", selected = false, onClick = { vm.setQuick(2) })
                    }
                }
            }

            SectionTitle("تکرار", AppIcons.Event)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (r in listOf(Repeat.NONE, Repeat.DAILY, Repeat.WEEKLY)) {
                    NeoChip(repeatLabel(r), selected = s.repeat == r, onClick = { vm.setRepeat(r) })
                }
            }
        }

        NeoButton(
            if (s.reminderEnabled) "تأیید و برنامه‌ریزی" else "ذخیره یادداشت",
            { trySave() }, Modifier.fillMaxWidth(), primary = true, enabled = !blocked,
        )
        NeoButton("انصراف", { vm.back() }, Modifier.fillMaxWidth())
    }

    if (newPackDialog) {
        PackNameDialog(
            title = "پک جدید", initial = "", confirmLabel = "ساخت",
            onConfirm = { vm.createPack(it, selectInEditor = true); newPackDialog = false },
            onDismiss = { newPackDialog = false },
        )
    }
}
