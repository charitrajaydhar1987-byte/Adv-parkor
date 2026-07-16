package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

// ==========================================
// 2D MATHEMATICS
// ==========================================
data class Vector2(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(o: Vector2) = Vector2(x + o.x, y + o.y)
    operator fun minus(o: Vector2) = Vector2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vector2(x * s, y * s)
    operator fun div(s: Float) = if (s != 0f) Vector2(x / s, y / s) else Vector2()
    
    fun length() = sqrt(x * x + y * y)
    fun normalized(): Vector2 {
        val l = length()
        return if (l > 0f) this / l else Vector2()
    }
}

// ==========================================
// GAME STATES & ENUMS
// ==========================================
enum class GameStateStatus { MAIN_MENU, HOW_TO_PLAY, PLAYING, PAUSED, GAME_OVER, VICTORY, SHOP, DIALOGUE }

enum class PlatformType {
    SOLID,          // standard concrete platforms
    WALL_RUN,       // neon-orange/cyan panels we can run and jump off
    BOUNCE,         // glowing bounce pads
    LAVA,           // hazard triggers
    KEY,            // collectable key
    GATE,           // locked blocker
    PORTAL          // finish portal
}

enum class SkinType(val id: String, val displayName: String, val cost: Int, val trailColor: Color) {
    DEFAULT("default", "Standard Crimson", 0, Color(0xFFFF5722)),
    CYBER_BLUE("cyber_blue", "Neon Cyber Blue", 150, Color(0xFF00E5FF)),
    SHADOW_NINJA("shadow_ninja", "Shadow Shinobi", 300, Color(0xFF9C27B0)),
    SPECTRUM("spectrum", "Spectral Prism", 500, Color(0xFFFFD700))
}

// ==========================================
// GAME OBJECTS
// ==========================================
data class Obstacle2D(
    val id: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val type: PlatformType,
    val color: Color,
    val label: String = ""
) {
    fun intersects(px: Float, py: Float, pSizeX: Float, pSizeY: Float): Boolean {
        return px - pSizeX / 2f < right &&
               px + pSizeX / 2f > left &&
               py - pSizeY / 2f < bottom &&
               py + pSizeY / 2f > top
    }
}

data class Coin2D(
    val id: Int,
    val position: Vector2,
    var isCollected: Boolean = false
)

data class Particle2D(
    var position: Vector2,
    var velocity: Vector2,
    var color: Color,
    var life: Float,      // from 1.0f down to 0.0f
    val size: Float,
    val decay: Float
)

data class DialogueLine(
    val speaker: String,
    val text: String,
    val speakerColor: Color
)

data class Level2D(
    val id: Int,
    val name: String,
    val description: String,
    val difficulty: String,
    val startPos: Vector2,
    val width: Float,     // Level visual horizontal boundary
    val height: Float,    // Level visual vertical boundary
    val obstacles: List<Obstacle2D>,
    val coins: List<Coin2D>,
    val storyIntro: List<DialogueLine>,
    val backdropColor: Color
)

// ==========================================
// 2D GAME ENGINE
// ==========================================
class GameEngine2D(var currentLevel: Level2D, val upgradesState: UpgradesState) {
    // Player physical status
    var position by mutableStateOf(currentLevel.startPos)
    var velocity by mutableStateOf(Vector2())
    val playerSizeX = 0.8f   // width in meters
    var playerSizeY by mutableStateOf(1.6f)  // height in meters (can shrink while sliding)
    
    // Movement Flags
    var isGrounded = false
    var isWallRunning = false
    var wallSide = 0         // -1: Wall on Left, 1: Wall on Right, 0: None
    var isSliding = false
    var slideTimer = 0f
    
    var isDashing = false
    var dashTimer = 0f
    var dashDir = Vector2(1f, 0f)
    var hasAirDash = true
    var doubleJumpsLeft = 0
    
    // Timers & Stats
    var timeElapsed by mutableStateOf(0f)
    var isGameOver by mutableStateOf(false)
    var isLevelCompleted by mutableStateOf(false)
    var coinsCollectedThisRun = 0
    var score by mutableStateOf(0)
    var checkpointPos = currentLevel.startPos
    var hasKey = false
    
    // Dynamic lists
    val coins = mutableStateOf<List<Coin2D>>(emptyList())
    val particles = mutableListOf<Particle2D>()
    var obstacles = mutableStateOf<List<Obstacle2D>>(emptyList())
    
    // Inputs
    private var inputX = 0f
    private var inputY = 0f
    
    // Coyote time and Jump buffering
    private var coyoteTimer = 0f
    private var jumpBufferTimer = 0f
    
    init {
        resetEngine()
    }
    
    fun resetEngine() {
        position = currentLevel.startPos
        velocity = Vector2()
        playerSizeY = 1.6f
        isGrounded = false
        isWallRunning = false
        wallSide = 0
        isSliding = false
        slideTimer = 0f
        isDashing = false
        dashTimer = 0f
        hasAirDash = true
        doubleJumpsLeft = if (upgradesState.hasDoubleJump) 1 else 0
        timeElapsed = 0f
        isGameOver = false
        isLevelCompleted = false
        coinsCollectedThisRun = 0
        score = 0
        checkpointPos = currentLevel.startPos
        hasKey = false
        
        // Deep copy level configurations
        coins.value = currentLevel.coins.map { it.copy() }
        obstacles.value = currentLevel.obstacles.map { it.copy() }
        particles.clear()
        
        coyoteTimer = 0f
        jumpBufferTimer = 0f
    }
    
    fun updateInputs(strafe: Float, forward: Float) {
        inputX = strafe
        inputY = forward
    }
    
    fun triggerJump(onHaptic: () -> Unit) {
        jumpBufferTimer = 0.15f // Buffer jump request for 150ms
    }
    
    fun triggerDash(onHaptic: () -> Unit) {
        if (!upgradesState.hasAirDash || !hasAirDash || isDashing || isSliding) return
        
        onHaptic()
        isDashing = true
        dashTimer = 0.18f // Dash duration
        hasAirDash = false
        
        // Dash direction is input horizontal direction, default to forward look (right)
        val dirX = if (abs(inputX) > 0.1f) sign(inputX) else 1f
        dashDir = Vector2(dirX, 0f).normalized()
        velocity = dashDir * 20f
        
        // Spawn cool dash sparks
        spawnParticles(position, Color(0xFF00E5FF), 15)
    }
    
    fun triggerSlide(onHaptic: () -> Unit) {
        if (isSliding || !isGrounded) return
        
        onHaptic()
        isSliding = true
        slideTimer = 0.6f // slide duration
        playerSizeY = 0.8f // shrink size
        
        // Add speed burst forward
        val dirX = if (abs(inputX) > 0.1f) sign(inputX) else 1f
        velocity = Vector2(dirX * 14f, velocity.y)
        
        // Spawn sliding friction dust
        spawnParticles(position + Vector2(0f, -0.4f), Color.White.copy(alpha = 0.6f), 8)
    }
    
    fun update(dt: Float, onHaptic: () -> Unit) {
        if (isGameOver || isLevelCompleted) return
        
        timeElapsed += dt
        
        // Update coyote time and jump buffer
        if (coyoteTimer > 0f) coyoteTimer -= dt
        if (jumpBufferTimer > 0f) jumpBufferTimer -= dt
        
        // Upgrades magnetic coin collection
        updateMagnet(dt)
        
        // Particles physics
        updateParticles(dt)
        
        if (isDashing) {
            dashTimer -= dt
            velocity = dashDir * 20f // Locked velocity during dash
            
            // Continuous dash sparks
            if (Math.random() < 0.4) {
                particles.add(Particle2D(
                    position = position - dashDir * 0.4f,
                    velocity = Vector2(-dashDir.x * 3f + (Math.random().toFloat() - 0.5f) * 2f, (Math.random().toFloat() - 0.5f) * 2f),
                    color = upgradesState.getTrailColor(),
                    life = 1f,
                    size = 5f,
                    decay = 4f
                ))
            }
            
            if (dashTimer <= 0f) {
                isDashing = false
                // transition velocity out smoothly
                velocity = Vector2(velocity.x * 0.5f, 0f)
            }
            
            // Apply dash position movement
            position += velocity * dt
            checkCollisions(dt, onHaptic)
            return
        }
        
        // Standard Movement Physics
        val gravity = 25f // m/s^2
        val maxSpeed = 10f
        val accel = 35f
        val friction = 18f
        
        // Apply Horizontal movement inputs
        if (abs(inputX) > 0.1f) {
            val targetSpeed = inputX * maxSpeed
            val speedDiff = targetSpeed - velocity.x
            val speedChange = accel * dt
            velocity = Vector2(
                x = velocity.x + speedChange.coerceAtMost(abs(speedDiff)) * sign(speedDiff),
                y = velocity.y
            )
        } else {
            // Apply ground/air friction
            val fCoeff = if (isGrounded) friction else friction * 0.3f
            val speedChange = fCoeff * dt
            if (abs(velocity.x) > 0.05f) {
                velocity = Vector2(
                    x = velocity.x - speedChange.coerceAtMost(abs(velocity.x)) * sign(velocity.x),
                    y = velocity.y
                )
            } else {
                velocity = Vector2(0f, velocity.y)
            }
        }
        
        // Slide sliding mechanics timer
        if (isSliding) {
            slideTimer -= dt
            // slide speed decay
            velocity = Vector2(velocity.x * (1f - 2.5f * dt), velocity.y)
            
            if (slideTimer <= 0f) {
                // Check if space above allows standing up
                if (canStandUp()) {
                    isSliding = false
                    playerSizeY = 1.6f
                } else {
                    // Stay crouched, slide extends
                    slideTimer = 0.1f
                }
            }
        }
        
        // Apply Gravity
        if (!isGrounded) {
            val activeGravity = if (isWallRunning) {
                4f // slow sliding down the wall
            } else {
                gravity
            }
            velocity = Vector2(velocity.x, velocity.y - activeGravity * dt)
        }
        
        // Wall Slide / Wall Run detection (only if jumping/falling against a wall)
        detectWallCling()
        
        // Handle Jump requests (using coyote time and double jump)
        if (jumpBufferTimer > 0f) {
            if (isGrounded || coyoteTimer > 0f) {
                // Ground Jump
                onHaptic()
                velocity = Vector2(velocity.x, 10.5f)
                isGrounded = false
                coyoteTimer = 0f
                jumpBufferTimer = 0f
                isSliding = false
                playerSizeY = 1.6f
                
                // Jump cloud particles
                spawnParticles(position - Vector2(0f, 0.7f), Color.White.copy(alpha = 0.5f), 10)
            } else if (isWallRunning && wallSide != 0) {
                // Wall Kick Jump: jump away from the wall normal with speed!
                onHaptic()
                velocity = Vector2(-wallSide * 9.5f, 10f)
                isWallRunning = false
                jumpBufferTimer = 0f
                
                // Wall sparks particles
                spawnParticles(position + Vector2(wallSide * 0.4f, 0f), Color(0xFFFF5722), 12)
            } else if (upgradesState.hasDoubleJump && doubleJumpsLeft > 0) {
                // Air Double Jump
                onHaptic()
                velocity = Vector2(velocity.x, 9.8f)
                doubleJumpsLeft--
                jumpBufferTimer = 0f
                
                // Glowing ring explosion
                spawnParticles(position, upgradesState.getTrailColor(), 15)
            }
        }
        
        // Clamp falling speeds safely
        val termVel = -22f
        if (velocity.y < termVel) {
            velocity = Vector2(velocity.x, termVel)
        }
        
        // Update position
        position += velocity * dt
        
        // Check map boundaries
        if (position.y < -4f) {
            triggerFellInLava(onHaptic)
        }
        
        // Check physical overlaps and resolve solid coordinate bounds
        checkCollisions(dt, onHaptic)
    }
    
    private fun detectWallCling() {
        if (isGrounded) {
            isWallRunning = false
            wallSide = 0
            return
        }
        
        // Look for wall running platform overlaps on left and right sides
        var leftCling = false
        var rightCling = false
        val offsetSearch = 0.55f // slightly wider than playerSizeX / 2f
        
        for (obs in obstacles.value) {
            if (obs.type == PlatformType.WALL_RUN) {
                // Check Left side
                if (position.x - offsetSearch < obs.right && position.x > obs.left &&
                    position.y - playerSizeY/2f < obs.bottom && position.y + playerSizeY/2f > obs.top) {
                    leftCling = true
                }
                // Check Right side
                if (position.x + offsetSearch > obs.left && position.x < obs.right &&
                    position.y - playerSizeY/2f < obs.bottom && position.y + playerSizeY/2f > obs.top) {
                    rightCling = true
                }
            }
        }
        
        if (leftCling && velocity.y < 1f) {
            isWallRunning = true
            wallSide = -1
            doubleJumpsLeft = if (upgradesState.hasDoubleJump) 1 else 0 // refresh air jumps on wall
        } else if (rightCling && velocity.y < 1f) {
            isWallRunning = true
            wallSide = 1
            doubleJumpsLeft = if (upgradesState.hasDoubleJump) 1 else 0
        } else {
            isWallRunning = false
            wallSide = 0
        }
    }
    
    private fun canStandUp(): Boolean {
        // Test height size 1.6f to see if standing up hits a solid roof block
        val testPosY = position.y + (1.6f - 0.8f) / 2f
        for (obs in obstacles.value) {
            if (obs.type == PlatformType.SOLID || obs.type == PlatformType.GATE) {
                if (obs.intersects(position.x, testPosY, playerSizeX, 1.6f)) {
                    return false
                }
            }
        }
        return true
    }
    
    private fun updateMagnet(dt: Float) {
        if (!upgradesState.hasMagnet) return
        
        val radius = 4.5f // meter pull radius
        val speed = 9.0f // meter/sec pull speed
        
        coins.value = coins.value.map { coin ->
            if (coin.isCollected) return@map coin
            val toPlayer = position - coin.position
            val dist = toPlayer.length()
            if (dist < radius) {
                val moveDir = toPlayer.normalized()
                val newPos = coin.position + moveDir * speed * dt
                // Return coin closer
                coin.copy(position = newPos)
            } else {
                coin
            }
        }
    }
    
    private fun checkCollisions(dt: Float, onHaptic: () -> Unit) {
        // Solid check resolving
        val tempObsList = obstacles.value
        
        // 1. Resolve Y Collisions (Grounded status, Roof collision)
        var groundedThisFrame = false
        
        for (obs in tempObsList) {
            if (obs.type == PlatformType.SOLID || obs.type == PlatformType.GATE) {
                if (obs.intersects(position.x, position.y, playerSizeX, playerSizeY)) {
                    // Overlapped. Calculate depth of penetration on Y axis
                    val penY = if (velocity.y <= 0f) {
                        // Standing on top of platform
                        obs.bottom - (position.y - playerSizeY / 2f)
                    } else {
                        // Collided with ceiling
                        obs.top - (position.y + playerSizeY / 2f)
                    }
                    
                    if (abs(penY) < 0.4f) { // resolve only reasonable pen depths
                        if (velocity.y <= 0f) {
                            position = Vector2(position.x, position.y + penY + 0.001f)
                            velocity = Vector2(velocity.x, 0f)
                            groundedThisFrame = true
                        } else {
                            position = Vector2(position.x, position.y + penY - 0.001f)
                            velocity = Vector2(velocity.x, 0f)
                        }
                    }
                }
            }
        }
        
        if (groundedThisFrame) {
            if (!isGrounded) {
                // landed
                hasAirDash = true
                doubleJumpsLeft = if (upgradesState.hasDoubleJump) 1 else 0
            }
            isGrounded = true
            coyoteTimer = 0.15f // coyote window of 150ms
        } else {
            isGrounded = false
        }
        
        // 2. Resolve X Collisions (Side Walls)
        for (obs in tempObsList) {
            if (obs.type == PlatformType.SOLID || obs.type == PlatformType.GATE) {
                if (obs.intersects(position.x, position.y, playerSizeX, playerSizeY)) {
                    val penX = if (velocity.x >= 0f) {
                        // Hit wall on right
                        obs.left - (position.x + playerSizeX / 2f)
                    } else {
                        // Hit wall on left
                        obs.right - (position.x - playerSizeX / 2f)
                    }
                    
                    if (abs(penX) < 0.4f) {
                        position = Vector2(position.x + penX, position.y)
                        velocity = Vector2(0f, velocity.y)
                    }
                }
            }
        }
        
        // 3. Trigger Intersects (Lava, Coins, Bouncy, Key, Finish)
        val collectedCoinsList = mutableListOf<Coin2D>()
        for (coin in coins.value) {
            if (!coin.isCollected) {
                val dist = (coin.position - position).length()
                if (dist < (playerSizeX + 0.3f)) {
                    coin.isCollected = true
                    coinsCollectedThisRun++
                    score += 50
                    onHaptic()
                    spawnParticles(coin.position, Color(0xFFFFD700), 10)
                }
            }
            collectedCoinsList.add(coin)
        }
        coins.value = collectedCoinsList
        
        // Process interactive triggers
        for (obs in tempObsList) {
            if (obs.intersects(position.x, position.y, playerSizeX, playerSizeY)) {
                when (obs.type) {
                    PlatformType.LAVA -> {
                        triggerFellInLava(onHaptic)
                    }
                    PlatformType.BOUNCE -> {
                        onHaptic()
                        velocity = Vector2(velocity.x, 15f) // high vertical boost!
                        isGrounded = false
                        isSliding = false
                        playerSizeY = 1.6f
                        spawnParticles(obs.getCenter(), Color(0xFF00FF7F), 12)
                    }
                    PlatformType.KEY -> {
                        if (!hasKey) {
                            onHaptic()
                            hasKey = true
                            score += 150
                            // Remove key from map
                            obstacles.value = obstacles.value.filter { it.id != obs.id }
                            
                            // Unlock GATES by removing them
                            obstacles.value = obstacles.value.filter { it.type != PlatformType.GATE }
                            spawnParticles(obs.getCenter(), Color(0xFF00E5FF), 20)
                        }
                    }
                    PlatformType.PORTAL -> {
                        isLevelCompleted = true
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun triggerFellInLava(onHaptic: () -> Unit) {
        onHaptic()
        isGameOver = true
        spawnParticles(position, Color(0xFFFF3333), 25)
    }
    
    private fun spawnParticles(pos: Vector2, color: Color, count: Int) {
        for (i in 0 until count) {
            val ang = (Math.random() * 2.0 * Math.PI).toFloat()
            val speed = 2f + Math.random().toFloat() * 6f
            particles.add(Particle2D(
                position = pos,
                velocity = Vector2(cos(ang) * speed, sin(ang) * speed + 1f),
                color = color,
                life = 1f,
                size = 3f + Math.random().toFloat() * 5f,
                decay = 2f + Math.random().toFloat() * 3f
            ))
        }
    }
    
    private fun updateParticles(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= p.decay * dt
            if (p.life <= 0f) {
                iterator.remove()
            } else {
                p.position += p.velocity * dt
                // add slight gravity decay to particles
                p.velocity = Vector2(p.velocity.x, p.velocity.y - 9.8f * dt * 0.4f)
            }
        }
    }
}

fun Obstacle2D.getCenter() = Vector2((left + right) / 2f, (top + bottom) / 2f)

// ==========================================
// PERSISTENT UPGRADES & SKINS
// ==========================================
class UpgradesState(private val sharedPrefs: SharedPreferences) {
    var coinsWallet: Int
        get() = sharedPrefs.getInt("coins_wallet", 0)
        set(value) = sharedPrefs.edit().putInt("coins_wallet", value).apply()
        
    var hasDoubleJump: Boolean
        get() = sharedPrefs.getBoolean("upg_double_jump", false)
        set(value) = sharedPrefs.edit().putBoolean("upg_double_jump", value).apply()
        
    var hasAirDash: Boolean
        get() = sharedPrefs.getBoolean("upg_air_dash", false)
        set(value) = sharedPrefs.edit().putBoolean("upg_air_dash", value).apply()
        
    var hasMagnet: Boolean
        get() = sharedPrefs.getBoolean("upg_magnet", false)
        set(value) = sharedPrefs.edit().putBoolean("upg_magnet", value).apply()
        
    var currentSkin: String
        get() = sharedPrefs.getString("current_skin", "default") ?: "default"
        set(value) = sharedPrefs.edit().putString("current_skin", value).apply()
        
    fun isSkinUnlocked(skinId: String): Boolean {
        if (skinId == "default") return true
        return sharedPrefs.getBoolean("skin_unlocked_$skinId", false)
    }
    
    fun unlockSkin(skinId: String) {
        sharedPrefs.edit().putBoolean("skin_unlocked_$skinId", true).apply()
    }
    
    fun getTrailColor(): Color {
        return when (currentSkin) {
            "cyber_blue" -> Color(0xFF00E5FF)
            "shadow_ninja" -> Color(0xFF9C27B0)
            "spectrum" -> Color(0xFFFF33CC) // cyclic glowing cyan pink magenta
            else -> Color(0xFFFF5722)
        }
    }
}

// ==========================================
// CENTRAL TRIAL LEVEL CREATOR
// ==========================================
fun create2DLevels(): List<Level2D> {
    return listOf(
        // LEVEL 1: CYBER ROOFTOPS (THE INCEPTION)
        Level2D(
            id = 1,
            name = "Rooftop Inception",
            description = "Dodge the industrial steam vents and slide under low laser barriers to establish a visual network uplink.",
            difficulty = "Easy",
            startPos = Vector2(2f, 2f),
            width = 65f,
            height = 16f,
            backdropColor = Color(0xFF0C0D14),
            storyIntro = listOf(
                DialogueLine("GEMINI COURIER", "Commander! The syndicate sector's security is offline for a short interval.", Color(0xFF00E5FF)),
                DialogueLine("GEMINI COURIER", "We need you to clear the neon skyscraper roofs. Watch out for low barriers, slide under them using the crouch command!", Color(0xFF00E5FF)),
                DialogueLine("RUNNER RED", "Acknowledged. Uplink path engaged. Initiating standard jump arrays.", Color(0xFFFF5722))
            ),
            obstacles = listOf(
                // Solid floor blocks
                Obstacle2D(101, 0f, 0f, 12f, 1.2f, PlatformType.SOLID, Color(0xFF1F1F2E)),
                Obstacle2D(102, 14f, 0f, 26f, 1.5f, PlatformType.SOLID, Color(0xFF1F1F2E)),
                // Crouch tunnel setup (slide under this SOLID ceiling)
                Obstacle2D(103, 20f, 3.1f, 25f, 5.0f, PlatformType.SOLID, Color(0xFFFF3333), "LASER CEILING"),
                
                // Gap platform with wall cling helper on sides
                Obstacle2D(104, 30f, 0f, 38f, 1.2f, PlatformType.SOLID, Color(0xFF1F1F2E)),
                // Neon vertical Wall-Run pad
                Obstacle2D(105, 30f, 1.2f, 31f, 5.5f, PlatformType.WALL_RUN, Color(0xFFFF5722), "WALL RUN"),
                
                // Final solid tower run
                Obstacle2D(106, 44f, 0f, 62f, 2.0f, PlatformType.SOLID, Color(0xFF1F1F2E)),
                // Bounce Pad to reach Portal
                Obstacle2D(107, 48f, 2.0f, 50f, 2.3f, PlatformType.BOUNCE, Color(0xFF00FF7F), "BOOST"),
                Obstacle2D(108, 55f, 2.0f, 62f, 3.5f, PlatformType.SOLID, Color(0xFF2E2E4A)),
                // Level Exit Portal
                Obstacle2D(109, 58f, 3.5f, 60f, 6.5f, PlatformType.PORTAL, Color(0xFF00FF66), "PORTAL"),
                
                // Pit Hazard Lava
                Obstacle2D(110, 12f, -10f, 14f, 0f, PlatformType.LAVA, Color(0xFF660000)),
                Obstacle2D(111, 26f, -10f, 30f, 0f, PlatformType.LAVA, Color(0xFF660000)),
                Obstacle2D(112, 38f, -10f, 44f, 0f, PlatformType.LAVA, Color(0xFF660000))
            ),
            coins = listOf(
                Coin2D(1, Vector2(7f, 2.2f)),
                Coin2D(2, Vector2(17f, 2.5f)),
                Coin2D(3, Vector2(23f, 2.0f)), // inside slide tunnel!
                Coin2D(4, Vector2(34f, 3.0f)),
                Coin2D(5, Vector2(49f, 5.5f))  // High boost coin!
            )
        ),
        
        // LEVEL 2: GLITCHED RUINS (KEY & SECURITY GATES)
        Level2D(
            id = 2,
            name = "Glitched Sanctuary",
            description = "Retrieve the core security key matrix to open the encrypted light gates and complete the trial.",
            difficulty = "Medium",
            startPos = Vector2(2f, 2f),
            width = 80f,
            height = 18f,
            backdropColor = Color(0xFF090E0A),
            storyIntro = listOf(
                DialogueLine("GEMINI COURIER", "Caution! The sanctuary gates are shut tight by cryptographic algorithms.", Color(0xFF00E5FF)),
                DialogueLine("GEMINI COURIER", "You must collect the glowing CYAN encryption KEY located on the high pillars to override the security barriers.", Color(0xFF00E5FF)),
                DialogueLine("RUNNER RED", "Retrieving keys. Speed vectors stabilized.", Color(0xFFFF5722))
            ),
            obstacles = listOf(
                // Ground blocks
                Obstacle2D(201, 0f, 0f, 15f, 1.2f, PlatformType.SOLID, Color(0xFF1E281F)),
                // Key pillar (floating platform)
                Obstacle2D(202, 21f, 3.8f, 25f, 4.2f, PlatformType.SOLID, Color(0xFF2E3E2F)),
                // Actual Key collectible triggers
                Obstacle2D(203, 22.5f, 4.3f, 23.5f, 5.3f, PlatformType.KEY, Color(0xFF00E5FF), "ENCRYPTION KEY"),
                
                // Intermediate platforms
                Obstacle2D(204, 30f, 0f, 40f, 1.5f, PlatformType.SOLID, Color(0xFF1E281F)),
                // Locked Security GATE (Blocks runner until key is retrieved)
                Obstacle2D(205, 38f, 1.5f, 39.5f, 6.0f, PlatformType.GATE, Color(0xFFFFA500), "LOCKED GATES"),
                
                // Post-Gate wall run challenges
                Obstacle2D(206, 44f, 0f, 52f, 1.2f, PlatformType.SOLID, Color(0xFF1E281F)),
                Obstacle2D(207, 44f, 1.2f, 45f, 5.5f, PlatformType.WALL_RUN, Color(0xFFFF5722)),
                
                // Final tower and exit
                Obstacle2D(208, 58f, 0f, 75f, 2.5f, PlatformType.SOLID, Color(0xFF2E3E2F)),
                Obstacle2D(209, 68f, 2.5f, 70f, 5.5f, PlatformType.WALL_RUN, Color(0xFF00E5FF)),
                Obstacle2D(210, 71f, 2.5f, 74f, 4.5f, PlatformType.PORTAL, Color(0xFF00FF66), "EXIT"),
                
                // Lava Hazards
                Obstacle2D(211, 15f, -10f, 30f, 0f, PlatformType.LAVA, Color(0xFF4A1005)),
                Obstacle2D(212, 40f, -10f, 58f, 0f, PlatformType.LAVA, Color(0xFF4A1005))
            ),
            coins = listOf(
                Coin2D(1, Vector2(10f, 2.2f)),
                Coin2D(2, Vector2(23f, 5.8f)), // directly above key!
                Coin2D(3, Vector2(34f, 2.5f)),
                Coin2D(4, Vector2(48f, 3.2f)),
                Coin2D(5, Vector2(62f, 4.0f))
            )
        ),
        
        // LEVEL 3: MAGMA FOUNDRY (DEEP INFERNO HAZARDS)
        Level2D(
            id = 3,
            name = "Magma Foundry",
            description = "A subterranean industrial melting pot. Wall run repeatedly over active volcanic vents to escape.",
            difficulty = "Hard",
            startPos = Vector2(2f, 2f),
            width = 90f,
            height = 20f,
            backdropColor = Color(0xFF140808),
            storyIntro = listOf(
                DialogueLine("GEMINI COURIER", "Caution! The core coolant line ruptured! Ambient lava levels are extremely dangerous.", Color(0xFFFF5722)),
                DialogueLine("GEMINI COURIER", "You must perform vertical wall climbs back-to-back. No ground is safe here!", Color(0xFFFF5722)),
                DialogueLine("RUNNER RED", "Thermal suit at capacity. Executing continuous speed runs.", Color(0xFFFF5722))
            ),
            obstacles = listOf(
                // Initial Safe Pad
                Obstacle2D(301, 0f, 0f, 10f, 1.2f, PlatformType.SOLID, Color(0xFF2D1612)),
                
                // Wall Run block 1
                Obstacle2D(302, 14f, 1.2f, 15.5f, 6.5f, PlatformType.WALL_RUN, Color(0xFFFF5722)),
                // Tiny middle landing stick
                Obstacle2D(303, 19f, 0f, 22f, 1.0f, PlatformType.SOLID, Color(0xFF2D1612)),
                
                // Wall Run block 2 (climb on left, bounce on right)
                Obstacle2D(304, 27f, 1.5f, 28.5f, 7.0f, PlatformType.WALL_RUN, Color(0xFF00E5FF)),
                
                // Higher landing pad
                Obstacle2D(305, 34f, 2.5f, 44f, 3.5f, PlatformType.SOLID, Color(0xFF2D1612)),
                Obstacle2D(306, 38f, 3.5f, 40f, 3.8f, PlatformType.BOUNCE, Color(0xFF00FF7F)),
                
                // Extreme gap
                Obstacle2D(307, 50f, 1.0f, 58f, 2.0f, PlatformType.SOLID, Color(0xFF2D1612)),
                Obstacle2D(308, 50f, 2.0f, 51.5f, 6.8f, PlatformType.WALL_RUN, Color(0xFFFF5722)),
                
                // Key and Gate override
                Obstacle2D(309, 63f, 4.0f, 65f, 5.0f, PlatformType.KEY, Color(0xFF00E5FF), "KEY"),
                Obstacle2D(310, 62f, 0f, 69f, 1.5f, PlatformType.SOLID, Color(0xFF2D1612)),
                Obstacle2D(311, 68f, 1.5f, 69f, 6.0f, PlatformType.GATE, Color(0xFFFF3333)),
                
                // Final run
                Obstacle2D(312, 75f, 0f, 88f, 2.2f, PlatformType.SOLID, Color(0xFF3D1D16)),
                Obstacle2D(313, 80f, 2.2f, 83f, 5.2f, PlatformType.PORTAL, Color(0xFF00FF66)),
                
                // Bottom Lava Ocean (Continuous)
                Obstacle2D(314, 10f, -10f, 75f, 0f, PlatformType.LAVA, Color(0xFFD50000))
            ),
            coins = listOf(
                Coin2D(1, Vector2(15f, 4.5f)),
                Coin2D(2, Vector2(28f, 5.0f)),
                Coin2D(3, Vector2(39f, 6.8f)),
                Coin2D(4, Vector2(54f, 4.2f)),
                Coin2D(5, Vector2(64f, 6.2f))
            )
        )
    )
}

// ==========================================
// VIEW MODEL WITH SHOP & DIALOGUE SEQUENCES
// ==========================================
class ParkourViewModel(context: Context) : ViewModel() {
    private val sharedPrefs = context.getSharedPreferences("parkour_2d_saved_data", Context.MODE_PRIVATE)
    
    // Upgrades and currency state
    val upgradesState = UpgradesState(sharedPrefs)
    
    // Game lists
    val levels = create2DLevels()
    var currentLevelIndex by mutableStateOf(0)
    
    // Game active states
    val engine: GameEngine2D
    
    private val _uiState = MutableStateFlow(GameStateStatus.MAIN_MENU)
    val uiState = _uiState.asStateFlow()
    
    // Personal best times map
    val bestTimesMap = mutableStateOf<Map<Int, Float>>(emptyMap())
    
    // Dialogue status
    var dialogueIndex by mutableStateOf(0)
    val activeDialogueLine: DialogueLine?
        get() {
            val story = levels[currentLevelIndex].storyIntro
            return if (story.isNotEmpty() && dialogueIndex < story.size) story[dialogueIndex] else null
        }
    
    // Settings
    var hapticEnabled by mutableStateOf(sharedPrefs.getBoolean("haptic_enabled", true))
    var touchSensitivity by mutableStateOf(sharedPrefs.getFloat("touch_sensitivity", 1.0f))
    
    init {
        engine = GameEngine2D(levels[0], upgradesState)
        loadSavedStats()
    }
    
    fun selectLevel(index: Int) {
        if (index in levels.indices) {
            currentLevelIndex = index
            engine.currentLevel = levels[index]
            engine.resetEngine()
            
            // Check if level has story dialogue lines
            if (levels[index].storyIntro.isNotEmpty()) {
                dialogueIndex = 0
                setUIStatus(GameStateStatus.DIALOGUE)
            } else {
                setUIStatus(GameStateStatus.PLAYING)
            }
        }
    }
    
    fun advanceDialogue() {
        dialogueIndex++
        if (dialogueIndex >= levels[currentLevelIndex].storyIntro.size) {
            setUIStatus(GameStateStatus.PLAYING)
        }
    }
    
    fun skipDialogue() {
        setUIStatus(GameStateStatus.PLAYING)
    }
    
    fun restartLevel() {
        engine.resetEngine()
        setUIStatus(GameStateStatus.PLAYING)
    }
    
    fun setUIStatus(status: GameStateStatus) {
        _uiState.value = status
    }
    
    fun buyUpgrade(type: String, cost: Int): Boolean {
        val currentWallet = upgradesState.coinsWallet
        if (currentWallet >= cost) {
            upgradesState.coinsWallet = currentWallet - cost
            when (type) {
                "double_jump" -> upgradesState.hasDoubleJump = true
                "air_dash" -> upgradesState.hasAirDash = true
                "magnet" -> upgradesState.hasMagnet = true
            }
            return true
        }
        return false
    }
    
    fun unlockAndEquipSkin(skin: SkinType): Boolean {
        if (upgradesState.isSkinUnlocked(skin.id)) {
            upgradesState.currentSkin = skin.id
            return true
        } else {
            val wallet = upgradesState.coinsWallet
            if (wallet >= skin.cost) {
                upgradesState.coinsWallet = wallet - skin.cost
                upgradesState.unlockSkin(skin.id)
                upgradesState.currentSkin = skin.id
                return true
            }
        }
        return false
    }
    
    fun saveBestTime(levelId: Int, time: Float) {
        val key = "level_pb_$levelId"
        val currentPB = sharedPrefs.getFloat(key, Float.MAX_VALUE)
        if (time < currentPB) {
            sharedPrefs.edit().putFloat(key, time).apply()
            loadSavedStats()
        }
        
        // Secure coins and add to wallet
        val levelCoins = engine.coinsCollectedThisRun
        upgradesState.coinsWallet += levelCoins
    }
    
    fun saveSettings() {
        sharedPrefs.edit()
            .putBoolean("haptic_enabled", hapticEnabled)
            .putFloat("touch_sensitivity", touchSensitivity)
            .apply()
    }
    
    private fun loadSavedStats() {
        val map = mutableMapOf<Int, Float>()
        for (lvl in levels) {
            val pb = sharedPrefs.getFloat("level_pb_${lvl.id}", Float.MAX_VALUE)
            if (pb != Float.MAX_VALUE) {
                map[lvl.id] = pb
            }
        }
        bestTimesMap.value = map
    }
}
