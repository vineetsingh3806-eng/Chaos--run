package com.example.data

/**
 * World 1 — Level 1-4: "The Floor Has Plans"
 *
 * Central Mechanic: Every surface has a schedule — ascending delayed-crumble spans,
 * alternating phase-shifted Timed Blink Platforms, an oscillating Moving Spike deck,
 * and ambush Falling Ceiling Blocks.
 */
object Level14FloorHasPlans {
  private const val T = World1Levels.TILE_SIZE

  fun build(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var nextId = 1

    fun addRow(startTileX: Int, endTileX: Int, tileY: Float, type: TileType) {
      for (tx in startTileX..endTileX) {
        tiles.add(
            GameTile(
                id = nextId++,
                x = tx * T,
                y = tileY * T,
                width = T,
                height = T,
                type = type
            )
        )
      }
    }

    // 1. Safe monsoon stone launch pad (Tiles 0 - 7, Y = 13)
    addRow(0, 7, 13f, TileType.CARVED_STONE)

    // (Tiles 8..20 = Three ascending DELAYED_CRUMBLE bridges!)

    // 2. Mid breather stone platform (Tiles 22 - 25, Y = 11.5)
    addRow(22, 25, 11.5f, TileType.CARVED_STONE)

    // (Tiles 27..33.5 = Alternating phase-shifted TIMED_BLINK_PLATFORM pair)
    // (Tile 35 = DISAPPEARING_FLOOR surprise right after Blink Pad B)

    // 3. Stone Patrol Deck (Tiles 36 - 44, Y = 12.5)
    addRow(36, 44, 12.5f, TileType.CARVED_STONE)

    // (Tiles 46 - 47 = DISAPPEARING_FLOOR)

    // 4. Bait Coin Stone Island (Tiles 48 - 50, Y = 12.5)
    addRow(48, 50, 12.5f, TileType.CARVED_STONE)

    // (Tiles 51.5 - 53 = Trapdoor FAKE_SAFE_PLATFORM)

    // 5. Final Sanctum Platform (Tiles 54 - 59, Y = 12.5)
    addRow(54, 59, 12.5f, TileType.CARVED_STONE)

    val exitX = 56.5f * T
    val exitY = 9.5f * T
    tiles.add(
        GameTile(
            id = nextId++,
            x = exitX,
            y = exitY,
            width = T * 2f,
            height = T * 3f,
            type = TileType.SUN_GATE,
            isSolid = false
        )
    )

    val coins = listOf(
        Coin(id = 1, x = 4.5f * T, y = 11.5f * T),    // Intro coin
        Coin(id = 2, x = 19.0f * T, y = 9.0f * T),    // Apex of crumbling staircase
        Coin(id = 3, x = 30.25f * T, y = 9.8f * T),   // Mid-air coin between blink pads
        Coin(id = 4, x = 49.4f * T, y = 10.2f * T)    // Greedy coin under ON_JUMP_ABOVE thwomp
    )

    val lag = when (difficulty) {
      DifficultyTier.NORMAL -> 0
      DifficultyTier.HARD -> -1
      DifficultyTier.NIGHTMARE -> -2
    }

    val blinkCycle = when (difficulty) {
      DifficultyTier.NORMAL -> 150
      DifficultyTier.HARD -> 136
      DifficultyTier.NIGHTMARE -> 124
    }

    val trollTraps = listOf(
        // Stair 1: Delayed Crumble (8..10, Y=13)
        TrollTrap(
            id = 1,
            type = TrollTrapType.DELAYED_CRUMBLE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 8f * T,
            initialY = 13f * T,
            width = T * 3f,
            height = T * 0.8f,
            telegraphDurationFrames = (26 + lag * 3).coerceAtLeast(16),
            killsPlayer = false,
            quipPopup = "DON'T PAUSE!"
        ),
        // Stair 2: Delayed Crumble (13..15, Y=11.8)
        TrollTrap(
            id = 2,
            type = TrollTrapType.DELAYED_CRUMBLE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 13f * T,
            initialY = 11.8f * T,
            width = T * 3f,
            height = T * 0.8f,
            telegraphDurationFrames = (24 + lag * 3).coerceAtLeast(15),
            killsPlayer = false
        ),
        // Stair 3: Delayed Crumble (18..20, Y=10.6)
        TrollTrap(
            id = 3,
            type = TrollTrapType.DELAYED_CRUMBLE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 18f * T,
            initialY = 10.6f * T,
            width = T * 3f,
            height = T * 0.8f,
            telegraphDurationFrames = (23 + lag * 3).coerceAtLeast(14),
            killsPlayer = false
        ),
        // Blink Pad A (phase 0 — starts solid!)
        TrollTrap(
            id = 4,
            type = TrollTrapType.TIMED_BLINK_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 27.0f * T,
            initialY = 11.5f * T,
            width = T * 2.5f,
            height = 28f,
            cycleFrames = blinkCycle,
            phaseOffsetFrames = 0,
            killsPlayer = false
        ),
        // Blink Pad B (180-degree phase shift — solid when Pad A ghosts out!)
        TrollTrap(
            id = 5,
            type = TrollTrapType.TIMED_BLINK_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 31.0f * T,
            initialY = 11.5f * T,
            width = T * 2.5f,
            height = 28f,
            cycleFrames = blinkCycle,
            phaseOffsetFrames = blinkCycle / 2,
            killsPlayer = false
        ),
        // Disappearing Floor ambush at landing edge (Tile 35)
        TrollTrap(
            id = 6,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 35f * T,
            initialY = 12.5f * T,
            width = T,
            height = T,
            telegraphDurationFrames = (4 + lag).coerceAtLeast(2),
            killsPlayer = false,
            quipPopup = "KEEP GOING!"
        ),
        // Oscillating Moving Spike patrolling the stone deck (37.5 -> 42.0)
        TrollTrap(
            id = 7,
            type = TrollTrapType.MOVING_SPIKE,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 37.5f * T,
            initialY = 12.5f * T - 30f,
            width = 44f,
            height = 30f,
            targetX = 42.0f * T,
            targetY = 12.5f * T - 30f,
            cycleFrames = when (difficulty) {
              DifficultyTier.NORMAL -> 118
              DifficultyTier.HARD -> 102
              DifficultyTier.NIGHTMARE -> 92
            },
            killsPlayer = true,
            quipPopup = "OOPS!"
        ),
        // Proximity Falling Ceiling Block slamming halfway across stone deck (40.5)
        TrollTrap(
            id = 8,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 40.5f * T,
            initialY = 5.5f * T,
            width = 54f,
            height = 54f,
            triggerMinX = 39.5f * T,
            triggerMaxX = 42.0f * T,
            triggerMinY = 6.5f * T,
            triggerMaxY = 13.0f * T,
            telegraphDurationFrames = (8 + lag).coerceAtLeast(4),
            targetY = 12.5f * T - 54f,
            moveSpeed = 13.5f,
            killsPlayer = true,
            quipPopup = "HEADS UP!"
        ),
        // Disappearing bridge planks before Coin #4 Island (46..47)
        TrollTrap(
            id = 9,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 46f * T,
            initialY = 12.5f * T,
            width = T,
            height = T,
            telegraphDurationFrames = (4 + lag).coerceAtLeast(2),
            killsPlayer = false
        ),
        TrollTrap(
            id = 10,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 47f * T,
            initialY = 12.5f * T,
            width = T,
            height = T,
            telegraphDurationFrames = (4 + lag).coerceAtLeast(2),
            killsPlayer = false
        ),
        // ON_JUMP_ABOVE ambush ceiling block over Coin #4 on Island 48..50
        TrollTrap(
            id = 11,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_JUMP_ABOVE,
            initialX = 48.8f * T,
            initialY = 5.2f * T,
            width = 54f,
            height = 54f,
            triggerMinX = 48.1f * T,
            triggerMaxX = 50.3f * T,
            triggerMinY = 5.5f * T,
            triggerMaxY = 12.9f * T,
            telegraphDurationFrames = (7 + lag).coerceAtLeast(3),
            targetY = 12.5f * T - 54f,
            moveSpeed = 15.0f,
            killsPlayer = true,
            quipPopup = "TIMING!"
        ),
        // Final trapdoor plank right before the Sanctum Platform (51.5..52.8)
        TrollTrap(
            id = 12,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 51.4f * T,
            initialY = 12.5f * T,
            width = T * 1.6f,
            height = T,
            telegraphDurationFrames = (5 + lag).coerceAtLeast(2),
            killsPlayer = false,
            quipPopup = "SO CLOSE!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 4,
        title = "The Floor Has Plans",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 25,
        difficulty = difficulty,
        subtitle = "Timing & Betrayal",
        description = "Crumbling monsoon stairs, blinking phase bridges, patrolling floor blades, and ambush stone ceilings.",
        theme = LevelTheme.MONSOON_RUN,
        hasHeavyRain = true,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
