package app.yadavar.pack

/** دستورهای صوتی مربوط به پک‌ها. */
sealed interface PackCommand {
    /** «یک پک با عنوان دانشگاه بساز» — [name] ممکن است خالی باشد (نام شنیده نشد). */
    data class Create(val name: String) : PackCommand

    /**
     * «فردا تحویل پروژه، به کارهای دانشگاه اضافه کن».
     * [taskText] متن کار بدون عبارت دستور است (ممکن است خالی باشد).
     * [explicit] یعنی کاربر صریحاً از واژهٔ «پک / کارهای …» استفاده کرده است؛ در غیر این صورت
     * فقط وقتی دستور محسوب می‌شود که پکی با همین نام از قبل وجود داشته باشد
     * (تا جمله‌هایی مثل «سبد رو توی ماشین بذار» اشتباهاً دستور تلقی نشوند).
     */
    data class AddToPack(val taskText: String, val packName: String, val explicit: Boolean) : PackCommand
}

/** تحلیل‌گر دستورهای پک؛ کاملاً مستقل از اندروید و قابل تست. */
object PackCommandParser {
    private const val ZW = "\u200c"
    /** فاصله (شامل نیم‌فاصله). */
    private const val SP = "[\\s\u200c]+"
    private const val PACK_WORD = "(?:پکیج|پکی|پک|دسته|گروه|پوشه)"
    private const val KAR_WORD = "(?:کارهای|کارای|کار|یادداشت[\\s\u200c]?های|یادداشت)"
    private const val ADD_VERB =
        "(?:اضافه$SP(?:کن|کنید|بکن|بکنید)|بذار|بزار|بگذار|بگذارید|بنداز|ثبت$SP(?:کن|کنید)|ذخیره$SP(?:کن|کنید)|قرار$SP(?:بده|بدهید))"
    private const val CREATE_VERB =
        "(?:بساز(?:ید|ی|ین)?|(?:درست|ایجاد|تعریف|خلق)$SP(?:کن|کنید|بکن|بکنید))"

    private val CREATE_VERB_RE = Regex("(?<=^|\\s)$CREATE_VERB(?=\\s|$)")
    private val PACK_RE = Regex("(?<=^|\\s)$PACK_WORD(?=\\s|$)")
    private val ADD_RE = Regex(
        "^(?:(.*)\\s+)?(?:به|توی|تو|در|داخل)$SP(?:($PACK_WORD)$SP)?(?:($KAR_WORD)$SP)?" +
            "(\\S+(?:\\s+\\S+){0,3}?)\\s+$ADD_VERB$",
    )
    private val SPACES = Regex("\\s+")
    private val SENTENCE_DOT = Regex("[.,]+(?=\\s|$)")
    private val LEAD_KEY = Regex("^(?:(?:$PACK_WORD|$KAR_WORD|مربوط$SP" + "به|مربوط)\\s+)+")
    private val LEAD_NAME = Regex(
        "^(?:(?:جدید|مربوط$SP" + "به|مربوط|برای|با$SP" + "عنوان|با$SP" + "نام|به$SP" + "عنوان|به$SP" + "نام|عنوان|نام|که|به|" +
            "برام|برایم|لطفا|لطفاً|یک|یه|$KAR_WORD)\\s+)+",
    )
    private val TRAIL_NAME = Regex("(?:\\s+(?:را|رو|هم|لطفا|لطفاً|جدید|و|برام|برایم|بهم))+$")
    private val TRAIL_BEFORE = Regex("(?:\\s+(?:یک|یه|لطفا|لطفاً|برام|برایم|بهم|و))+$")
    private val LEAD_TASK = Regex("^(?:این$SP" + "کار|این)(?:$SP(?:را|رو))?(?:\\s+|$)")
    private val TRAIL_TASK = Regex("(?:\\s+و)+$")
    private val FILLER_ONLY = Regex("^(?:(?:یک|یه|لطفا|لطفاً|برام|برایم|بهم|و|جدید)\\s*)+$")

    /** متن را برای تطبیق دستور سبک‌پاک می‌کند (بدون دست‌زدن به رقم‌ها و نیم‌فاصله). */
    internal fun light(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            when {
                ch == 'ي' || ch == 'ى' -> sb.append('ی')
                ch == 'ك' -> sb.append('ک')
                ch in '\u064B'..'\u065F' || ch == '\u0640' -> Unit
                ch == '\u200f' || ch == '\u200e' || ch == '\u00a0' -> sb.append(' ')
                ch == '،' || ch == '؛' || ch == '؟' || ch == '?' || ch == '!' ||
                    ch == '«' || ch == '»' || ch == '"' || ch == '(' || ch == ')' -> sb.append(' ')
                else -> sb.append(ch)
            }
        }
        return sb.toString().replace(SENTENCE_DOT, " ").replace(SPACES, " ").trim()
    }

    private fun latinDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            sb.append(
                when (ch) {
                    in '\u06F0'..'\u06F9' -> '0' + (ch - '\u06F0')
                    in '\u0660'..'\u0669' -> '0' + (ch - '\u0660')
                    else -> ch
                },
            )
        }
        return sb.toString()
    }

    /** کلید مقایسهٔ نام پک: «کارهای دانشگاه»، «پک دانشگاه» و «دانشگاه» یکی حساب می‌شوند. */
    fun key(name: String): String {
        var t = latinDigits(light(name)).replace(ZW, " ").lowercase()
        t = t.replace(SPACES, " ").trim()
        return LEAD_KEY.replace(t, "")
    }

    /** نام نمایشی پک از متن گفته‌شده؛ خالی یعنی نام مشخص نیست. */
    private fun cleanName(s: String, trail: Regex = TRAIL_NAME): String {
        var t = s.trim()
        while (true) {
            val n = trail.replace(LEAD_NAME.replace(t, ""), "").trim()
            if (n == t) break
            t = n
        }
        return if (FILLER_ONLY.matches(t)) "" else t
    }

    fun parse(raw: String): PackCommand? {
        val t = light(raw)
        if (t.isBlank()) return null
        val add = parseAdd(t)
        if (add != null && add.taskText.isNotBlank()) return add
        parseCreate(t)?.let { return it }
        return add
    }

    private fun parseCreate(t: String): PackCommand.Create? {
        val verb = CREATE_VERB_RE.find(t) ?: return null
        val s = (t.substring(0, verb.range.first) + " " + t.substring(verb.range.last + 1)).replace(SPACES, " ").trim()
        val pack = PACK_RE.find(s) ?: return null
        val after = s.substring(pack.range.last + 1)
        val before = s.substring(0, pack.range.first)
        var name = cleanName(after)
        if (name.isBlank()) name = cleanName(before, TRAIL_BEFORE)
        if (name.split(' ').size > 5) return null
        return PackCommand.Create(name)
    }

    private fun parseAdd(t: String): PackCommand.AddToPack? {
        val m = ADD_RE.matchEntire(t) ?: return null
        val packWord = m.groupValues[2]
        val karWord = m.groupValues[3]
        val name = cleanName(m.groupValues[4])
        if (name.isBlank() || key(name).isBlank()) return null
        var task = m.groupValues[1].trim()
        // «یک پک به نام دانشگاه اضافه کن» ساخت پک است، نه افزودن کار.
        if (task.isNotEmpty() && PACK_RE.containsMatchIn(task.substringAfterLast(' '))) return null
        task = LEAD_TASK.replace(task, "").let { TRAIL_TASK.replace(it, "") }.trim()
        val explicit = packWord.isNotEmpty() || karWord.isNotEmpty()
        if (task.isEmpty() && !explicit) return null
        return PackCommand.AddToPack(taskText = task, packName = name, explicit = explicit)
    }
}
