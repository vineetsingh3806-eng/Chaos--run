package com.example.data

/**
 * Phase 7 — Nightmare Tier Challenges (ch_25 to ch_28).
 *
 * Uses zero-telegraph instant SURPRISE traps alongside dense trap interactions:
 * - ch_25: No Time to React (Instant floor collapses & instant spike pops)
 * - ch_26: The Ceiling Wants You (Hidden instant ceiling drops, moving platforms & narrow escapes)
 * - ch_27: Coin of Betrayal (Coin-triggered instant traps, fake rewards & chained hazards)
 * - ch_28: The Moving Nightmare (Instant moving crush/stalker walls, reverse conveyors & collapsing platforms)
 */
object NightmareChallenges25To28 {
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
   * ch_25: "No Time to React"
   * Instant DISAPPEARING_FLOOR collapses & instant SURPRISE_SPIKE_POP ambushes (0f telegraph).
   */
  fun buildChallenge25(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Solid start (0..4) -> instant floor bridge -> spike deck (9..12) -> double collapsing gap -> CP1 (18..22)
    addRow(tiles, nextId, 0, 4, 13f, TileType.MOSSY_TIMBER)
    addRow(tiles, nextId, 9, 12, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 18, 22, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 20.0f)

    // Act 2: Airborne spike + overhead instant ceiling combo (23..29) -> instant spike ambush (31..39) -> CP2
    addRow(tiles, nextId, 23, 29, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 31, 39, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 37.0f)

    // Act 3: Triple staggered instant disappearing pads -> split finale deck (49..53 + 56..59)
    addRow(tiles, nextId, 49, 53, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 56, 59, 13f, TileType.CARVED_STONE)

    val exitX = 52.5f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 6.8f * T, y = 11.4f * T),
        Coin(id = 2, x = 15.2f * T, y = 11.0f * T),
        Coin(id = 3, x = 26.0f * T, y = 10.6f * T),
        Coin(id = 4, x = 44.5f * T, y = 11.0f * T),
        Coin(id = 5, x = 50.6f * T, y = 10.4f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 5.2f * T,
            initialY = 13f * T,
            width = 3.2f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 10.4f * T,
            initialY = 13f * T - 32f,
            width = 48f,
            height = 32f,
            triggerMinX = 9.6f * T,
            triggerMaxX = 11.8f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 80,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 13.5f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 15.8f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 25.0f * T,
            initialY = 13f * T - 32f,
            width = 48f,
            height = 32f,
            triggerMinX = 24.1f * T,
            triggerMaxX = 26.3f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 85,
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
            initialX = 27.2f * T,
            initialY = 6.2f * T,
            width = 50f,
            height = 50f,
            targetX = 27.2f * T,
            targetY = 13f * T - 50f,
            moveSpeed = 11.5f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 33.2f * T,
            initialY = 13f * T - 32f,
            width = 52f,
            height = 32f,
            triggerMinX = 32.3f * T,
            triggerMaxX = 34.8f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 80,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 40.5f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 9,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 43.3f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 10,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 46.1f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 11,
            type = TrollTrapType.RELOCATING_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = exitX,
            initialY = exitY,
            width = 2f * T,
            height = 3f * T,
            triggerMinX = 51.5f * T,
            triggerMaxX = 54.0f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            linkedDoorTargetX = 56.8f * T,
            linkedDoorTargetY = exitY,
            linkedTrapId = 12,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            quipPopup = null
        ),
        TrollTrap(
            id = 12,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 11,
            chainDelayFrames = 0,
            initialX = 54.0f * T,
            initialY = 13f * T,
            width = 2f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            quipPopup = null
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 25,
        title = "No Time to React",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 36,
        difficulty = difficulty,
        subtitle = "Instant Collapses & Ambush Spikes",
        description = "Floors vanish beneath your boots and floor spikes snap skyward on the exact frame you enter.",
        theme = LevelTheme.WHISPERING_WOODS,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_26: "The Ceiling Wants You"
   * Hidden instant FALLING_CEILING_BLOCK drops, YO_YO_PLATFORM climbs & EVASIVE_PLATFORM escapes.
   */
  fun buildChallenge26(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Twin instant ceiling corridor (0..10) -> Yo-Yo crossing -> landing drop -> CP1 (15..21)
    addRow(tiles, nextId, 0, 10, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 15, 21, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 19.5f)

    // Act 2: Yo-Yo lift (22.5T) to upper bridge (25..31, y=10.2T) + catch floor below -> CP2 (33..38)
    addRow(tiles, nextId, 22, 31, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 25, 31, 10.2f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 33, 38, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 36.5f)

    // Act 3: Double ceiling ambush (39..45) -> Evasive bridge -> final slam before exit (49..56)
    addRow(tiles, nextId, 39, 45, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 49, 56, 13f, TileType.CARVED_STONE)

    val exitX = 53.0f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 6.5f * T, y = 11.4f * T),
        Coin(id = 2, x = 12.6f * T, y = 10.4f * T),
        Coin(id = 3, x = 28.0f * T, y = 8.8f * T),
        Coin(id = 4, x = 42.5f * T, y = 11.4f * T),
        Coin(id = 5, x = 50.8f * T, y = 11.2f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 5.8f * T,
            initialY = 6.0f * T,
            width = 52f,
            height = 52f,
            triggerMinX = 5.0f * T,
            triggerMaxX = 7.4f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = 5.8f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 11.5f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 8.4f * T,
            initialY = 6.0f * T,
            width = 52f,
            height = 52f,
            triggerMinX = 7.7f * T,
            triggerMaxX = 9.9f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = 8.4f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 12.0f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.YO_YO_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 11.8f * T,
            initialY = 12.6f * T,
            width = 2.0f * T,
            height = T,
            targetX = 12.6f * T,
            targetY = 10.6f * T,
            cycleFrames = 115,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 16.5f * T,
            initialY = 6.2f * T,
            width = 52f,
            height = 52f,
            triggerMinX = 15.6f * T,
            triggerMaxX = 17.8f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = 16.5f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 11.5f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.YO_YO_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 22.5f * T,
            initialY = 12.6f * T,
            width = 1.8f * T,
            height = T,
            targetX = 22.5f * T,
            targetY = 9.8f * T,
            cycleFrames = 110,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 27.4f * T,
            initialY = 4.6f * T,
            width = 52f,
            height = 52f,
            triggerMinX = 26.5f * T,
            triggerMaxX = 29.0f * T,
            triggerMinY = 5.0f * T,
            triggerMaxY = 10.2f * T,
            targetX = 27.4f * T,
            targetY = 10.2f * T - 52f,
            moveSpeed = 11.5f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 40.5f * T,
            initialY = 6.0f * T,
            width = 52f,
            height = 52f,
            triggerMinX = 39.6f * T,
            triggerMaxX = 41.8f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = 40.5f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 12.0f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 43.2f * T,
            initialY = 6.0f * T,
            width = 52f,
            height = 52f,
            triggerMinX = 42.3f * T,
            triggerMaxX = 44.6f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = 43.2f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 12.0f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 9,
            type = TrollTrapType.EVASIVE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 46.0f * T,
            initialY = 12.8f * T,
            width = 2.2f * T,
            height = T,
            targetX = 47.4f * T,
            targetY = 11.6f * T,
            moveSpeed = 5.0f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 10,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 50.5f * T,
            initialY = 6.2f * T,
            width = 52f,
            height = 52f,
            triggerMinX = 49.8f * T,
            triggerMaxX = 51.8f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = 50.5f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 12.2f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 26,
        title = "The Ceiling Wants You",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 38,
        difficulty = difficulty,
        subtitle = "Silent Canopy Slams & Moving Lifts",
        description = "Every overhead keystone drops without a shadow or tremble — keep moving between narrow lifts.",
        theme = LevelTheme.HOLLOW_OF_ECHOES,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_27: "Coin of Betrayal"
   * Coin-triggered instant traps, fake rewards & chained hazards with safe high bypass routes.
   */
  fun buildChallenge27(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Bait Coin 1 + High Arch (5..9, y=10.3T) -> bridge 12..16 -> CP1 (17..21)
    addRow(tiles, nextId, 0, 11, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 5, 9, 10.3f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 17, 21, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 19.0f)

    // Act 2: Bait Coin 3 crush corridor (22..33) -> CP2 (34..38)
    addRow(tiles, nextId, 22, 38, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 24, 29, 10.3f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 36.0f)

    // Act 3: Bait Coins 4 & 5 + Fake Exit Door + Silent Floor Trap before Exit Deck (39..57)
    addRow(tiles, nextId, 39, 51, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 54, 57, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 41, 45, 10.3f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 46, 47, 11.4f, TileType.CARVED_STONE)

    val exitX = 54.6f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 6.5f * T, y = 11.4f * T),
        Coin(id = 2, x = 13.8f * T, y = 11.4f * T),
        Coin(id = 3, x = 25.5f * T, y = 11.4f * T),
        Coin(id = 4, x = 42.5f * T, y = 11.4f * T),
        Coin(id = 5, x = 49.4f * T, y = 8.8f * T)
    )

    val traps = listOf(
        // Coin 1 -> instant Ceiling Block (1) + instant Spike Pop (2)
        TrollTrap(
            id = 1,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 1,
            initialX = 6.2f * T,
            initialY = 6.0f * T,
            width = 52f,
            height = 52f,
            targetX = 6.2f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 12.0f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true,
            linkedTrapId = 2
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 1,
            chainDelayFrames = 0,
            initialX = 8.4f * T,
            initialY = 13f * T - 32f,
            width = 52f,
            height = 32f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 80,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        // Coin 2 -> instantly vanishes bridge 3 beneath you
        TrollTrap(
            id = 3,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 2,
            initialX = 12.2f * T,
            initialY = 13f * T,
            width = 3.8f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        // Coin 3 -> instant Moving Crush Wall (4) + chained Ceiling Drop (5)
        TrollTrap(
            id = 4,
            type = TrollTrapType.MOVING_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 3,
            initialX = 28.5f * T,
            initialY = 13f * T - 86f,
            width = T,
            height = 86f,
            targetX = 23.2f * T,
            targetY = 13f * T - 86f,
            moveSpeed = 7.2f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true,
            linkedTrapId = 5
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 4,
            chainDelayFrames = 0,
            initialX = 31.0f * T,
            initialY = 6.2f * T,
            width = 52f,
            height = 52f,
            targetX = 31.0f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 11.8f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        // Coin 4 -> instant Rune Beam across lower lane
        TrollTrap(
            id = 6,
            type = TrollTrapType.TELEGRAPHED_RUNE_BEAM,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 4,
            initialX = 41.2f * T,
            initialY = 13f * T - 26f,
            width = 4.5f * T,
            height = 24f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 50,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        // Fake Exit Door before real Sun Gate
        TrollTrap(
            id = 7,
            type = TrollTrapType.FAKE_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 48.4f * T,
            initialY = 10f * T,
            width = 2f * T,
            height = 3f * T,
            triggerMinX = 47.5f * T,
            triggerMaxX = 50.8f * T,
            triggerMinY = 10.2f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true,
            quipPopup = "GREEDY EXIT!"
        ),
        // Coin 5 silently drops exit bridge ahead beneath real gate
        TrollTrap(
            id = 8,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            initialX = 52.0f * T,
            initialY = 13f * T,
            width = 2f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            quipPopup = null
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 27,
        title = "Coin of Betrayal",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 36,
        difficulty = difficulty,
        subtitle = "Cursed Gold & Instant Chains",
        description = "Every gleaming coin pulls an unseen tripwire — skip the greed on the upper arches or outrun the chain.",
        theme = LevelTheme.RUINS_OF_LOST_KING,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_28: "The Moving Nightmare"
   * Instant STALKER_CRUSH_WALL / MOVING_CRUSH_WALL pursuits, REVERSE_CONVEYOR_BELT & collapsing platforms.
   */
  fun buildChallenge28(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Conveyor + instant Stalker chasing over instant falling slabs -> CP1 (17..22)
    addRow(tiles, nextId, 0, 5, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 17, 22, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 19.5f)

    // Act 2: Instant crush wall + reversing belt beneath collapsing upper Fake Safe Platform -> CP2 (34..39)
    addRow(tiles, nextId, 23, 27, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 33, 39, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 36.5f)

    // Act 3: Stalker #2 across twin instant collapsing Fake Safe Platforms to exit (50..56)
    addRow(tiles, nextId, 40, 42, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 50, 56, 13f, TileType.CARVED_STONE)

    val exitX = 53.2f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 8.0f * T, y = 11.4f * T),
        Coin(id = 2, x = 14.5f * T, y = 11.0f * T),
        Coin(id = 3, x = 29.8f * T, y = 9.0f * T),
        Coin(id = 4, x = 44.0f * T, y = 11.0f * T),
        Coin(id = 5, x = 51.2f * T, y = 11.2f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 6.0f * T,
            initialY = 13f * T,
            width = 4.6f * T,
            height = T,
            cycleFrames = 100,
            gustFlipIntervalFrames = 100,
            gustForceX = -2.4f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 3.2f * T,
            initialY = 13f * T - 86f,
            width = T,
            height = 86f,
            triggerMinX = 7.0f * T,
            triggerMaxX = 9.5f * T,
            targetX = 16.5f * T,
            targetY = 13f * T - 86f,
            moveSpeed = 5.0f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 220,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 11.4f * T,
            initialY = 13f * T,
            width = 1.8f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 14.1f * T,
            initialY = 13f * T,
            width = 1.8f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.MOVING_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 26.8f * T,
            initialY = 13f * T - 86f,
            width = T,
            height = 86f,
            triggerMinX = 23.6f * T,
            triggerMaxX = 26.2f * T,
            targetX = 22.8f * T,
            targetY = 13f * T - 86f,
            moveSpeed = 6.8f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 28.0f * T,
            initialY = 13f * T,
            width = 5.0f * T,
            height = T,
            cycleFrames = 100,
            gustFlipIntervalFrames = 100,
            gustForceX = -2.5f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 28.6f * T,
            initialY = 10.4f * T,
            width = 2.4f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 38.8f * T,
            initialY = 13f * T - 86f,
            width = T,
            height = 86f,
            triggerMinX = 40.4f * T,
            triggerMaxX = 42.5f * T,
            targetX = 49.5f * T,
            targetY = 13f * T - 86f,
            moveSpeed = 5.2f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 220,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 9,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 43.0f * T,
            initialY = 13f * T,
            width = 2.2f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 10,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 46.5f * T,
            initialY = 13f * T,
            width = 2.2f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 11,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            chainDelayFrames = 0,
            initialX = 52.3f * T,
            initialY = 13f * T - 32f,
            width = 46f,
            height = 32f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 70,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 12,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 3,
            chainDelayFrames = 4,
            initialX = 31.6f * T,
            initialY = 5.6f * T,
            width = 50f,
            height = 50f,
            targetX = 31.6f * T,
            targetY = 13f * T - 50f,
            moveSpeed = 13.5f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 28,
        title = "The Moving Nightmare",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 40,
        difficulty = difficulty,
        subtitle = "Instant Stalkers & Collapsing Belts",
        description = "Relentless stone walls surge the instant you step forward while belts flip and safe ledges drop out.",
        theme = LevelTheme.MONSOON_RUN,
        trollTraps = traps,
        isTrollLevel = true
    )
  }
}
