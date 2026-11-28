package com.example.data

/**
 * Challenge 08: "The Runaway Floor"
 *
 * Central Mechanics: EVASIVE_PLATFORM (winged platforms gliding sideways when approached,
 * paired with fair alternative landing ledges) & YO_YO_PLATFORM (smooth vertical lifts).
 */
object Challenge08RunawayFloor {
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

    // 1. Launchpad (0..3, Y=13) + disappearing edge (4..5)
    addRow(0, 3, 13f, TileType.CARVED_STONE)

    // Chasm 6..14: Yo-Yo Lift #1 -> Evasive Platform #1 + fair lower backup stone (11.2..13.2, Y=13.2)
    tiles.add(GameTile(id = nextId++, x = 11.2f * T, y = 13.2f * T, width = 2.0f * T, height = 0.8f * T, type = TileType.CARVED_STONE))

    // 2. Mid island (14.8..18.5, Y=13)
    addRow(15, 18, 13f, TileType.CARVED_STONE)

    // Chasm 19..23: Yo-Yo Lift #2

    // 3. Checkpoint sanctuary (23.5..27.5, Y=12.5)
    addRow(24, 27, 12.5f, TileType.CARVED_STONE)
    tiles.add(
        GameTile(
            id = nextId++,
            x = 25.0f * T,
            y = 11.5f * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )

    // Chasm 28..37: Evasive Platform #2 + fair lower backup stone (29.2..31.0, Y=13.2) -> Yo-Yo Lift #3
    tiles.add(GameTile(id = nextId++, x = 29.2f * T, y = 13.2f * T, width = 1.8f * T, height = 0.8f * T, type = TileType.CARVED_STONE))

    // 4. Exit sanctum (38..44, Y=12.5)
    addRow(38, 44, 12.5f, TileType.CARVED_STONE)
    val exitX = 41.5f * T
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
        Coin(id = 1, x = 3.2f * T, y = 11.5f * T),
        Coin(id = 2, x = 7.7f * T, y = 9.1f * T),
        Coin(id = 3, x = 21.0f * T, y = 8.6f * T),
        Coin(id = 4, x = 30.8f * T, y = 9.8f * T),
        Coin(id = 5, x = 39.3f * T, y = 9.5f * T) // Optional elevated bait coin above exit approach
    )

    val trollTraps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.YO_YO_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 6.5f * T,
            initialY = 13.0f * T,
            width = 2.5f * T,
            height = 24f,
            targetX = 6.5f * T,
            targetY = 10.6f * T,
            cycleFrames = 112,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.EVASIVE_PLATFORM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 10.6f * T,
            initialY = 11.5f * T,
            width = 2.5f * T,
            height = 24f,
            triggerMinX = 9.3f * T,
            triggerMaxX = 11.5f * T,
            triggerMinY = 7.5f * T,
            triggerMaxY = 13.8f * T,
            telegraphDurationFrames = 0,
            targetX = 12.8f * T,
            targetY = 11.5f * T,
            moveSpeed = 6.5f,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SEE YA!"
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.YO_YO_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 19.6f * T,
            initialY = 13.1f * T,
            width = 2.5f * T,
            height = 24f,
            targetX = 19.6f * T,
            targetY = 10.0f * T,
            cycleFrames = 116,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.EVASIVE_PLATFORM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 28.8f * T,
            initialY = 11.4f * T,
            width = 2.5f * T,
            height = 24f,
            triggerMinX = 27.6f * T,
            triggerMaxX = 29.6f * T,
            triggerMinY = 7.5f * T,
            triggerMaxY = 13.8f * T,
            telegraphDurationFrames = 0,
            targetX = 31.4f * T,
            targetY = 11.4f * T,
            moveSpeed = 6.8f,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "NOPE!"
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.YO_YO_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 34.4f * T,
            initialY = 12.8f * T,
            width = 2.5f * T,
            height = 24f,
            targetX = 34.4f * T,
            targetY = 9.9f * T,
            cycleFrames = 108,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        // Coin Betrayal: collecting optional Coin #5 triggers a quick telegraphed ceiling drop
        TrollTrap(
            id = 6,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            initialX = 39.1f * T,
            initialY = 4.8f * T,
            width = 54f,
            height = 54f,
            telegraphDurationFrames = 6,
            targetY = 12.5f * T - 54f,
            moveSpeed = 14.5f,
            killsPlayer = true,
            quipPopup = "GREEDY COIN!"
        ),
        // Combo: After Evasive Platform #4 dodges away, the backup stone pops an instant delayed spike
        TrollTrap(
            id = 7,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 4,
            chainDelayFrames = 14,
            initialX = 29.8f * T,
            initialY = 13.2f * T - 28f,
            width = 42f,
            height = 28f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "BACKUP TRAP!"
        ),
        // Opening surprise: launch edge before Yo-Yo Lift #1 collapses on step
        TrollTrap(
            id = 8,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 4.0f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "GONE!"
        ),
        // High Coin #2 above Yo-Yo Lift #1 releases a surprise keystone on the mid island landing
        TrollTrap(
            id = 9,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 2,
            chainDelayFrames = 8,
            initialX = 15.6f * T,
            initialY = 5.8f * T,
            width = 50f,
            height = 50f,
            telegraphDurationFrames = 0,
            targetX = 15.6f * T,
            targetY = 13f * T - 50f,
            moveSpeed = 14.0f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "LIFT TAX!"
        ),
        // Mid-island Betrayal Switch wakes a delayed floor spike ahead
        TrollTrap(
            id = 10,
            type = TrollTrapType.BETRAYAL_SWITCH,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 16.5f * T,
            initialY = 13f * T - 22f,
            width = 44f,
            height = 22f,
            telegraphDurationFrames = 0,
            linkedTrapId = 11,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "CLICK!"
        ),
        TrollTrap(
            id = 11,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 10,
            chainDelayFrames = 6,
            initialX = 17.6f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 55,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "ISLAND BITE!"
        ),
        // Coin #1 at the opening ledge triggers an immediate surprise ceiling drop
        TrollTrap(
            id = 12,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 1,
            chainDelayFrames = 2,
            initialX = 3.6f * T,
            initialY = 5.8f * T,
            width = 48f,
            height = 48f,
            targetX = 3.6f * T,
            targetY = 13f * T - 48f,
            moveSpeed = 13.8f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "EARLY DROP!"
        ),
        // Distraction Combo: Evasive Platform #1 gliding aside wakes an overhead keystone over the backup stone
        TrollTrap(
            id = 13,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 2,
            chainDelayFrames = 14,
            initialX = 12.0f * T,
            initialY = 5.8f * T,
            width = 48f,
            height = 48f,
            targetX = 12.0f * T,
            targetY = 13.2f * T - 48f,
            moveSpeed = 13.5f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "BACKUP SLAM!"
        ),
        // High Coin #3 above Yo-Yo Lift #2 arms a floor spike just before the Checkpoint Bell
        TrollTrap(
            id = 14,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 3,
            chainDelayFrames = 6,
            initialX = 24.1f * T,
            initialY = 12.5f * T - 28f,
            width = 42f,
            height = 28f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 55,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "CHECKPOINT TAX!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 8,
        title = "The Runaway Floor",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 30,
        difficulty = difficulty,
        subtitle = "Evasive Platforms & Yo-Yo Lifts",
        description = "Time vertical yo-yo stone lifts and read when evasive ledges dodge aside with backup landings below.",
        theme = LevelTheme.MONSOON_RUN,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
