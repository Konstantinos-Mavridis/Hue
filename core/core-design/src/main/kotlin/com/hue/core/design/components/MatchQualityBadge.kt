package com.hue.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hue.core.color.model.MatchQuality

@Composable
fun MatchQualityBadge(quality: MatchQuality, modifier: Modifier = Modifier) {
    val (label, bg) = when (quality) {
        MatchQuality.EXCELLENT -> "Excellent" to Color(0xFF1B7A4A)
        MatchQuality.VERY_GOOD -> "Very Good" to Color(0xFF2E7D32)
        MatchQuality.GOOD      -> "Good"      to Color(0xFF558B2F)
        MatchQuality.FAIR      -> "Fair"      to Color(0xFFE65100)
        MatchQuality.POOR      -> "Poor"      to Color(0xFFC62828)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
