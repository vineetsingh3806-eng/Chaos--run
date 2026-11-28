package com.example.data

/**
 * Challenge 06: "The Cracking Road"
 *
 * Central Mechanics: SPLIT_BRIDGE (outer solid wings survive when center cracks open)
 * & PHANTOM_LEDGE (ghosted ledges with rune clues that phase out under step and reform).
 */
object Challenge06CrackingRoad {
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

    // 1. Opening launchpad (0..3, Y=13) -> Disappearing seam (4..5) -> step (6, Y=13)
    addRow(0, 3, 13f, TileType.CARVED_STONE)
    addRow(6, 6, 13f, TileType.CARVED_STONE)
    // Gap 7..10 bridged by Split Bridge #1

    // 2. Mid landing terrace (11..15, Y=13)
    addRow(11, 15, 13f, TileType.CARVED_STONE)
    // Chasm 16..23 crossed via Phantom Ledges #1 & #2

    // 3. Checkpoint sanctuary (24..28, Y=12.5)
    addRow(24, 28, 12.5f, TileType.CARVED_STONE)
    tiles.add(
        GameTile(
            id = nextId++,
            x = 25.5f * T,
            y = 11.5f * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )

    // Gap 29..33 bridged by Split Bridge #2 -> stone pier (34..36, Y=12.5)
    addRow(34, 36, 12.5f, TileType.CARVED_STONE)
    // Gap 37..39.5 crossed via Phantom Ledge #3

    // 4. Exit sanctum (40..41 disappearing threshold -> 42..50 solid, Y=13)
    addRow(42, 50, 13f, TileType.CARVED_STONE)
    val exitX = 43.5f * T
    val exitY = 10f * T
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
        Coin(id = 1, x = 4.8f * T, y = 10.4f * T),
        Coin(id = 2, x = 13.0f * T, y = 11.5f * T),
        Coin(id = 3, x = 21.4f * T, y = 10.2f * T),
        Coin(id = 4, x = 38.2f * T, y = 10.4f * T),
        Coin(id = 5, x = 42.3f * T, y = 11.5f * T)
    )

    val lag = when (difficulty) {
      DifficultyTier.NORMAL -> 0
      DifficultyTier.HARD -> -1
      DifficultyTier.NIGHTMARE -> -2
    }

    val trollTraps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.SPLIT_BRIDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 7f * T,
            initialY = 13f * T,
            width = 4f * T,
            height = T,
            telegraphDurationFrames = (12 + lag).coerceAtLeast(10),
            killsPlayer = false,
            quipPopup = "MIND THE GAP!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.PHANTOM_LEDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 16.4f * T,
            initialY = 12.4f * T,
            width = 2.6f * T,
            height = 24f,
            telegraphDurationFrames = (10 + lag).coerceAtLeast(8),
            cycleFrames = 102,
            killsPlayer = false,
            quipPopup = "FADED!"
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.PHANTOM_LEDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 20.2f * T,
            initialY = 11.9f * T,
            width = 2.6f * T,
            height = 24f,
            telegraphDurationFrames = 0,
            cycleFrames = 100,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "KEEP MOVING!"
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.SPLIT_BRIDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 29.3f * T,
            initialY = 12.5f * T,
            width = 4.2f * T,
            height = T,
            telegraphDurationFrames = (12 + lag).coerceAtLeast(10),
            killsPlayer = false,
            quipPopup = "SPLIT APART!"
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.PHANTOM_LEDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 36.8f * T,
            initialY = 12.1f * T,
            width = 2.6f * T,
            height = 24f,
            telegraphDurationFrames = 0,
            cycleFrames = 102,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        // Combo: Split Bridge #1 wakes a chained floor spike on the mid terrace
        TrollTrap(
            id = 6,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 1,
            chainDelayFrames = 10,
            initialX = 12.1f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 12,
            activeDurationFrames = 70,
            killsPlayer = true,
            quipPopup = "WATCH YOUR STEP!"
        ),
        // Combo: Collecting Coin #2 on the mid terrace drops a fast overhead keystone
        TrollTrap(
            id = 7,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 2,
            chainDelayFrames = 2,
            initialX = 13.2f * T,
            initialY = 6.5f * T,
            width = 50f,
            height = 52f,
            telegraphDurationFrames = 8,
            targetX = 13.2f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 14.5f,
            killsPlayer = true,
            quipPopup = "BAIT DROP!"
        ),
        // Late surprise: Disappearing threshold just before the Sun Gate stone
        TrollTrap(
            id = 8,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 40.0f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SO CLOSE!"
        ),
        // Mid-late instant spike pop on the narrow stone pier before Phantom Ledge #3
        TrollTrap(
            id = 9,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 35.2f * T,
            initialY = 12.5f * T - 28f,
            width = 42f,
            height = 28f,
            triggerMinX = 34.4f * T,
            triggerMaxX = 36.2f * T,
            triggerMinY = 8.5f * T,
            triggerMaxY = 13.0f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "PIER TRAP!"
        ),
        // Early surprise seam right under Coin #1 so ch_06 starts active immediately
        TrollTrap(
            id = 10,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 4.0f * T,
            initialY = 13f * T,
            width = 2.0f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "CRACKED START!"
        ),
        // Combo: Aerial Coin #4 drops a surprise keystone onto the landing stone ahead!
        TrollTrap(
            id = 11,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 4,
            chainDelayFrames = 4,
            initialX = 41.2f * T,
            initialY = 5.8f * T,
            width = 48f,
            height = 48f,
            telegraphDurationFrames = 0,
            targetX = 41.2f * T,
            targetY = 13f * T - 48f,
            moveSpeed = 14.0f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "BRIDGE TAX!"
        ),
        // Combo: Phantom Ledge #2 triggers a delayed ceiling drop just before the Checkpoint Bell
        TrollTrap(
            id = 12,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 3,
            chainDelayFrames = 26,
            initialX = 24.4f * T,
            initialY = 5.6f * T,
            width = 48f,
            height = 48f,
            telegraphDurationFrames = 0,
            targetX = 24.4f * T,
            targetY = 12.5f * T - 48f,
            moveSpeed = 13.8f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "BELL CHECK!"
        ),
        // Finale Twist: Sun Gate unexpectedly leaps farther down the sanctum on approach
        TrollTrap(
            id = 13,
            type = TrollTrapType.RELOCATING_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = exitX,
            initialY = exitY,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 42.3f * T,
            triggerMaxX = 45.2f * T,
            triggerMinY = 8.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            linkedDoorTargetX = 47.6f * T,
            linkedDoorTargetY = exitY,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "CRACKING EXIT!"
        ),
        // Chained spike guard between original exit perch and relocated Sun Gate
        TrollTrap(
            id = 14,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 13,
            chainDelayFrames = 8,
            initialX = 46.0f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 60,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "DOOR TAX!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 6,
        title = "The Cracking Road",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 28,
        difficulty = difficulty,
        subtitle = "Split Bridges & Phantom Ledges",
        description = "Read center seams on wooden spans and leap cleanly off fading phantom ledges before they vanish.",
        theme = LevelTheme.WHISPERING_WOODS,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
