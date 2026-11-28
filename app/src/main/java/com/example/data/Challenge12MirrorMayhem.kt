package com.example.data

/**
 * Challenge 12: "Mirror Mayhem"
 *
 * Central Mechanics: GUST_REVERSAL_ZONE (periodic horizontal wind flips with pre-flip
 * telegraph tinting) & MIRROR_CURSE_TOTEM (temporary readable left/right control inversion
 * with purple halo and clean reset on death/retry).
 */
object Challenge12MirrorMayhem {
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

    // 1. Launchpad & Gust Arena #1 (0..4, 5 Fake Safe, 6..10, 12..15, Y=13 with manageable center gap at tile 11)
    addRow(0, 4, 13f, TileType.CARVED_STONE)
    addRow(6, 10, 13f, TileType.CARVED_STONE)
    addRow(12, 15, 13f, TileType.CARVED_STONE)

    // 2. Mirror Curse Arena (16..19, Y=13 -> 20 disappearing -> 21..26, Y=13, low central step 21..23, Y=12.0)
    addRow(16, 19, 13f, TileType.CARVED_STONE)
    addRow(21, 26, 13f, TileType.CARVED_STONE)
    addRow(21, 23, 12.0f, TileType.CARVED_STONE)

    // 3. Checkpoint Sanctuary (27..30, Y=13)
    addRow(27, 30, 13f, TileType.CARVED_STONE)
    tiles.add(
        GameTile(
            id = nextId++,
            x = 28.0f * T,
            y = 12f * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )

    // 4. Gust Reversal #2 + Final Mirror Curse Dash + Mirage Exit Finale (31..41, 42..43 disappearing, 44..53, Y=13)
    addRow(31, 41, 13f, TileType.CARVED_STONE)
    addRow(44, 53, 13f, TileType.CARVED_STONE)
    val exitX = 50.0f * T
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
        Coin(id = 1, x = 4.0f * T, y = 10.4f * T),
        Coin(id = 2, x = 11.0f * T, y = 10.8f * T),
        Coin(id = 3, x = 22.0f * T, y = 10.5f * T),
        Coin(id = 4, x = 35.0f * T, y = 11.2f * T),
        Coin(id = 5, x = 48.5f * T, y = 11.5f * T)
    )

    val trollTraps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.GUST_REVERSAL_ZONE,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 6.5f * T,
            initialY = 8.5f * T,
            width = 8.5f * T,
            height = 4.5f * T,
            cycleFrames = 116,
            gustFlipIntervalFrames = 116,
            gustForceX = -0.32f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.MIRROR_CURSE_TOTEM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 17.5f * T,
            initialY = 13f * T - 64f,
            width = 40f,
            height = 64f,
            triggerMinX = 16.5f * T,
            triggerMaxX = 19.2f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 12,
            effectDurationFrames = 140,
            killsPlayer = false,
            quipPopup = "LEFT IS RIGHT!"
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.GUST_REVERSAL_ZONE,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 31.5f * T,
            initialY = 8.5f * T,
            width = 7.5f * T,
            height = 4.5f * T,
            cycleFrames = 120,
            gustFlipIntervalFrames = 120,
            gustForceX = 0.34f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.MIRROR_CURSE_TOTEM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 40.0f * T,
            initialY = 13f * T - 64f,
            width = 40f,
            height = 64f,
            triggerMinX = 39.0f * T,
            triggerMaxX = 41.5f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            effectDurationFrames = 120,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "MIRRORED!"
        ),
        // Fake Exit Finale Part A: Non-blocking mirage Sun Gate poofs on approach!
        TrollTrap(
            id = 5,
            type = TrollTrapType.FAKE_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 44.8f * T,
            initialY = 10f * T,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 43.9f * T,
            triggerMaxX = 47.0f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 14,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "MIRAGE GATE!"
        ),
        // Fake Exit Finale Part B: Chained floor spike bursts just past the mirage, retracting cleanly!
        TrollTrap(
            id = 6,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 5,
            initialX = 47.2f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 12,
            activeDurationFrames = 75,
            killsPlayer = true,
            quipPopup = "NOT SO FAST!"
        ),
        // Combo: Mirror Curse Totem #2 triggers an instant ceiling drop at the arena exit while controls are mirrored
        TrollTrap(
            id = 7,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 2,
            chainDelayFrames = 24,
            initialX = 24.6f * T,
            initialY = 6.2f * T,
            width = 50f,
            height = 52f,
            telegraphDurationFrames = 0,
            targetX = 24.6f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 13.5f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "MIRROR DROP!"
        ),
        // Mirror arena seam collapses while controls invert
        TrollTrap(
            id = 8,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 20.0f * T,
            initialY = 13f * T,
            width = T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "WATCH THAT STEP!"
        ),
        // Disappearing floor gap just after Mirror Totem #2 before Mirage Gate
        TrollTrap(
            id = 9,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 42.0f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "BACKWARD LEAP!"
        ),
        // Early Coin #1 jump bait flips controls briefly right before Gust Arena #1!
        TrollTrap(
            id = 10,
            type = TrollTrapType.MIRROR_CURSE_TOTEM,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 1,
            chainDelayFrames = 0,
            initialX = 4.0f * T,
            initialY = 13f * T - 64f,
            width = 40f,
            height = 64f,
            telegraphDurationFrames = 0,
            effectDurationFrames = 75,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "MINI CURSE!"
        ),
        // Intro trapdoor tile right on the opening sprint so ch_12 never starts flat
        TrollTrap(
            id = 11,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 5.0f * T,
            initialY = 13f * T,
            width = T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "FALSE START!"
        ),
        // Coin #4 in Gust Arena #2 pops an instant floor spike in the crosswind
        TrollTrap(
            id = 12,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 4,
            chainDelayFrames = 0,
            initialX = 35.2f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "GUST BITE!"
        ),
        // Jumping through Gust Arena #2 wakes a mid-air ceiling block
        TrollTrap(
            id = 13,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_JUMP_ABOVE,
            initialX = 37.4f * T,
            initialY = 5.8f * T,
            width = 48f,
            height = 50f,
            triggerMinX = 36.4f * T,
            triggerMaxX = 38.6f * T,
            triggerMinY = 6.5f * T,
            triggerMaxY = 12.2f * T,
            targetX = 37.4f * T,
            targetY = 13f * T - 50f,
            moveSpeed = 13.8f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "WIND DROP!"
        ),
        // Final Coin #5 directly ahead of the Sun Gate triggers an instant keystone drop
        TrollTrap(
            id = 14,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            chainDelayFrames = 0,
            initialX = 48.8f * T,
            initialY = 5.8f * T,
            width = 48f,
            height = 50f,
            targetX = 48.8f * T,
            targetY = 13f * T - 50f,
            moveSpeed = 14.2f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "FINAL GREED!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 12,
        title = "Mirror Mayhem",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 30,
        difficulty = difficulty,
        subtitle = "Shifting Gusts & Mirror Totems",
        description = "Watch wind chevrons flip direction and adapt when violet totems temporarily mirror left and right controls.",
        theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
