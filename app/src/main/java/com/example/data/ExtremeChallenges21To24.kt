package com.example.data

/**
 * Phase 6 — Extreme Challenge Tier (ch_21 to ch_24).
 *
 * - ch_21: The Pursuer's Vault (Stalker Crush Wall + Split Bridge + Phantom Ledge + Slam Cage)
 * - ch_22: Monsoon Cyclone (Gust Reversal + Blink Platforms + Launch Pads + Rune Beams)
 * - ch_23: Vault of Deception (Fake Checkpoints + Decoy Switches + Locked Rune Gate + Fake Exits)
 * - ch_24: Trial of the Eternal Sun (Grand Finale combining belts, embers, pincers, mirror curse & fleeing Sun Gate)
 */
object ExtremeChallenges21To24 {
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
   * ch_21: "The Pursuer's Vault"
   * Extreme Challenge #1 — Two checkpoints, Stalker Crush Wall pursuit, Split Bridge, Surge & Slam Cage finale.
   */
  fun buildChallenge21(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Conveyor + Split Bridge escape from Stalker (0..22)
    addRow(tiles, nextId, 0, 5, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 16, 22, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 19.0f)
    // Act 2: Ember Basin + Phantom Ledge Vault (23..39)
    addRow(tiles, nextId, 23, 30, 14.4f, TileType.SOLID_EARTH)
    tiles.add(GameTile(id = nextId(), x = 25.0f * T, y = 10.6f * T, width = 2.2f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    addRow(tiles, nextId, 34, 39, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 36.5f)
    // Act 3: Final Stalker Sprint + Disappearing Seam + Exit Slam Cage vault (40..57)
    addRow(tiles, nextId, 40, 46, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 49, 57, 13f, TileType.CARVED_STONE)

    val exitX = 54.0f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 8.5f * T, y = 11.4f * T),
        Coin(id = 2, x = 13.5f * T, y = 11.2f * T),
        Coin(id = 3, x = 26.1f * T, y = 9.2f * T),
        Coin(id = 4, x = 44.5f * T, y = 11.4f * T),
        Coin(id = 5, x = 51.5f * T, y = 8.6f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 6f * T,
            initialY = 13f * T,
            width = 5f * T,
            height = T,
            cycleFrames = 94,
            gustFlipIntervalFrames = 94,
            gustForceX = -2.6f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.SPLIT_BRIDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 11.5f * T,
            initialY = 13f * T,
            width = 3.8f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 3.5f * T,
            initialY = 13f * T - 86f,
            width = T,
            height = 86f,
            triggerMinX = 7.2f * T,
            triggerMaxX = 9.5f * T,
            targetX = 16.5f * T,
            targetY = 13f * T - 86f,
            moveSpeed = 5.2f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 220,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "NO TURNING BACK!"
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.RISING_EMBER_SURGE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 23.2f * T,
            initialY = 13.6f * T,
            width = 6.6f * T,
            height = 0.8f * T,
            triggerMinX = 22.0f * T,
            triggerMaxX = 29.8f * T,
            triggerMinY = 7.0f * T,
            triggerMaxY = 14.8f * T,
            targetY = 11.2f * T,
            telegraphDurationFrames = 0,
            moveSpeed = 4.9f,
            effectDurationFrames = 44,
            cycleFrames = 104,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.PHANTOM_LEDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 30.8f * T,
            initialY = 12.6f * T,
            width = 2.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            cycleFrames = 102,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 38.8f * T,
            initialY = 13f * T - 86f,
            width = T,
            height = 86f,
            triggerMinX = 41.2f * T,
            triggerMaxX = 43.5f * T,
            targetX = 50.5f * T,
            targetY = 13f * T - 86f,
            moveSpeed = 5.4f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 220,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "VAULT THE CAGE!"
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.EXIT_SLAM_CAGE,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 51.0f * T,
            initialY = 10f * T,
            width = 2.2f * T,
            height = 3.0f * T,
            triggerMinX = 49.4f * T,
            triggerMaxX = 52.8f * T,
            telegraphDurationFrames = 0,
            effectDurationFrames = 65,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "VAULT OVER!"
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 47.2f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "DON'T STUMBLE!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 21,
        title = "The Pursuer's Vault",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 38,
        difficulty = difficulty,
        subtitle = "Stalker Pursuit & Cage Vaults",
        description = "Outrun relentless stalker monoliths across collapsing spans, ghost ledges, and a slamming exit cage.",
        theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_22: "Monsoon Cyclone"
   * Extreme Challenge #2 — Gust Reversals + Blink Platforms + Launch Pad Beam Shaft + Evasive Finale.
   */
  fun buildChallenge22(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Blink Platform Crossing in Cyclone Wind (0..22)
    addRow(tiles, nextId, 0, 5, 13f, TileType.MOSSY_TIMBER)
    addRow(tiles, nextId, 16, 21, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 18.5f)
    // Act 2: Launch Pad Climb dodging Horizontal Rune Beams (22..38)
    addRow(tiles, nextId, 22, 26, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 28, 32, 9.6f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 34, 39, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 36.5f)
    // Act 3: Tilting Plank + Evasive Platform + Oscillating Spike Finale (40..57)
    addRow(tiles, nextId, 49, 57, 13f, TileType.CARVED_STONE)

    val exitX = 54.0f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 8.0f * T, y = 11.0f * T),
        Coin(id = 2, x = 13.0f * T, y = 11.0f * T),
        Coin(id = 3, x = 30.0f * T, y = 8.2f * T),
        Coin(id = 4, x = 42.5f * T, y = 10.8f * T),
        Coin(id = 5, x = 51.5f * T, y = 10.6f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.TIMED_BLINK_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 6.6f * T,
            initialY = 13f * T,
            width = 2.6f * T,
            height = T,
            cycleFrames = 122,
            phaseOffsetFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.TIMED_BLINK_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 11.5f * T,
            initialY = 12.6f * T,
            width = 2.6f * T,
            height = T,
            cycleFrames = 122,
            phaseOffsetFrames = 44,
            killsPlayer = false
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.GUST_REVERSAL_ZONE,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 6.2f * T,
            initialY = 8.0f * T,
            width = 9.2f * T,
            height = 5.0f * T,
            cycleFrames = 98,
            gustFlipIntervalFrames = 98,
            gustForceX = -0.35f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 24.8f * T,
            initialY = 13f * T - 22f,
            width = 48f,
            height = 22f,
            launchImpulseVy = -16.6f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 70,
            cycleFrames = 100,
            maxActivations = 3,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.TELEGRAPHED_RUNE_BEAM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 27.6f * T,
            initialY = 8.3f * T,
            width = 5.2f * T,
            height = 24f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 32,
            cycleFrames = 104,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "HIGH BEAM!"
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.TILTING_PLANK,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 40.2f * T,
            initialY = 13f * T,
            width = 3.6f * T,
            height = 22f,
            maxTiltDegrees = 22f,
            moveSpeed = 9.2f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.EVASIVE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 44.8f * T,
            initialY = 12.6f * T,
            width = 2.4f * T,
            height = 24f,
            targetX = 46.8f * T,
            targetY = 12.2f * T,
            moveSpeed = 6.6f,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.MOVING_SPIKE,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 50.0f * T,
            initialY = 13f * T - 28f,
            width = 42f,
            height = 28f,
            targetX = 52.6f * T,
            targetY = 13f * T - 28f,
            cycleFrames = 92,
            telegraphDurationFrames = 0,
            killsPlayer = true
        ),
        TrollTrap(
            id = 9,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            chainDelayFrames = 0,
            initialX = 51.5f * T,
            initialY = 6.0f * T,
            width = 50f,
            height = 52f,
            targetX = 51.5f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 14.5f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "CYCLONE DROP!"
        ),
        // Coin #3 on the upper beam ledge drops an instant keystone on the landing edge
        TrollTrap(
            id = 10,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 3,
            chainDelayFrames = 4,
            initialX = 31.2f * T,
            initialY = 4.2f * T,
            width = 48f,
            height = 48f,
            targetX = 31.2f * T,
            targetY = 9.6f * T - 48f,
            moveSpeed = 13.8f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SKY TAX!"
        ),
        // Betrayal switch before Launch Pad #4 arms a delayed spike on the Act 2 landing stone
        TrollTrap(
            id = 11,
            type = TrollTrapType.BETRAYAL_SWITCH,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 22.8f * T,
            initialY = 13f * T - 22f,
            width = 44f,
            height = 22f,
            telegraphDurationFrames = 0,
            linkedTrapId = 12,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "PAD ARMED!"
        ),
        TrollTrap(
            id = 12,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 11,
            chainDelayFrames = 32,
            initialX = 34.8f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 60,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "DELAYED BITE!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 22,
        title = "Monsoon Cyclone",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 38,
        difficulty = difficulty,
        subtitle = "Cyclone Winds & Blink Gauntlet",
        description = "Time blinking platforms in reversing monsoon gales, weave past sky beams, and ride an evasive ledge home.",
        theme = LevelTheme.MONSOON_RUN,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_23: "Vault of Deception"
   * Extreme Challenge #3 — Fake Checkpoints, Betrayal Switches, Key-Coin Locked Gate & Mirage Exits.
   */
  fun buildChallenge23(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Hall of False Bells (0..22) — Fake bell on lower path (11.5), real Checkpoint 1 on upper terrace (16..20, Y=10.5) & lower deck 16..22
    addRow(tiles, nextId, 0, 22, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 16, 20, 10.2f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 18.0f, floorTileY = 10.2f)
    // Act 2: Locked Sanctum Courtyard (23..38) with raised Key Coin alcove (28..31, Y=10.2) + Checkpoint 2 (35..38)
    addRow(tiles, nextId, 23, 38, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 28, 31, 10.2f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 36.5f)
    // Act 3: Mirage Exit Corridor + Disappearing Dash Seam (39..50, 51..52 disappearing, 53..57)
    addRow(tiles, nextId, 39, 50, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 53, 57, 13f, TileType.CARVED_STONE)

    val exitX = 49.5f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 7.5f * T, y = 11.4f * T),
        Coin(id = 2, x = 18.0f * T, y = 8.4f * T),
        Coin(id = 3, x = 29.5f * T, y = 8.8f * T),  // Key coin #1 unlocks Gate
        Coin(id = 4, x = 42.5f * T, y = 11.4f * T),
        Coin(id = 5, x = 52.0f * T, y = 11.4f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.FAKE_CHECKPOINT,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 11.5f * T,
            initialY = 12f * T,
            width = T,
            height = T,
            triggerMinX = 10.5f * T,
            triggerMaxX = 13.5f * T,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "CURSED BELL!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 14.2f * T,
            initialY = 13f * T - 22f,
            width = 48f,
            height = 22f,
            launchImpulseVy = -15.8f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 70,
            cycleFrames = 100,
            maxActivations = 3,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.BETRAYAL_SWITCH,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 29.0f * T,
            initialY = 13f * T - 26f,
            width = 44f,
            height = 26f,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            linkedTrapId = 4,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "TRAP SWITCH!"
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.PINCER_WALLS,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 3,
            chainDelayFrames = 4,
            initialX = 27.6f * T,
            initialY = 13f * T - 86f,
            width = 3.4f * T,
            height = 86f,
            minSafeGapWidth = 56f,
            telegraphDurationFrames = 0,
            moveSpeed = 5.8f,
            cycleFrames = 96,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SQUEEZE!"
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.LOCKED_RUNE_GATE,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 3,
            initialX = 33.2f * T,
            initialY = 13f * T - 96f,
            width = 34f,
            height = 96f,
            targetX = 33.2f * T,
            targetY = 13f * T - 200f,
            moveSpeed = 7.2f,
            telegraphDurationFrames = 8,
            killsPlayer = false,
            quipPopup = "KEY ACCEPTED!"
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.FAKE_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 44.5f * T,
            initialY = 10f * T,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 43.4f * T,
            triggerMaxX = 46.8f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 55,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "MIRAGE GATE!"
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.RELOCATING_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = exitX,
            initialY = exitY,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 48.2f * T,
            triggerMaxX = 51.0f * T,
            linkedDoorTargetX = 54.0f * T,
            linkedDoorTargetY = exitY,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "ONE MORE STEP!"
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 51.2f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "CHASM OF LIES!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 23,
        title = "Vault of Deception",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 40,
        difficulty = difficulty,
        subtitle = "False Bells, Gates & Mirage Doors",
        description = "Reject cursed bells and trap switches, climb to the true checkpoint and rune key, and outsmart both fake and fleeing exits.",
        theme = LevelTheme.RUINS_OF_LOST_KING,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_24: "Trial of the Eternal Sun"
   * Grand Finale — 3 full acts, 2 Checkpoints, weaving Conveyor, Beams, Ember Surge, Mirror Curse, Key Gate, Stalker & Fleeing Gate.
   */
  fun buildChallenge24(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Reversing Conveyor + Beam Corridor + Spring Launch (0..20)
    addRow(tiles, nextId, 0, 5, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 12, 21, 13f, TileType.CARVED_STONE)
    tiles.add(GameTile(id = nextId(), x = 14.0f * T, y = 10.6f * T, width = 2.0f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    addCheckpoint(tiles, nextId, 19.5f)
    // Act 2: Ember Basin + Pincers + Mirror Curse Terrace (22..40)
    addRow(tiles, nextId, 22, 29, 14.4f, TileType.SOLID_EARTH)
    tiles.add(GameTile(id = nextId(), x = 24.5f * T, y = 10.2f * T, width = 2.4f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    addRow(tiles, nextId, 30, 41, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 39.5f)
    // Act 3: Key-Coin Sealed Sanctum + Stalker Dash + Fleeing Eternal Sun Gate (42..62)
    addRow(tiles, nextId, 42, 62, 13f, TileType.CARVED_STONE)
    tiles.add(GameTile(id = nextId(), x = 44.5f * T, y = 10.5f * T, width = 2.6f * T, height = 0.6f * T, type = TileType.CARVED_STONE))

    val exitX = 53.5f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 9.0f * T, y = 11.4f * T),
        Coin(id = 2, x = 15.0f * T, y = 9.2f * T),
        Coin(id = 3, x = 25.7f * T, y = 8.8f * T),
        Coin(id = 4, x = 45.8f * T, y = 9.1f * T),  // Key Coin opens Act 3 Sanctum Gate
        Coin(id = 5, x = 57.5f * T, y = 11.4f * T)
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
            cycleFrames = 92,
            gustFlipIntervalFrames = 92,
            gustForceX = -2.6f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.TELEGRAPHED_RUNE_BEAM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 13.0f * T,
            initialY = 13f * T - 26f,
            width = 5.0f * T,
            height = 24f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 34,
            cycleFrames = 102,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 21.0f * T,
            initialY = 13f * T - 22f,
            width = 48f,
            height = 22f,
            launchImpulseVy = -16.2f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 70,
            cycleFrames = 100,
            maxActivations = 3,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.RISING_EMBER_SURGE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 22.2f * T,
            initialY = 13.6f * T,
            width = 6.6f * T,
            height = 0.8f * T,
            triggerMinX = 21.5f * T,
            triggerMaxX = 29.0f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 14.8f * T,
            targetY = 11.2f * T,
            telegraphDurationFrames = 0,
            moveSpeed = 4.9f,
            effectDurationFrames = 44,
            cycleFrames = 104,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.PINCER_WALLS,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 31.0f * T,
            initialY = 13f * T - 86f,
            width = 3.4f * T,
            height = 86f,
            triggerMinX = 30.2f * T,
            triggerMaxX = 34.4f * T,
            minSafeGapWidth = 54f,
            telegraphDurationFrames = 0,
            moveSpeed = 5.9f,
            cycleFrames = 96,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.MIRROR_CURSE_TOTEM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 35.8f * T,
            initialY = 13f * T - 64f,
            width = 40f,
            height = 64f,
            triggerMinX = 34.8f * T,
            triggerMaxX = 37.6f * T,
            telegraphDurationFrames = 0,
            effectDurationFrames = 115,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SUN'S TEST!"
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.LOCKED_RUNE_GATE,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 4,
            initialX = 48.0f * T,
            initialY = 13f * T - 96f,
            width = 34f,
            height = 96f,
            targetX = 48.0f * T,
            targetY = 13f * T - 200f,
            moveSpeed = 7.2f,
            telegraphDurationFrames = 8,
            killsPlayer = false,
            quipPopup = "FINAL SEAL OPEN!"
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 49.0f * T,
            initialY = 13f * T - 86f,
            width = T,
            height = 86f,
            triggerMinX = 50.6f * T,
            triggerMaxX = 52.8f * T,
            targetX = 57.5f * T,
            targetY = 13f * T - 86f,
            moveSpeed = 5.4f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 210,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "FINAL DASH!"
        ),
        TrollTrap(
            id = 9,
            type = TrollTrapType.FLEEING_SUN_GATE,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = exitX,
            initialY = exitY,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 52.2f * T,
            triggerMaxX = 55.6f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = 58.6f * T,
            targetY = exitY,
            secondaryX = 59.5f * T,
            secondaryY = exitY,
            maxActivations = 1,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "ONE LAST DODGE!"
        ),
        TrollTrap(
            id = 10,
            type = TrollTrapType.EXIT_SLAM_CAGE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 52.8f * T,
            initialY = 10.0f * T,
            width = 2.6f * T,
            height = 3f * T,
            triggerMinX = 52.2f * T,
            triggerMaxX = 55.2f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            effectDurationFrames = 55,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SEALED SUN!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 24,
        title = "Trial of the Eternal Sun",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 44,
        difficulty = difficulty,
        subtitle = "Grand Finale — Every Trick United",
        description = "The ultimate gauntlet: survive belts, beams, rising embers, pincers, mirror curses, a sealed key gate, and the fleeing Sun Gate.",
        theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
        trollTraps = traps,
        isTrollLevel = true
    )
  }
}
