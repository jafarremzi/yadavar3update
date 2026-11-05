package app.yadavar

import app.yadavar.pack.PackCommand
import app.yadavar.pack.PackCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PackCommandParserTest {
    private fun p(s: String) = PackCommandParser.parse(s)
    private fun add(task: String, pack: String, explicit: Boolean) = PackCommand.AddToPack(task, pack, explicit)

    // ---------- ساخت پک ----------
    @Test fun createRelatedTo() { assertEquals(PackCommand.Create("دانشگاه"), p("پک مربوط به کارهای دانشگاه رو بساز")) }
    @Test fun createWithTitle() { assertEquals(PackCommand.Create("دانشگاه"), p("یک پک با عنوان دانشگاه بساز")) }
    @Test fun createGeneral() { assertEquals(PackCommand.Create("عمومی"), p("پک کارهای عمومی را بساز")) }
    @Test fun createFor() { assertEquals(PackCommand.Create("سایت"), p("یک پک برای کارهای سایت درست کن")) }
    @Test fun createNameBeforePack() { assertEquals(PackCommand.Create("دانشگاه"), p("برای کارهای دانشگاه یک پک بساز")) }
    @Test fun createByName() { assertEquals(PackCommand.Create("پروژه ها"), p("یه پک جدید به نام پروژه ها ایجاد کن")) }
    @Test fun createPolite() { assertEquals(PackCommand.Create("ورزش"), p("لطفا پکی به نام ورزش بسازید")) }
    @Test fun createWithPunctuation() { assertEquals(PackCommand.Create("دانشگاه"), p("پک دانشگاه را بساز.")) }
    @Test fun createWithoutName() { assertEquals(PackCommand.Create(""), p("یک پک بساز")) }

    // ---------- افزودن کار به پک ----------
    @Test fun addSameSentence() {
        assertEquals(add("فردا تحویل پروژه", "دانشگاه", true), p("فردا تحویل پروژه، به کارهای دانشگاه اضافه کن"))
    }
    @Test fun addWithPackWord() {
        assertEquals(add("فردا ساعت ۹ جلسه دارم", "سایت", true), p("فردا ساعت ۹ جلسه دارم به پک سایت اضافه کن"))
    }
    @Test fun addWithPutVerb() {
        assertEquals(add("خرید شیر", "عمومی", true), p("خرید شیر رو توی کارهای عمومی بذار").let {
            // «رو» جزو متن کار می‌ماند؛ فقط وجود دستور و نام پک بررسی می‌شود
            (it as PackCommand.AddToPack).copy(taskText = it.taskText.removeSuffix(" رو"))
        })
    }
    @Test fun addImplicitPackName() {
        assertEquals(add("تحویل پروژه", "دانشگاه", false), p("تحویل پروژه به دانشگاه اضافه کن"))
    }
    @Test fun addKeepsEarlierPrepositions() {
        assertEquals(
            add("به من یادآوری کن فردا تحویل پروژه", "دانشگاه", true),
            p("به من یادآوری کن فردا تحویل پروژه به کارهای دانشگاه اضافه کن"),
        )
    }
    @Test fun addWithoutTask() { assertEquals(add("", "دانشگاه", true), p("این کار را به کارهای دانشگاه اضافه کن")) }
    @Test fun addWhenTaskContainsCreateVerb() {
        assertEquals(add("فردا غذا درست کن", "آشپزی", true), p("فردا غذا درست کن به پک آشپزی اضافه کن"))
    }
    @Test fun addWithZeroWidthJoiner() {
        assertEquals(add("تحویل پروژه", "دانشگاه", true), p("تحویل پروژه به کارهای دانشگاه اضافه\u200cکن"))
    }
    @Test fun addTrailingAlso() {
        assertEquals(add("تحویل پروژه", "دانشگاه", true), p("تحویل پروژه به کارهای دانشگاه هم اضافه کن"))
    }

    // ---------- جمله‌های عادی دستور نیستند ----------
    @Test fun plainReminderIsNotCommand() { assertNull(p("فردا ساعت ۹ صبح یادآوری کن جلسه دارم")) }
    @Test fun packWordInTitleIsNotCommand() { assertNull(p("یک پک شیر بخرم")) }
    @Test fun otherPrepositionIsNotCommand() { assertNull(p("چای را به آقا بده")) }
    @Test fun blankIsNotCommand() { assertNull(p("   ")) }
    @Test fun looseSentenceIsFlaggedNonExplicit() {
        // نتیجهٔ نهایی را ViewModel با وجود/عدم وجود پک «ماشین» تعیین می‌کند
        assertEquals(add("سبد رو", "ماشین", false), p("سبد رو توی ماشین بذار"))
    }

    // ---------- تطبیق نام ----------
    @Test fun keyIgnoresFillers() {
        assertEquals(PackCommandParser.key("دانشگاه"), PackCommandParser.key("کارهای دانشگاه"))
        assertEquals(PackCommandParser.key("دانشگاه"), PackCommandParser.key("پک دانشگاه"))
        assertEquals(PackCommandParser.key("عمومی"), PackCommandParser.key("پک کارهای عمومی"))
    }
    @Test fun keyNormalizesDigitsAndCase() {
        assertEquals("پروژه 2", PackCommandParser.key("پروژه ۲"))
        assertEquals("site", PackCommandParser.key("Site"))
    }
    @Test fun keyNormalizesArabicLetters() { assertEquals(PackCommandParser.key("کتابخانه"), PackCommandParser.key("كتابخانه")) }
    @Test fun keyKeepsSingleFillerWordName() { assertEquals("کار", PackCommandParser.key("کار")) }
}

class PackIconsTest {
    private fun k(n: String) = app.yadavar.pack.PackIcons.kindFor(n)
    private val K = app.yadavar.pack.PackIconKind.values().associateBy { it.name }

    @Test fun universityGetsSchool() { assertEquals(K["SCHOOL"], k("دانشگاه")) }
    @Test fun siteGetsWeb() { assertEquals(K["WEB"], k("سایت")) }
    @Test fun generalGetsGrid() { assertEquals(K["GENERAL"], k("عمومی")) }
    @Test fun fillersIgnored() { assertEquals(K["SCHOOL"], k("کارهای دانشگاه")) }
    @Test fun inflectedForms() {
        assertEquals(K["SCHOOL"], k("درس‌ها"))
        assertEquals(K["SHOP"], k("خریدهای هفته"))
        assertEquals(K["SCHOOL"], k("کلاس‌های دانشگاهی"))
    }
    @Test fun latinNames() {
        assertEquals(K["WEB"], k("Website"))
        assertEquals(K["CODE"], k("Coding"))
        assertEquals(K["FITNESS"], k("GYM"))
    }
    @Test fun multiWord() {
        assertEquals(K["SCHOOL"], k("پایان نامه"))
        assertEquals(K["WORK"], k("کسب و کار"))
    }
    @Test fun plainWorkWord() { assertEquals(K["WORK"], k("کار")) }
    @Test fun specificBeatsGeneric() { assertEquals(K["WEB"], k("پروژه سایت")) }
    @Test fun unknownGetsFolder() {
        assertEquals(K["FOLDER"], k("پروژه آلفا"))
        assertEquals(K["FOLDER"], k("   "))
    }
    @Test fun noFalsePositiveOnSubstring() {
        // «کدام» نباید «کد» حساب شود، «اپل» نباید «اپ»
        assertEquals(K["FOLDER"], k("کدام"))
        assertEquals(K["FOLDER"], k("اپل"))
    }
}
