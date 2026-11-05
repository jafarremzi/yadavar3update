package app.yadavar.ui

import androidx.compose.ui.graphics.vector.ImageVector
import app.yadavar.pack.PackIconKind
import app.yadavar.pack.PackIcons

/** آیکن وکتور هر نوع پک. */
fun packVector(kind: PackIconKind): ImageVector = when (kind) {
    PackIconKind.GENERAL -> AppIcons.GridView
    PackIconKind.SCHOOL -> AppIcons.School
    PackIconKind.WEB -> AppIcons.Language
    PackIconKind.CODE -> AppIcons.Code
    PackIconKind.WORK -> AppIcons.Work
    PackIconKind.SHOP -> AppIcons.ShoppingCart
    PackIconKind.HOME -> AppIcons.Home
    PackIconKind.FITNESS -> AppIcons.FitnessCenter
    PackIconKind.BOOK -> AppIcons.MenuBook
    PackIconKind.HEALTH -> AppIcons.MedicalServices
    PackIconKind.MONEY -> AppIcons.Payments
    PackIconKind.PEOPLE -> AppIcons.Group
    PackIconKind.TRAVEL -> AppIcons.Flight
    PackIconKind.FOOD -> AppIcons.Restaurant
    PackIconKind.MUSIC -> AppIcons.MusicNote
    PackIconKind.CAR -> AppIcons.DirectionsCar
    PackIconKind.FAVORITE -> AppIcons.Favorite
    PackIconKind.FOLDER -> AppIcons.Folder
}

/** آیکن پک از روی نام آن (دانشگاه ← کلاه فارغ‌التحصیلی، سایت ← کره زمین، …). */
fun packIconFor(name: String): ImageVector = packVector(PackIcons.kindFor(name))
