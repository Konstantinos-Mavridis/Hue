package com.hue.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hue.core.design.theme.HueShapes

@Composable
fun ColorSwatch(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    label: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = modifier
                .size(size)
                .clip(HueShapes.medium)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, HueShapes.medium)
        )
        if (label != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PantoneChip(
    color: Color,
    code: String,
    name: String,
    deltaELabel: String,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        modifier = modifier
            .width(88.dp)
            .clip(HueShapes.medium)
            .border(borderWidth, borderColor, HueShapes.medium)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(color)
        )
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                fontSize = 9.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = deltaELabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 9.sp
            )
        }
    }
}
