package app.yadavar.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asFrameworkPaint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

/**
 * سیستم طراحی «نئومورفیک تیره».
 * فقط وقتی تم تیره فعال است ([LocalNeo] = true) ظاهر نئومورفیک نمایش داده می‌شود؛
 * در تم روشن همین کامپوننت‌ها به ظاهر ساده و استاندارد Material برمی‌گردند.
 */
val LocalNeo = compositionLocalOf { false }

object Neo {
    val BgTop = Color(0xFF0F1B30)
    val BgBottom = Color(0xFF080F1D)
    val FaceHi = Color(0xFF1B2C47)
    val FaceLo = Color(0xFF121F35)
    val ShadowDark = Color(0xFF040913)
    val ShadowLight = Color(0xFF283C5B)
    val InsetTop = Color(0xFF0A1424)
    val InsetBottom = Color(0xFF14233B)
    val Teal = Color(0xFF2DE2D0)
    val TealSoft = Color(0xFF7DF3E8)
    val Cyan = Color(0xFF22C7F2)
    val OnTeal = Color(0xFF04231F)
    val TextPrimary = Color(0xFFEAF2FF)
    val TextSecondary = Color(0xFF9DB0CC)
}

// ---------------------------------------------------------------------------------------------
// سایه‌ها
// ---------------------------------------------------------------------------------------------

/**
 * سایهٔ دوگانهٔ نئومورفیک: سایهٔ تیره پایین‌راست و نور روشن بالا‌چپ (و اختیاری درخشش رنگی).
 * روی اندروید ۸ و ۹ سیستم سایهٔ بلور را برای شکل‌ها نشان نمی‌دهد؛ آنجا فقط خود سطح دیده می‌شود.
 */
private fun Modifier.neoShadow(corner: Dp, depth: Dp, glow: Color? = null): Modifier = drawBehind {
    val r = minOf(corner.toPx(), size.minDimension / 2f)
    val d = depth.toPx()
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val fp = paint.asFrameworkPaint()
        fp.isAntiAlias = true
        fp.color = Neo.FaceLo.toArgb()
        fun cast(dx: Float, dy: Float, blur: Float, color: Color) {
            fp.setShadowLayer(blur, dx, dy, color.toArgb())
            canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
        }
        cast(d, d, d * 1.8f, Neo.ShadowDark.copy(alpha = 0.85f))
        cast(-d, -d, d * 1.8f, Neo.ShadowLight.copy(alpha = 0.45f))
        if (glow != null) cast(0f, 0f, d * 2.6f, glow.copy(alpha = 0.55f))
    }
}

/** سطح نئومورفیک: برجسته (پیش‌فرض) یا فرورفته ([inset]، مثلاً برای فیلد متن). */
@Composable
fun NeoSurface(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    depth: Dp = 6.dp,
    glow: Color? = null,
    inset: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    var m = modifier
    if (LocalNeo.current) {
        m = if (inset) {
            m.clip(shape)
                .background(Brush.verticalGradient(listOf(Neo.InsetTop, Neo.InsetBottom)), shape)
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Neo.ShadowDark, Neo.ShadowLight.copy(alpha = 0.5f))),
                    shape,
                )
        } else {
            m.neoShadow(corner, depth, glow)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Neo.FaceHi, Neo.FaceLo)), shape)
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.Transparent, Neo.Teal.copy(alpha = 0.12f)),
                    ),
                    shape,
                )
        }
    } else {
        m = m.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant, shape)
    }
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m, content = content)
}

// ---------------------------------------------------------------------------------------------
// پس‌زمینه
// ---------------------------------------------------------------------------------------------

private fun DrawScope.drawNeoBackdrop() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Neo.BgTop, Neo.BgBottom)))
    // درخشش نرم بالا‌راست
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Neo.Teal.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(w * 0.95f, h * 0.02f), radius = w * 0.7f,
        ),
        radius = w * 0.7f, center = Offset(w * 0.95f, h * 0.02f),
    )
    // موج‌های پایین (مثل طرح): یکی فیروزه‌ای نیمه‌شفاف، یکی سرمه‌ای تیره
    val wave1 = Path().apply {
        moveTo(w, h * 0.84f)
        cubicTo(w * 0.80f, h * 0.86f, w * 0.66f, h * 0.94f, w * 0.40f, h)
        lineTo(w, h)
        close()
    }
    drawPath(
        wave1,
        Brush.linearGradient(
            colors = listOf(Neo.Teal.copy(alpha = 0.00f), Neo.Teal.copy(alpha = 0.20f)),
            start = Offset(w * 0.5f, h * 0.88f), end = Offset(w, h),
        ),
    )
    val wave2 = Path().apply {
        moveTo(0f, h * 0.90f)
        cubicTo(w * 0.22f, h * 0.88f, w * 0.34f, h * 0.97f, w * 0.52f, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        wave2,
        Brush.linearGradient(
            colors = listOf(Neo.Cyan.copy(alpha = 0.10f), Color.Transparent),
            start = Offset(0f, h), end = Offset(w * 0.5f, h * 0.9f),
        ),
    )
}

@Composable
fun NeoBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val m = if (LocalNeo.current) {
        modifier.fillMaxSize().drawBehind { drawNeoBackdrop() }
    } else {
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    }
    Box(m, content = content)
}

// ---------------------------------------------------------------------------------------------
// دکمه‌ها
// ---------------------------------------------------------------------------------------------

@Composable
fun NeoIconButton(
    icon: ImageVector,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    tint: Color? = null,
) {
    if (LocalNeo.current) {
        NeoSurface(modifier.size(buttonSize), corner = 999.dp, onClick = onClick, depth = 4.dp) {
            Icon(
                icon, description, Modifier.align(Alignment.Center).size(iconSize),
                tint = tint ?: Neo.TextPrimary,
            )
        }
    } else {
        IconButton(onClick = onClick, modifier = modifier) {
            Icon(icon, description, tint = tint ?: LocalContentColor.current)
        }
    }
}

/** دکمهٔ گرد کوچک با متن (مثل «+» و «−» در گام‌دهنده‌ها). */
@Composable
fun NeoStepButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (LocalNeo.current) {
        NeoSurface(modifier.size(44.dp), corner = 999.dp, onClick = onClick, depth = 4.dp) {
            Text(
                text, Modifier.align(Alignment.Center), color = Neo.Teal,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            )
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
    }
}

/** دکمهٔ قرصی: [primary] با گرادیان فیروزه‌ای و درخشش، در غیر این صورت برجستهٔ نئومورفیک. */
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    if (!LocalNeo.current) {
        val content: @Composable RowScope.() -> Unit = {
            if (icon != null) {
                Icon(icon, null, Modifier.size(18.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            }
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (primary) Button(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
        else OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
        return
    }
    val pill = RoundedCornerShape(999.dp)
    val fade = if (enabled) 1f else 0.4f
    if (primary) {
        var m = modifier.alpha(fade)
            .neoShadow(999.dp, 5.dp, Neo.Teal)
            .clip(pill)
            .background(Brush.horizontalGradient(listOf(Color(0xFF63F3E6), Color(0xFF22CBD6))), pill)
        if (enabled) m = m.clickable(onClick = onClick)
        Box(m.heightIn(min = 52.dp).padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = Neo.OnTeal)
                Text(
                    text, color = Neo.OnTeal, fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    } else {
        NeoSurface(
            modifier.alpha(fade), corner = 999.dp, onClick = if (enabled) onClick else null, depth = 5.dp,
        ) {
            Row(
                Modifier.align(Alignment.Center).heightIn(min = 52.dp).padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = Neo.Teal)
                Text(
                    text, color = Neo.Teal, fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
fun NeoChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    if (!LocalNeo.current) {
        FilterChip(selected = selected, onClick = onClick, label = { Text(label, maxLines = 1) }, modifier = modifier)
        return
    }
    val pill = RoundedCornerShape(999.dp)
    val tint = if (selected) Neo.OnTeal else Neo.Teal
    val row: @Composable () -> Unit = {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = tint)
            Text(
                label, maxLines = 1, color = if (selected) Neo.OnTeal else Neo.TextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
    if (selected) {
        Box(
            modifier.neoShadow(999.dp, 4.dp, Neo.Teal).clip(pill)
                .background(Brush.horizontalGradient(listOf(Color(0xFF63F3E6), Color(0xFF22CBD6))), pill)
                .clickable(onClick = onClick),
        ) { row() }
    } else {
        NeoSurface(modifier, corner = 999.dp, onClick = onClick, depth = 3.dp) { row() }
    }
}

// ---------------------------------------------------------------------------------------------
// آیکن، فیلد، پیشرفت، عنوان
// ---------------------------------------------------------------------------------------------

/** کاشی چهارگوش گرد با آیکن درخشان (برای پک‌ها، کارت‌ها و عنوان بخش‌ها). */
@Composable
fun NeoIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tileSize: Dp = 56.dp,
    iconSize: Dp = tileSize * 0.5f,
    tint: Color? = null,
) {
    val neo = LocalNeo.current
    NeoSurface(modifier.size(tileSize), corner = tileSize * 0.32f, depth = 4.dp) {
        val glowModifier = if (neo) {
            Modifier.fillMaxSize().drawBehind {
                val rad = this.size.minDimension * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Neo.Teal.copy(alpha = 0.22f), Color.Transparent),
                        center = center, radius = rad,
                    ),
                    radius = rad, center = center,
                )
            }
        } else {
            Modifier.fillMaxSize()
        }
        Box(glowModifier, contentAlignment = Alignment.Center) {
            Icon(
                icon, null, Modifier.size(iconSize),
                tint = tint ?: if (neo) Neo.Teal else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    leadingIcon: ImageVector? = null,
) {
    if (!LocalNeo.current) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = modifier, label = { Text(label) },
            singleLine = singleLine, minLines = minLines,
            leadingIcon = leadingIcon?.let { ic -> @Composable { Icon(ic, contentDescription = null) } },
        )
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label, Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelMedium, color = Neo.TextSecondary,
        )
        NeoSurface(Modifier.fillMaxWidth(), corner = 18.dp, inset = true) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (leadingIcon != null) Icon(leadingIcon, null, Modifier.size(20.dp), tint = Neo.Teal)
                BasicTextField(
                    value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f),
                    singleLine = singleLine, minLines = minLines,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Neo.TextPrimary),
                    cursorBrush = SolidColor(Neo.Teal),
                )
            }
        }
    }
}

/** نوار پیشرفت: شیار فرورفته + پر شدن فیروزه‌ای با نقطهٔ درخشان؛ در راست‌به‌چپ از راست پر می‌شود. */
@Composable
fun NeoProgress(progress: Float, modifier: Modifier = Modifier) {
    if (!LocalNeo.current) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier.fillMaxWidth().heightIn(min = 6.dp).clip(RoundedCornerShape(3.dp)),
        )
        return
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val p = progress.coerceIn(0f, 1f)
    Canvas(modifier.fillMaxWidth().height(16.dp)) {
        val w = size.width
        val h = size.height
        val trackH = 8.dp.toPx()
        val top = (h - trackH) / 2f
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Neo.InsetTop, Neo.InsetBottom), startY = top, endY = top + trackH,
            ),
            topLeft = Offset(0f, top), size = Size(w, trackH), cornerRadius = CornerRadius(trackH / 2f),
        )
        val dot = h / 2f
        val reach = (w - 2 * dot) * p + dot            // فاصلهٔ مرکز نقطه از لبهٔ شروع
        val fillW = reach + dot
        val left = if (rtl) w - fillW else 0f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                if (rtl) listOf(Neo.Cyan, Neo.Teal.copy(alpha = 0.35f)) else listOf(Neo.Teal.copy(alpha = 0.35f), Neo.Cyan),
            ),
            topLeft = Offset(left.coerceAtLeast(0f), top), size = Size(fillW.coerceAtMost(w), trackH),
            cornerRadius = CornerRadius(trackH / 2f),
        )
        val cx = if (rtl) w - reach else reach
        drawCircle(Neo.Teal.copy(alpha = 0.35f), radius = dot, center = Offset(cx, h / 2f))
        drawCircle(Neo.TealSoft, radius = dot * 0.62f, center = Offset(cx, h / 2f))
    }
}

/** عنوان بخش با کاشی آیکن (اختیاری). */
@Composable
fun SectionTitle(text: String, icon: ImageVector? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) NeoIconTile(icon, tileSize = 42.dp, iconSize = 22.dp)
        Text(
            text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
fun NeoTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) NeoIconButton(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", onBack)
        Text(
            title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground,
        )
        actions()
    }
}

// ---------------------------------------------------------------------------------------------
// موج کنار میکروفون
// ---------------------------------------------------------------------------------------------

/** میله‌های موج صدا؛ نزدیک میکروفون بلندترند و هنگام گوش دادن متحرک می‌شوند. */
@Composable
fun Waveform(active: Boolean, mirrored: Boolean, modifier: Modifier = Modifier) {
    val phase = if (active) {
        val t = rememberInfiniteTransition(label = "wave")
        val v by t.animateFloat(
            initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "phase",
        )
        v
    } else {
        0f
    }
    Canvas(modifier) {
        val n = 5
        val cell = size.width / n
        val barW = 5.dp.toPx()
        for (i in 0 until n) {
            val nearMic = if (mirrored) n - 1 - i else i          // ۰ = دورترین، n-1 = نزدیک‌ترین
            val base = 0.22f + 0.78f * (nearMic + 1f) / n
            val live = if (active) 0.45f + 0.55f * abs(sin(phase + i * 0.9f)) else 0.85f
            val bh = size.height * base * live
            val x = cell * (i + 0.5f) - barW / 2f
            val y = (size.height - bh) / 2f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Neo.Teal.copy(alpha = 0.95f), Neo.Cyan.copy(alpha = 0.45f)), startY = y, endY = y + bh,
                ),
                topLeft = Offset(x, y), size = Size(barW, bh), cornerRadius = CornerRadius(barW / 2f),
            )
        }
    }
}
