package com.x695c.tuner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x695c.tuner.data.GameTuningConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    gameConfigs: Map<String, GameTuningConfig>,
    configAvailable: Boolean = false,
    onGameSelect: (String) -> Unit,
    onAddGame: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddGameDialog by remember { mutableStateOf(false) }
    var newPackageName by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar - MD3 Expressive
        TopAppBar(
            title = { 
                Text(
                    text = "Game Tuning",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showAddGameDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Game")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Game List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                // Config status warning
                if (!configAvailable) {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Config file not detected. Settings are for reference only.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    text = "Select a game to configure tuning settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(gameConfigs.entries.toList().sortedBy { it.key }) { (packageName, config) ->
                val gameName = getGameName(packageName)
                ElevatedCard(
                    onClick = { onGameSelect(packageName) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideogameAsset,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gameName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { 
                                        Text(
                                            text = "Thermal: ${config.thermalPolicy.description.split(" ").first()}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.height(24.dp)
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = { 
                                        Text(
                                            text = "GPU: ${config.gpuMarginMode.value}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Add Game Button
                FilledTonalButton(
                    onClick = { showAddGameDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Custom Game")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add custom games by package name. Get package name from app info or Play Store URL.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Add Game Dialog
    if (showAddGameDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddGameDialog = false
                newPackageName = ""
            },
            title = { 
                Text(
                    text = "Add Custom Game",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter the package name of the game you want to add:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPackageName,
                        onValueChange = { newPackageName = it },
                        label = { Text("Package Name") },
                        placeholder = { Text("com.example.game") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Example: com.tencent.ig for PUBG Mobile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        if (newPackageName.isNotBlank() && !gameConfigs.containsKey(newPackageName)) {
                            onAddGame(newPackageName.trim())
                        }
                        showAddGameDialog = false
                        newPackageName = ""
                    },
                    enabled = newPackageName.isNotBlank() && !gameConfigs.containsKey(newPackageName)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAddGameDialog = false
                        newPackageName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun getGameName(packageName: String): String {
    return when (packageName) {
        "com.tencent.tmgp.sgame" -> "Honor of Kings"
        "com.tencent.tmgp.sgamece" -> "Honor of Kings (CE)"
        "com.tencent.ig" -> "PUBG Mobile"
        "com.tencent.igce" -> "PUBG Mobile (CE)"
        "com.tencent.iglite" -> "PUBG Mobile Lite"
        "com.tencent.tmgp.pubgmhd" -> "PUBG Mobile HD"
        "com.tencent.tmgp.pubgm" -> "PUBG Mobile (CN)"
        "com.dts.freefireth" -> "Free Fire"
        "com.tencent.tmgp.cf" -> "CrossFire Mobile"
        "com.miHoYo.enterprise.NGHSoD" -> "Genshin Impact"
        "com.miHoYo.bh3.mi" -> "Honkai Impact 3rd"
        "com.tencent.tmgp.bh3" -> "Honkai Impact 3rd (CN)"
        "com.netease.ko" -> "Knives Out"
        "com.netease.hyxd" -> "Cyber Hunter"
        "com.netease.mrzh" -> "LifeAfter"
        "com.epicgames.fortnite" -> "Fortnite"
        "com.madfingergames.legends" -> "Shadowgun Legends"
        "com.gameloft.android.ANMP.GloftA9HM" -> "Asphalt 9"
        "com.studiowildcard.wardrumstudios.ark" -> "ARK: Survival Evolved"
        "com.tencent.mobileqq" -> "Mobile QQ"
        "com.ss.android.ugc.aweme" -> "TikTok"
        "com.sina.weibo" -> "Weibo"
        "com.activision.callofduty.shooter" -> "Call of Duty Mobile"
        "com.mobile.legends" -> "Mobile Legends"
        else -> packageName.substringAfterLast(".").replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
