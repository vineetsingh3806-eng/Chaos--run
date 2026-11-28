package com.example.data

/**
 * Phase 6 — Very Difficult Tier Challenges (ch_17 to ch_20).
 *
 * - ch_17: Clockwork Ambush (Conveyor + Pincer Walls + Chained Spike + Vertical Beam)
 * - ch_18: Skyward Mirage (Launch Pads + Evasive Platforms + Ember Basin + Fleeing Sun Gate)
 * - ch_19: Curse of the Totem (Mirror Curse Totems + Conveyors + Staggered Rune Beams)
 * - ch_20: Domino Cavern (Cascading ON_COIN_COLLECTED / ON_TRAP_ACTIVATED Domino Chains)
 */
object VeryDifficultChallenges17To20 {
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
   * ch_17: "Clockwork Ambush"
   * Mechanics: Reversing conveyor feeding Pincer Chamber + chained ceiling/spike + vertical beams.
   */
  fun buildChallenge17(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    addRow(tiles, nextId, 0, 5, 13f, TileType.CARVED_STONE)
    // Conveyor 6..10 feeds stone Pincer Chamber 11..20 -> disappearing seam 21..22 -> 23
    addRow(tiles, nextId, 11, 20, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 23, 23, 13f, TileType.CARVED_STONE)
    // Checkpoint Sanctuary 25..29 (1-tile hop 23->25)
    addRow(tiles, nextId, 25, 29, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 27.0f)
    // Conveyor 30..34 -> Clockwork Beam & Crusher Gauntlet 35..51
    addRow(tiles, nextId, 35, 51, 13f, TileType.CARVED_STONE)

    val exitX = 48.5f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 8.5f * T, y = 11.4f * T),
        Coin(id = 2, x = 14.2f * T, y = 11.4f * T),
        Coin(id = 3, x = 32.5f * T, y = 11.2f * T),
        Coin(id = 4, x = 38.8f * T, y = 11.2f * T),
        Coin(id = 5, x = 46.0f * T, y = 11.4f * T)
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
            cycleFrames = 96,
            gustFlipIntervalFrames = 96,
            gustForceX = 2.5f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.PINCER_WALLS,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 12.5f * T,
            initialY = 13f * T - 86f,
            width = 3.4f * T,
            height = 86f,
            triggerMinX = 11.8f * T,
            triggerMaxX = 15.8f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            minSafeGapWidth = 56f,
            telegraphDurationFrames = 8,
            moveSpeed = 5.8f,
            cycleFrames = 92,
            killsPlayer = true,
            quipPopup = "CENTER SAFE!"
        ),
        // Combo: Pincer Chamber #1 triggers an instant floor spike past the chamber exit
        TrollTrap(
            id = 3,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 2,
            chainDelayFrames = 30,
            initialX = 17.5f * T,
            initialY = 13f * T - 30f,
            width = 48f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "AFTERSHOCK!"
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 30f * T,
            initialY = 13f * T,
            width = 5f * T,
            height = T,
            cycleFrames = 96,
            phaseOffsetFrames = 40,
            gustFlipIntervalFrames = 96,
            gustForceX = -2.6f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.REVERSING_HAZARD,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 36.0f * T,
            initialY = 13f * T - 38f,
            width = 38f,
            height = 38f,
            triggerMinX = 34.8f * T,
            triggerMaxX = 37.5f * T,
            targetX = 39.2f * T,
            targetY = 13f * T - 38f,
            moveSpeed = 5.4f,
            telegraphDurationFrames = 0,
            cycleFrames = 130,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "REBOUND SAW!"
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 40.0f * T,
            initialY = 13f * T - 86f,
            width = T,
            height = 86f,
            triggerMinX = 41.8f * T,
            triggerMaxX = 44.0f * T,
            targetX = 46.8f * T,
            targetY = 13f * T - 86f,
            moveSpeed = 5.4f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 200,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "OUTRUN IT!"
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 21.1f * T,
            initialY = 13f * T,
            width = 1.8f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "MIND THE GAP!"
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            chainDelayFrames = 0,
            initialX = 46.5f * T,
            initialY = 6.0f * T,
            width = 50f,
            height = 52f,
            targetX = 46.5f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 14.0f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "CLOCKWORK DROP!"
        ),
        // Coin #3 above Belt #2 wakes a fast ceiling block before the Timed Beam
        TrollTrap(
            id = 9,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 3,
            chainDelayFrames = 2,
            initialX = 33.4f * T,
            initialY = 5.8f * T,
            width = 48f,
            height = 48f,
            targetX = 33.4f * T,
            targetY = 13f * T - 48f,
            moveSpeed = 13.8f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "BELT CRUSHER!"
        ),
        // Mid-jump ambush between Pincer #1 and Disappearing Floor #7
        TrollTrap(
            id = 10,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_JUMP_ABOVE,
            initialX = 19.2f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            triggerMinX = 18.2f * T,
            triggerMaxX = 20.2f * T,
            triggerMinY = 8.5f * T,
            triggerMaxY = 12.6f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 60,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "LEAP TRAP!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 17,
        title = "Clockwork Ambush",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 34,
        difficulty = difficulty,
        subtitle = "Belts, Pincers & Stalker Timing",
        description = "Control your momentum exiting belts into pincer chambers, time vertical beams, and sprint ahead of the final crusher.",
        theme = LevelTheme.RUINS_OF_LOST_KING,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_18: "Skyward Mirage"
   * Mechanics: LAUNCH_BETRAYAL_PAD + EVASIVE_PLATFORM + RISING_EMBER_SURGE + FLEEING_SUN_GATE.
   */
  fun buildChallenge18(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    addRow(tiles, nextId, 0, 6, 13f, TileType.CARVED_STONE)
    // High sky islands over Ember Basin #1 (tiles 7..17, sunken floor Y=14.4)
    addRow(tiles, nextId, 7, 17, 14.4f, TileType.SOLID_EARTH)
    tiles.add(GameTile(id = nextId(), x = 8.0f * T, y = 9.8f * T, width = 2.6f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    // Evasive platform at 12.2..14.6 glides to 14.8..17.2 -> disappearing edge 18..19 -> 20..22
    addRow(tiles, nextId, 20, 22, 13f, TileType.CARVED_STONE)
    // Checkpoint Sanctuary 23..27
    addRow(tiles, nextId, 23, 27, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 25.0f)
    // Yo-yo lift over Ember Basin #2 (28..35) -> Fleeing Gate Finale Deck (36..52)
    addRow(tiles, nextId, 28, 35, 14.4f, TileType.SOLID_EARTH)
    tiles.add(GameTile(id = nextId(), x = 32.5f * T, y = 9.8f * T, width = 2.2f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    addRow(tiles, nextId, 36, 52, 13f, TileType.CARVED_STONE)

    val exitX = 41.0f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 9.3f * T, y = 8.4f * T),
        Coin(id = 2, x = 14.0f * T, y = 8.6f * T),
        Coin(id = 3, x = 30.2f * T, y = 8.8f * T),
        Coin(id = 4, x = 44.5f * T, y = 11.4f * T),
        Coin(id = 5, x = 49.0f * T, y = 11.4f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 5.0f * T,
            initialY = 13f * T - 22f,
            width = 48f,
            height = 22f,
            launchImpulseVy = -16.6f,
            telegraphDurationFrames = 12,
            activeDurationFrames = 70,
            cycleFrames = 100,
            maxActivations = 3,
            killsPlayer = true,
            quipPopup = "SKY BOUND!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.RISING_EMBER_SURGE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 7.2f * T,
            initialY = 13.6f * T,
            width = 9.6f * T,
            height = 0.8f * T,
            triggerMinX = 7.2f * T,
            triggerMaxX = 16.5f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 14.8f * T,
            targetY = 11.2f * T,
            telegraphDurationFrames = 9,
            moveSpeed = 4.7f,
            effectDurationFrames = 46,
            cycleFrames = 108,
            killsPlayer = true
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.EVASIVE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 12.0f * T,
            initialY = 10.2f * T,
            width = 2.4f * T,
            height = 24f,
            targetX = 14.8f * T,
            targetY = 10.2f * T,
            moveSpeed = 6.6f,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SLIPPED SIDEWAYS!"
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.YO_YO_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 29.0f * T,
            initialY = 12.2f * T,
            width = 2.4f * T,
            height = 24f,
            targetX = 29.0f * T,
            targetY = 9.2f * T,
            cycleFrames = 104,
            killsPlayer = false
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.RISING_EMBER_SURGE,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 3,
            initialX = 28.2f * T,
            initialY = 13.6f * T,
            width = 6.6f * T,
            height = 0.8f * T,
            targetY = 11.0f * T,
            telegraphDurationFrames = 0,
            moveSpeed = 4.9f,
            effectDurationFrames = 46,
            cycleFrames = 106,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "COIN IGNITION!"
        ),
        // Fleeing Sun Gate hops twice: 41.0 -> 45.5 -> 49.0
        TrollTrap(
            id = 6,
            type = TrollTrapType.FLEEING_SUN_GATE,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = exitX,
            initialY = exitY,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 39.6f * T,
            triggerMaxX = 43.2f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = 45.5f * T,
            targetY = exitY,
            secondaryX = 49.0f * T,
            secondaryY = exitY,
            maxActivations = 2,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "TOO SLOW!"
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 18.0f * T,
            initialY = 13f * T,
            width = 1.8f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "DROP IN!"
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 6,
            chainDelayFrames = 14,
            initialX = 43.2f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "CHASE TRAP!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 18,
        title = "Skyward Mirage",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 34,
        difficulty = difficulty,
        subtitle = "Aerial Springs & Fleeing Gates",
        description = "Chain spring launches onto sky islands above surging embers, then chase down the fleeing Sun Gate across the monsoon terrace.",
        theme = LevelTheme.MONSOON_RUN,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_19: "Curse of the Totem"
   * Mechanics: MIRROR_CURSE_TOTEM + REVERSE_CONVEYOR_BELT + TELEGRAPHED_RUNE_BEAM + FAKE_SAFE_PLATFORM.
   */
  fun buildChallenge19(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    addRow(tiles, nextId, 0, 8, 13f, TileType.CARVED_STONE)
    // Conveyor belt 9..15 -> Stone safe staging 16..19 -> Disappearing edge 20..21 -> 22
    addRow(tiles, nextId, 16, 19, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 22, 22, 13f, TileType.CARVED_STONE)
    // Checkpoint Sanctuary 24..28 (1-tile gap 22->24)
    addRow(tiles, nextId, 24, 28, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 26.0f)
    // Beam + Mirror Totem Terrace 29..42 + Fake Safe edge hop 43..45 + Final Sun Gate Deck 46..52
    addRow(tiles, nextId, 29, 41, 13f, TileType.CARVED_STONE)
    tiles.add(GameTile(id = nextId(), x = 35.0f * T, y = 10.6f * T, width = 2.4f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    addRow(tiles, nextId, 46, 52, 13f, TileType.CARVED_STONE)

    val exitX = 49.0f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 3.8f * T, y = 10.4f * T),
        Coin(id = 2, x = 12.0f * T, y = 11.2f * T),
        Coin(id = 3, x = 36.2f * T, y = 9.2f * T),
        Coin(id = 4, x = 44.0f * T, y = 10.5f * T),
        Coin(id = 5, x = 47.2f * T, y = 11.4f * T)
    )

    val traps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.MIRROR_CURSE_TOTEM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 4.8f * T,
            initialY = 13f * T - 64f,
            width = 40f,
            height = 64f,
            triggerMinX = 4.0f * T,
            triggerMaxX = 6.4f * T,
            telegraphDurationFrames = 0,
            effectDurationFrames = 130,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "CONTROLS INVERTED!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 9f * T,
            initialY = 13f * T,
            width = 7f * T,
            height = T,
            cycleFrames = 98,
            gustFlipIntervalFrames = 98,
            gustForceX = -2.5f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 18.2f * T,
            initialY = 6.2f * T,
            width = 52f,
            height = 52f,
            triggerMinX = 17.2f * T,
            triggerMaxX = 20.0f * T,
            targetX = 18.2f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 14.0f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SKY DROP!"
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.MIRROR_CURSE_TOTEM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 30.5f * T,
            initialY = 13f * T - 64f,
            width = 40f,
            height = 64f,
            triggerMinX = 29.5f * T,
            triggerMaxX = 32.5f * T,
            telegraphDurationFrames = 0,
            effectDurationFrames = 125,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "MIRRORED AGAIN!"
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.TELEGRAPHED_RUNE_BEAM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 33.5f * T,
            initialY = 13f * T - 26f,
            width = 6.5f * T,
            height = 24f,
            telegraphDurationFrames = 9,
            activeDurationFrames = 34,
            cycleFrames = 102,
            killsPlayer = true,
            quipPopup = "LOW BEAM!"
        ),
        // Bridge 42..45: Fake safe platform (sprouts spikes instantly on step!)
        TrollTrap(
            id = 6,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 42.5f * T,
            initialY = 13f * T,
            width = 2.4f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "DON'T LINGER!"
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 20.1f * T,
            initialY = 13f * T,
            width = 1.8f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "HOLLOW STONE!"
        ),
        TrollTrap(
            id = 8,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 1,
            chainDelayFrames = 6,
            initialX = 7.2f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 55,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "TOTEM TAX!"
        ),
        // Coin #3 on the high alcove triggers a ceiling ambush upon drop-down past the low beam
        TrollTrap(
            id = 9,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 3,
            chainDelayFrames = 8,
            initialX = 39.2f * T,
            initialY = 5.8f * T,
            width = 48f,
            height = 48f,
            targetX = 39.2f * T,
            targetY = 13f * T - 48f,
            moveSpeed = 13.8f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "ALCOVE PAYBACK!"
        ),
        // Coin #5 before the Sun Gate snaps a final threshold spike
        TrollTrap(
            id = 10,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            chainDelayFrames = 0,
            initialX = 48.2f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "FINAL CURSE!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 19,
        title = "Curse of the Totem",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 35,
        difficulty = difficulty,
        subtitle = "Mirror Curses & Beam Chambers",
        description = "Stay calm when violet totems invert your steering across reversing belts, rune sightlines, and spiked bridge tiles.",
        theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
        trollTraps = traps,
        isTrollLevel = true
    )
  }

  /**
   * ch_20: "Domino Cavern"
   * Mechanics: Multi-stage cascading ON_COIN_COLLECTED + ON_TRAP_ACTIVATED sequences with safe upper route.
   */
  fun buildChallenge20(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var idSeq = 1
    val nextId = { idSeq++ }

    addRow(tiles, nextId, 0, 12, 13f, TileType.CARVED_STONE)
    // Safe upper arch 8..10, 11..13 collapsing fake safe, 14..16 (Y=10.2) vs Domino floor below
    addRow(tiles, nextId, 8, 10, 10.2f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 14, 16, 10.2f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 17, 23, 13f, TileType.CARVED_STONE)
    // Checkpoint Sanctuary 25..29
    addRow(tiles, nextId, 25, 29, 13f, TileType.CARVED_STONE)
    addCheckpoint(tiles, nextId, 27.0f)
    // Domino Hallway #2: Split Bridge -> Surge -> Beam -> Exit Deck (30..50)
    addRow(tiles, nextId, 30, 32, 13f, TileType.CARVED_STONE)
    addRow(tiles, nextId, 36, 42, 14.4f, TileType.SOLID_EARTH)
    tiles.add(GameTile(id = nextId(), x = 38.0f * T, y = 10.6f * T, width = 2.4f * T, height = 0.6f * T, type = TileType.CARVED_STONE))
    addRow(tiles, nextId, 43, 51, 13f, TileType.CARVED_STONE)

    val exitX = 48.0f * T
    val exitY = 10f * T
    addSunGate(tiles, nextId, exitX, exitY)

    val coins = listOf(
        Coin(id = 1, x = 9.0f * T, y = 11.6f * T),   // Coin bait starts Domino Chain A
        Coin(id = 2, x = 12.0f * T, y = 8.8f * T),
        Coin(id = 3, x = 20.5f * T, y = 11.4f * T),
        Coin(id = 4, x = 39.2f * T, y = 9.2f * T),
        Coin(id = 5, x = 45.5f * T, y = 11.4f * T)
    )

    val traps = listOf(
        // Domino Chain A: Coin 1 -> Trap 1 (Crumble) -> Trap 2 (Beam) -> Trap 3 (Ceiling Block)
        TrollTrap(
            id = 1,
            type = TrollTrapType.DELAYED_CRUMBLE,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 1,
            initialX = 13.0f * T,
            initialY = 13f * T,
            width = 3.0f * T,
            height = T,
            telegraphDurationFrames = 14,
            killsPlayer = false,
            linkedTrapId = 2,
            quipPopup = "CHAIN STARTED!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.REVERSING_HAZARD,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 1,
            chainDelayFrames = 6,
            initialX = 17.2f * T,
            initialY = 13f * T - 38f,
            width = 38f,
            height = 38f,
            targetX = 20.5f * T,
            targetY = 13f * T - 38f,
            moveSpeed = 5.5f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 180,
            killsPlayer = true,
            linkedTrapId = 3,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "DOMINO SAW!"
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 2,
            chainDelayFrames = 8,
            initialX = 20.5f * T,
            initialY = 6.2f * T,
            width = 52f,
            height = 52f,
            telegraphDurationFrames = 0,
            targetX = 20.5f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 13.8f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "DOMINO DROP!"
        ),
        // Domino Chain B: Split Bridge 32.5..35.5 -> Ember Surge -> Final Spike Pop
        TrollTrap(
            id = 4,
            type = TrollTrapType.SPLIT_BRIDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 32.5f * T,
            initialY = 13f * T,
            width = 3.4f * T,
            height = T,
            telegraphDurationFrames = 12,
            killsPlayer = false,
            linkedTrapId = 5,
            quipPopup = "BRIDGE SPLIT!"
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.RISING_EMBER_SURGE,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 4,
            chainDelayFrames = 6,
            initialX = 36.2f * T,
            initialY = 13.6f * T,
            width = 6.4f * T,
            height = 0.8f * T,
            targetY = 11.4f * T,
            telegraphDurationFrames = 0,
            moveSpeed = 4.8f,
            effectDurationFrames = 46,
            cycleFrames = 108,
            killsPlayer = true,
            linkedTrapId = 6,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "DOMINO SURGE!"
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 5,
            chainDelayFrames = 14,
            initialX = 44.5f * T,
            initialY = 13f * T - 30f,
            width = 48f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "FINAL SNAP!"
        ),
        // Upper arch trapdoor under Coin #2 so even the upper bypass tests reaction
        TrollTrap(
            id = 7,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 11.5f * T,
            initialY = 10.2f * T,
            width = 2.0f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "UPPER TRAP!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 20,
        title = "Domino Cavern",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 36,
        difficulty = difficulty,
        subtitle = "Cascading Chain Reactions",
        description = "Every bait coin and split bridge sets off multi-stage domino sequences — take the high arches or ride each chain cleanly.",
        theme = LevelTheme.HOLLOW_OF_ECHOES,
        trollTraps = traps,
        isTrollLevel = true
    )
  }
}
