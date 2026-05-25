package com.hue.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onScanFabric: () -> Unit,
    onViewHistory: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App wordmark
            Text(
                "Hue",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Fabric Colour Expert",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
            )

            Spacer(Modifier.height(64.dp))

            Button(
                onClick = onScanFabric,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 18.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(10.dp))
                Text("Scan fabric", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 18.dp)
            ) {
                Icon(Icons.Default.History, null)
                Spacer(Modifier.width(10.dp))
                Text("History", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(48.dp))

            Text(
                "Identify PANTONE FHI colours and\ncolour seasons from your fabrics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
