package app.yadavar.ui

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.yadavar.data.AppSettings
import app.yadavar.data.DEFAULT_PACK_ID
import app.yadavar.data.DEFAULT_PACK_NAME
import app.yadavar.data.NoteEntity
import app.yadavar.data.NoteRepository
import app.yadavar.data.PackEntity
import app.yadavar.data.Repeat
import app.yadavar.data.SettingsStore
import app.yadavar.pack.PackCommand
import app.yadavar.pack.PackCommandParser
import app.yadavar.reminder.ReminderEngine
import app.yadavar.reminder.ReminderScheduler
import app.yadavar.speech.SpeechController
import app.yadavar.time.Fmt
import app.yadavar.time.ParseStatus
import app.yadavar.time.PersianTimeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class Screen { HOME, EDITOR, NOTES, PACKS, SETTINGS }
enum class VoicePhase { READY, LISTENING, PROCESSING, SAVED, ERROR }

data class VoiceUi(
    val phase: VoicePhase = VoicePhase.READY,
    val partial: String = "",
    val seconds: Int = 0,
    val error: String? = null,
    val needsAppSettings: Boolean = false,
)

data class EditorState(
    val existing: NoteEntity? = null,
    val text: String = "",
    val title: String = "",
    val titleTouched: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderTouched: Boolean = false,
    val dateTime: LocalDateTime? = null,
    val alternative: LocalDateTime? = null,
    val timeTouched: Boolean = false,
    val status: ParseStatus = ParseStatus.NO_TIME,
    val message: String? = null,
    val repeat: Int = Repeat.NONE,
    val fromVoice: Boolean = false,
    /** مدت زمان نسبی گفته‌شده (دقیقه)؛ اگر کاربر زمان را دستی تغییر ندهد، هنگام تأیید از همان لحظه حساب می‌شود. */
    val relativeMinutes: Long? = null,
    /** پکی که کار در آن ذخیره می‌شود. */
    val packId: Long = DEFAULT_PACK_ID,
)

/** «به کارهای X اضافه کن» ولی پک X وجود ندارد؛ منتظر تأیید کاربر برای ساخت آن. */
data class PackPrompt(val name: String, val taskText: String)

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repo = NoteRepository(app)
    private val settingsStore = SettingsStore(app)

    val settings: StateFlow<AppSettings> = settingsStore.state
    val notes: StateFlow<List<NoteEntity>> =
        repo.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val packs: StateFlow<List<PackEntity>> =
        repo.packs.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            listOf(PackEntity(DEFAULT_PACK_ID, DEFAULT_PACK_NAME, 0)),
        )

    private val _packFilter = MutableStateFlow<Long?>(null)
    /** فیلتر پک در صفحهٔ «همه یادداشت‌ها»؛ null یعنی همهٔ پک‌ها. */
    val packFilter: StateFlow<Long?> = _packFilter.asStateFlow()
    private val _packPrompt = MutableStateFlow<PackPrompt?>(null)
    val packPrompt: StateFlow<PackPrompt?> = _packPrompt.asStateFlow()

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()
    private val _voice = MutableStateFlow(VoiceUi())
    val voice: StateFlow<VoiceUi> = _voice.asStateFlow()
    private val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    private val _resumeTick = MutableStateFlow(0)
    val resumeTick: StateFlow<Int> = _resumeTick.asStateFlow()

    private var returnScreen = Screen.HOME
    private var packsReturn = Screen.HOME
    private var timerJob: Job? = null

    private val speech = SpeechController(app, object : SpeechController.Callbacks {
        override fun onPartial(text: String) {
            _voice.update { if (it.phase == VoicePhase.LISTENING) it.copy(partial = text) else it }
        }

        override fun onFinal(text: String) = handleRecognized(text)

        override fun onError(message: String) {
            timerJob?.cancel()
            _voice.value = VoiceUi(phase = VoicePhase.ERROR, error = message)
        }
    })

    init {
        viewModelScope.launch(Dispatchers.IO) { ReminderEngine.rescheduleAll(app, showMissed = true) }
    }

    private fun say(m: String) { _messages.tryEmit(m) }

    // ---------- صدا ----------
    fun startListening() {
        if (_voice.value.phase == VoicePhase.LISTENING) return
        if (!speech.isAvailable()) {
            _voice.value = VoiceUi(
                phase = VoicePhase.ERROR,
                error = "سرویس تشخیص گفتار روی این دستگاه پیدا نشد. برنامه «Google» یا سرویس تشخیص گفتار دستگاه را نصب/فعال کنید؛ تا آن زمان می‌توانید از «یادداشت متنی» استفاده کنید.",
            )
            return
        }
        _voice.value = VoiceUi(phase = VoicePhase.LISTENING)
        try {
            speech.start()
        } catch (e: Exception) {
            _voice.value = VoiceUi(phase = VoicePhase.ERROR, error = "شروع تشخیص گفتار ممکن نشد.")
            return
        }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var s = 0
            while (true) {
                delay(1000)
                s++
                _voice.update { if (it.phase == VoicePhase.LISTENING) it.copy(seconds = s) else it }
                if (s >= MAX_LISTEN_SECONDS) { stopListening(); break }
            }
        }
    }

    fun stopListening() {
        if (_voice.value.phase != VoicePhase.LISTENING) return
        timerJob?.cancel()
        _voice.update { it.copy(phase = VoicePhase.PROCESSING) }
        speech.stop()
    }

    /** لغو بدون ذخیره؛ هنگام خروج از برنامه هم صدا زده می‌شود تا میکروفون آزاد شود. */
    fun cancelListening() {
        timerJob?.cancel()
        speech.destroy()
        if (_voice.value.phase == VoicePhase.LISTENING || _voice.value.phase == VoicePhase.PROCESSING) {
            _voice.value = VoiceUi()
        }
    }

    fun onMicPermissionDenied() {
        _voice.value = VoiceUi(
            phase = VoicePhase.ERROR,
            error = "برای استفاده از دستیار صوتی، مجوز میکروفون لازم است. آن را در تنظیمات برنامه فعال کنید.",
            needsAppSettings = true,
        )
    }

    private fun handleRecognized(text: String) {
        timerJob?.cancel()
        _voice.value = VoiceUi(phase = VoicePhase.PROCESSING)
        viewModelScope.launch {
            delay(300)
            when (val cmd = PackCommandParser.parse(text)) {
                is PackCommand.Create -> createPackByVoice(cmd.name)
                is PackCommand.AddToPack -> addToPackByVoice(cmd, text)
                null -> { openEditorFromText(text, fromVoice = true); _voice.value = VoiceUi() }
            }
        }
    }

    // ---------- دستورهای صوتی پک ----------
    private suspend fun createPackByVoice(name: String) {
        if (name.isBlank()) {
            say("نام پک را نشنیدم. مثلاً بگویید «یک پک با عنوان دانشگاه بساز».")
            _voice.value = VoiceUi()
            return
        }
        val existing = repo.findPackByName(name)
        if (existing != null) {
            say("پک «${existing.name}» از قبل وجود دارد.")
            _voice.value = VoiceUi()
            return
        }
        val pack = repo.createPack(name)
        if (pack == null) {
            say("نام پک معتبر نیست.")
            _voice.value = VoiceUi()
        } else {
            say("پک «${pack.name}» ساخته شد.")
            _voice.value = VoiceUi(phase = VoicePhase.SAVED)
            delay(2500)
            _voice.update { if (it.phase == VoicePhase.SAVED) VoiceUi() else it }
        }
    }

    private suspend fun addToPackByVoice(cmd: PackCommand.AddToPack, original: String) {
        val pack = repo.findPackByName(cmd.packName)
        _voice.value = VoiceUi()
        when {
            pack != null && cmd.taskText.isNotBlank() -> openEditorFromText(cmd.taskText, fromVoice = true, packId = pack.id)
            // دستور است ولی متن کار گفته نشده
            pack != null || cmd.explicit && cmd.taskText.isBlank() ->
                say("متن کار را هم بگویید؛ مثلاً «فردا تحویل پروژه، به کارهای ${pack?.name ?: cmd.packName} اضافه کن».")
            // پک وجود ندارد؛ قبل از ساختن از کاربر تأیید می‌گیریم
            cmd.explicit -> _packPrompt.value = PackPrompt(cmd.packName, cmd.taskText)
            // جملهٔ معمولی که شبیه دستور بود (مثل «سبد رو توی ماشین بذار»)؛ مثل یادداشت عادی رفتار می‌کنیم
            else -> openEditorFromText(original, fromVoice = true)
        }
    }

    /** کاربر ساخت پک جدید را تأیید کرد. */
    fun confirmPackPrompt() {
        val p = _packPrompt.value ?: return
        _packPrompt.value = null
        viewModelScope.launch {
            val pack = repo.createPack(p.name)
            if (pack == null) say("نام پک معتبر نیست؛ کار در «عمومی» قرار می‌گیرد.")
            else say("پک «${pack.name}» ساخته شد.")
            openEditorFromText(p.taskText, fromVoice = true, packId = pack?.id ?: DEFAULT_PACK_ID)
        }
    }

    /** کاربر نخواست پک ساخته شود؛ کار در «عمومی» می‌ماند. */
    fun declinePackPrompt() {
        val p = _packPrompt.value ?: return
        _packPrompt.value = null
        openEditorFromText(p.taskText, fromVoice = true)
    }

    // ---------- ناوبری ----------
    fun go(s: Screen) {
        if (s == Screen.PACKS) packsReturn = if (_screen.value == Screen.NOTES) Screen.NOTES else Screen.HOME
        _screen.value = s
    }

    fun back() {
        when (_screen.value) {
            Screen.EDITOR -> _screen.value = returnScreen
            Screen.PACKS -> _screen.value = packsReturn
            Screen.NOTES, Screen.SETTINGS -> _screen.value = Screen.HOME
            Screen.HOME -> {}
        }
    }

    fun onResume() { _resumeTick.update { it + 1 } }

    fun openNote(id: Long) {
        viewModelScope.launch { repo.get(id)?.let { editNote(it) } }
    }

    // ---------- ویرایشگر ----------
    fun newTextNote() {
        returnScreen = _screen.value
        _editor.value = EditorState(packId = _packFilter.value ?: DEFAULT_PACK_ID)
        _screen.value = Screen.EDITOR
    }

    private fun openEditorFromText(text: String, fromVoice: Boolean, packId: Long = DEFAULT_PACK_ID) {
        val p = PersianTimeParser.parse(text, LocalDateTime.now())
        returnScreen = Screen.HOME
        _editor.value = EditorState(
            text = text,
            title = Fmt.digits(p.title),
            reminderEnabled = p.suggestReminder,
            dateTime = p.suggested ?: if (p.suggestReminder) defaultTime() else null,
            alternative = p.alternative,
            status = p.status,
            message = p.message,
            fromVoice = fromVoice,
            relativeMinutes = p.relativeMinutes,
            packId = packId,
        )
        _screen.value = Screen.EDITOR
    }

    fun editNote(n: NoteEntity) {
        returnScreen = if (_screen.value == Screen.NOTES) Screen.NOTES else Screen.HOME
        val dt = n.remindAt?.let { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), ZoneId.systemDefault()) }
        _editor.value = EditorState(
            existing = n, text = n.text, title = n.title, titleTouched = true,
            reminderEnabled = n.remindAt != null, reminderTouched = true,
            dateTime = dt, timeTouched = true,
            status = if (dt != null) statusFor(dt) else ParseStatus.NO_TIME,
            repeat = n.repeat,
            packId = n.packId,
        )
        _screen.value = Screen.EDITOR
    }

    private fun defaultTime(): LocalDateTime =
        LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1)

    private fun statusFor(dt: LocalDateTime) =
        if (dt.isAfter(LocalDateTime.now())) ParseStatus.OK else ParseStatus.IN_PAST

    fun onTextChange(t: String) {
        val p = PersianTimeParser.parse(t, LocalDateTime.now())
        _editor.update { s ->
            var n = s.copy(text = t)
            if (!s.titleTouched) n = n.copy(title = Fmt.digits(p.title))
            if (!s.timeTouched && p.suggested != null) {
                n = n.copy(
                    dateTime = p.suggested, alternative = p.alternative, status = p.status, message = p.message,
                    relativeMinutes = p.relativeMinutes,
                )
            }
            if (!s.reminderTouched) n = n.copy(reminderEnabled = p.suggestReminder && n.dateTime != null)
            n
        }
    }

    fun onTitleChange(t: String) { _editor.update { it.copy(title = t, titleTouched = true) } }

    fun setReminderEnabled(on: Boolean) {
        _editor.update { s ->
            val dt = s.dateTime ?: if (on) defaultTime() else null
            s.copy(
                reminderEnabled = on, reminderTouched = true, dateTime = dt,
                status = if (dt != null) statusFor(dt) else s.status,
            )
        }
    }

    fun adjust(unit: ChronoUnit, amount: Long) {
        _editor.update { s ->
            val dt = (s.dateTime ?: defaultTime()).plus(amount, unit)
            s.copy(dateTime = dt, alternative = null, timeTouched = true, status = statusFor(dt), message = null)
        }
    }

    fun setQuick(kind: Int) {
        val now = LocalDateTime.now().withSecond(0).withNano(0)
        val dt = when (kind) {
            3 -> LocalDateTime.now().withNano(0).plusMinutes(1)
            0 -> now.plusMinutes(10)
            1 -> now.plusHours(1)
            else -> now.toLocalDate().plusDays(1).atTime(9, 0)
        }
        _editor.update { it.copy(dateTime = dt, alternative = null, timeTouched = true, status = statusFor(dt), message = null) }
    }

    fun useAlternative() {
        _editor.update { s ->
            val alt = s.alternative ?: return@update s
            s.copy(dateTime = alt, alternative = s.dateTime, timeTouched = true, status = statusFor(alt), message = null)
        }
    }

    fun setRepeat(r: Int) { _editor.update { it.copy(repeat = r) } }

    fun save() {
        val s = _editor.value
        val text = s.text.trim()
        if (text.isEmpty()) { say("متن خالی است."); return }
        var remindAt: Long? = null
        // «یک دقیقه دیگر» گفته شده ولی تا تأیید زمان گذشته؛ از لحظه‌ی تأیید دوباره حساب می‌شود.
        val dtFinal: LocalDateTime? = s.dateTime.let { d ->
            val rel = s.relativeMinutes
            if (s.reminderEnabled && d != null && !d.isAfter(LocalDateTime.now()) && rel != null && !s.timeTouched) {
                LocalDateTime.now().withNano(0).plusMinutes(rel)
            } else d
        }
        if (s.reminderEnabled) {
            val dt = dtFinal
            if (dt == null || !dt.isAfter(LocalDateTime.now())) {
                say("زمان یادآوری در گذشته است؛ زمان را اصلاح کنید.")
                return
            }
            remindAt = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        val base = s.existing
        val note = NoteEntity(
            id = base?.id ?: 0L,
            title = if (remindAt != null) s.title.trim() else "",
            text = text,
            createdAt = base?.createdAt ?: System.currentTimeMillis(),
            remindAt = remindAt,
            repeat = if (remindAt != null) s.repeat else Repeat.NONE,
            done = if (remindAt != null) false else (base?.done ?: false),
            fired = false,
            packId = s.packId,
        )
        viewModelScope.launch {
            val out = repo.save(note)
            val dt = dtFinal
            val msg = StringBuilder()
            if (out.scheduled && dt != null) {
                msg.append("یادآوری برای ").append(Fmt.dateTime(dt, settings.value.use24h)).append(" برنامه‌ریزی شد.")
                if (!out.exact) msg.append(" چون مجوز آلارم دقیق داده نشده، زمان ممکن است کمی تأخیر داشته باشد (تنظیمات ← مجوزها).")
                if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) msg.append(" اعلان‌های برنامه خاموش است و یادآوری نمایش داده نمی‌شود.")
            } else {
                msg.append("یادداشت ذخیره شد.")
            }
            say(msg.toString())
            _screen.value = Screen.HOME
            _voice.value = VoiceUi(phase = VoicePhase.SAVED)
            delay(2500)
            _voice.update { if (it.phase == VoicePhase.SAVED) VoiceUi() else it }
        }
    }

    // ---------- پک‌ها ----------
    fun setEditorPack(id: Long) { _editor.update { it.copy(packId = id) } }
    fun setPackFilter(id: Long?) { _packFilter.value = id }

    /** نمایش کارهای یک پک در صفحهٔ «همه یادداشت‌ها». */
    fun showPack(id: Long) {
        _packFilter.value = id
        _screen.value = Screen.NOTES
    }

    /** ساخت پک از رابط (صفحهٔ پک‌ها یا ویرایشگر)؛ با [selectInEditor] پک جدید در ویرایشگر انتخاب می‌شود. */
    fun createPack(name: String, selectInEditor: Boolean = false) {
        viewModelScope.launch {
            val existing = repo.findPackByName(name)
            val pack = existing ?: repo.createPack(name)
            when {
                pack == null -> say("نام پک معتبر نیست.")
                existing != null -> say("پک «${pack.name}» از قبل وجود دارد.")
                else -> say("پک «${pack.name}» ساخته شد.")
            }
            if (selectInEditor && pack != null) setEditorPack(pack.id)
        }
    }

    fun renamePack(id: Long, name: String) {
        viewModelScope.launch {
            if (repo.renamePack(id, name)) say("نام پک تغییر کرد.")
            else say("این نام قابل استفاده نیست (خالی است، تکراری است یا پک پیش‌فرض است).")
        }
    }

    fun deletePack(id: Long) {
        viewModelScope.launch {
            if (repo.deletePack(id)) {
                if (_packFilter.value == id) _packFilter.value = null
                say("پک حذف شد؛ کارهای آن به «$DEFAULT_PACK_NAME» منتقل شدند.")
            }
        }
    }

    // ---------- یادداشت‌ها ----------
    fun toggleDone(n: NoteEntity) { viewModelScope.launch { repo.setDone(n, !n.done) } }
    fun delete(n: NoteEntity) { viewModelScope.launch { repo.delete(n); say("حذف شد.") } }

    // ---------- تنظیمات ----------
    fun setTheme(v: Int) = settingsStore.setTheme(v)
    fun setUse24h(v: Boolean) = settingsStore.setUse24h(v)
    fun setSound(v: Boolean) = settingsStore.setSound(v)
    fun setVibrate(v: Boolean) = settingsStore.setVibrate(v)
    fun canScheduleExact(): Boolean = ReminderScheduler(app).canScheduleExact()

    override fun onCleared() {
        timerJob?.cancel()
        speech.destroy()
    }

    companion object { const val MAX_LISTEN_SECONDS = 60 }
}
