package com.hue.feature.matching.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hue.core.color.model.Season
import com.hue.core.design.components.*
import com.hue.core.design.theme.SeasonColors
import com.hue.domain.model.FabricAnalysis
import com.hue.domain.model.PantoneMatchResult
import kotlin.math.roundToInt

@Composable
fun ResultsScreen(
    croppedImagePath: String,
    onBack: () -> Unit,
    onSaveComplete: () -> Unit,
    onAnalyseAnother: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(croppedImagePath) {
        if (state is ResultsUiState.Idle) viewModel.analyse(croppedImagePath)
    }

    when (val s = state) {
        is ResultsUiState.Idle, is ResultsUiState.Loading -> LoadingScreen()
        is ResultsUiState.Error -> ErrorScreen(message = s.message, onBack = onBack)
        is ResultsUiState.Success -> ResultsContent(
            state = s,
            onBack = onBack,
            onSelectMatch = { viewModel.selectMatch(it) },
            onToggleAdvanced = { viewModel.toggleAdvanced() },
            onSave = {
                viewModel.saveToHistory()
                onSaveComplete()
            },
            onAnalyseAnother = onAnalyseAnother
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Analysing fabric colour…", style = MaterialTheme.typography.bodyMedium,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(64.dp),
             tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text("Analysis failed", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Try again") }
    }
}

@Composable
private fun ResultsContent(
    state: ResultsUiState.Success,
    onBack: () -> Unit,
    onSelectMatch: (Int) -> Unit,
    onToggleAdvanced: () -> Unit,
    onSave: () -> Unit,
    onAnalyseAnother: () -> Unit
) {
    val analysis = state.analysis
    val selectedMatch = analysis.topMatches.getOrNull(state.selectedMatchIndex)
    val displayColor = selectedMatch?.color?.hex?.let { parseHex(it) }
        ?: Color(
            android.graphics.Color.rgb(
                analysis.dominantRgb.r, analysis.dominantRgb.g, analysis.dominantRgb.b
            )
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    if (!state.isSaved) {
                        IconButton(onClick = onSave) { Icon(Icons.Default.BookmarkBorder, "Save") }
                    } else {
                        Icon(Icons.Default.Bookmark, "Saved",
                             tint = MaterialTheme.colorScheme.primary,
                             modifier = Modifier.padding(12.dp))
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onAnalyseAnother,
                        modifier = Modifier.weight(1f)
                    ) { Text("New scan") }
                    if (!state.isSaved) {
                        Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                            Text("Save to History")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Large dominant colour swatch
            DominantSwatchSection(
                color = displayColor,
                thumbnailPath = analysis.thumbnailPath,
                hex = selectedMatch?.color?.hex ?: analysis.dominantRgb.hex,
                rgb = analysis.dominantRgb
            )

            // PANTONE matches carousel
            if (analysis.topMatches.isNotEmpty()) {
                PantoneMatchesSection(
                    matches = analysis.topMatches,
                    selectedIndex = state.selectedMatchIndex,
                    onSelectMatch = onSelectMatch
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // Season card
            SeasonSection(analysis = analysis)

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // Warm/Cool axis
            WarmCoolSection(analysis = analysis)

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // Advanced details (collapsible)
            AdvancedDetailsSection(
                analysis = analysis,
                selectedMatch = selectedMatch,
                isExpanded = state.showAdvanced,
                onToggle = onToggleAdvanced
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DominantSwatchSection(
    color: Color,
    thumbnailPath: String,
    hex: String,
    rgb: com.hue.core.color.model.RgbColor
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(color),
        contentAlignment = Alignment.BottomStart
    ) {
        // Thumbnail overlay
        AsyncImage(
            model = thumbnailPath,
            contentDescription = "Fabric thumbnail",
            modifier = Modifier
                .size(80.dp)
                .padding(16.dp)
                .align(Alignment.BottomEnd)
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = hex.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = if (color.luminance() > 0.4f) Color.Black else Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "RGB ${rgb.r} · ${rgb.g} · ${rgb.b}",
                style = MaterialTheme.typography.bodySmall,
                color = (if (color.luminance() > 0.4f) Color.Black else Color.White).copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PantoneMatchesSection(
    matches: List<PantoneMatchResult>,
    selectedIndex: Int,
    onSelectMatch: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            "PANTONE FHI Matches",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(matches) { idx, match ->
                Column(
                    modifier = Modifier.clickable { onSelectMatch(idx) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PantoneChip(
                        color = parseHex(match.color.hex),
                        code = match.color.code,
                        name = match.color.name,
                        deltaELabel = "ΔE %.2f".format(match.deltaE),
                        isSelected = idx == selectedIndex
                    )
                    MatchQualityBadge(quality = match.matchQuality)
                }
            }
        }
    }
}

@Composable
private fun SeasonSection(analysis: FabricAnalysis) {
    val season = analysis.season
    val seasonUi = season.primarySeason.toUi()

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Colour Season", style = MaterialTheme.typography.titleMedium)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SeasonBadge(season = seasonUi)
            season.secondarySeason?.let { sec ->
                Text("/ ${sec.displayName}", style = MaterialTheme.typography.bodyMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            TemperatureBadge(isWarm = season.temperature == com.hue.core.color.model.ColorTemperature.WARM)
        }

        // Confidence bar
        ConfidenceBar(confidence = season.confidence)

        // Explanation text
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                season.explanation,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConfidenceBar(confidence: Double) {
    val pct = (confidence * 100).roundToInt()
    val barColor = when {
        confidence > 0.75 -> Color(0xFF2E7D32)
        confidence > 0.50 -> Color(0xFFF57F17)
        else              -> Color(0xFFC62828)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Confidence", style = MaterialTheme.typography.labelMedium,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$pct%", style = MaterialTheme.typography.labelMedium, color = barColor)
        }
        LinearProgressIndicator(
            progress = confidence.toFloat().coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth(),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun WarmCoolSection(analysis: FabricAnalysis) {
    val hue = analysis.dominantLab.hueAngle.toFloat()
    val warmFraction = when {
        hue in 15f..75f  -> 0.85f
        hue in 165f..300f -> 0.15f
        else              -> 0.5f
    }
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Warm–Cool Axis", style = MaterialTheme.typography.titleMedium)
        WarmCoolGradientBar(fraction = warmFraction)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Warm", style = MaterialTheme.typography.labelSmall,
                 color = Color(0xFFE07840))
            Text("Cool", style = MaterialTheme.typography.labelSmall,
                 color = Color(0xFF4080B8))
        }
    }
}

@Composable
private fun WarmCoolGradientBar(fraction: Float) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxWidth().height(12.dp)
    ) {
        val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
            colors = listOf(Color(0xFFE07840), Color(0xFF4080B8))
        )
        drawRoundRect(brush, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
        // Marker
        val markerX = size.width * fraction
        drawCircle(Color.White, radius = 8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(markerX, size.height / 2))
        drawCircle(Color.Black.copy(alpha = 0.3f), radius = 8.dp.toPx(),
                   center = androidx.compose.ui.geometry.Offset(markerX, size.height / 2),
                   style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
    }
}

@Composable
private fun AdvancedDetailsSection(
    analysis: FabricAnalysis,
    selectedMatch: PantoneMatchResult?,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Advanced details", style = MaterialTheme.typography.titleMedium)
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("L*a*b*", "L=%.1f  a=%.1f  b=%.1f".format(
                    analysis.dominantLab.l, analysis.dominantLab.a, analysis.dominantLab.b))
                DetailRow("Chroma (C*)", "%.1f".format(analysis.dominantLab.chroma))
                DetailRow("Hue angle", "%.1f°".format(analysis.dominantLab.hueAngle))
                DetailRow("Lightness category", analysis.season.lightness.name.lowercase().replaceFirstChar { it.uppercaseChar() })
                DetailRow("Chroma category", analysis.season.chroma.name.lowercase().replaceFirstChar { it.uppercaseChar() })
                selectedMatch?.let {
                    DetailRow("Best ΔE2000", "%.3f".format(it.deltaE))
                    DetailRow("Match quality", it.matchQuality.label)
                }
                DetailRow("Region variance", "%.1f".format(analysis.regionVariance))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
    }
}

private fun parseHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) { Color.Gray }

private fun Season.toUi(): SeasonUi = when (this) {
    Season.SPRING -> SeasonUi.SPRING
    Season.SUMMER -> SeasonUi.SUMMER
    Season.AUTUMN -> SeasonUi.AUTUMN
    Season.WINTER -> SeasonUi.WINTER
}

private val com.hue.core.color.model.RgbColor.hex: String
    get() = "#%02X%02X%02X".format(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
