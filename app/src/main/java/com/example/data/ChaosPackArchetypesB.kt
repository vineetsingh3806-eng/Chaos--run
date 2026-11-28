package com.example.data

import com.example.data.ChaosPackCommon.T
import com.example.data.ChaosPackCommon.TrapBuilder
import com.example.data.ChaosPackCommon.addCheckpoint
import com.example.data.ChaosPackCommon.addCoin
import com.example.data.ChaosPackCommon.addRow
import com.example.data.ChaosPackCommon.addSunGate
import com.example.data.ChaosPackCommon.floorTypeFor
import com.example.data.ChaosPackCommon.pursuitSpeedFor
import com.example.data.ChaosPackCommon.quipOrNull
import com.example.data.ChaosPackCommon.speedFor
import com.example.data.ChaosPackCommon.surpriseCategory
import com.example.data.ChaosPackCommon.telegraphFor

/**
 * Expansion Level Archetypes 5..9 for Phase 11 (ch_37..ch_200).
 */
internal object ChaosPackArchetypesB {

  fun buildArchetype(
      meta: ExpansionChallengeMeta,
      difficulty: DifficultyTier
  ): LevelDefinition {
    return when (meta.archetype) {
      5 -> buildMirageFakeoutSanctum(meta, difficulty)
      6 -> buildEmberSpringTower(meta, difficulty)
      7 -> buildDominoChainCavern(meta, difficulty)
      8 -> buildPincerStalkerCanyon(meta, difficulty)
      else -> buildMasterChaosMashup(meta, difficulty)
    }
  }

  // Archetype 5: Fake Checkpoints, Mirror Curse Totem, Fake Exit Doors & Temp Vanish Floors
  private fun buildMirageFakeoutSanctum(meta: ExpansionChallengeMeta, difficulty: DifficultyTier): LevelDefinition {
    val idx = meta.orderIndex
    val v = (idx / 10) % 4
    val twist = ((idx - 42).coerceAtLeast(0) / 10) % 3
    val tiles = mutableListOf<GameTile>()
    val coins = mutableListOf<Coin>()
    val drums = mutableListOf<BounceDrum>()
    val pendulums = mutableListOf<SwingingPendulum>()
    val tb = TrapBuilder()
    var tileId = 1
    val nextTile = { tileId++ }
    val floor = floorTypeFor(meta.theme)

    addRow(tiles, nextTile, 0, 8, 13f, floor)
    addRow(tiles, nextTile, 12, 19 + v, 13f, floor)
    if (twist == 1) {
      addRow(tiles, nextTile, 14, 17, 9.4f, TileType.CARVED_STONE)
    } else if (twist == 2) {
      drums.add(BounceDrum(id = 1, x = (29.5f + v) * T, y = 13f * T - 22f, launchVy = -15.6f))
      pendulums.add(
          SwingingPendulum(
              id = 1,
              anchorX = (38.0f + v) * T,
              anchorY = 6.5f * T,
              length = 4.2f * T,
              maxAngleRad = 0.72f,
              periodFrames = 148,
              bobRadius = 16f
          )
      )
    }
    addRow(tiles, nextTile, 23 + v, 31 + v, 13f, floor)
    addCheckpoint(tiles, nextTile, (27.5f + v)) // Real checkpoint AFTER the fake one!
    addRow(tiles, nextTile, 35 + v, 43 + v, 13f, floor)
    addRow(tiles, nextTile, 45 + v, 54 + v, 13f, floor)

    val exitX = (50.5f + v) * T
    val exitY = 10f * T
    addSunGate(tiles, nextTile, exitX, exitY)

    addCoin(coins, 1, 10.0f, 11.2f)
    addCoin(coins, 2, if (twist == 1) 15.5f else 21.0f + v, if (twist == 1) 8.0f else 11.2f)
    addCoin(coins, 3, 33.0f + v, 11.0f)
    addCoin(coins, 4, 44.0f + v, 11.2f)

    val cat1 = surpriseCategory(idx, 1)
    val cat2 = surpriseCategory(idx, 2)

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.TEMP_VANISH_REFORM_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 9.0f * T,
            initialY = 13f * T,
            width = 3.0f * T,
            height = T,
            cycleFrames = 80,
            telegraphDurationFrames = telegraphFor(idx, cat1, 12),
            behaviorCategory = cat1,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.FAKE_CHECKPOINT,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 15.0f * T,
            initialY = 12f * T,
            width = T,
            height = T,
            triggerMinX = 13.8f * T,
            triggerMaxX = 16.8f * T,
            triggerMinY = 10.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = telegraphFor(idx, cat2, 12),
            activeDurationFrames = 75,
            behaviorCategory = cat2,
            killsPlayer = true,
            quipPopup = quipOrNull(idx, cat2, true, "FAKE BELL!")
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.MIRROR_CURSE_TOTEM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = (24.5f + v) * T,
            initialY = 11.5f * T,
            width = 40f,
            height = 64f,
            triggerMinX = (23.6f + v) * T,
            triggerMaxX = (26.2f + v) * T,
            triggerMinY = 10.0f * T,
            triggerMaxY = 13.5f * T,
            effectDurationFrames = 110,
            telegraphDurationFrames = telegraphFor(idx, cat1, 14),
            behaviorCategory = cat1,
            killsPlayer = false,
            quipPopup = quipOrNull(idx, cat1, false, "MIND FLIP!")
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.SPLITTING_TWIN_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = (32.0f + v) * T,
            initialY = 13f * T,
            width = 3.0f * T,
            height = T,
            moveSpeed = speedFor(idx, 4.9f),
            cycleFrames = 130,
            telegraphDurationFrames = telegraphFor(idx, cat1, 14),
            behaviorCategory = cat1,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.FAKE_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = (40.5f + v) * T,
            initialY = 10f * T,
            width = 2f * T,
            height = 3f * T,
            triggerMinX = (38.5f + v) * T,
            triggerMaxX = (43.5f + v) * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 12,
            activeDurationFrames = 65,
            behaviorCategory = TrapBehaviorCategory.TELEGRAPHED,
            killsPlayer = true,
            quipPopup = "MIRAGE GATE!"
        )
    )

    if (twist == 1) {
      tb.add(
          TrollTrap(
              id = tb.nextId(),
              type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
              triggerType = TrapTriggerType.ON_STEP,
              initialX = (36.8f + v) * T,
              initialY = 13f * T - 20f,
              width = 52f,
              height = 20f,
              launchImpulseVy = -14.8f,
              telegraphDurationFrames = 0,
              behaviorCategory = TrapBehaviorCategory.SURPRISE,
              killsPlayer = false,
              maxActivations = 3,
              quipPopup = "VAULT!"
          )
      )
    }

    return LevelDefinition(
        worldId = 1,
        levelId = idx,
        title = meta.title,
        subtitle = meta.subtitle,
        description = "False bells and mirror totems twist every step toward the real Sun Gate.",
        theme = meta.theme,
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        bounceDrums = drums,
        pendulums = pendulums,
        trollTraps = tb.traps,
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 50 + (v * 3),
        difficulty = difficulty,
        enableForestSetPieces = true,
        isTrollLevel = true,
        minCamY = -96f,
        maxCamY = 240f
    )
  }

  // Archetype 6: Rising Ember Surge Basins, Timed Blink Platforms & Delayed Crumble
  private fun buildEmberSpringTower(meta: ExpansionChallengeMeta, difficulty: DifficultyTier): LevelDefinition {
    val idx = meta.orderIndex
    val v = (idx / 10) % 4
    val twist = ((idx - 43).coerceAtLeast(0) / 10) % 3
    val tiles = mutableListOf<GameTile>()
    val coins = mutableListOf<Coin>()
    val drums = mutableListOf<BounceDrum>()
    val critters = mutableListOf<ForestCritter>()
    val tb = TrapBuilder()
    var tileId = 1
    val nextTile = { tileId++ }
    val floor = floorTypeFor(meta.theme)

    addRow(tiles, nextTile, 0, 7, 13f, floor)
    addRow(tiles, nextTile, 8, 12, 14.3f, TileType.SOLID_EARTH) // Ember basin floor
    addRow(tiles, nextTile, 13, 17, 11.2f, TileType.CARVED_STONE)
    addRow(tiles, nextTile, 21, 27 + v, 13f, floor)
    addCheckpoint(tiles, nextTile, 24.5f)
    addRow(tiles, nextTile, 33 + v, 38 + v, if (twist == 2) 10.0f else 10.6f, TileType.MOSSY_TIMBER)
    addRow(tiles, nextTile, 41 + v, 51 + v, 13f, floor)

    drums.add(BounceDrum(id = 1, x = (26.5f + v) * T, y = 13f * T - 22f, launchVy = -16.4f))
    if (twist == 2) {
      critters.add(
          ForestCritter(
              id = 1,
              x = (34.5f + v) * T,
              y = 10.0f * T - 26f,
              baseY = 10.0f * T - 26f,
              minX = (33.4f + v) * T,
              maxX = (37.6f + v) * T,
              vx = 1.9f,
              behavior = CritterBehavior.HOPPING
          )
      )
    }

    val exitX = (47.5f + v) * T
    val exitY = 10f * T
    addSunGate(tiles, nextTile, exitX, exitY)

    addCoin(coins, 1, 10.0f, 10.0f)
    addCoin(coins, 2, 19.0f, if (twist == 1) 8.8f else 10.0f)
    addCoin(coins, 3, 30.0f + v, if (twist == 2) 9.0f else 9.6f)
    addCoin(coins, 4, 39.5f + v, 9.8f)

    val cat1 = surpriseCategory(idx, 1)
    val cat2 = surpriseCategory(idx, 2)

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.RISING_EMBER_SURGE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 8.4f * T,
            initialY = 14.2f * T,
            width = 3.8f * T,
            height = 38f,
            triggerMinX = 7.4f * T,
            triggerMaxX = 12.6f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 14.5f * T,
            targetX = 8.4f * T,
            targetY = 12.2f * T,
            moveSpeed = pursuitSpeedFor(idx, 3.9f),
            cycleFrames = 130,
            effectDurationFrames = 44,
            telegraphDurationFrames = telegraphFor(idx, cat1, 12),
            behaviorCategory = cat1,
            killsPlayer = true,
            quipPopup = quipOrNull(idx, cat1, true, "EMBER SURGE!")
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.DELAYED_CRUMBLE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 18.0f * T,
            initialY = 12.0f * T,
            width = 2.6f * T,
            height = T,
            telegraphDurationFrames = telegraphFor(idx, cat2, 16),
            behaviorCategory = cat2,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.TIMED_BLINK_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = (28.5f + v) * T,
            initialY = 11.2f * T,
            width = 3.4f * T,
            height = 0.8f * T,
            cycleFrames = 150,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.TELEGRAPHED,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.REVERSING_HAZARD,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = (45.0f + v) * T,
            initialY = 13f * T - 34f,
            width = 34f,
            height = 34f,
            triggerMinX = (41.2f + v) * T,
            triggerMaxX = (45.8f + v) * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.8f * T,
            targetX = (41.4f + v) * T,
            targetY = 13f * T - 34f,
            moveSpeed = speedFor(idx, 5.2f),
            activeDurationFrames = 300,
            telegraphDurationFrames = telegraphFor(idx, cat1, 12),
            behaviorCategory = cat1,
            killsPlayer = true,
            quipPopup = quipOrNull(idx, cat1, true, "REVERSING SAW!")
        )
    )

    if (twist == 1) {
      tb.add(
          TrollTrap(
              id = tb.nextId(),
              type = TrollTrapType.SPLITTING_TWIN_FLOOR,
              triggerType = TrapTriggerType.ON_STEP,
              initialX = (39.0f + v) * T,
              initialY = 11.4f * T,
              width = 2.4f * T,
              height = T,
              moveSpeed = speedFor(idx, 4.8f),
              cycleFrames = 130,
              telegraphDurationFrames = telegraphFor(idx, cat2, 12),
              behaviorCategory = cat2,
              killsPlayer = false
          )
      )
    }

    return LevelDefinition(
        worldId = 1,
        levelId = idx,
        title = meta.title,
        subtitle = meta.subtitle,
        description = "Vault rising ember basins using blink platforms and crisp double jumps.",
        theme = meta.theme,
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        bounceDrums = drums,
        critters = critters,
        trollTraps = tb.traps,
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 52 + (v * 3),
        difficulty = difficulty,
        enableForestSetPieces = true,
        isTrollLevel = true,
        minCamY = -96f,
        maxCamY = 240f
    )
  }

  // Archetype 7: Domino Chain Cavern (Coin & Step chain reactions)
  private fun buildDominoChainCavern(meta: ExpansionChallengeMeta, difficulty: DifficultyTier): LevelDefinition {
    val idx = meta.orderIndex
    val v = (idx / 10) % 4
    val twist = ((idx - 44).coerceAtLeast(0) / 10) % 3
    val tiles = mutableListOf<GameTile>()
    val coins = mutableListOf<Coin>()
    val drums = mutableListOf<BounceDrum>()
    val tb = TrapBuilder()
    var tileId = 1
    val nextTile = { tileId++ }
    val floor = floorTypeFor(meta.theme)

    addRow(tiles, nextTile, 0, 12, 13f, floor)
    if (twist == 1) {
      addRow(tiles, nextTile, 10, 13, 9.4f, TileType.MOSSY_TIMBER)
    }
    addRow(tiles, nextTile, 15, 22, 13f, floor)
    addRow(tiles, nextTile, 24, 30 + v, 13f, floor)
    addCheckpoint(tiles, nextTile, 26.5f)
    if (twist == 2) {
      drums.add(BounceDrum(id = 1, x = (28.5f + v) * T, y = 13f * T - 22f, launchVy = -15.6f))
      addRow(tiles, nextTile, 33 + v, 35 + v, 9.6f, TileType.CARVED_STONE)
    }
    addRow(tiles, nextTile, 32 + v, 42 + v, 13f, floor)
    addRow(tiles, nextTile, 44 + v, 53 + v, 13f, floor)

    val exitX = (49.5f + v) * T
    val exitY = 10f * T
    addSunGate(tiles, nextTile, exitX, exitY)

    // Coin 1 sits safely above path & triggers a 3-step domino chain!
    addCoin(coins, 1, 9.5f, 11.2f)
    addCoin(coins, 2, 19.0f, if (twist == 1) 8.4f else 11.2f)
    addCoin(coins, 3, 36.5f + v, if (twist == 2) 8.2f else 11.2f)

    val cat1 = surpriseCategory(idx, 1)
    val cat2 = surpriseCategory(idx, 2)

    val t1 = tb.nextId()
    val t2 = tb.nextId()
    val t3 = tb.nextId()

    // Chain 1: Coin #1 -> Opening Ambush Wall -> Reversing Saw -> Fake Safe Platform
    tb.add(
        TrollTrap(
            id = t1,
            type = TrollTrapType.OPENING_AMBUSH_WALL,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 1,
            initialX = 11.0f * T,
            initialY = 11.0f * T,
            width = 1.1f * T,
            height = 2.0f * T,
            targetX = 11.0f * T,
            targetY = 8.4f * T,
            moveSpeed = speedFor(idx, 6.2f),
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            linkedTrapId = t2,
            quipPopup = "CHAIN #1!"
        )
    )

    tb.add(
        TrollTrap(
            id = t2,
            type = TrollTrapType.REVERSING_HAZARD,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = t1,
            initialX = 16.5f * T,
            initialY = 13f * T - 34f,
            width = 34f,
            height = 34f,
            targetX = 9.0f * T,
            targetY = 13f * T - 34f,
            moveSpeed = speedFor(idx, 5.4f),
            activeDurationFrames = 280,
            chainDelayFrames = 8,
            telegraphDurationFrames = telegraphFor(idx, cat1, 10),
            behaviorCategory = cat1,
            killsPlayer = true,
            linkedTrapId = t3,
            quipPopup = quipOrNull(idx, cat1, true, "CHAIN #2!")
        )
    )

    tb.add(
        TrollTrap(
            id = t3,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = t2,
            initialX = 17.5f * T,
            initialY = 13f * T,
            width = 2.0f * T,
            height = T,
            chainDelayFrames = 14,
            telegraphDurationFrames = telegraphFor(idx, cat2, 16),
            behaviorCategory = cat2,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.SLIDING_FLOOR_SHIFT,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 12.2f * T,
            initialY = 13f * T,
            width = 2.6f * T,
            height = T,
            targetX = 13.4f * T,
            targetY = 12.2f * T,
            moveSpeed = speedFor(idx, 5.0f),
            cycleFrames = 140,
            telegraphDurationFrames = telegraphFor(idx, cat1, 12),
            behaviorCategory = cat1,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.YO_YO_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = (30.5f + v) * T,
            initialY = 12.4f * T,
            width = 2.8f * T,
            height = 0.8f * T,
            targetX = (30.5f + v) * T,
            targetY = 9.8f * T,
            cycleFrames = 130,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.TELEGRAPHED,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.REVERSING_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = (42.5f + v) * T,
            initialY = 13f * T,
            width = 2.4f * T,
            height = 0.9f * T,
            targetX = (44.0f + v) * T,
            targetY = 13f * T,
            moveSpeed = speedFor(idx, 5.2f),
            maxActivations = 3,
            telegraphDurationFrames = telegraphFor(idx, cat2, 10),
            behaviorCategory = cat2,
            killsPlayer = false
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = idx,
        title = meta.title,
        subtitle = meta.subtitle,
        description = "Every coin and step kicks off a domino cascade—anticipate the second gag!",
        theme = meta.theme,
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        bounceDrums = drums,
        trollTraps = tb.traps,
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 52 + (v * 3),
        difficulty = difficulty,
        enableForestSetPieces = true,
        isTrollLevel = true,
        minCamY = -96f,
        maxCamY = 240f
    )
  }

  // Archetype 8: Pincer Jaws, Stalker Wall, Horizontal Floor Rune Beam (NO tall spike-gate walls!)
  private fun buildPincerStalkerCanyon(meta: ExpansionChallengeMeta, difficulty: DifficultyTier): LevelDefinition {
    val idx = meta.orderIndex
    val v = (idx / 10) % 4
    val twist = ((idx - 45).coerceAtLeast(0) / 10) % 3
    val tiles = mutableListOf<GameTile>()
    val coins = mutableListOf<Coin>()
    val drums = mutableListOf<BounceDrum>()
    val tb = TrapBuilder()
    var tileId = 1
    val nextTile = { tileId++ }
    val floor = floorTypeFor(meta.theme)

    addRow(tiles, nextTile, 0, 14, 13f, floor)
    if (twist == 1) {
      addRow(tiles, nextTile, 10, 13, 8.8f, TileType.MOSSY_TIMBER)
      drums.add(BounceDrum(id = 1, x = 7.4f * T, y = 13f * T - 22f, launchVy = -15.4f))
    }
    addRow(tiles, nextTile, 17, 25 + v, 13f, floor)
    addCheckpoint(tiles, nextTile, 21.5f)
    addRow(tiles, nextTile, 28 + v, 39 + v, 13f, floor)
    addRow(tiles, nextTile, 42 + v, 53 + v, 13f, floor)

    val exitX = (49.5f + v) * T
    val exitY = 10f * T
    addSunGate(tiles, nextTile, exitX, exitY)

    addCoin(coins, 1, if (twist == 1) 11.5f else 15.5f, if (twist == 1) 7.4f else 11.2f)
    addCoin(coins, 2, 26.5f + v, 11.2f)
    addCoin(coins, 3, 35.0f + v, 10.6f)
    addCoin(coins, 4, 40.5f + v, 11.2f)

    val cat1 = surpriseCategory(idx, 1)
    val cat2 = surpriseCategory(idx, 2)

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.PINCER_WALLS,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 9.0f * T,
            initialY = 10.2f * T,
            width = 4.2f * T,
            height = 2.8f * T,
            triggerMinX = 8.4f * T,
            triggerMaxX = 13.8f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            minSafeGapWidth = 60f,
            moveSpeed = speedFor(idx, 4.9f),
            cycleFrames = 140,
            telegraphDurationFrames = telegraphFor(idx, cat1, 14),
            behaviorCategory = cat1,
            killsPlayer = true,
            quipPopup = quipOrNull(idx, cat1, true, "PINCER JAWS!")
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.SPLITTING_TWIN_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 14.2f * T,
            initialY = 13f * T,
            width = 2.8f * T,
            height = T,
            moveSpeed = speedFor(idx, 4.8f),
            cycleFrames = 130,
            telegraphDurationFrames = telegraphFor(idx, cat2, 12),
            behaviorCategory = cat2,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = (28.0f + v) * T,
            initialY = 10.2f * T,
            width = 1.2f * T,
            height = 2.8f * T,
            triggerMinX = (28.5f + v) * T,
            triggerMaxX = (34.5f + v) * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            targetX = (36.5f + v) * T,
            targetY = 10.2f * T,
            moveSpeed = pursuitSpeedFor(idx, 4.2f),
            telegraphDurationFrames = telegraphFor(idx, cat1, 12),
            behaviorCategory = cat1,
            killsPlayer = true,
            quipPopup = quipOrNull(idx, cat1, true, "IT FOLLOWS!")
        )
    )

    // Low horizontal floor beam (easily jumped / double-jumped, NOT a vertical gate)
    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.TELEGRAPHED_RUNE_BEAM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = (43.5f + v) * T,
            initialY = 13f * T - 24f,
            width = 2.6f * T,
            height = 24f,
            cycleFrames = 130,
            activeDurationFrames = 36,
            telegraphDurationFrames = 14,
            behaviorCategory = TrapBehaviorCategory.TELEGRAPHED,
            killsPlayer = true
        )
    )

    if (twist == 2) {
      tb.add(
          TrollTrap(
              id = tb.nextId(),
              type = TrollTrapType.TEMP_VANISH_REFORM_FLOOR,
              triggerType = TrapTriggerType.ON_STEP,
              initialX = (25.4f + v) * T,
              initialY = 12.6f * T,
              width = 2.6f * T,
              height = T,
              cycleFrames = 75,
              telegraphDurationFrames = telegraphFor(idx, cat2, 12),
              behaviorCategory = cat2,
              killsPlayer = false
          )
      )
    }

    return LevelDefinition(
        worldId = 1,
        levelId = idx,
        title = meta.title,
        subtitle = meta.subtitle,
        description = "Sprint past closing stone jaws and leap low rune beams with your double jump.",
        theme = meta.theme,
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        bounceDrums = drums,
        trollTraps = tb.traps,
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 52 + (v * 3),
        difficulty = difficulty,
        enableForestSetPieces = true,
        isTrollLevel = true,
        minCamY = -96f,
        maxCamY = 240f
    )
  }

  // Archetype 9: Master Chaos Mashup combining dual paths, new traps, and finale gag
  private fun buildMasterChaosMashup(meta: ExpansionChallengeMeta, difficulty: DifficultyTier): LevelDefinition {
    val idx = meta.orderIndex
    val v = (idx / 10) % 4
    val twist = ((idx - 46).coerceAtLeast(0) / 10) % 3
    val tiles = mutableListOf<GameTile>()
    val coins = mutableListOf<Coin>()
    val drums = mutableListOf<BounceDrum>()
    val pendulums = mutableListOf<SwingingPendulum>()
    val tb = TrapBuilder()
    var tileId = 1
    val nextTile = { tileId++ }
    val floor = floorTypeFor(meta.theme)

    addRow(tiles, nextTile, 0, 7, 13f, floor)
    addRow(tiles, nextTile, 11, 16, if (twist == 2) 9.6f else 10.2f, TileType.MOSSY_TIMBER) // Upper branch
    addRow(tiles, nextTile, 11, 16, 13.4f, floor)                // Lower branch
    addRow(tiles, nextTile, 21, 27 + v, 13f, floor)
    addCheckpoint(tiles, nextTile, 24.5f)
    addRow(tiles, nextTile, 33 + v, 40 + v, 13f, floor)
    addRow(tiles, nextTile, 45 + v, 55 + v, 13f, floor)

    drums.add(BounceDrum(id = 1, x = 6.5f * T, y = 13f * T - 22f, launchVy = -15.6f))
    if (twist == 1) {
      drums.add(BounceDrum(id = 2, x = (26.5f + v) * T, y = 13f * T - 22f, launchVy = -15.2f))
    } else if (twist == 2) {
      pendulums.add(
          SwingingPendulum(
              id = 1,
              anchorX = (42.5f + v) * T,
              anchorY = 6.4f * T,
              length = 4.2f * T,
              maxAngleRad = 0.7f,
              periodFrames = 142,
              bobRadius = 16f
          )
      )
    }

    val exitX = (51.5f + v) * T
    val exitY = 10f * T
    addSunGate(tiles, nextTile, exitX, exitY)

    addCoin(coins, 1, 13.5f, if (twist == 2) 8.2f else 8.8f)
    addCoin(coins, 2, 13.5f, 11.8f)
    addCoin(coins, 3, 29.5f + v, if (twist == 1) 9.2f else 11.0f)
    addCoin(coins, 4, 42.5f + v, 10.6f)

    val cat1 = surpriseCategory(idx, 1)
    val cat2 = surpriseCategory(idx, 2)
    val cat3 = surpriseCategory(idx, 3)

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.SLIDING_FLOOR_SHIFT,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 8.0f * T,
            initialY = 13f * T,
            width = 2.8f * T,
            height = T,
            targetX = 9.4f * T,
            targetY = 13f * T,
            moveSpeed = speedFor(idx, 5.4f),
            cycleFrames = 130,
            telegraphDurationFrames = telegraphFor(idx, cat1, 12),
            behaviorCategory = cat1,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 13.0f * T,
            initialY = 10.2f * T,
            width = 2.0f * T,
            height = T,
            telegraphDurationFrames = telegraphFor(idx, cat2, 8),
            behaviorCategory = cat2,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.TEMP_VANISH_REFORM_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 17.5f * T,
            initialY = 13f * T,
            width = 3.0f * T,
            height = T,
            cycleFrames = 75,
            telegraphDurationFrames = telegraphFor(idx, cat3, 12),
            behaviorCategory = cat3,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.REVERSING_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = (28.5f + v) * T,
            initialY = 12.6f * T,
            width = 3.2f * T,
            height = 0.8f * T,
            targetX = (32.0f + v) * T,
            targetY = 12.6f * T,
            moveSpeed = speedFor(idx, 5.2f),
            maxActivations = 3,
            telegraphDurationFrames = telegraphFor(idx, cat1, 10),
            behaviorCategory = cat1,
            killsPlayer = false
        )
    )

    val openId = tb.nextId()
    val sawId = tb.nextId()
    tb.add(
        TrollTrap(
            id = openId,
            type = TrollTrapType.OPENING_AMBUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = (35.5f + v) * T,
            initialY = 11.0f * T,
            width = 1.2f * T,
            height = 2.0f * T,
            triggerMinX = (33.5f + v) * T,
            triggerMaxX = (35.4f + v) * T,
            triggerMinY = 10.0f * T,
            triggerMaxY = 13.5f * T,
            targetX = (35.5f + v) * T,
            targetY = 8.4f * T,
            moveSpeed = speedFor(idx, 6.4f),
            telegraphDurationFrames = telegraphFor(idx, cat2, 10),
            behaviorCategory = cat2,
            killsPlayer = false,
            linkedTrapId = sawId
        )
    )

    tb.add(
        TrollTrap(
            id = sawId,
            type = TrollTrapType.REVERSING_HAZARD,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = openId,
            initialX = (39.0f + v) * T,
            initialY = 13f * T - 34f,
            width = 34f,
            height = 34f,
            targetX = (33.2f + v) * T,
            targetY = 13f * T - 34f,
            moveSpeed = speedFor(idx, 5.6f),
            activeDurationFrames = 320,
            telegraphDurationFrames = telegraphFor(idx, cat3, 10),
            behaviorCategory = cat3,
            killsPlayer = true,
            quipPopup = quipOrNull(idx, cat3, true, "SURPRISE SAW!")
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.SPLITTING_TWIN_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = (41.0f + v) * T,
            initialY = 13f * T,
            width = 3.6f * T,
            height = T,
            moveSpeed = speedFor(idx, 5.0f),
            cycleFrames = 130,
            telegraphDurationFrames = telegraphFor(idx, cat1, 14),
            behaviorCategory = cat1,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.FLEEING_SUN_GATE,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = exitX,
            initialY = exitY,
            width = 2f * T,
            height = 3f * T,
            triggerMinX = exitX - 1.2f * T,
            triggerMaxX = exitX + 2.4f * T,
            triggerMinY = exitY - T,
            triggerMaxY = exitY + 3.5f * T,
            targetX = (47.5f + v) * T,
            targetY = exitY,
            secondaryX = exitX,
            secondaryY = exitY,
            maxActivations = 2,
            telegraphDurationFrames = 8,
            behaviorCategory = TrapBehaviorCategory.TELEGRAPHED,
            killsPlayer = false,
            quipPopup = if (idx == 200) "CROWN DODGE!" else "ALMOST!"
        )
    )

    if (twist == 1) {
      tb.add(
          TrollTrap(
              id = tb.nextId(),
              type = TrollTrapType.MIRROR_CURSE_TOTEM,
              triggerType = TrapTriggerType.ON_PROXIMITY_X,
              initialX = (46.0f + v) * T,
              initialY = 11.5f * T,
              width = 40f,
              height = 64f,
              triggerMinX = (45.2f + v) * T,
              triggerMaxX = (47.0f + v) * T,
              triggerMinY = 10.0f * T,
              triggerMaxY = 13.5f * T,
              effectDurationFrames = 90,
              telegraphDurationFrames = telegraphFor(idx, cat2, 14),
              behaviorCategory = cat2,
              killsPlayer = false,
              quipPopup = quipOrNull(idx, cat2, false, "MIND FLIP!")
          )
      )
    }

    return LevelDefinition(
        worldId = 1,
        levelId = idx,
        title = meta.title,
        subtitle = meta.subtitle,
        description = "Every chaos trick united—read the terrain and double-jump to glory.",
        theme = meta.theme,
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        bounceDrums = drums,
        pendulums = pendulums,
        trollTraps = tb.traps,
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 56 + (v * 3),
        difficulty = difficulty,
        enableForestSetPieces = true,
        isTrollLevel = true,
        minCamY = -96f,
        maxCamY = 240f
    )
  }
}
