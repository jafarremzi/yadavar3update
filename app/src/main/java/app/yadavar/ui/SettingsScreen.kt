package app.yadavar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PermissionCard(title: String, ok: Boolean, explanation: String, actionLabel: String, onAction: () -> Unit) {
    NeoSurface(Modifier.fillMaxWidth(), corner = 22.dp, depth = 5.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title + " — " + if (ok) "فعال ✓" else "غیرفعال", fontWeight = FontWeight.SemiBold,
                color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text(explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            NeoButton(actionLabel, onAction)
        }
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val s by vm.settings.collectAsState()
    @Suppress("UNUSED_VARIABLE") val tick by vm.resumeTick.collectAsState() // بازخوانی وضعیت مجوزها پس از برگشت از تنظیمات سیستم
    val ctx = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NeoTopBar("تنظیمات", onBack = { vm.back() })

        SectionTitle("ظاهر")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeoChip("تیره", selected = s.theme == 1, onClick = { vm.setTheme(1) })
            NeoChip("روشن", selected = s.theme == 2, onClick = { vm.setTheme(2) })
            NeoChip("مطابق سیستم", selected = s.theme == 0, onClick = { vm.setTheme(0) })
        }
        Text(
            "ظاهر نئومورفیک در تم تیره نمایش داده می‌شود؛ تم روشن ظاهر ساده‌تری دارد.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle("زمان و اعلان")
        NeoSurface(Modifier.fillMaxWidth(), corner = 22.dp, depth = 5.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SwitchRow("قالب ۲۴ ساعته", s.use24h, vm::setUse24h)
                SwitchRow("صدای اعلان", s.sound, vm::setSound)
                SwitchRow("لرزش", s.vibrate, vm::setVibrate)
            }
        }
        Text(
            "صدا و لرزش در اندروید روی «کانال اعلان» اعمال می‌شود و ممکن است در تنظیمات سیستم هم بتوانید آن را تغییر دهید. حالت «مزاحم نشوید» و سکوت گوشی را هم رعایت می‌کند.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle("مجوزهای برنامه")
        PermissionCard(
            "اعلان‌ها", PermissionHelper.notificationsEnabled(ctx),
            "برای نمایش یادآوری لازم است. بدون آن، یادآوری ثبت می‌شود اما اعلانی دیده نمی‌شود.",
            "تنظیمات اعلان",
        ) { PermissionHelper.openNotificationSettings(ctx) }
        PermissionCard(
            "آلارم دقیق", PermissionHelper.exactAlarmAllowed(ctx),
            "برای اجرای یادآوری سر ساعت دقیق. بدون آن، اندروید ممکن است یادآوری را چند دقیقه دیرتر نمایش دهد.",
            "اجازه آلارم دقیق",
        ) { PermissionHelper.openExactAlarmSettings(ctx) }
        PermissionCard(
            "معافیت از بهینه‌سازی باتری", PermissionHelper.ignoringBatteryOptimizations(ctx),
            "اختیاری: روی بعضی گوشی‌ها (مثلاً سامسونگ، شیائومی، هوآوی) سیستم برنامه‌های بسته‌شده را محدود می‌کند و ممکن است یادآوری دیر برسد.",
            "تنظیمات باتری",
        ) { PermissionHelper.openBatterySettings(ctx) }
        Text(
            "میکروفون: فقط وقتی دکمه را لمس می‌کنید فعال می‌شود و با خروج از برنامه آزاد می‌شود. تشخیص گفتار توسط سرویس نصب‌شده روی گوشی انجام می‌شود و ممکن است به اینترنت نیاز داشته باشد؛ کار کردن آفلاین فارسی تضمین نمی‌شود.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NeoButton("تنظیمات برنامه در سیستم", { PermissionHelper.openAppSettings(ctx) })

        SectionTitle("محدودیت‌های شناخته‌شده")
        Text(
            "• اگر برنامه را از «برنامه‌های اخیر» با Force Stop ببندید، اندروید همه آلارم‌های آن را پاک می‌کند؛ با باز کردن دوباره برنامه، یادآوری‌ها دوباره برنامه‌ریزی می‌شوند و موارد از دست‌رفته نمایش داده می‌شوند.\n• پس از ریبوت و به‌روزرسانی برنامه، یادآوری‌ها خودکار بازیابی می‌شوند.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle("سازنده", AppIcons.Favorite)
        NeoSurface(Modifier.fillMaxWidth(), corner = 26.dp, depth = 7.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    NeoIconTile(AppIcons.Instagram, tileSize = 58.dp, iconSize = 30.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "سازنده و توسعه‌دهندهٔ برنامه", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            AppInfo.INSTAGRAM_HANDLE, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                NeoButton(
                    "مشاهدهٔ صفحهٔ اینستاگرام",
                    {
                        // اگر اینستاگرام یا مرورگری برای باز کردن لینک نبود، برنامه نباید بسته شود
                        try { uriHandler.openUri(AppInfo.INSTAGRAM_URL) } catch (e: Exception) { /* بدون برنامهٔ مناسب: نادیده گرفته می‌شود */ }
                    },
                    Modifier.fillMaxWidth(), icon = AppIcons.Instagram, primary = true,
                )
            }
        }
    }
}
