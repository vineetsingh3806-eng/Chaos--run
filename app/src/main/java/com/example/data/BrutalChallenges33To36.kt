package com.example.data

/**
 * Phase 7 — Brutal Tier Challenges (ch_33 to ch_36).
 *
 * - ch_33: Above and Below (Two distinct dangerous routes — upper vs. lower — with different trap combos)
 * - ch_34: The Unforgiving Tower (Vertical climbing, launch pads, ceiling hazards & falling platforms)
 * - ch_35: Survival Instinct (Multi-stage survival combining varied mechanics without warning)
 * - ch_36: The Last Laugh (Brutal multi-stage finale using the most demanding instant + interacting combos)
 */
object BrutalChallenges33To36 {
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
   * ch_33: "Above and Below"
   * Two distinct routes per act (Upper Canopy vs Lower Basin/Corridor) with contrasting trap sets.
   */
  fun buildChallenge33(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Entry (0..7)
    addRow(tiles, nextId, 0, 7, 13f, TileType.MOSSY_TIMBER)
    tiles.add(GameTile(id = nextId(), x = 7.2f * T, y = 11.5f * T, width = 1.6f * T, height = 0.6f * T, type = TileType.CARVED_STONE))

    // Split 1 (9..21): Upper Route (y=10.2T) vs Lower Route (y=13.4T)
    addRow(tiles, nextId, 9, 10, 10.2f, TileType.MOSSY_TIMBER)
    addRow(tiles, nextId, 13, 14, 10.2f, TileType.MOSSY_TIMBER)
    addRow(tiles, nextId, 20, 21, 10.2f, TileType.MOSSY_TIMBER)
    addRow(tiles, nextId, 9, 16, 13.4f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 21, 25, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 23.5f)

    // Split 2 (27..39): Launch Pad Upper Route (y=9.6T) vs Ember Basin Lower Route (y=14.4T)
    addRow(tiles, nextId, 26, 27, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 28, 33, 9.6f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 37, 39, 10.0f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 28, 34, 14.4f, TileType.SOLID_EARTH)
    addRow(tiles, nextId, 35, 44, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 42.0f)

    // Final Dual Deck (45..57): Fake Exit on Lower + Real Sun Gate at End
    addRow(tiles, nextId, 45, 57, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 47, 51, 10.2f, TileType.CARVED_STONE)

    val exitX = 54.0f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 11.8f * T, y = 8.6f * T),   // Upper route 1
        Coin(id = 2, x = 14.8f * T, y = 11.8f * T),  // Lower route 1
        Coin(id = 3, x = 35.0f * T, y = 8.0f * T),   // Upper route 2
        Coin(id = 4, x = 31.5f * T, y = 12.6f * T),  // Lower route 2
        Coin(id = 5, x = 49.0f * T, y = 8.8f * T)
    )

    val traps = listOf(
        // Upper 1: Disappearing Floor -> Fake Safe Platform -> Evasive Platform
        TrollTrap(
            id = 1,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 11.0f * T,
            initialY = 10.2f * T,
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
            initialX = 15.0f * T,
            initialY = 10.2f * T,
            width = 2.0f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.EVASIVE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 17.4f * T,
            initialY = 10.2f * T,
            width = 2.0f * T,
            height = T,
            targetX = 18.6f * T,
            targetY = 10.2f * T,
            moveSpeed = 4.8f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        // Lower 1: Falling Ceiling -> Instant Spike -> Reverse Conveyor
        TrollTrap(
            id = 4,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 11.2f * T,
            initialY = 10.8f * T,
            width = 48f,
            height = 48f,
            triggerMinX = 10.3f * T,
            triggerMaxX = 12.4f * T,
            triggerMinY = 11.2f * T,
            triggerMaxY = 13.8f * T,
            targetX = 11.2f * T,
            targetY = 13.4f * T - 48f,
            moveSpeed = 10.5f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 14.4f * T,
            initialY = 13.4f * T - 32f,
            width = 48f,
            height = 32f,
            triggerMinX = 13.6f * T,
            triggerMaxX = 15.6f * T,
            triggerMinY = 11.4f * T,
            triggerMaxY = 13.8f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 75,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 17.0f * T,
            initialY = 13.4f * T,
            width = 3.8f * T,
            height = T,
            cycleFrames = 100,
            gustFlipIntervalFrames = 100,
            gustForceX = -2.4f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        // Split 2 Access Launch Pad -> Upper Mirror Totem & Phantom Ledge
        TrollTrap(
            id = 7,
            type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 26.2f * T,
            initialY = 13f * T - 22f,
            width = 48f,
            height = 22f,
            launchImpulseVy = -16.2f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            cycleFrames = 100,
            maxActivations = 4,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.MIRROR_CURSE_TOTEM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 30.2f * T,
            initialY = 9.6f * T - 60f,
            width = 38f,
            height = 60f,
            triggerMinX = 29.2f * T,
            triggerMaxX = 31.6f * T,
            triggerMinY = 6.5f * T,
            triggerMaxY = 10.0f * T,
            telegraphDurationFrames = 0,
            effectDurationFrames = 90,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            quipPopup = "CURSED!"
        ),
        TrollTrap(
            id = 9,
            type = TrollTrapType.PHANTOM_LEDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 34.0f * T,
            initialY = 9.6f * T,
            width = 2.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            cycleFrames = 110,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        // Split 2 Lower: Instant Rising Ember Surge + Instant Pincer Walls
        TrollTrap(
            id = 10,
            type = TrollTrapType.RISING_EMBER_SURGE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 28.4f * T,
            initialY = 13.6f * T,
            width = 5.4f * T,
            height = 0.8f * T,
            triggerMinX = 28.0f * T,
            triggerMaxX = 34.0f * T,
            triggerMinY = 11.2f * T,
            triggerMaxY = 14.8f * T,
            targetY = 11.4f * T,
            telegraphDurationFrames = 0,
            moveSpeed = 4.2f,
            effectDurationFrames = 44,
            cycleFrames = 115,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 11,
            type = TrollTrapType.PINCER_WALLS,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 36.2f * T,
            initialY = 13f * T - 86f,
            width = 3.4f * T,
            height = 86f,
            triggerMinX = 35.4f * T,
            triggerMaxX = 39.6f * T,
            triggerMinY = 10.5f * T,
            triggerMaxY = 13.4f * T,
            minSafeGapWidth = 52f,
            moveSpeed = 5.4f,
            telegraphDurationFrames = 0,
            cycleFrames = 105,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        // Final Lower Fake Exit
        TrollTrap(
            id = 12,
            type = TrollTrapType.FAKE_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 48.0f * T,
            initialY = 10f * T,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 46.8f * T,
            triggerMaxX = 50.2f * T,
            triggerMinY = 10.5f * T,
            triggerMaxY = 13.4f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        // Upper Final Deck betrayal so taking the high route no longer skips finale danger
        TrollTrap(
            id = 13,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            chainDelayFrames = 0,
            initialX = 49.2f * T,
            initialY = 4.0f * T,
            width = 48f,
            height = 48f,
            targetX = 49.2f * T,
            targetY = 10.2f * T - 48f,
            moveSpeed = 13.5f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 14,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 50.4f * T,
            initialY = 10.2f * T - 30f,
            width = 44f,
            height = 30f,
            triggerMinX = 49.5f * T,
            triggerMaxX = 51.4f * T,
            triggerMinY = 6.5f * T,
            triggerMaxY = 10.6f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 70,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 33,
        title = "Above and Below",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 40,
        difficulty = difficulty,
        subtitle = "Dual Canopy & Cavern Gauntlets",
        description = "Choose the fragile canopy above or the crushing embers below — neither path offers a moment's warning.",
        theme = LevelTheme.WHISPERING_WOODS,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_34: "The Unforgiving Tower"
   * Vertical climbing with Launch Pads, Yo-Yo lifts, instant ceiling hazards & falling platforms.
   */
  fun buildChallenge34(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Base recovery floor running underneath Act 1..2 (0..25)
    addRow(tiles, nextId, 0, 24, 13f, TileType.CARVED_STONE)
    // Tier 2 Skywalk (10..14 & 18..23 at y=9.6T), CP1 at 20.5T
    addRow(tiles, nextId, 10, 14, 9.6f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 18, 23, 9.6f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 20.5f, floorTileY = 9.6f)

    // Spire Peak (27..35 at y=6.2T) reached via Yo-Yo lift (24.5T, 9.6T <-> 6.2T) -> drop onto 35..41 (y=9.8T), CP2
    addRow(tiles, nextId, 27, 32, 6.2f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 35, 42, 9.8f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 39.0f, floorTileY = 9.8f)

    // Catch floor under Act 2..3 (25..46) with spring recovery
    addRow(tiles, nextId, 25, 46, 13f, TileType.CARVED_STONE)

    // Summit Launch Vault -> Final Tower Deck (47..56 at y=9.8T), Sun Gate at 53.0T
    addRow(tiles, nextId, 47, 56, 9.8f, TileType.CARVED_STONE)

    val exitX = 53.0f * T
    val exitY = 6.8f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 8.4f * T, y = 9.6f * T),
        Coin(id = 2, x = 16.5f * T, y = 8.0f * T),
        Coin(id = 3, x = 29.5f * T, y = 4.8f * T),
        Coin(id = 4, x = 44.5f * T, y = 8.4f * T),
        Coin(id = 5, x = 50.5f * T, y = 8.4f * T)
    )

    val traps = listOf(
        // Launch Pad 1 -> Tier 2 (y=9.6T)
        TrollTrap(
            id = 1,
            type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 8.0f * T,
            initialY = 13f * T - 22f,
            width = 48f,
            height = 22f,
            launchImpulseVy = -16.6f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            cycleFrames = 100,
            maxActivations = 5,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        // Recovery Launch Pad on base floor in case player falls before reaching CP1
        TrollTrap(
            id = 2,
            type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 16.5f * T,
            initialY = 13f * T - 22f,
            width = 48f,
            height = 22f,
            launchImpulseVy = -16.6f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            cycleFrames = 100,
            maxActivations = 5,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        // Tier 2 Instant Ceiling Drop & Instant Disappearing Bridge
        TrollTrap(
            id = 3,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 12.2f * T,
            initialY = 4.8f * T,
            width = 50f,
            height = 50f,
            triggerMinX = 11.4f * T,
            triggerMaxX = 13.5f * T,
            triggerMinY = 5.0f * T,
            triggerMaxY = 9.8f * T,
            targetX = 12.2f * T,
            targetY = 9.6f * T - 50f,
            moveSpeed = 11.5f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 15.2f * T,
            initialY = 9.6f * T,
            width = 2.4f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        // Yo-Yo Lift from 9.6T up to Spire Peak 6.2T
        TrollTrap(
            id = 5,
            type = TrollTrapType.YO_YO_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 24.4f * T,
            initialY = 9.6f * T,
            width = 1.9f * T,
            height = T,
            targetX = 24.4f * T,
            targetY = 6.2f * T,
            cycleFrames = 115,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        // Spire Peak: Gust Reversal + Instant Spike + Fake Safe Drop onto 35..42 (y=9.8T)
        TrollTrap(
            id = 6,
            type = TrollTrapType.GUST_REVERSAL_ZONE,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 27.2f * T,
            initialY = 2.8f * T,
            width = 5.5f * T,
            height = 3.4f * T,
            cycleFrames = 100,
            gustFlipIntervalFrames = 100,
            gustForceX = -0.36f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 30.0f * T,
            initialY = 6.2f * T - 32f,
            width = 48f,
            height = 32f,
            triggerMinX = 29.2f * T,
            triggerMaxX = 31.2f * T,
            triggerMinY = 3.0f * T,
            triggerMaxY = 6.5f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 75,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 33.2f * T,
            initialY = 6.2f * T,
            width = 1.8f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        // Recovery Yo-Yo lift in case player drops to base floor at x=36..44
        TrollTrap(
            id = 9,
            type = TrollTrapType.YO_YO_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 33.5f * T,
            initialY = 12.6f * T,
            width = 1.8f * T,
            height = T,
            targetX = 33.5f * T,
            targetY = 9.8f * T,
            cycleFrames = 110,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        // Final Tower Vault: Launch Pad (43.0T) over instant Rune Beam + Falling Ceiling
        TrollTrap(
            id = 10,
            type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 43.0f * T,
            initialY = 9.8f * T - 22f,
            width = 48f,
            height = 22f,
            launchImpulseVy = -16.2f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            cycleFrames = 100,
            maxActivations = 5,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 11,
            type = TrollTrapType.TELEGRAPHED_RUNE_BEAM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 44.8f * T,
            initialY = 9.8f * T - 24f,
            width = 2.6f * T,
            height = 24f,
            triggerMinX = 43.6f * T,
            triggerMaxX = 46.8f * T,
            triggerMinY = 5.5f * T,
            triggerMaxY = 10.2f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 36,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 12,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 49.8f * T,
            initialY = 4.6f * T,
            width = 52f,
            height = 52f,
            triggerMinX = 48.9f * T,
            triggerMaxX = 51.2f * T,
            triggerMinY = 5.0f * T,
            triggerMaxY = 10.0f * T,
            targetX = 49.8f * T,
            targetY = 9.8f * T - 52f,
            moveSpeed = 12.0f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 13,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            chainDelayFrames = 4,
            initialX = 52.2f * T,
            initialY = 9.8f * T - 32f,
            width = 46f,
            height = 32f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 34,
        title = "The Unforgiving Tower",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 42,
        difficulty = difficulty,
        subtitle = "Skyward Springs & Canopy Slams",
        description = "Ascend the storm-swept monolith on spiked springs and yo-yo lifts while the tower drops its own stones.",
        theme = LevelTheme.MONSOON_RUN,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_35: "Survival Instinct"
   * Multi-stage survival combining varied trap mechanics without warning.
   */
  fun buildChallenge35(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Instant Mirror Curse + Reverse Conveyor + Disappearing Floor -> CP1 (17..22)
    addRow(tiles, nextId, 0, 7, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 17, 22, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 19.5f)

    // Act 2: Sunken Ember Basin (23..30, y=14.4T) + Phantom Ledge -> Pincer Walls -> CP2 (31..39)
    addRow(tiles, nextId, 23, 30, 14.4f, TileType.SOLID_EARTH)
    addRow(tiles, nextId, 31, 39, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 37.0f)

    // Act 3: Stalker Crush Wall pursuit past Ceiling Drop & Spike Pop to exit (40..57)
    addRow(tiles, nextId, 40, 57, 13f, TileType.CARVED_STONE)

    val exitX = 54.0f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 10.5f * T, y = 11.2f * T),
        Coin(id = 2, x = 26.8f * T, y = 9.2f * T),
        Coin(id = 3, x = 33.8f * T, y = 11.2f * T),
        Coin(id = 4, x = 44.5f * T, y = 11.4f * T),
        Coin(id = 5, x = 51.0f * T, y = 11.0f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.MIRROR_CURSE_TOTEM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 5.8f * T,
            initialY = 13f * T - 64f,
            width = 40f,
            height = 64f,
            triggerMinX = 4.9f * T,
            triggerMaxX = 7.4f * T,
            telegraphDurationFrames = 0,
            effectDurationFrames = 95,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            quipPopup = "MIND BENT!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 8.2f * T,
            initialY = 13f * T,
            width = 5.4f * T,
            height = T,
            cycleFrames = 100,
            gustFlipIntervalFrames = 100,
            gustForceX = -2.4f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 14.2f * T,
            initialY = 13f * T,
            width = 2.2f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.RISING_EMBER_SURGE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 23.2f * T,
            initialY = 13.6f * T,
            width = 6.6f * T,
            height = 0.8f * T,
            triggerMinX = 22.4f * T,
            triggerMaxX = 30.2f * T,
            triggerMinY = 6.5f * T,
            triggerMaxY = 14.8f * T,
            targetY = 11.2f * T,
            telegraphDurationFrames = 0,
            moveSpeed = 4.4f,
            effectDurationFrames = 44,
            cycleFrames = 115,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.PHANTOM_LEDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 25.4f * T,
            initialY = 10.6f * T,
            width = 2.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            cycleFrames = 110,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.PINCER_WALLS,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 32.0f * T,
            initialY = 13f * T - 86f,
            width = 3.4f * T,
            height = 86f,
            triggerMinX = 31.2f * T,
            triggerMaxX = 35.4f * T,
            minSafeGapWidth = 52f,
            moveSpeed = 5.5f,
            telegraphDurationFrames = 0,
            cycleFrames = 105,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 38.8f * T,
            initialY = 13f * T - 86f,
            width = T,
            height = 86f,
            triggerMinX = 41.0f * T,
            triggerMaxX = 43.2f * T,
            targetX = 52.0f * T,
            targetY = 13f * T - 86f,
            moveSpeed = 5.3f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 220,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 44.8f * T,
            initialY = 6.2f * T,
            width = 52f,
            height = 52f,
            triggerMinX = 43.9f * T,
            triggerMaxX = 46.2f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = 44.8f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 12.2f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 9,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 48.6f * T,
            initialY = 13f * T - 32f,
            width = 48f,
            height = 32f,
            triggerMinX = 47.8f * T,
            triggerMaxX = 50.0f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 75,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 10,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            chainDelayFrames = 0,
            initialX = 52.0f * T,
            initialY = 13f * T - 32f,
            width = 48f,
            height = 32f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 75,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 35,
        title = "Survival Instinct",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 42,
        difficulty = difficulty,
        subtitle = "Zero-Warning Multi-Hazard Trial",
        description = "Mirrored controls, surging embers, sudden pincers, and an unannounced stalker wall put pure instinct to the test.",
        theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_36: "The Last Laugh"
   * Brutal multi-stage finale uniting instant chains, key-coin sanctum gate, stalker pursuit & fleeing Sun Gate.
   */
  fun buildChallenge36(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    // Act 1: Instant Spike -> Disappearing Floor -> Split Bridge chained Ceiling Drop -> CP1 (16..22)
    addRow(tiles, nextId, 0, 7, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 16, 22, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 19.5f)

    // Act 2: Conveyor -> Pincers -> Launch Pad over Ember Basin (34..39) -> CP2 (40..44)
    addRow(tiles, nextId, 23, 23, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 28, 33, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 34, 39, 14.4f, TileType.SOLID_EARTH)
    tiles.add(GameTile(id = nextId(), x = 35.6f * T, y = 10.6f * T, width = 2.4f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    addRow(tiles, nextId, 40, 44, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 41.8f)

    // Act 3: Key Coin Sealed Sanctum + Mirror Curse + Stalker Dash + Slam Cage + Fleeing Eternal Exit (45..66)
    addRow(tiles, nextId, 45, 66, 13f, TileType.CARVED_STONE)
    tiles.add(GameTile(id = nextId(), x = 46.2f * T, y = 10.4f * T, width = 2.4f * T, height = 0.6f * T, type = TileType.CARVED_STONE))

    val exitX = 56.5f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 9.0f * T, y = 11.0f * T),
        Coin(id = 2, x = 25.8f * T, y = 11.4f * T),
        Coin(id = 3, x = 36.8f * T, y = 9.2f * T),
        Coin(id = 4, x = 47.4f * T, y = 9.0f * T),  // Key Coin unlocks Locked Rune Gate & trips instant Mirror Totem
        Coin(id = 5, x = 60.0f * T, y = 11.2f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 5.4f * T,
            initialY = 13f * T - 32f,
            width = 48f,
            height = 32f,
            triggerMinX = 4.6f * T,
            triggerMaxX = 6.8f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 75,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 8.0f * T,
            initialY = 13f * T,
            width = 2.4f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.SPLIT_BRIDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 11.2f * T,
            initialY = 13f * T,
            width = 3.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            linkedTrapId = 4
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 3,
            chainDelayFrames = 0,
            initialX = 16.6f * T,
            initialY = 6.2f * T,
            width = 52f,
            height = 52f,
            targetX = 16.6f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 12.2f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 24.0f * T,
            initialY = 13f * T,
            width = 4.0f * T,
            height = T,
            cycleFrames = 96,
            gustFlipIntervalFrames = 96,
            gustForceX = -2.5f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.PINCER_WALLS,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 29.0f * T,
            initialY = 13f * T - 86f,
            width = 3.4f * T,
            height = 86f,
            triggerMinX = 28.2f * T,
            triggerMaxX = 32.4f * T,
            minSafeGapWidth = 52f,
            moveSpeed = 5.5f,
            telegraphDurationFrames = 0,
            cycleFrames = 105,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 32.8f * T,
            initialY = 13f * T - 22f,
            width = 48f,
            height = 22f,
            launchImpulseVy = -16.2f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 70,
            cycleFrames = 100,
            maxActivations = 4,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.RISING_EMBER_SURGE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 34.2f * T,
            initialY = 13.6f * T,
            width = 5.6f * T,
            height = 0.8f * T,
            triggerMinX = 33.4f * T,
            triggerMaxX = 39.8f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 14.8f * T,
            targetY = 11.2f * T,
            telegraphDurationFrames = 0,
            moveSpeed = 4.4f,
            effectDurationFrames = 44,
            cycleFrames = 115,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        // Coin 4 opens Sealed Sanctum Gate (9) AND chains instant Mirror Totem (10)
        TrollTrap(
            id = 9,
            type = TrollTrapType.LOCKED_RUNE_GATE,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 4,
            initialX = 50.2f * T,
            initialY = 13f * T - 96f,
            width = 34f,
            height = 96f,
            targetX = 50.2f * T,
            targetY = 13f * T - 200f,
            moveSpeed = 7.2f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            linkedTrapId = 10,
            quipPopup = "SANCTUM UNSEALED!"
        ),
        TrollTrap(
            id = 10,
            type = TrollTrapType.MIRROR_CURSE_TOTEM,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 9,
            chainDelayFrames = 0,
            initialX = 48.8f * T,
            initialY = 13f * T - 64f,
            width = 40f,
            height = 64f,
            telegraphDurationFrames = 0,
            effectDurationFrames = 90,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            quipPopup = "THE LAST LAUGH!"
        ),
        TrollTrap(
            id = 11,
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 51.2f * T,
            initialY = 13f * T - 86f,
            width = T,
            height = 86f,
            triggerMinX = 52.8f * T,
            triggerMaxX = 54.8f * T,
            targetX = 60.0f * T,
            targetY = 13f * T - 86f,
            moveSpeed = 5.4f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 220,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = true
        ),
        TrollTrap(
            id = 12,
            type = TrollTrapType.EXIT_SLAM_CAGE,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 56.2f * T,
            initialY = 10f * T,
            width = 2.4f * T,
            height = 3.0f * T,
            triggerMinX = 54.8f * T,
            triggerMaxX = 58.6f * T,
            telegraphDurationFrames = 0,
            effectDurationFrames = 65,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            quipPopup = "NOT YET!"
        ),
        TrollTrap(
            id = 13,
            type = TrollTrapType.FLEEING_SUN_GATE,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = exitX,
            initialY = exitY,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 55.2f * T,
            triggerMaxX = 58.8f * T,
            targetX = 62.2f * T,
            targetY = exitY,
            secondaryX = 63.0f * T,
            secondaryY = exitY,
            maxActivations = 1,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            quipPopup = "CATCH THIS!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 36,
        title = "The Last Laugh",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 46,
        difficulty = difficulty,
        subtitle = "Brutal Finale — No Warnings Left",
        description = "Every instant trick in the forest fires in rapid sequence: chained collapses, cursed sanctums, stalkers, and a fleeing Sun Gate.",
        theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
        trollTraps = traps,
        isTrollLevel = true
    )
  }
}
