package com.x695c.optimizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x695c.optimizer.data.GameOptimizationConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    gameConfigs: Map<String, GameOptimizationConfig>,
    onGameSelect: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = "Game Optimization",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Game List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Select a game to configure optimization settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(gameConfigs.entries.toList()) { (packageName, config) ->
                val gameName = getGameName(packageName)
                Card(
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
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gameName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
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
                                Spacer(modifier = Modifier.width(4.dp))
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
                            contentDescription = null
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                            text = "These games have default optimizations from the device. Modify as needed.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
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
        else -> packageName.substringAfterLast(".").replace("_", " ").capitalize()
    }
}
