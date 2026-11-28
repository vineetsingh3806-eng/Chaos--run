package com.example.data

/**
 * Phase 7 — Nightmare & Brutal Tier Challenges (ch_29 to ch_32).
 *
 * - ch_29: Nothing Is Safe (Fake safe platforms, disappearing floors, fake checkpoint & instant spike pops)
 * - ch_30: The Fake Exit Returns (Multiple fake exits, relocating gates & sudden trap activation)
 * - ch_31: Chain Reaction (Dense instant ON_TRAP_ACTIVATED & ON_COIN_COLLECTED combinations)
 * - ch_32: The Trap Corridor (Fast consecutive instant traps with tight movement timing)
 */
object NightmareBrutalChallenges29To32 {
  private const val T = World1Levels.TILE_SIZE

  private fun addRow(
      tiles: MutableList<GameTile>,
      nextId: () -> Int,
      startTileX: Int,
      endTileX: Int,
      tileY: Float,
      type: TileType
  ) {
    for (tx in startTileX..endTileX) {
      tiles.add(
          GameTile(
              id = nextId(),
              x = tx * T,
              y = tileY * T,
              width = T,
              height = T,
              type = type
          )
      )
    }
  }

  private fun addCheckpoint(tiles: MutableList<GameTile>, nextId: () -> Int, tileX: Float, floorTileY: Float = 13f) {
    tiles.add(
        GameTile(
            id = nextId(),
            x = tileX * T,
            y = (floorTileY - 1f) * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )
  }

  private fun addSunGate(tiles: MutableList<GameTile>, nextId: () -> Int, exitX: Float, exitY: Float) {
    tiles.add(
        GameTile(
            id = nextId(),
            x = exitX,
            y = exitY,
            width = T * 2f,
            height = T * 3f,
            type = TileType.SUN_GATE,
            isSolid = false
        )
    )
  }

  /**
   * ch_29: "Nothing Is Safe"
   * Instant FAKE_SAFE_PLATFORMs (collapsing & spiked), DISAPPEARING_FLOORs, FAKE_CHECKPOINT & instant spikes.
   */
  fun buildChallenge29(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Twin instant collapsing Fake Safe Platforms -> spiked middle island (13..17) -> CP1 (18..22)
    addRow(tiles, nextId, 0, 5, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 13, 13, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 16, 22, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 19.8f)

    // Act 2: Fake Checkpoint decoy on stone (23..29) -> disappearing stepping slabs -> CP2 (35..39)
    addRow(tiles, nextId, 23, 29, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 35, 39, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 37.0f)

    // Act 3: Instant spike deck (40..47) -> final collapsing platform -> exit deck (51..56)
    addRow(tiles, nextId, 40, 47, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 51, 56, 13f, TileType.CARVED_STONE)

    val exitX = 53.2f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 7.0f * T, y = 11.2f * T),
        Coin(id = 2, x = 14.8f * T, y = 10.6f * T),
        Coin(id = 3, x = 25.8f * T, y = 10.4f * T),
        Coin(id = 4, x = 43.8f * T, y = 10.8f * T),
        Coin(id = 5, x = 49.0f * T, y = 11.0f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 6.0f * T,
            initialY = 13f * T,
            width = 2.0f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 9.5f * T,
            initialY = 13f * T,
            width = 2.0f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 14.0f * T,
            initialY = 13f * T,
            width = 2.0f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.FAKE_CHECKPOINT,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 25.4f * T,
            initialY = 12f * T,
            width = T,
            height = T,
            triggerMinX = 24.7f * T,
            triggerMaxX = 27.1f * T,
            triggerMinY = 11.2f * T,
            triggerMaxY = 13.4f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 60,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 30.2f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 32.6f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 41.8f * T,
            initialY = 13f * T - 32f,
            width = 48f,
            height = 32f,
            triggerMinX = 41.0f * T,
            triggerMaxX = 43.1f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 75,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.TELEGRAPHED_RUNE_BEAM,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 7,
            chainDelayFrames = 14,
            initialX = 44.2f * T,
            initialY = 13f * T - 24f,
            width = 2.6f * T,
            height = 24f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 34,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 9,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 48.0f * T,
            initialY = 13f * T,
            width = 2.0f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 10,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 28.0f * T,
            initialY = 6.0f * T,
            width = 50f,
            height = 50f,
            triggerMinX = 27.1f * T,
            triggerMaxX = 29.1f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = 28.0f * T,
            targetY = 13f * T - 50f,
            moveSpeed = 13.5f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 11,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 1,
            chainDelayFrames = 6,
            initialX = 10.2f * T,
            initialY = 5.8f * T,
            width = 50f,
            height = 50f,
            targetX = 10.2f * T,
            targetY = 13f * T - 50f,
            moveSpeed = 13.5f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 29,
        title = "Nothing Is Safe",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 38,
        difficulty = difficulty,
        subtitle = "Rigged Ledges & False Shrines",
        description = "Solid stone betrays your footsteps, shrines bite back, and only continuous momentum keeps you alive.",
        theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_30: "The Fake Exit Returns"
   * Multiple instant FAKE_EXIT_DOORs, RELOCATING_EXIT_DOOR, FLEEING_SUN_GATE & EXIT_SLAM_CAGE.
   */
  fun buildChallenge30(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Disappearing approach (0..4 solid, 5..6 disappearing, 7..21) + First Fake Exit Door ambush + instant spike -> CP1 (0..21)
    addRow(tiles, nextId, 0, 4, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 7, 21, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 19.0f)

    // Act 2: Second Fake Exit wired to Falling Ceiling -> gap -> CP2 (22..39)
    addRow(tiles, nextId, 22, 30, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 33, 39, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 36.5f)

    // Act 3: Relocating + Fleeing Sun Gate + Instant Slam Cage finale (40..60)
    addRow(tiles, nextId, 40, 60, 13f, TileType.CARVED_STONE)

    val exitX = 44.0f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 9.0f * T, y = 10.6f * T),
        Coin(id = 2, x = 15.0f * T, y = 10.6f * T),
        Coin(id = 3, x = 26.5f * T, y = 10.4f * T),
        Coin(id = 4, x = 46.5f * T, y = 11.2f * T),
        Coin(id = 5, x = 53.5f * T, y = 8.4f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.FAKE_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 10.0f * T,
            initialY = 10f * T,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 8.8f * T,
            triggerMaxX = 12.2f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 15.0f * T,
            initialY = 13f * T - 32f,
            width = 52f,
            height = 32f,
            triggerMinX = 14.2f * T,
            triggerMaxX = 16.5f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 75,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.FAKE_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 25.5f * T,
            initialY = 10f * T,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 24.3f * T,
            triggerMaxX = 27.8f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true,
            linkedTrapId = 4
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 3,
            chainDelayFrames = 0,
            initialX = 28.4f * T,
            initialY = 6.2f * T,
            width = 52f,
            height = 52f,
            targetX = 28.4f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 11.8f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.RELOCATING_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = exitX,
            initialY = exitY,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 42.2f * T,
            triggerMaxX = 45.8f * T,
            linkedDoorTargetX = 50.8f * T,
            linkedDoorTargetY = exitY,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            quipPopup = "WRONG GATE!"
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.EXIT_SLAM_CAGE,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 50.6f * T,
            initialY = 10f * T,
            width = 2.4f * T,
            height = 3.0f * T,
            triggerMinX = 49.0f * T,
            triggerMaxX = 52.8f * T,
            telegraphDurationFrames = 0,
            effectDurationFrames = 65,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            quipPopup = "LOCKED!"
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.FLEEING_SUN_GATE,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 50.8f * T,
            initialY = exitY,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 49.4f * T,
            triggerMaxX = 53.0f * T,
            targetX = 56.8f * T,
            targetY = exitY,
            secondaryX = 57.5f * T,
            secondaryY = exitY,
            maxActivations = 1,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            quipPopup = "OVER HERE!"
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 5.2f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 30,
        title = "The Fake Exit Returns",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 40,
        difficulty = difficulty,
        subtitle = "Mirage Portals & Fleeing Sanctums",
        description = "Every finish door is a lie until you outlast the slamming cage and corner the fleeing Sun Gate.",
        theme = LevelTheme.RUINS_OF_LOST_KING,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_31: "Chain Reaction" (Brutal)
   * Dense instant ON_TRAP_ACTIVATED & ON_COIN_COLLECTED multi-stage chains (0f telegraph).
   */
  fun buildChallenge31(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Proximity Chain A (Spike -> Floor -> Ceiling -> Crush Wall) -> CP1 (17..22)
    addRow(tiles, nextId, 0, 9, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 13, 22, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 19.5f)

    // Act 2: Coin-triggered Chain B (Beam -> Ceiling -> Spike -> Evasive) + upper arch -> CP2 (35..39)
    addRow(tiles, nextId, 23, 33, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 24, 29, 10.3f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 35, 39, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 37.0f)

    // Act 3: Split Bridge Chain C -> simultaneous Pincer Walls & Ceiling Block -> Exit (45..56)
    addRow(tiles, nextId, 40, 41, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 45, 56, 13f, TileType.CARVED_STONE)

    val exitX = 53.0f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 11.2f * T, y = 11.0f * T),
        Coin(id = 2, x = 25.0f * T, y = 11.4f * T),
        Coin(id = 3, x = 26.8f * T, y = 8.9f * T),
        Coin(id = 4, x = 43.2f * T, y = 11.2f * T),
        Coin(id = 5, x = 50.8f * T, y = 11.2f * T)
    )

    val traps = listOf(
        // Chain A: 1 -> 2, 3 -> 4
        TrollTrap(
            id = 1,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 7.4f * T,
            initialY = 13f * T - 32f,
            width = 48f,
            height = 32f,
            triggerMinX = 6.5f * T,
            triggerMaxX = 8.8f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 75,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true,
            linkedTrapId = 2
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 1,
            chainDelayFrames = 0,
            initialX = 10.0f * T,
            initialY = 13f * T,
            width = 2.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            linkedTrapId = 3
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 2,
            chainDelayFrames = 0,
            initialX = 14.0f * T,
            initialY = 6.2f * T,
            width = 52f,
            height = 52f,
            targetX = 14.0f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 12.0f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true,
            linkedTrapId = 4
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.MOVING_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 3,
            chainDelayFrames = 0,
            initialX = 16.5f * T,
            initialY = 13f * T - 86f,
            width = T,
            height = 86f,
            targetX = 14.2f * T,
            targetY = 13f * T - 86f,
            moveSpeed = 6.5f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        // Chain B: Coin 2 -> 5 -> 6 -> 7
        TrollTrap(
            id = 5,
            type = TrollTrapType.TELEGRAPHED_RUNE_BEAM,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 2,
            initialX = 25.5f * T,
            initialY = 13f * T - 26f,
            width = 4.2f * T,
            height = 24f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 48,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true,
            linkedTrapId = 6
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 5,
            chainDelayFrames = 0,
            initialX = 30.2f * T,
            initialY = 6.2f * T,
            width = 52f,
            height = 52f,
            targetX = 30.2f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 12.0f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true,
            linkedTrapId = 7
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 6,
            chainDelayFrames = 0,
            initialX = 31.8f * T,
            initialY = 13f * T - 32f,
            width = 48f,
            height = 32f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 75,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        // Chain C: Split Bridge 8 -> simultaneous Pincer (9) & Ceiling (10)
        TrollTrap(
            id = 8,
            type = TrollTrapType.SPLIT_BRIDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 41.6f * T,
            initialY = 13f * T,
            width = 3.2f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 9,
            type = TrollTrapType.PINCER_WALLS,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 8,
            chainDelayFrames = 0,
            initialX = 46.2f * T,
            initialY = 13f * T - 86f,
            width = 3.4f * T,
            height = 86f,
            minSafeGapWidth = 52f,
            moveSpeed = 5.6f,
            telegraphDurationFrames = 0,
            cycleFrames = 105,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 10,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 8,
            chainDelayFrames = 0,
            initialX = 50.2f * T,
            initialY = 6.2f * T,
            width = 52f,
            height = 52f,
            targetX = 50.2f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 12.2f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 31,
        title = "Chain Reaction",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 40,
        difficulty = difficulty,
        subtitle = "Instant Cascading Detonations",
        description = "One footstep or coin triggers simultaneous spikes, collapsing slabs, pincers, and ceiling blocks.",
        theme = LevelTheme.HOLLOW_OF_ECHOES,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_32: "The Trap Corridor" (Brutal)
   * Fast consecutive instant traps demanding crisp rhythm across tight corridors.
   */
  fun buildChallenge32(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Rapid corridor (0..22) -> CP1
    addRow(tiles, nextId, 0, 22, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 19.8f)

    // Act 2: Conveyor -> Beam + upper bypass -> Crush Wall -> CP2 (23..40)
    addRow(tiles, nextId, 23, 40, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 28, 33, 10.3f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 37.5f)

    // Act 3: Stalker chase across instant spikes & collapsing bridge to exit (41..57)
    addRow(tiles, nextId, 41, 47, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 50, 57, 13f, TileType.CARVED_STONE)

    val exitX = 54.0f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 6.2f * T, y = 11.0f * T),
        Coin(id = 2, x = 13.0f * T, y = 11.4f * T),
        Coin(id = 3, x = 30.5f * T, y = 8.9f * T),
        Coin(id = 4, x = 44.8f * T, y = 10.8f * T),
        Coin(id = 5, x = 51.8f * T, y = 11.2f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 5.8f * T,
            initialY = 13f * T - 32f,
            width = 48f,
            height = 32f,
            triggerMinX = 5.0f * T,
            triggerMaxX = 7.1f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 75,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 8.6f * T,
            initialY = 6.0f * T,
            width = 52f,
            height = 52f,
            triggerMinX = 7.7f * T,
            triggerMaxX = 10.0f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = 8.6f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 12.2f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.PINCER_WALLS,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 11.4f * T,
            initialY = 13f * T - 86f,
            width = 3.4f * T,
            height = 86f,
            triggerMinX = 10.6f * T,
            triggerMaxX = 14.8f * T,
            minSafeGapWidth = 52f,
            moveSpeed = 5.6f,
            telegraphDurationFrames = 0,
            cycleFrames = 105,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 16.0f * T,
            initialY = 13f * T - 32f,
            width = 48f,
            height = 32f,
            triggerMinX = 15.2f * T,
            triggerMaxX = 17.5f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 75,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 23.5f * T,
            initialY = 13f * T,
            width = 4.5f * T,
            height = T,
            cycleFrames = 96,
            gustFlipIntervalFrames = 96,
            gustForceX = -2.6f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.TELEGRAPHED_RUNE_BEAM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 29.0f * T,
            initialY = 13f * T - 26f,
            width = 4.2f * T,
            height = 24f,
            triggerMinX = 28.0f * T,
            triggerMaxX = 32.5f * T,
            triggerMinY = 10.8f * T,
            triggerMaxY = 13.6f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 36,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.MOVING_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 35.0f * T,
            initialY = 13f * T - 86f,
            width = T,
            height = 86f,
            triggerMinX = 32.8f * T,
            triggerMaxX = 34.8f * T,
            targetX = 31.6f * T,
            targetY = 13f * T - 86f,
            moveSpeed = 7.0f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 39.2f * T,
            initialY = 13f * T - 86f,
            width = T,
            height = 86f,
            triggerMinX = 41.2f * T,
            triggerMaxX = 43.2f * T,
            targetX = 50.5f * T,
            targetY = 13f * T - 86f,
            moveSpeed = 5.4f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 220,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 9,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 44.4f * T,
            initialY = 13f * T - 32f,
            width = 48f,
            height = 32f,
            triggerMinX = 43.6f * T,
            triggerMaxX = 45.8f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 75,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 10,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 47.6f * T,
            initialY = 13f * T,
            width = 2.0f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 32,
        title = "The Trap Corridor",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 38,
        difficulty = difficulty,
        subtitle = "Relentless Back-to-Back Gauntlet",
        description = "Tight corridors packed with back-to-back instant spikes, jaws, beams, and a final stalker wall sprint.",
        theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
        trollTraps = traps,
        isTrollLevel = true
    )
  }
}
