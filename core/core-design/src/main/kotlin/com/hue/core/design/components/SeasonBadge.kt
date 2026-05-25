package com.hue.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hue.core.design.theme.SeasonColors

enum class SeasonUi(
    val label: String,
    val color: Color,
    val icon: ImageVector,
    val iconDescription: String
) {
    SPRING("Spring", SeasonColors.Spring, Icons.Default.LocalFlorist, "Flower icon for Spring"),
    SUMMER("Summer", SeasonColors.Summer, Icons.Default.WbSunny,     "Sun icon for Summer"),
    AUTUMN("Autumn", SeasonColors.Autumn, Icons.Default.Park,         "Leaf icon for Autumn"),
    WINTER("Winter", SeasonColors.Winter, Icons.Default.AcUnit,       "Snowflake icon for Winter");
}

@Composable
fun SeasonBadge(
    season: SeasonUi,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    val bgColor = season.color.copy(alpha = 0.15f)
    val fgColor = season.color

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { contentDescription = "${season.label} season badge" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showIcon) {
            Icon(
                imageVector = season.icon,
                contentDescription = season.iconDescription,
                tint = fgColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = season.label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = fgColor
        )
    }
}

@Composable
fun TemperatureBadge(isWarm: Boolean, modifier: Modifier = Modifier) {
    val (label, color) = if (isWarm) "Warm" to Color(0xFFE07840)
                         else        "Cool" to Color(0xFF4080B8)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isWarm) Icons.Default.Whatshot else Icons.Default.AcUnit,
            contentDescription = if (isWarm) "Warm temperature" else "Cool temperature",
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
