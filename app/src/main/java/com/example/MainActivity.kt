package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.isActive
import kotlin.math.*

fun getDifficultyColor(difficulty: String): Color {
    return when (difficulty.lowercase()) {
        "easy" -> Color(0xFF00FF88)
        "medium" -> Color(0xFF00E5FF)
        "hard" -> Color(0xFFFF3333)
        else -> Color.White
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    val context = LocalContext.current
                    val viewModel = remember { ParkourViewModel(context) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(Color(0xFF07080C))
                    ) {
                        val state by viewModel.uiState.collectAsState()
                        
                        when (state) {
                            GameStateStatus.MAIN_MENU -> MainMenuScreen(viewModel)
                            GameStateStatus.HOW_TO_PLAY -> HowToPlayScreen(viewModel)
                            GameStateStatus.PLAYING -> GameplayScreen(viewModel)
                            GameStateStatus.PAUSED -> PauseScreen(viewModel)
                            GameStateStatus.GAME_OVER -> GameOverScreen(viewModel)
                            GameStateStatus.VICTORY -> VictoryScreen(viewModel)
                            GameStateStatus.SHOP -> ShopScreen(viewModel)
                            GameStateStatus.DIALOGUE -> DialogueScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: MAIN MENU (2D ADVENTURE THEMED)
// ==========================================
@Composable
fun MainMenuScreen(viewModel: ParkourViewModel) {
    val bestTimes = viewModel.bestTimesMap.value
    val completedLevels = viewModel.levels.count { bestTimes.containsKey(it.id) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header / Hero Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "CYBER RUNNER 2D",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFFFF5722),
                        modifier = Modifier.testTag("app_title")
                    )
                }
                Text(
                    text = "Side-Scrolling Parkour & Intelligence Trial Sandbox",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            
            // Stats & Wallet HUD Card
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Completed Trials
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141A29)),
                    border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$completedLevels / ${viewModel.levels.size} Cleared",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Shards/Coins Wallet
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1A12)),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Wallet Shards",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${viewModel.upgradesState.coinsWallet} SHARDS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Level Selector Panel (Left side)
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .fillMaxHeight()
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111219)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CHOOSE SECTOR MISSION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(viewModel.levels) { index, level ->
                                val bestTime = bestTimes[level.id]
                                val isSelected = viewModel.currentLevelIndex == index
                                val borderAlpha = if (isSelected) 0.8f else 0.1f
                                val borderColor = if (isSelected) Color(0xFFFF5722) else Color.White
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectLevel(index) }
                                        .testTag("level_card_$index"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0x1AFF5722) else Color(0xFF1A1B24)
                                    ),
                                    border = BorderStroke(1.5.dp, borderColor.copy(alpha = borderAlpha)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = level.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                // Difficulty tag
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = getDifficultyColor(level.difficulty).copy(alpha = 0.15f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = level.difficulty.uppercase(),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = getDifficultyColor(level.difficulty)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = level.description,
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.padding(top = 4.dp),
                                                maxLines = 1
                                            )
                                        }
                                        
                                        // Record time or Start Indicator
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (bestTime != null) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Timer,
                                                        contentDescription = "Personal Best",
                                                        tint = Color(0xFFFFD700),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = String.format("%.2fs", bestTime),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFFD700)
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = "NO RECORD",
                                                    fontSize = 9.sp,
                                                    color = Color.White.copy(alpha = 0.3f),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = "Enter Mission",
                                                tint = if (isSelected) Color(0xFFFF5722) else Color.White.copy(alpha = 0.2f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Side panel (Right side)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Upgrades & Tech Shop Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111219)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ARMORY & TECH CENTER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1F1E29), shape = CircleShape)
                                    .border(1.dp, Color(0xFF00E5FF), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PrecisionManufacturing,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Enhance Speed Sensors",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Unlock double jumps, cyber air-dashes, and magnet sensors.",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.setUIStatus(GameStateStatus.SHOP) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("shop_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23253A)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("TECH SHOP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = { viewModel.setUIStatus(GameStateStatus.HOW_TO_PLAY) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("how_to_play_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22242E)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("HOW TO PLAY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                // Giant PLAY TRIAL Button
                Button(
                    onClick = { viewModel.selectLevel(viewModel.currentLevelIndex) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("play_level_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Text(
                            text = "START ADVENTURE CORRIDOR",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: HOW TO PLAY (RE-IMAGINED 2D MANUAL)
// ==========================================
@Composable
fun HowToPlayScreen(viewModel: ParkourViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HOW TO INFILTRATE THE SECTORS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF5722)
            )
            
            IconButton(onClick = { viewModel.setUIStatus(GameStateStatus.MAIN_MENU) }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val guides = listOf(
                Triple("BASIC STEER & RUN", "Tap Left/Right arrows (or keyboard A/D) to accelerate forward. You can move fast and carry jump momentum.", Icons.Default.Navigation),
                Triple("DOUBLE JUMP GEAR", "Equip standard jump booster in Shop. Tap Jump (or Space) while airborne to jump again over massive gaps.", Icons.Default.DoubleArrow),
                Triple("WALL CLING & SLIDE", "Jump directly against vertical neon blocks. You will stick and slide down slowly, ready for a wall kick!", Icons.Default.FlightTakeoff),
                Triple("WALL KICK LEAP", "While sliding on a wall run node, tap Jump to perform a WALL KICK. Launches you up and away dynamically!", Icons.Default.Bolt),
                Triple("CROUCH SLIDE / DASH", "Slide under low laser beams with the Crouch (S) button, or unleash an Air Dash (Shift) to shoot forward!", Icons.Default.DirectionsRun)
            )
            
            guides.forEach { guide ->
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF15161F)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x15FF5722), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = guide.third, contentDescription = null, tint = Color(0xFFFF5722), modifier = Modifier.size(20.dp))
                        }
                        
                        Text(
                            text = guide.first,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = guide.second,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: ADVENTURE DIALOGUE STORY OVERLAY
// ==========================================
@Composable
fun DialogueScreen(viewModel: ParkourViewModel) {
    val activeLine = viewModel.activeDialogueLine
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07080C)),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Blur level canvas representation in background for cinematic feel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(8.dp)
                .alpha(0.3f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(Color(0xFF1A1B24))
            }
        }
        
        // Narrative overlay panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(180.dp)
                .testTag("dialogue_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F101A)),
            border = BorderStroke(1.5.dp, activeLine?.speakerColor?.copy(alpha = 0.5f) ?: Color(0xFF00E5FF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive avatar portrait
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0x1A000000), shape = CircleShape)
                        .border(2.5.dp, activeLine?.speakerColor ?: Color(0xFF00E5FF), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (activeLine?.speaker?.contains("GEMINI") == true) Icons.Default.SmartToy else Icons.Default.Face,
                        contentDescription = null,
                        tint = activeLine?.speakerColor ?: Color(0xFF00E5FF),
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                // Conversation text area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = activeLine?.speaker ?: "SECURE COMMUNICATOR",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = activeLine?.speakerColor ?: Color(0xFF00E5FF)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activeLine?.text ?: "Connection sync in progress...",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )
                    }
                    
                    // Control Deck
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.skipDialogue() },
                            modifier = Modifier.testTag("skip_dialogue_button")
                        ) {
                            Text("SKIP COMS", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { viewModel.advanceDialogue() },
                            colors = ButtonDefaults.buttonColors(containerColor = activeLine?.speakerColor ?: Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("next_dialogue_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("CONTINUE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 4: ARMORY TECH & UPGRADES SHOP
// ==========================================
@Composable
fun ShopScreen(viewModel: ParkourViewModel) {
    val wallet = viewModel.upgradesState.coinsWallet
    val upgrades = viewModel.upgradesState
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Shop Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                Text(
                    text = "SYNDICATE TECH SHOP",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            
            // Shard indicator
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1A12)),
                border = BorderStroke(1.5.dp, Color(0xFFFFD700))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Stars, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                    Text(
                        text = "$wallet SHARDS AVAILABLE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Column Left: Functional Upgrades
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("TACTICAL SENSOR UPGRADES", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                
                // Double Jump Item
                ShopItemRow(
                    title = "Double Jump Jetpack",
                    description = "Execute a second vertical propulsion mid-air to clear massive gaps.",
                    cost = 500,
                    isUnlocked = upgrades.hasDoubleJump,
                    onBuy = { viewModel.buyUpgrade("double_jump", 500) },
                    icon = Icons.Default.DoubleArrow,
                    wallet = wallet
                )
                
                // Air Dash Item
                ShopItemRow(
                    title = "Cyber Air-Dash Thrust",
                    description = "Perform a supersonic horizontal dash mid-air. Refreshes upon landing.",
                    cost = 300,
                    isUnlocked = upgrades.hasAirDash,
                    onBuy = { viewModel.buyUpgrade("air_dash", 300) },
                    icon = Icons.Default.Bolt,
                    wallet = wallet
                )
                
                // Magnet Item
                ShopItemRow(
                    title = "Magnet Boot Shard-Puller",
                    description = "Automatically pull and absorb nearby gold energy shards within 4.5m.",
                    cost = 200,
                    isUnlocked = upgrades.hasMagnet,
                    onBuy = { viewModel.buyUpgrade("magnet", 200) },
                    icon = Icons.Default.WifiTethering,
                    wallet = wallet
                )
            }
            
            // Column Right: Custom Skins PREVIEW & BUY
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("COSMETIC NANOSUIT SKINS", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    
                    val skins = listOf(
                        SkinType.DEFAULT,
                        SkinType.CYBER_BLUE,
                        SkinType.SHADOW_NINJA,
                        SkinType.SPECTRUM
                    )
                    
                    skins.forEach { skin ->
                        val isEquipped = upgrades.currentSkin == skin.id
                        val isUnlocked = upgrades.isSkinUnlocked(skin.id)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.unlockAndEquipSkin(skin) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isEquipped) Color(0x1F00E5FF) else Color(0xFF13141F)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isEquipped) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.05f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Custom visual colored skin circle preview
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(skin.trailColor, shape = CircleShape)
                                            .border(1.5.dp, Color.White, shape = CircleShape)
                                    )
                                    Column {
                                        Text(skin.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(
                                            text = if (skin.cost == 0) "Free Base Suit" else "${skin.cost} Shards",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                
                                Button(
                                    onClick = { viewModel.unlockAndEquipSkin(skin) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isEquipped) Color(0xFF00E5FF) else if (isUnlocked) Color(0xFF222435) else Color(0xFFFFD700)
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    val bText = if (isEquipped) "EQUIPPED" else if (isUnlocked) "EQUIP" else "BUY"
                                    val bColor = if (isEquipped) Color.Black else Color.White
                                    Text(bText, fontSize = 9.sp, fontWeight = FontWeight.Black, color = bColor)
                                }
                            }
                        }
                    }
                }
                
                // Return to Main Menu
                Button(
                    onClick = { viewModel.setUIStatus(GameStateStatus.MAIN_MENU) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("shop_back_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25273C)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("RETURN TO CENTRAL COMMAND", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ShopItemRow(
    title: String,
    description: String,
    cost: Int,
    isUnlocked: Boolean,
    onBuy: () -> Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    wallet: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13141F)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1C1D29), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(description, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), lineHeight = 13.sp)
                }
            }
            
            Button(
                onClick = { onBuy() },
                enabled = !isUnlocked && wallet >= cost,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUnlocked) Color(0xFF1E2F23) else Color(0xFFFFD700),
                    disabledContainerColor = if (isUnlocked) Color(0xFF1E2F23) else Color(0xFF1D1F2B)
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(30.dp)
            ) {
                val txt = if (isUnlocked) "UNLOCKED" else "$cost SHARDS"
                val col = if (isUnlocked) Color(0xFF00FF88) else if (wallet >= cost) Color.Black else Color.White.copy(alpha = 0.3f)
                Text(txt, fontSize = 9.sp, fontWeight = FontWeight.Black, color = col)
            }
        }
    }
}

// ==========================================
// SCREEN 5: GAMEPLAY SCREEN (2D VIEWPORT RENDERING)
// ==========================================
@Composable
fun GameplayScreen(viewModel: ParkourViewModel) {
    val engine = viewModel.engine
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    
    // Virtual Direction Inputs
    var isLeftPressed by remember { mutableStateOf(false) }
    var isRightPressed by remember { mutableStateOf(false) }
    
    val triggerHaptic = {
        if (viewModel.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Capture Left/Right continuous input loops
    LaunchedEffect(isLeftPressed, isRightPressed) {
        while (isActive) {
            val sx = if (isLeftPressed) -1f else if (isRightPressed) 1f else 0f
            engine.updateInputs(sx, 0f)
            kotlinx.coroutines.delay(16) // ~60hz
        }
    }

    // High performance game ticks using Frame Nanos
    LaunchedEffect(key1 = true) {
        var lastNanos = System.nanoTime()
        focusRequester.requestFocus() // Bind keyboard inputs instantly!
        while (isActive) {
            withFrameNanos { frameNanos ->
                val dt = (frameNanos - lastNanos) / 1_000_000_000f
                lastNanos = frameNanos
                
                // physics frame step (capped for performance)
                engine.update(dt.coerceAtMost(0.05f), triggerHaptic)
                
                // Win/Loss triggers
                if (engine.isLevelCompleted) {
                    viewModel.saveBestTime(engine.currentLevel.id, engine.timeElapsed)
                    viewModel.setUIStatus(GameStateStatus.VICTORY)
                }
                if (engine.isGameOver) {
                    viewModel.setUIStatus(GameStateStatus.GAME_OVER)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Capture hardware keyboards inputs (A, D, Space, Down/S, Shift/X)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.A, Key.DirectionLeft -> {
                            isLeftPressed = true
                            true
                        }
                        Key.D, Key.DirectionRight -> {
                            isRightPressed = true
                            true
                        }
                        Key.Spacebar, Key.W, Key.DirectionUp -> {
                            engine.triggerJump(triggerHaptic)
                            true
                        }
                        Key.S, Key.DirectionDown, Key.C -> {
                            engine.triggerSlide(triggerHaptic)
                            true
                        }
                        Key.ShiftLeft, Key.ShiftRight, Key.X -> {
                            engine.triggerDash(triggerHaptic)
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.A, Key.DirectionLeft -> {
                            isLeftPressed = false
                            true
                        }
                        Key.D, Key.DirectionRight -> {
                            isRightPressed = false
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // 1. Vector 2D Scene Side-scroller viewport Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("2d_canvas")
        ) {
            draw2DScene(engine, size.width, size.height)
        }

        // 2. HUD Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stats (Timer, Speed)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC0B0C10)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                        Text(
                            text = String.format("ELAPSED: %.2fs", engine.timeElapsed),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC0B0C10)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = Color(0xFFFF5722), modifier = Modifier.size(14.dp))
                        val spd = engine.velocity.length()
                        Text(
                            text = String.format("SPEED: %.1f m/s", spd),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }

            // Central alert indicators (Dash, Wallrun, Slide status, Key status)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = engine.isWallRunning,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xCCFF5722))) {
                        Text(
                            "WALL SLIDE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = engine.isSliding,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF00E5FF))) {
                        Text(
                            "CROUCH SLIDING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = engine.hasKey,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF00E5FF)),
                        border = BorderStroke(1.dp, Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                            Text("CORE OVERRIDE ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        }
                    }
                }
            }

            // HUD Right (Coins Shards, Pause Menu)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC0B0C10)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stars, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(15.dp))
                        Text(
                            text = "${engine.coinsCollectedThisRun} / ${engine.currentLevel.coins.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.setUIStatus(GameStateStatus.PAUSED) },
                    modifier = Modifier
                        .background(Color(0x99121214), shape = CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), shape = CircleShape)
                        .size(36.dp)
                        .testTag("pause_button")
                ) {
                    Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // 3. ON-SCREEN DIGITAL CONTROLLER TACTICAL DECK
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Directional Buttons (Left side)
            Row(
                modifier = Modifier.align(Alignment.BottomStart),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Arrow Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0x33000000), shape = CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.2f), shape = CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { isLeftPressed = true },
                                onDragEnd = { isLeftPressed = false },
                                onDragCancel = { isLeftPressed = false },
                                onDrag = { change, _ -> change.consume() }
                            )
                        }
                        .testTag("left_steer_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Move Left", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                // Right Arrow Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0x33000000), shape = CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.2f), shape = CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { isRightPressed = true },
                                onDragEnd = { isRightPressed = false },
                                onDragCancel = { isRightPressed = false },
                                onDrag = { change, _ -> change.consume() }
                            )
                        }
                        .testTag("right_steer_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Move Right", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            // Skill Action Buttons Deck (Right side)
            Row(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // DASH SKILL Button
                if (viewModel.upgradesState.hasAirDash) {
                    Button(
                        onClick = { engine.triggerDash(triggerHaptic) },
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("dash_action_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (engine.hasAirDash) Color(0xFF00E5FF) else Color(0x33FFFFFF)
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = "Air Dash", tint = if (engine.hasAirDash) Color.Black else Color.White, modifier = Modifier.size(22.dp))
                    }
                }

                // CROUCH SLIDE Button
                Button(
                    onClick = { engine.triggerSlide(triggerHaptic) },
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("slide_action_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E3D52)),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(imageVector = Icons.Default.DirectionsRun, contentDescription = "Slide Crouch", tint = Color.White, modifier = Modifier.size(22.dp))
                }

                // PRIMARY JUMP BUTTON
                Button(
                    onClick = { engine.triggerJump(triggerHaptic) },
                    modifier = Modifier
                        .size(76.dp)
                        .testTag("jump_action_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (engine.isWallRunning) Color(0xFFFF5722) else Color(0xFF00FF88)
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    val icon = if (engine.isWallRunning) Icons.Default.FlightTakeoff else Icons.Default.ArrowUpward
                    Icon(imageVector = icon, contentDescription = "Jump Action", tint = Color.Black, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

// ==========================================
// SCREEN 6: PAUSE SCREEN
// ==========================================
@Composable
fun PauseScreen(viewModel: ParkourViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(360.dp)
                .padding(16.dp)
                .testTag("pause_menu_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13141C)),
            border = BorderStroke(1.5.dp, Color(0xFFFF5722).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SIMULATION SUSPENDED",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF5722)
                )
                
                // Sensitivity Slider
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("LOOK HORIZON CLAMP", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        Text(String.format("%.1fx", viewModel.touchSensitivity), fontSize = 10.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = viewModel.touchSensitivity,
                        onValueChange = {
                            viewModel.touchSensitivity = it
                            viewModel.saveSettings()
                        },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                // Haptic Toggler
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E202B), shape = RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .clickable {
                            viewModel.hapticEnabled = !viewModel.hapticEnabled
                            viewModel.saveSettings()
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("Haptic Spark Buzz", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Switch(
                        checked = viewModel.hapticEnabled,
                        onCheckedChange = {
                            viewModel.hapticEnabled = it
                            viewModel.saveSettings()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00E5FF),
                            checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.4f)
                        )
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.restartLevel() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232534))
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESTART", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { viewModel.setUIStatus(GameStateStatus.PLAYING) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("resume_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESUME", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                TextButton(
                    onClick = { viewModel.setUIStatus(GameStateStatus.MAIN_MENU) },
                    modifier = Modifier.testTag("main_menu_button")
                ) {
                    Text("ABANDON MISSION", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// SCREEN 7: GAME OVER SCREEN
// ==========================================
@Composable
fun GameOverScreen(viewModel: ParkourViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDF170707)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(340.dp)
                .padding(16.dp)
                .testTag("game_over_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1111)),
            border = BorderStroke(1.5.dp, Color(0xFFFF3333))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Dangerous,
                    contentDescription = null,
                    tint = Color(0xFFFF3333),
                    modifier = Modifier.size(44.dp)
                )
                
                Text(
                    text = "RUN FAILS DETECTED",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF3333)
                )
                
                Text(
                    text = "Critical: System telemetry collapsed in hazardous boundary pools.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.setUIStatus(GameStateStatus.MAIN_MENU) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF221F24))
                    ) {
                        Text("MENU", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { viewModel.restartLevel() },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("retry_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3333))
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RETRY RUN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 8: VICTORY SCREEN
// ==========================================
@Composable
fun VictoryScreen(viewModel: ParkourViewModel) {
    val engine = viewModel.engine
    val recordMap = viewModel.bestTimesMap.value
    val personalBest = recordMap[engine.currentLevel.id] ?: engine.timeElapsed
    val isNewPB = engine.timeElapsed <= personalBest
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDF061B10)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(380.dp)
                .padding(16.dp)
                .testTag("victory_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B13)),
            border = BorderStroke(1.5.dp, Color(0xFF00FF66))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(44.dp)
                )
                
                Text(
                    text = "SECTOR INFILTRATED!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00FF66)
                )
                
                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("COMPLETED IN", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format("%.2fs", engine.timeElapsed),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SHARDS SECURED", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        val obtained = engine.coinsCollectedThisRun
                        Text(
                            text = "+$obtained SHARDS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                if (isNewPB) {
                    Box(
                        modifier = Modifier
                            .background(Color(0x2200FF66), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⭐ NEW SECTOR SPEED RECORD ⭐",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00FF66)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.setUIStatus(GameStateStatus.MAIN_MENU) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22242D))
                    ) {
                        Text("MENU", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    val hasNext = viewModel.currentLevelIndex + 1 < viewModel.levels.size
                    Button(
                        onClick = {
                            if (hasNext) {
                                viewModel.selectLevel(viewModel.currentLevelIndex + 1)
                            } else {
                                viewModel.restartLevel()
                            }
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("victory_next_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66))
                    ) {
                        val lbl = if (hasNext) "NEXT SECTOR" else "RUN AGAIN"
                        Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    }
                }
            }
        }
    }
}

// ==========================================
// CENTRAL PROCEDURAL 2D GRAPHICS SCENE DRAWER
// ==========================================
fun DrawScope.draw2DScene(engine: GameEngine2D, width: Float, height: Float) {
    val level = engine.currentLevel
    val scale = height / 12f // 12 meters shown vertically
    
    // Viewport boundaries
    val viewportWidthMeters = width / scale
    val viewportHeightMeters = 12f
    
    // Lerp Camera horizontally and vertically centered around player
    val targetCamX = (engine.position.x - viewportWidthMeters / 2.2f).coerceIn(0f, max(0f, level.width - viewportWidthMeters))
    val targetCamY = (engine.position.y - viewportHeightMeters / 2.3f).coerceIn(0f, max(0f, level.height - viewportHeightMeters))
    
    val camX = targetCamX
    val camY = targetCamY

    // 1. Draw Layered Parallax Background
    // Layer A (Distant - Sky stars/haze)
    drawRect(level.backdropColor)
    
    // Layer B (Deep Skylines - Cyber skyscrapers outlines)
    val skyX = -camX * 0.18f
    for (i in 0..10) {
        val wPos = skyX + i * 180f
        val hHeight = 150f + sin(i.toFloat() * 1.5f) * 60f
        drawRect(
            color = Color(0xFF0F121C).copy(alpha = 0.5f),
            topLeft = Offset(wPos, height - hHeight),
            size = Size(100f, hHeight)
        )
        // neon dot windows on backdrop
        for (wRow in 0..3) {
            for (wCol in 0..1) {
                drawCircle(
                    color = Color(0xFF00FFCC).copy(alpha = 0.15f),
                    radius = 1.5f,
                    center = Offset(wPos + 25f + wCol * 35f, height - hHeight + 30f + wRow * 25f)
                )
            }
        }
    }
    
    // Layer C (Mid Skyscrapers)
    val midSkyX = -camX * 0.45f
    for (i in 0..12) {
        val wPos = midSkyX + i * 140f
        val hHeight = 80f + cos(i.toFloat() * 2.3f) * 45f
        drawRect(
            color = Color(0xFF161A29).copy(alpha = 0.7f),
            topLeft = Offset(wPos, height - hHeight),
            size = Size(75f, hHeight)
        )
    }

    // Coordinate Transform Utility: (world) -> (screen)
    fun toScreenX(wx: Float): Float = (wx - camX) * scale
    fun toScreenY(wy: Float): Float = height - (wy - camY) * scale
    
    // 2. Render Platforms and Security Obstacles
    for (obs in engine.obstacles.value) {
        val sLeft = toScreenX(obs.left)
        val sRight = toScreenX(obs.right)
        val sTop = toScreenY(obs.bottom)
        val sBottom = toScreenY(obs.top)
        
        val rWidth = sRight - sLeft
        val rHeight = sBottom - sTop
        
        val rect = Rect(sLeft, sTop, sRight, sBottom)
        
        when (obs.type) {
            PlatformType.SOLID -> {
                // Dark block with high contrast outline
                drawRect(
                    color = Color(0xFF1C1D2A),
                    topLeft = Offset(sLeft, sTop),
                    size = Size(rWidth, rHeight)
                )
                drawRect(
                    color = obs.color, // Neon outlines
                    topLeft = Offset(sLeft, sTop),
                    size = Size(rWidth, rHeight),
                    style = Stroke(width = 2.5f)
                )
            }
            PlatformType.WALL_RUN -> {
                // Glowing orange/blue wall sliders with moving diagonal stripes
                drawRect(
                    color = obs.color.copy(alpha = 0.18f),
                    topLeft = Offset(sLeft, sTop),
                    size = Size(rWidth, rHeight)
                )
                drawRect(
                    color = obs.color,
                    topLeft = Offset(sLeft, sTop),
                    size = Size(rWidth, rHeight),
                    style = Stroke(width = 4f)
                )
                // Draw cool neon inner stripes
                val path = Path().apply {
                    moveTo(sLeft, sBottom)
                    lineTo(sLeft + min(rWidth, rHeight), sTop)
                    moveTo(sRight - min(rWidth, rHeight), sBottom)
                    lineTo(sRight, sTop)
                }
                drawPath(path = path, color = obs.color.copy(alpha = 0.5f), style = Stroke(width = 2f))
            }
            PlatformType.BOUNCE -> {
                // Spring pad
                drawRect(
                    color = obs.color.copy(alpha = 0.3f),
                    topLeft = Offset(sLeft, sTop),
                    size = Size(rWidth, rHeight)
                )
                drawRect(
                    color = obs.color,
                    topLeft = Offset(sLeft, sTop),
                    size = Size(rWidth, rHeight),
                    style = Stroke(width = 3f)
                )
                // Sine wave energy curve on top
                val path = Path().apply {
                    moveTo(sLeft, sTop + rHeight / 2f)
                    quadraticTo(sLeft + rWidth / 2f, sTop - 8f, sRight, sTop + rHeight / 2f)
                }
                drawPath(path = path, color = obs.color, style = Stroke(width = 3.5f))
            }
            PlatformType.LAVA -> {
                // Glowing volcanic geyser/lava pits
                val lavaGrad = Brush.verticalGradient(
                    colors = listOf(obs.color, Color(0xFF4A0000))
                )
                drawRect(
                    brush = lavaGrad,
                    topLeft = Offset(sLeft, sTop),
                    size = Size(rWidth, rHeight)
                )
                // Bubbling visual wave
                val step = rWidth / 4f
                val wavePath = Path().apply {
                    moveTo(sLeft, sTop)
                    for (wi in 0..4) {
                        val cx = sLeft + wi * step + step / 2f
                        val cy = sTop + (if (wi % 2 == 0) -4f else 4f)
                        val ex = sLeft + (wi + 1) * step
                        quadraticTo(cx, cy, ex, sTop)
                    }
                    lineTo(sRight, sBottom)
                    lineTo(sLeft, sBottom)
                    close()
                }
                drawPath(path = wavePath, color = obs.color.copy(alpha = 0.4f))
            }
            PlatformType.KEY -> {
                // Golden core keycard drawing
                val cx = sLeft + rWidth / 2f
                val cy = sTop + rHeight / 2f
                
                // Outer rotating ring
                val rotAngle = (engine.timeElapsed * 150f) % 360f
                withTransform({
                    rotate(rotAngle, pivot = Offset(cx, cy))
                }) {
                    drawRect(
                        color = Color(0xFF00E5FF),
                        topLeft = Offset(cx - 10f, cy - 10f),
                        size = Size(20f, 20f),
                        style = Stroke(width = 2f)
                    )
                }
                // Key shape
                drawCircle(color = Color(0xFF00E5FF), radius = 6f, center = Offset(cx, cy - 2f))
                drawRect(color = Color(0xFF00E5FF), topLeft = Offset(cx - 2f, cy + 2f), size = Size(4f, 10f))
                drawRect(color = Color(0xFF00E5FF), topLeft = Offset(cx - 2f, cy + 8f), size = Size(8f, 3f))
            }
            PlatformType.GATE -> {
                // Red security laser blocking wall
                drawRect(
                    color = obs.color.copy(alpha = 0.2f),
                    topLeft = Offset(sLeft, sTop),
                    size = Size(rWidth, rHeight)
                )
                drawRect(
                    color = obs.color,
                    topLeft = Offset(sLeft, sTop),
                    size = Size(rWidth, rHeight),
                    style = Stroke(width = 1.5f)
                )
                // Glowing horizontal security warning lines
                val lasers = 5
                val lStep = rHeight / lasers
                for (li in 1 until lasers) {
                    val ly = sTop + li * lStep
                    drawLine(
                        color = obs.color.copy(alpha = 0.8f),
                        start = Offset(sLeft, ly),
                        end = Offset(sRight, ly),
                        strokeWidth = 3f
                    )
                }
            }
            PlatformType.PORTAL -> {
                // Level escape portal ring
                val cx = sLeft + rWidth / 2f
                val cy = sTop + rHeight / 2f
                val radius = max(rWidth, rHeight) * 0.7f
                
                // Spiral vortex circles
                val count = 3
                for (pi in 0 until count) {
                    val pRad = radius * ((pi.toFloat() + (engine.timeElapsed * 1.5f) % 1.0f) / count)
                    drawCircle(
                        color = obs.color.copy(alpha = 1f - (pRad / radius)),
                        radius = pRad,
                        center = Offset(cx, cy),
                        style = Stroke(width = 3f)
                    )
                }
                // Core glow
                drawCircle(color = Color.White, radius = 10f, center = Offset(cx, cy))
            }
        }
        
        // Render labels (like "GATE" or "WALL RUN") for better visual cues
        if (obs.label.isNotEmpty() && rWidth > 20f) {
            // Simply render small color highlights
            drawCircle(color = obs.color, radius = 2.5f, center = Offset(sLeft + 15f, sTop - 8f))
        }
    }

    // 3. Render Shard Coins
    for (coin in engine.coins.value) {
        if (!coin.isCollected) {
            val scx = toScreenX(coin.position.x)
            val scy = toScreenY(coin.position.y)
            
            val rotAngle = (engine.timeElapsed * 240f) % 360f
            
            withTransform({
                rotate(rotAngle, pivot = Offset(scx, scy))
            }) {
                // Diamond gold shape
                val path = Path().apply {
                    moveTo(scx, scy - 10f)
                    lineTo(scx + 7f, scy)
                    lineTo(scx, scy + 10f)
                    lineTo(scx - 7f, scy)
                    close()
                }
                drawPath(path = path, color = Color(0xFFFFD700))
                drawPath(path = path, color = Color.White, style = Stroke(width = 1.5f))
            }
            // Glow aura
            drawCircle(color = Color(0x33FFD700), radius = 15f, center = Offset(scx, scy))
        }
    }

    // 4. Draw Interactive Particles
    for (p in engine.particles) {
        val px = toScreenX(p.position.x)
        val py = toScreenY(p.position.y)
        drawCircle(
            color = p.color.copy(alpha = p.life),
            radius = p.size * p.life,
            center = Offset(px, py)
        )
    }

    // 5. Draw 2D Procedural Animated Player Ninja Runner
    val pSizeX = engine.playerSizeX * scale
    val pSizeY = engine.playerSizeY * scale
    val px = toScreenX(engine.position.x)
    val py = toScreenY(engine.position.y)
    
    val suitColor = engine.upgradesState.getTrailColor()

    // Draw stylish shadow motion trials if dashing/speeding
    if (engine.isDashing) {
        for (ti in 1..3) {
            val offsetMeters = engine.dashDir * (ti * 0.45f)
            val shadowX = toScreenX(engine.position.x - offsetMeters.x)
            val shadowY = toScreenY(engine.position.y - offsetMeters.y)
            drawRoundRect(
                color = suitColor.copy(alpha = 0.25f / ti),
                topLeft = Offset(shadowX - pSizeX / 2f, shadowY - pSizeY / 2f),
                size = Size(pSizeX, pSizeY),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
            )
        }
    }

    // Drawing actual Runner Body Capsule
    drawRoundRect(
        color = Color(0xFF13141F),
        topLeft = Offset(px - pSizeX / 2f, py - pSizeY / 2f),
        size = Size(pSizeX, pSizeY),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
    )
    // Suit Frame (The equipped cosmetic glow!)
    drawRoundRect(
        color = suitColor,
        topLeft = Offset(px - pSizeX / 2f, py - pSizeY / 2f),
        size = Size(pSizeX, pSizeY),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
        style = Stroke(width = 3.5f)
    )

    // Glowing Visor / Helmet Head indicator
    val visorHeight = 12f
    val visorWidth = pSizeX * 0.7f
    val visorY = py - pSizeY / 2f + 10f
    
    val visorX = if (engine.velocity.x >= 0f) {
        px - visorWidth / 3f // looking right
    } else {
        px - visorWidth * 2f / 3f // looking left
    }
    
    drawRoundRect(
        color = suitColor,
        topLeft = Offset(visorX, visorY),
        size = Size(visorWidth, visorHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
    )

    // Dynamic procedural animated leg limbs
    val legCycle = (engine.timeElapsed * 16f) % (2f * PI.toFloat())
    val legY = py + pSizeY / 2f
    
    if (engine.isGrounded && abs(engine.velocity.x) > 0.1f) {
        // Runner Running limbs
        val leftOffset = sin(legCycle) * 15f
        val rightOffset = -sin(legCycle) * 15f
        
        // Left leg
        drawLine(
            color = suitColor,
            start = Offset(px - 10f, py),
            end = Offset(px - 15f + leftOffset, legY + 8f),
            strokeWidth = 4f
        )
        // Right leg
        drawLine(
            color = suitColor,
            start = Offset(px + 10f, py),
            end = Offset(px + 15f + rightOffset, legY + 8f),
            strokeWidth = 4f
        )
    } else if (engine.isWallRunning) {
        // Angled wall bracing legs
        val wallKickX = if (engine.wallSide == -1) px - 15f else px + 15f
        drawLine(
            color = suitColor,
            start = Offset(px, py + 10f),
            end = Offset(wallKickX, py + 25f),
            strokeWidth = 4f
        )
    } else {
        // Airborne simple tuck legs
        drawLine(
            color = suitColor,
            start = Offset(px - 8f, py + 10f),
            end = Offset(px - 12f, py + 22f),
            strokeWidth = 4f
        )
        drawLine(
            color = suitColor,
            start = Offset(px + 8f, py + 10f),
            end = Offset(px + 12f, py + 22f),
            strokeWidth = 4f
        )
    }
}
