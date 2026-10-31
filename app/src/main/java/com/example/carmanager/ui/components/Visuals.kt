package com.example.carmanager.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EuBlue = Color(0xFF003399)

/** Număr de înmatriculare desenat ca o plăcuță românească: banda albastră „RO” + text negru pe alb. */
@Composable
fun PlateBadge(plate: String, modifier: Modifier = Modifier, large: Boolean = false) {
    val h = if (large) 34.dp else 26.dp
    Row(
        modifier
            .height(h)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White)
            .border(1.5.dp, Color(0xFF1A1A1A), RoundedCornerShape(5.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.fillMaxHeight().width(if (large) 18.dp else 14.dp).background(EuBlue),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(
                "RO", color = Color.White, fontSize = if (large) 9.sp else 7.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Text(
            formatPlate(plate),
            color = Color(0xFF111111),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (large) 17.sp else 13.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = if (large) 10.dp else 7.dp),
        )
    }
}

/** „NT28ABC” -> „NT 28 ABC”, „B123ABC” -> „B 123 ABC” (backend-ul le salvează fără spații). */
fun formatPlate(plate: String): String {
    val m = Regex("^([A-Z]{1,2})(\\d{2,3})([A-Z]{3})$").find(plate) ?: return plate
    return m.groupValues.drop(1).joinToString(" ")
}

/** Pătrățel rotunjit colorat cu o iconiță în mijloc. */
@Composable
fun IconTile(
    @DrawableRes icon: Int? = null,
    vector: ImageVector? = null,
    tint: Color,
    size: Dp = 44.dp,
) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 3.2f)).background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        val iconModifier = Modifier.size(size * 0.55f)
        when {
            icon != null -> Icon(painterResource(icon), null, iconModifier, tint = tint)
            vector != null -> Icon(vector, null, iconModifier, tint = tint)
        }
    }
}

/** Mic card de statistică: iconiță, valoare mare, etichetă. */
@Composable
fun StatTile(
    @DrawableRes icon: Int,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = tint)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

@Composable
fun ProgressLine(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        color = color,
        trackColor = color.copy(alpha = 0.15f),
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
        modifier = modifier.height(6.dp),
    )
}
