package app.yadavar.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** پک پیش‌فرض («عمومی»): همیشه وجود دارد، قابل حذف یا تغییر نام نیست و کارهای بدون پک در آن قرار می‌گیرند. */
const val DEFAULT_PACK_ID = 1L
const val DEFAULT_PACK_NAME = "عمومی"

/** پک: گروهی از کارها (مثلاً «دانشگاه» یا «سایت»). */
@Entity(tableName = "packs")
data class PackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** عنوان کار (برای یادآوری‌ها؛ برای یادداشت ساده خالی است). */
    val title: String = "",
    val text: String,
    val createdAt: Long,
    /** زمان یادآوری (میلی‌ثانیه UTC)؛ null یعنی یادداشت ساده. */
    val remindAt: Long? = null,
    val repeat: Int = Repeat.NONE,
    val done: Boolean = false,
    /** آیا اعلان این نوبت نمایش داده شده است؟ */
    val fired: Boolean = false,
    /** پکی که این کار در آن است ([DEFAULT_PACK_ID] = عمومی). */
    @ColumnInfo(defaultValue = "1") val packId: Long = DEFAULT_PACK_ID,
) {
    val displayTitle: String get() = title.ifBlank { text }
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE remindAt IS NOT NULL AND done = 0")
    suspend fun getActiveReminders(): List<NoteEntity>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    // ----- پک‌ها -----
    @Query("SELECT * FROM packs ORDER BY id")
    fun observePacks(): Flow<List<PackEntity>>

    @Query("SELECT * FROM packs ORDER BY id")
    suspend fun getPacks(): List<PackEntity>

    @Query("SELECT COUNT(*) FROM packs WHERE id = :id")
    suspend fun packCount(id: Long): Int

    @Insert
    suspend fun insertPack(pack: PackEntity): Long

    @Query("UPDATE packs SET name = :name WHERE id = :id")
    suspend fun renamePack(id: Long, name: String)

    @Query("DELETE FROM packs WHERE id = :id")
    suspend fun deletePack(id: Long)

    @Query("UPDATE notes SET packId = :toPackId WHERE packId = :fromPackId")
    suspend fun moveNotes(fromPackId: Long, toPackId: Long)
}

/** نسخه ۲: جدول پک‌ها + ستون packId روی یادداشت‌ها؛ همهٔ کارهای قبلی به پک «عمومی» می‌روند. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `packs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO `packs` (`id`, `name`, `createdAt`) VALUES ($DEFAULT_PACK_ID, '$DEFAULT_PACK_NAME', 0)",
        )
        db.execSQL("ALTER TABLE `notes` ADD COLUMN `packId` INTEGER NOT NULL DEFAULT $DEFAULT_PACK_ID")
    }
}

/** مهاجرت‌ها: هنگام افزایش version باید Migration صریح اضافه شود (از fallbackToDestructiveMigration عمداً استفاده نشده). */
@Database(entities = [NoteEntity::class, PackEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "yadavar.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : RoomDatabase.Callback() {
                        // هم نصب تازه و هم پس از مهاجرت: پک پیش‌فرض همیشه وجود دارد.
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                "INSERT OR IGNORE INTO `packs` (`id`, `name`, `createdAt`) " +
                                    "VALUES ($DEFAULT_PACK_ID, '$DEFAULT_PACK_NAME', 0)",
                            )
                        }
                    })
                    .build().also { instance = it }
            }
    }
}
