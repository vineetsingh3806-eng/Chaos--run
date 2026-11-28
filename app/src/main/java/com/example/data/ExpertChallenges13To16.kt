package com.example.data

/**
 * Phase 6 — Expert Tier Challenges (ch_13 to ch_16).
 *
 * - ch_13: Belt & Betrayal (REVERSE_CONVEYOR_BELT + Coin Bait Ceiling + Split Bridge)
 * - ch_14: Ember Basin Ascent (LAUNCH_BETRAYAL_PAD + RISING_EMBER_SURGE + Yo-Yo Lift)
 * - ch_15: The Locked Sanctum (LOCKED_RUNE_GATE + Betrayal Switch Decoys + Trailing Spike)
 * - ch_16: Beam & Gale (TELEGRAPHED_RUNE_BEAM + GUST_REVERSAL_ZONE + Tilting Plank)
 */
object ExpertChallenges13To16 {
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
   * ch_13: "Belt & Betrayal"
   * Mechanics: REVERSE_CONVEYOR_BELT + Coin Bait Ceiling + Split Bridge + Surprise Spike Pocket.
   */
  fun buildChallenge13(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    addRow(tiles, nextId, 0, 5, 13f, TileType.MOSSY_TIMBER)
    // Conveyor #1 spans tiles 6..11 (Y=13) with safe stone alcove above for bait coin
    tiles.add(GameTile(id = nextId(), x = 8.0f * T, y = 10.6f * T, width = 2.0f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    addRow(tiles, nextId, 12, 14, 13f, TileType.CARVED_STONE)
    // Gap 15..18 bridged by Split Bridge (tiles 15.5..18.5)
    addRow(tiles, nextId, 19, 21, 13f, TileType.CARVED_STONE)
    // Checkpoint Sanctuary 23..26 (small 1-tile hop 21->23)
    addRow(tiles, nextId, 23, 27, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 24.5f)
    // Dual opposing conveyors 28..33 & 35..40 framing a spike trap clearing 41..48
    addRow(tiles, nextId, 34, 34, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 41, 52, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 41, 42, 11.4f, TileType.CARVED_STONE)

    val exitX = 49.0f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 9.0f * T, y = 9.3f * T),   // Coin bait overhead triggers falling block
        Coin(id = 2, x = 17.0f * T, y = 11.2f * T),
        Coin(id = 3, x = 31.0f * T, y = 11.2f * T),
        Coin(id = 4, x = 38.0f * T, y = 11.2f * T),
        Coin(id = 5, x = 45.0f * T, y = 8.8f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 6f * T,
            initialY = 13f * T,
            width = 6f * T,
            height = T,
            cycleFrames = 100,
            gustFlipIntervalFrames = 100,
            gustForceX = -2.4f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 1,
            chainDelayFrames = 4,
            initialX = 8.4f * T,
            initialY = 6.0f * T,
            width = 52f,
            height = 50f,
            telegraphDurationFrames = 8,
            targetX = 8.4f * T,
            targetY = 10.6f * T - 50f,
            moveSpeed = 13.0f,
            killsPlayer = true,
            quipPopup = "BAIT CRUSH!"
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.SPLIT_BRIDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 15.2f * T,
            initialY = 13f * T,
            width = 3.6f * T,
            height = T,
            telegraphDurationFrames = 12,
            killsPlayer = false,
            quipPopup = "CENTER DROPS!"
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 28f * T,
            initialY = 13f * T,
            width = 6f * T,
            height = T,
            cycleFrames = 96,
            gustFlipIntervalFrames = 96,
            gustForceX = 2.5f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 35f * T,
            initialY = 13f * T,
            width = 6f * T,
            height = T,
            cycleFrames = 96,
            phaseOffsetFrames = 48,
            gustFlipIntervalFrames = 96,
            gustForceX = -2.6f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.FAKE_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 44.0f * T,
            initialY = 10f * T,
            width = 2f * T,
            height = 3f * T,
            triggerMinX = 43.0f * T,
            triggerMaxX = 46.2f * T,
            triggerMinY = 10.2f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "WRONG BELT STOP!"
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 34.0f * T,
            initialY = 6.2f * T,
            width = 48f,
            height = 50f,
            triggerMinX = 33.1f * T,
            triggerMaxX = 35.1f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = 34.0f * T,
            targetY = 13f * T - 50f,
            moveSpeed = 14.0f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "REST IS A TRAP!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 13,
        title = "Belt & Betrayal",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 30,
        difficulty = difficulty,
        subtitle = "Reversing Conveyors & Coin Baits",
        description = "Read flashing conveyor chevrons before direction reversals and watch overhead when snagging tempting alcove coins.",
        theme = LevelTheme.WHISPERING_WOODS,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_14: "Ember Basin Ascent"
   * Mechanics: LAUNCH_BETRAYAL_PAD + RISING_EMBER_SURGE + YO_YO_PLATFORM.
   */
  fun buildChallenge14(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // 1. Start deck + Spring Pad launch up to elevated stone balcony (tiles 8..12, Y=9.5)
    addRow(tiles, nextId, 0, 7, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 8, 12, 9.6f, TileType.CARVED_STONE)
    // 2. Ember Basin #1 (tiles 13..22) with sunken floor at Y=14.2 and two stepping islands at Y=10.5
    addRow(tiles, nextId, 13, 22, 14.4f, TileType.SOLID_EARTH)
    tiles.add(GameTile(id = nextId(), x = 15.0f * T, y = 10.6f * T, width = 2.0f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    tiles.add(GameTile(id = nextId(), x = 19.0f * T, y = 10.2f * T, width = 2.0f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    // 3. Checkpoint Sanctuary (tiles 23..27, Y=13)
    addRow(tiles, nextId, 23, 27, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 25.0f)
    // 4. Lower spring approach + Yo-Yo Lift + Ember Basin #2 -> Exit Deck (tiles 28..49)
    addRow(tiles, nextId, 28, 32, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 36, 42, 14.4f, TileType.SOLID_EARTH)
    tiles.add(GameTile(id = nextId(), x = 38.0f * T, y = 10.0f * T, width = 2.2f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    addRow(tiles, nextId, 43, 44, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 47, 51, 13f, TileType.CARVED_STONE)

    val exitX = 48.2f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 6.8f * T, y = 9.0f * T),
        Coin(id = 2, x = 16.0f * T, y = 9.2f * T),
        Coin(id = 3, x = 20.0f * T, y = 8.8f * T),
        Coin(id = 4, x = 34.0f * T, y = 9.0f * T),
        Coin(id = 5, x = 39.1f * T, y = 8.6f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 5.6f * T,
            initialY = 13f * T - 22f,
            width = 48f,
            height = 22f,
            launchImpulseVy = -16.4f,
            telegraphDurationFrames = 12,
            activeDurationFrames = 75,
            cycleFrames = 100,
            maxActivations = 3,
            killsPlayer = true,
            quipPopup = "SPRING SPIKED!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.RISING_EMBER_SURGE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 13.2f * T,
            initialY = 13.6f * T,
            width = 9.4f * T,
            height = 0.8f * T,
            triggerMinX = 12.2f * T,
            triggerMaxX = 21.5f * T,
            triggerMinY = 7.0f * T,
            triggerMaxY = 14.8f * T,
            targetY = 11.4f * T,
            telegraphDurationFrames = 12,
            moveSpeed = 4.5f,
            effectDurationFrames = 48,
            cycleFrames = 116,
            killsPlayer = true,
            quipPopup = "EMBER SURGE!"
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 30.8f * T,
            initialY = 13f * T - 22f,
            width = 48f,
            height = 22f,
            launchImpulseVy = -16.0f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 70,
            cycleFrames = 100,
            maxActivations = 3,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "UP AND AWAY!"
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.YO_YO_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 33.0f * T,
            initialY = 12.0f * T,
            width = 2.4f * T,
            height = 24f,
            targetX = 33.0f * T,
            targetY = 9.2f * T,
            cycleFrames = 108,
            killsPlayer = false
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.RISING_EMBER_SURGE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 36.2f * T,
            initialY = 13.6f * T,
            width = 6.4f * T,
            height = 0.8f * T,
            triggerMinX = 35.0f * T,
            triggerMaxX = 42.5f * T,
            triggerMinY = 7.0f * T,
            triggerMaxY = 14.8f * T,
            targetY = 11.0f * T,
            telegraphDurationFrames = 0,
            moveSpeed = 4.8f,
            effectDurationFrames = 44,
            cycleFrames = 108,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "HOT TIDE!"
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 10.4f * T,
            initialY = 9.6f * T - 30f,
            width = 44f,
            height = 30f,
            triggerMinX = 9.7f * T,
            triggerMaxX = 11.4f * T,
            triggerMinY = 6.5f * T,
            triggerMaxY = 9.8f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "BALCONY BITE!"
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 45.2f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "FALSE DECK!"
        ),
        // Coin #4 above Yo-Yo Lift triggers a surprise ceiling drop onto the high basin perch
        TrollTrap(
            id = 8,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 4,
            chainDelayFrames = 4,
            initialX = 38.6f * T,
            initialY = 4.6f * T,
            width = 48f,
            height = 48f,
            targetX = 38.6f * T,
            targetY = 10.0f * T - 48f,
            moveSpeed = 13.5f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "LIFT DROP!"
        ),
        // Betrayal Switch past Ember Basin #2 silently arms a delayed Sun Gate spike
        TrollTrap(
            id = 9,
            type = TrollTrapType.BETRAYAL_SWITCH,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 43.5f * T,
            initialY = 13f * T - 22f,
            width = 44f,
            height = 22f,
            telegraphDurationFrames = 0,
            linkedTrapId = 10,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SEAL SWITCH?"
        ),
        TrollTrap(
            id = 10,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 9,
            chainDelayFrames = 26,
            initialX = 47.4f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "ASH BITE!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 14,
        title = "Ember Basin Ascent",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 32,
        difficulty = difficulty,
        subtitle = "Spring Pads & Rising Ember Basins",
        description = "Launch upward off timed spring pads before they bare teeth, and claim high stone islands above surging ember tides.",
        theme = LevelTheme.HOLLOW_OF_ECHOES,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_15: "The Locked Sanctum"
   * Mechanics: LOCKED_RUNE_GATE + BETRAYAL_SWITCH decoy + Key Coin unlock + TRAILING_SPIKE.
   */
  fun buildChallenge15(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // 1. Sealed Gate Hallway #1 (tiles 0..16, Y=13) with upper balcony (8..12, Y=10.6)
    addRow(tiles, nextId, 0, 16, 13f, TileType.CARVED_STONE)
    tiles.add(GameTile(id = nextId(), x = 8.0f * T, y = 10.6f * T, width = 3.5f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    // 2. Trailing Spike Bridge + Phantom Ledge (tiles 17..25, Y=13)
    addRow(tiles, nextId, 17, 21, 13f, TileType.CARVED_STONE)
    // Phantom Ledge at 22..24 -> Checkpoint Sanctuary 25..29
    addRow(tiles, nextId, 25, 29, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 27.0f)
    // 3. Final Sanctum Gate Chamber (tiles 30..53, Y=13) with raised key-coin pedestal (34..37, Y=10.5)
    addRow(tiles, nextId, 30, 53, 13f, TileType.CARVED_STONE)
    tiles.add(GameTile(id = nextId(), x = 34.0f * T, y = 10.5f * T, width = 3.0f * T, height = 0.6f * T, type = TileType.CARVED_STONE))

    val exitX = 46.8f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 5.0f * T, y = 11.4f * T),
        Coin(id = 2, x = 9.7f * T, y = 9.1f * T),   // Key coin #1 opens Gate #1
        Coin(id = 3, x = 20.5f * T, y = 11.4f * T),
        Coin(id = 4, x = 35.5f * T, y = 9.0f * T),  // Key coin #2 opens Gate #2
        Coin(id = 5, x = 45.0f * T, y = 10.4f * T)
    )

    val traps = listOf(
        // Decoy lever on ground floor springs falling ceiling block instead of opening gate!
        TrollTrap(
            id = 1,
            type = TrollTrapType.BETRAYAL_SWITCH,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 11.5f * T,
            initialY = 13f * T - 26f,
            width = 44f,
            height = 26f,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            linkedTrapId = 2,
            quipPopup = "WRONG SWITCH!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 1,
            chainDelayFrames = 2,
            initialX = 11.3f * T,
            initialY = 6.2f * T,
            width = 52f,
            height = 52f,
            telegraphDurationFrames = 0,
            targetX = 11.3f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 13.5f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "DECOY DROP!"
        ),
        // Gate #1 unlocked cleanly by overhead Key Coin #2
        TrollTrap(
            id = 3,
            type = TrollTrapType.LOCKED_RUNE_GATE,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 2,
            initialX = 14.5f * T,
            initialY = 13f * T - 96f,
            width = 34f,
            height = 96f,
            targetX = 14.5f * T,
            targetY = 13f * T - 200f,
            moveSpeed = 6.8f,
            telegraphDurationFrames = 8,
            killsPlayer = false,
            quipPopup = "SANCTUM UNSEALED!"
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.TRAILING_SPIKE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 17.5f * T,
            initialY = 13f * T - 28f,
            width = 42f,
            height = 28f,
            triggerMinX = 18.2f * T,
            triggerMaxX = 21.5f * T,
            targetX = 21.2f * T,
            targetY = 13f * T - 28f,
            moveSpeed = 5.2f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 160,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "RUN!"
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.PHANTOM_LEDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 22.0f * T,
            initialY = 13f * T,
            width = 2.4f * T,
            height = T,
            telegraphDurationFrames = 0,
            cycleFrames = 102,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "GHOST LEDGE!"
        ),
        // Final Gate #2 unlocked by Key Coin #4 on the raised pedestal
        TrollTrap(
            id = 6,
            type = TrollTrapType.LOCKED_RUNE_GATE,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 4,
            initialX = 43.0f * T,
            initialY = 13f * T - 96f,
            width = 34f,
            height = 96f,
            targetX = 43.0f * T,
            targetY = 13f * T - 200f,
            moveSpeed = 6.8f,
            telegraphDurationFrames = 8,
            killsPlayer = false,
            quipPopup = "SEAL BROKEN!"
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.MOVING_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 41.5f * T,
            initialY = 13f * T - 80f,
            width = T,
            height = 80f,
            triggerMinX = 37.8f * T,
            triggerMaxX = 40.5f * T,
            targetX = 33.2f * T,
            targetY = 13f * T - 80f,
            moveSpeed = 7.5f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "AMBUSH WALL!"
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.RELOCATING_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = exitX,
            initialY = exitY,
            width = 2f * T,
            height = 3f * T,
            triggerMinX = 45.8f * T,
            triggerMaxX = 49.0f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            linkedDoorTargetX = 50.6f * T,
            linkedDoorTargetY = exitY,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SEAL BROKEN... PSYCH!"
        ),
        TrollTrap(
            id = 9,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            chainDelayFrames = 0,
            initialX = 48.8f * T,
            initialY = 6.0f * T,
            width = 50f,
            height = 50f,
            targetX = 48.8f * T,
            targetY = 13f * T - 50f,
            moveSpeed = 14.0f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SANCTUM DROP!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 15,
        title = "The Locked Sanctum",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 32,
        difficulty = difficulty,
        subtitle = "Sealed Rune Gates & Decoy Levers",
        description = "Skip floor betrayal levers that drop ceilings and claim elevated rune keys to slide sealed sanctuary gates open.",
        theme = LevelTheme.RUINS_OF_LOST_KING,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_16: "Beam & Gale"
   * Mechanics: TELEGRAPHED_RUNE_BEAM (horizontal & vertical) + GUST_REVERSAL_ZONE + TILTING_PLANK.
   */
  fun buildChallenge16(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // 1. Beam Hallway #1 (0..14, Y=13) with safe overhead hop stones at Y=10.6
    addRow(tiles, nextId, 0, 14, 13f, TileType.CARVED_STONE)
    tiles.add(GameTile(id = nextId(), x = 7.0f * T, y = 10.6f * T, width = 2.0f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    // 2. Gale + Tilting Plank Crossing (15..24) -> Checkpoint Sanctuary (25..29)
    addRow(tiles, nextId, 20, 22, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 25, 29, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 27.0f)
    // 3. Crossfire Chamber + Evasive Exit Hop (30..49, Y=13)
    addRow(tiles, nextId, 30, 39, 13f, TileType.CARVED_STONE)
    // Gap 40..43 bridged by Evasive Platform -> Final Sun Gate Deck (44..49)
    addRow(tiles, nextId, 44, 49, 13f, TileType.CARVED_STONE)

    val exitX = 46.8f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 8.0f * T, y = 9.2f * T),
        Coin(id = 2, x = 17.0f * T, y = 11.0f * T),
        Coin(id = 3, x = 33.5f * T, y = 11.4f * T),
        Coin(id = 4, x = 37.5f * T, y = 11.4f * T),
        Coin(id = 5, x = 42.0f * T, y = 10.8f * T)
    )

    val traps = listOf(
        // Horizontal sweeping beam across ground lane 6..12 (jump onto 7..9 alcove or time fire window)
        TrollTrap(
            id = 1,
            type = TrollTrapType.TELEGRAPHED_RUNE_BEAM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 5.5f * T,
            initialY = 13f * T - 26f,
            width = 6.5f * T,
            height = 24f,
            telegraphDurationFrames = 14,
            activeDurationFrames = 34,
            cycleFrames = 104,
            killsPlayer = true,
            quipPopup = "RUNE BEAM!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.TILTING_PLANK,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 15.2f * T,
            initialY = 13f * T,
            width = 3.8f * T,
            height = 22f,
            maxTiltDegrees = 20f,
            moveSpeed = 8f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.GUST_REVERSAL_ZONE,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 15.0f * T,
            initialY = 8.5f * T,
            width = 9.5f * T,
            height = 4.5f * T,
            cycleFrames = 105,
            gustFlipIntervalFrames = 105,
            gustForceX = -0.36f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        // Reversing saw + rising ambush hatch replacing old twin vertical jump gates
        TrollTrap(
            id = 4,
            type = TrollTrapType.REVERSING_HAZARD,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 32.0f * T,
            initialY = 13f * T - 38f,
            width = 38f,
            height = 38f,
            triggerMinX = 30.6f * T,
            triggerMaxX = 34.0f * T,
            targetX = 38.2f * T,
            targetY = 13f * T - 38f,
            moveSpeed = 5.4f,
            telegraphDurationFrames = 0,
            cycleFrames = 140,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "GALE SAW!"
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.OPENING_AMBUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 35.8f * T,
            initialY = 13f * T - 84f,
            width = 44f,
            height = 84f,
            triggerMinX = 34.4f * T,
            triggerMaxX = 36.8f * T,
            targetX = 35.8f * T,
            targetY = 13f * T - 144f,
            moveSpeed = 5.5f,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "HIDDEN HATCH!"
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.EVASIVE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 40.3f * T,
            initialY = 13f * T,
            width = 2.4f * T,
            height = 24f,
            targetX = 41.6f * T,
            targetY = 12.2f * T,
            moveSpeed = 6.5f,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "HOLD ON!"
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 21.0f * T,
            initialY = 6.2f * T,
            width = 48f,
            height = 50f,
            triggerMinX = 20.1f * T,
            triggerMaxX = 22.2f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = 21.0f * T,
            targetY = 13f * T - 50f,
            moveSpeed = 14.0f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "LANDING SLAM!"
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.TELEGRAPHED_RUNE_BEAM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 44.2f * T,
            initialY = 13f * T - 24f,
            width = 2.4f * T,
            height = 24f,
            triggerMinX = 44.3f * T,
            triggerMaxX = 46.2f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 40,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "FINAL LASER!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 16,
        title = "Beam & Gale",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 32,
        difficulty = difficulty,
        subtitle = "Rune Sightlines & Shifting Winds",
        description = "Watch crisp aiming sightlines lock before rune beams ignite, and balance tilting planks against reversing monsoon gusts.",
        theme = LevelTheme.MONSOON_RUN,
        trollTraps = traps,
        isTrollLevel = true
    )
  }
}
