package com.example.data

/**
 * Challenge 10: "Walls Are Watching"
 *
 * Central Mechanics: PINCER_WALLS (synchronized left/right crushing jaws preserving a
 * readable central safe pocket) & STALKER_CRUSH_WALL (telegraphed horizontal pursuit monolith).
 */
object Challenge10WallsAreWatching {
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

    // 1. Launchpad (0..2) -> Reverse Conveyor Belt (3..5) -> Pincer Chamber #1 (6..14, Y=13) + Disappearing Exit (15..16)
    addRow(0, 2, 13f, TileType.CARVED_STONE)
    addRow(6, 14, 13f, TileType.CARVED_STONE)

    // 2. Stalker Hallway #1 (17..26, Y=13) with raised alcove (20.5..22.5, Y=10.6)
    addRow(17, 26, 13f, TileType.CARVED_STONE)
    tiles.add(GameTile(id = nextId++, x = 20.5f * T, y = 10.6f * T, width = 2.0f * T, height = 0.6f * T, type = TileType.CARVED_STONE))

    // 3. Checkpoint Sanctuary (27..31, Y=13)
    addRow(27, 31, 13f, TileType.CARVED_STONE)
    tiles.add(
        GameTile(
            id = nextId++,
            x = 28.5f * T,
            y = 12f * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )

    // 4. Pincer Chamber #2 + Stalker Dash Exit (32..50, Y=13)
    addRow(32, 50, 13f, TileType.CARVED_STONE)
    val exitX = 48.0f * T
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
        Coin(id = 1, x = 4.5f * T, y = 11.5f * T),
        Coin(id = 2, x = 8.5f * T, y = 11.4f * T),
        Coin(id = 3, x = 21.5f * T, y = 9.2f * T),
        Coin(id = 4, x = 35.1f * T, y = 11.4f * T),
        Coin(id = 5, x = 44.6f * T, y = 11.5f * T)
    )

    val trollTraps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.PINCER_WALLS,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 6.8f * T,
            initialY = 13f * T - 84f,
            width = 3.4f * T,
            height = 84f,
            triggerMinX = 6.0f * T,
            triggerMaxX = 10.2f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            minSafeGapWidth = 60f,
            telegraphDurationFrames = 7,
            moveSpeed = 5.6f,
            cycleFrames = 96,
            killsPlayer = true,
            quipPopup = "HOLD CENTER!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 25.4f * T,
            initialY = 13f * T - 84f,
            width = T,
            height = 84f,
            triggerMinX = 19.6f * T,
            triggerMaxX = 22.4f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 7,
            targetX = 18.2f * T,
            targetY = 13f * T - 84f,
            moveSpeed = 5.3f,
            activeDurationFrames = 220,
            killsPlayer = true,
            quipPopup = "WALL ON THE MOVE!"
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.PINCER_WALLS,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 33.5f * T,
            initialY = 13f * T - 84f,
            width = 3.2f * T,
            height = 84f,
            triggerMinX = 32.8f * T,
            triggerMaxX = 37.0f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            minSafeGapWidth = 56f,
            telegraphDurationFrames = 7,
            moveSpeed = 5.8f,
            cycleFrames = 92,
            killsPlayer = true,
            quipPopup = "THREAD THE GAP!"
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 38.0f * T,
            initialY = 13f * T - 84f,
            width = T,
            height = 84f,
            triggerMinX = 40.4f * T,
            triggerMaxX = 42.6f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 7,
            targetX = 46.6f * T,
            targetY = 13f * T - 84f,
            moveSpeed = 5.5f,
            activeDurationFrames = 240,
            killsPlayer = true,
            quipPopup = "DASH!"
        ),
        // Combo: Pincer Chamber #1 triggers an instant spike ambush on the exit corridor
        TrollTrap(
            id = 5,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 1,
            chainDelayFrames = 36,
            initialX = 11.8f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "NO REST!"
        ),
        // Corridor betrayal: floor between Pincer #1 and Stalker Hallway #1 vanishes on step
        TrollTrap(
            id = 6,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 15.0f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "MIND THE SEAM!"
        ),
        // Coin #5 greed drop near Stalker Dash finale
        TrollTrap(
            id = 7,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            chainDelayFrames = 0,
            initialX = 44.6f * T,
            initialY = 6.0f * T,
            width = 50f,
            height = 52f,
            telegraphDurationFrames = 0,
            targetX = 44.6f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 14.0f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "STONE SENTINEL!"
        ),
        // Intro Reverse Conveyor Belt pulls players unexpectedly toward Pincer Chamber #1
        TrollTrap(
            id = 8,
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 3.0f * T,
            initialY = 13f * T,
            width = 3.0f * T,
            height = T,
            cycleFrames = 110,
            gustFlipIntervalFrames = 110,
            gustForceX = -2.0f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        // Act 2 surprise Launch Pad springs players unexpectedly during the Stalker Dash finale!
        TrollTrap(
            id = 9,
            type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 39.6f * T,
            initialY = 13f * T - 18f,
            width = 48f,
            height = 18f,
            telegraphDurationFrames = 14,
            launchImpulseVy = -14.8f,
            cycleFrames = 120,
            killsPlayer = false,
            quipPopup = "SPRING DASH!"
        ),
        // Coin #1 on the intro belt silently arms a ceiling slam inside Pincer Chamber #1
        TrollTrap(
            id = 10,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 1,
            chainDelayFrames = 28,
            initialX = 7.4f * T,
            initialY = 5.8f * T,
            width = 48f,
            height = 48f,
            targetX = 7.4f * T,
            targetY = 13f * T - 48f,
            moveSpeed = 13.8f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "BELT PAYBACK!"
        ),
        // Jumping onto the Stalker #1 safe alcove wakes an overhead ceiling ambush
        TrollTrap(
            id = 11,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_JUMP_ABOVE,
            initialX = 21.8f * T,
            initialY = 4.6f * T,
            width = 48f,
            height = 48f,
            triggerMinX = 20.4f * T,
            triggerMaxX = 22.8f * T,
            triggerMinY = 5.5f * T,
            triggerMaxY = 10.6f * T,
            targetX = 21.8f * T,
            targetY = 10.6f * T - 48f,
            moveSpeed = 13.5f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "ALCOVE CRUSH!"
        ),
        // Launch Pad #9 unexpectedly chains into a mid-air falling keystone down the dash corridor
        TrollTrap(
            id = 12,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 9,
            chainDelayFrames = 14,
            initialX = 41.5f * T,
            initialY = 5.6f * T,
            width = 50f,
            height = 50f,
            targetX = 41.5f * T,
            targetY = 13f * T - 50f,
            moveSpeed = 14.0f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "MID-AIR SLAM!"
        ),
        // Sudden floor spike pops directly in front of the Sun Gate threshold
        TrollTrap(
            id = 13,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 47.4f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            triggerMinX = 46.2f * T,
            triggerMaxX = 48.2f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "DOOR GUARD!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 10,
        title = "Walls Are Watching",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 30,
        difficulty = difficulty,
        subtitle = "Pincer Jaws & Stalker Walls",
        description = "Stand center-pocket inside closing pincer jaws and dash or hop above encroaching stalker monoliths.",
        theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
