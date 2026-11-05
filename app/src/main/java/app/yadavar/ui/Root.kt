package app.yadavar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.DialogProperties

@Composable
fun YadAvarRoot(vm: MainViewModel, dark: Boolean) {
    YadAvarTheme(dark) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val snackbar = remember { SnackbarHostState() }
            LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
            val screen by vm.screen.collectAsState()
            val packPrompt by vm.packPrompt.collectAsState()
            BackHandler(enabled = screen != Screen.HOME) { vm.back() }
            // پس‌زمینهٔ نئومورفیک (گرادیان + موج‌ها) پشت همهٔ صفحه‌ها؛ Scaffold شفاف است
            NeoBackground {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbar) },
                    containerColor = Color.Transparent,
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        when (screen) {
                            Screen.HOME -> HomeScreen(vm)
                            Screen.EDITOR -> EditorScreen(vm)
                            Screen.NOTES -> NotesScreen(vm)
                            Screen.PACKS -> PacksScreen(vm)
                            Screen.SETTINGS -> SettingsScreen(vm)
                        }
                    }
                }
            }
            // «به کارهای X اضافه کن» ولی پک X وجود ندارد: قبل از ساخت از کاربر می‌پرسیم.
            packPrompt?.let { p ->
                AlertDialog(
                    onDismissRequest = { vm.declinePackPrompt() },
                    title = { Text("پک «${p.name}» وجود ندارد") },
                    text = { Text("پک جدید ساخته شود و این کار داخل آن برود؟\n«${p.taskText}»") },
                    confirmButton = { TextButton(onClick = { vm.confirmPackPrompt() }) { Text("ساخت پک و ادامه") } },
                    dismissButton = { TextButton(onClick = { vm.declinePackPrompt() }) { Text("ادامه در «عمومی»") } },
                    properties = DialogProperties(dismissOnClickOutside = false),
                )
            }
        }
    }
}
