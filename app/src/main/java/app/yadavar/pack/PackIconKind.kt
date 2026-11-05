package app.yadavar.pack

/** نوع آیکن یک پک؛ از روی نام پک خودکار تعیین می‌شود (بدون ذخیره در دیتابیس). */
enum class PackIconKind {
    GENERAL, SCHOOL, WEB, CODE, WORK, SHOP, HOME, FITNESS, BOOK, HEALTH,
    MONEY, PEOPLE, TRAVEL, FOOD, MUSIC, CAR, FAVORITE, FOLDER,
}

/** تعیین آیکن پک از روی نام، با واژه‌های کلیدی فارسی و انگلیسی؛ برای نام ناشناخته «پوشه». */
object PackIcons {
    private class Rule(val kind: PackIconKind, val words: List<String>)

    // ترتیب مهم است: قانون‌های اول برنده‌اند.
    private val rules = listOf(
        Rule(PackIconKind.GENERAL, listOf("عمومی", "general", "متفرقه", "سایر")),
        Rule(PackIconKind.SCHOOL, listOf(
            "دانشگاه", "دانشکده", "دانشجو", "مدرسه", "کلاس", "درس", "درسی", "امتحان", "تحصیل", "پایان نامه",
            "uni", "university", "school", "college", "study", "class", "exam", "homework",
        )),
        Rule(PackIconKind.WEB, listOf(
            "سایت", "وب", "وبسایت", "وبلاگ", "اینترنت", "دامنه", "آنلاین", "هاست", "site", "web", "website",
            "blog", "domain", "online", "hosting",
        )),
        Rule(PackIconKind.CODE, listOf(
            "برنامه نویسی", "برنامه نویس", "کد", "کدنویسی", "نرم افزار", "اپلیکیشن", "اپ", "توسعه", "برنامه",
            "code", "dev", "coding", "app", "software", "programming",
        )),
        Rule(PackIconKind.SHOP, listOf("خرید", "فروشگاه", "بازار", "سوپرمارکت", "shop", "shopping", "buy", "market", "store")),
        Rule(PackIconKind.HEALTH, listOf(
            "سلامت", "پزشک", "پزشکی", "دکتر", "دارو", "درمان", "بیمارستان", "نوبت دکتر", "health", "doctor", "medical",
        )),
        Rule(PackIconKind.MONEY, listOf(
            "مالی", "پول", "حساب", "بانک", "قسط", "بودجه", "هزینه", "درآمد", "مالیات", "money", "bank", "finance", "budget", "bills",
        )),
        Rule(PackIconKind.FITNESS, listOf("ورزش", "باشگاه", "تمرین", "تناسب", "دویدن", "fitness", "gym", "sport", "workout", "run")),
        Rule(PackIconKind.BOOK, listOf("کتاب", "مطالعه", "خواندن", "رمان", "book", "books", "read", "reading")),
        Rule(PackIconKind.TRAVEL, listOf("سفر", "مسافرت", "پرواز", "بلیط", "گردشگری", "trip", "travel", "flight", "tour", "vacation")),
        Rule(PackIconKind.FOOD, listOf("غذا", "آشپزی", "رستوران", "کافه", "food", "cook", "cooking", "restaurant", "cafe", "recipe")),
        Rule(PackIconKind.MUSIC, listOf("موسیقی", "آهنگ", "music", "song", "songs")),
        Rule(PackIconKind.CAR, listOf("ماشین", "خودرو", "تعمیرگاه", "car", "auto", "garage")),
        Rule(PackIconKind.PEOPLE, listOf("خانواده", "دوست", "دوستان", "مهمانی", "تیم", "family", "friends", "team", "party")),
        Rule(PackIconKind.HOME, listOf("خانه", "منزل", "خونه", "اسباب کشی", "home", "house")),
        Rule(PackIconKind.WORK, listOf("اداره", "اداری", "شرکت", "شغل", "کسب و کار", "کسبوکار", "بیزینس", "کار", "work", "office", "job", "business")),
        Rule(PackIconKind.FAVORITE, listOf("علاقه", "عشق", "شخصی", "favorite", "favourite", "love", "personal")),
    )

    /** واژه‌ها را با نام مقایسه می‌کند: برابری کامل یک کلمه، یا شروع کلمه (برای واژه‌های ≥ ۴ حرف)، یا وجود عبارت چندکلمه‌ای. */
    fun kindFor(name: String): PackIconKind {
        val key = PackCommandParser.key(name).replace("\u200c", " ")
        if (key.isBlank()) return PackIconKind.FOLDER
        val tokens = key.split(' ').filter { it.isNotEmpty() }
        for (r in rules) {
            for (w in r.words) {
                val hit = if (' ' in w) key.contains(w)
                else tokens.any { it == w || (w.length >= 4 && it.startsWith(w)) }
                if (hit) return r.kind
            }
        }
        return PackIconKind.FOLDER
    }
}
