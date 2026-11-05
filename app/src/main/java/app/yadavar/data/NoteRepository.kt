package app.yadavar.data

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.room.withTransaction
import app.yadavar.pack.PackCommandParser
import app.yadavar.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow

data class SaveOutcome(val scheduled: Boolean, val exact: Boolean)

class NoteRepository(context: Context) {
    private val ctx = context.applicationContext
    private val db = AppDatabase.get(ctx)
    private val dao = db.noteDao()
    private val scheduler = ReminderScheduler(ctx)

    val notes: Flow<List<NoteEntity>> = dao.observeAll()
    val packs: Flow<List<PackEntity>> = dao.observePacks()

    suspend fun get(id: Long): NoteEntity? = dao.getById(id)

    suspend fun save(input: NoteEntity): SaveOutcome {
        // اگر پک انتخاب‌شده در این فاصله حذف شده باشد، کار در «عمومی» ذخیره می‌شود.
        val note = if (dao.packCount(input.packId) > 0) input else input.copy(packId = DEFAULT_PACK_ID)
        val id = if (note.id == 0L) dao.insert(note) else { dao.update(note); note.id }
        val at = note.remindAt
        if (at != null && !note.done && at > System.currentTimeMillis()) {
            return SaveOutcome(scheduled = true, exact = scheduler.schedule(id, at))
        }
        cancelAlarmAndNotification(id)
        return SaveOutcome(scheduled = false, exact = true)
    }

    suspend fun delete(note: NoteEntity) {
        cancelAlarmAndNotification(note.id)
        dao.delete(note)
    }

    suspend fun setDone(note: NoteEntity, done: Boolean) {
        dao.update(note.copy(done = done))
        val at = note.remindAt
        if (done || at == null) cancelAlarmAndNotification(note.id)
        else if (at > System.currentTimeMillis()) scheduler.schedule(note.id, at)
    }

    private fun cancelAlarmAndNotification(id: Long) {
        scheduler.cancel(id)
        NotificationManagerCompat.from(ctx).cancel(id.toInt())
    }

    // ---------- پک‌ها ----------

    /** نام پک را مرتب می‌کند: فاصله‌های اضافه حذف و طول محدود می‌شود. */
    private fun tidy(name: String) = name.trim().replace(Regex("\\s+"), " ").take(MAX_PACK_NAME)

    /** پکی که با این نام (بدون در نظر گرفتن «پک/کارهای» و تفاوت حروف/ارقام) وجود دارد، یا null. */
    suspend fun findPackByName(name: String): PackEntity? {
        val key = PackCommandParser.key(name)
        if (key.isBlank()) return null
        return dao.getPacks().firstOrNull { PackCommandParser.key(it.name) == key }
    }

    /** پک جدید می‌سازد؛ اگر پکی با همین نام باشد همان را برمی‌گرداند. null یعنی نام نامعتبر است. */
    suspend fun createPack(name: String): PackEntity? {
        val clean = tidy(name)
        if (PackCommandParser.key(clean).isBlank()) return null
        findPackByName(clean)?.let { return it }
        val pack = PackEntity(name = clean, createdAt = System.currentTimeMillis())
        return pack.copy(id = dao.insertPack(pack))
    }

    /** تغییر نام پک؛ false اگر پک پیش‌فرض است، نام خالی است یا پک دیگری همین نام را دارد. */
    suspend fun renamePack(id: Long, name: String): Boolean {
        if (id == DEFAULT_PACK_ID) return false
        val clean = tidy(name)
        if (PackCommandParser.key(clean).isBlank()) return false
        val clash = findPackByName(clean)
        if (clash != null && clash.id != id) return false
        dao.renamePack(id, clean)
        return true
    }

    /** حذف پک؛ کارهای داخلش به «عمومی» منتقل می‌شوند (نه حذف). false اگر پک پیش‌فرض است. */
    suspend fun deletePack(id: Long): Boolean {
        if (id == DEFAULT_PACK_ID) return false
        db.withTransaction {
            dao.moveNotes(id, DEFAULT_PACK_ID)
            dao.deletePack(id)
        }
        return true
    }

    companion object { const val MAX_PACK_NAME = 30 }
}
