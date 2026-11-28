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
 * Expansion Level Archetypes 0..4 for Phase 11 (ch_37..ch_200).
 * Every layout varies segment spans, heights, traps, and puzzle rhythms deterministically.
 */
internal object ChaosPackArchetypesA {

  fun buildArchetype(
      meta: ExpansionChallengeMeta,
      difficulty: DifficultyTier
  ): LevelDefinition {
    return when (meta.archetype) {
      0 -> buildShiftingGauntlet(meta, difficulty)
      1 -> buildTwinSplitBridges(meta, difficulty)
      2 -> buildDoubleJumpCanopy(meta, difficulty)
      3 -> buildAmbushWallVault(meta, difficulty)
      else -> buildConveyorGaleRun(meta, difficulty)
    }
  }

  // Archetype 0: Sliding floors, vanish-reform bridge, reversing hazard & canopy shortcut
  private fun buildShiftingGauntlet(meta: ExpansionChallengeMeta, difficulty: DifficultyTier): LevelDefinition {
    val idx = meta.orderIndex
    val v = (idx / 10) % 4
    val twist = ((idx - 37).coerceAtLeast(0) / 10) % 3
    val tiles = mutableListOf<GameTile>()
    val coins = mutableListOf<Coin>()
    val drums = mutableListOf<BounceDrum>()
    val tb = TrapBuilder()
    var tileId = 1
    val nextTile = { tileId++ }
    val floor = floorTypeFor(meta.theme)

    addRow(tiles, nextTile, 0, 7, 13f, floor)
    addRow(tiles, nextTile, 12, 16 + v, 13f, floor)
    addRow(tiles, nextTile, 13, 15, 9.8f, TileType.MOSSY_TIMBER) // Upper double-jump route
    addRow(tiles, nextTile, 21 + v, 27 + v, 13f, floor)
    addCheckpoint(tiles, nextTile, (24 + v).toFloat())
    if (twist == 1) {
      addRow(tiles, nextTile, 30 + v, 32 + v, 9.6f, TileType.CARVED_STONE)
      drums.add(BounceDrum(id = 1, x = (26.5f + v) * T, y = 13f * T - 22f, launchVy = -15.8f))
    } else if (twist == 2) {
      addRow(tiles, nextTile, 38 + v, 40 + v, 9.8f, TileType.MOSSY_TIMBER)
      drums.add(BounceDrum(id = 1, x = (34.5f + v) * T, y = 13f * T - 22f, launchVy = -15.4f))
    }
    addRow(tiles, nextTile, 33 + v, 40 + v, 13f, floor)
    addRow(tiles, nextTile, 45 + v, 53 + v, 13f, floor)

    val exitX = (49.5f + v) * T
    val exitY = 10f * T
    addSunGate(tiles, nextTile, exitX, exitY)

    addCoin(coins, 1, 9.5f, 11.4f)
    addCoin(coins, 2, 14.0f, 8.4f)
    addCoin(coins, 3, 29.5f + v, if (twist == 1) 8.2f else 11.2f)
    addCoin(coins, 4, 42.5f + v, if (twist == 2) 8.4f else 10.6f)

    val cat1 = surpriseCategory(idx, 1)
    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.SLIDING_FLOOR_SHIFT,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 8.0f * T,
            initialY = 13f * T,
            width = 3.0f * T,
            height = T,
            targetX = 9.4f * T,
            targetY = 13f * T,
            moveSpeed = speedFor(idx, 5.4f),
            cycleFrames = 140,
            telegraphDurationFrames = telegraphFor(idx, cat1, 14),
            behaviorCategory = cat1,
            killsPlayer = false,
            quipPopup = quipOrNull(idx, cat1, false, "FLOOR SLIP!")
        )
    )

    val cat2 = surpriseCategory(idx, 2)
    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.REVERSING_HAZARD,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = (16.5f + v) * T,
            initialY = 13f * T - 34f,
            width = 34f,
            height = 34f,
            triggerMinX = 12.5f * T,
            triggerMaxX = (17.5f + v) * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 14.0f * T,
            targetX = 12.2f * T,
            targetY = 13f * T - 34f,
            moveSpeed = speedFor(idx, 5.2f),
            activeDurationFrames = 320,
            telegraphDurationFrames = telegraphFor(idx, cat2, 12),
            behaviorCategory = cat2,
            killsPlayer = true,
            quipPopup = quipOrNull(idx, cat2, true, "BOOMERANG SAW!")
        )
    )

    val cat3 = surpriseCategory(idx, 3)
    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.TEMP_VANISH_REFORM_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = (17.5f + v) * T,
            initialY = 13f * T,
            width = 3.0f * T,
            height = T,
            cycleFrames = 75,
            telegraphDurationFrames = telegraphFor(idx, cat3, 12),
            behaviorCategory = cat3,
            killsPlayer = false,
            quipPopup = quipOrNull(idx, cat3, false, "BRB!")
        )
    )

    val cat4 = surpriseCategory(idx, 4)
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
            moveSpeed = speedFor(idx, 5.0f),
            maxActivations = 3,
            telegraphDurationFrames = telegraphFor(idx, cat4, 10),
            behaviorCategory = cat4,
            killsPlayer = false,
            quipPopup = quipOrNull(idx, cat4, false, "WRONG WAY!")
        )
    )

    val cat5 = surpriseCategory(idx, 5)
    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = (36.0f + v) * T,
            initialY = 8.4f * T,
            width = 52f,
            height = 52f,
            triggerMinX = (34.8f + v) * T,
            triggerMaxX = (37.6f + v) * T,
            triggerMinY = 8.0f * T,
            triggerMaxY = 14.0f * T,
            targetX = (36.0f + v) * T,
            targetY = 13f * T - 52f,
            moveSpeed = speedFor(idx, 9.2f),
            telegraphDurationFrames = telegraphFor(idx, cat5, 12),
            behaviorCategory = cat5,
            killsPlayer = true,
            quipPopup = quipOrNull(idx, cat5, true, "WATCH ABOVE!")
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
            moveSpeed = speedFor(idx, 4.8f),
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
            targetX = (46.2f + v) * T,
            targetY = exitY,
            secondaryX = exitX,
            secondaryY = exitY,
            maxActivations = if (idx % 2 == 0) 2 else 1,
            telegraphDurationFrames = 8,
            behaviorCategory = TrapBehaviorCategory.TELEGRAPHED,
            killsPlayer = false,
            quipPopup = "OVER HERE!"
        )
    )

    if (twist == 2) {
      tb.add(
          TrollTrap(
              id = tb.nextId(),
              type = TrollTrapType.PHANTOM_LEDGE,
              triggerType = TrapTriggerType.ON_STEP,
              initialX = (19.4f + v) * T,
              initialY = 10.6f * T,
              width = 2.0f * T,
              height = T,
              cycleFrames = 85,
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
        description = "Shift across sliding timber and double-jump past boomerang saws.",
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
        parTimeSeconds = 48 + (v * 3),
        difficulty = difficulty,
        enableForestSetPieces = true,
        isTrollLevel = true,
        minCamY = -96f,
        maxCamY = 240f
    )
  }

  // Archetype 1: Dual split sky-bridges & splitting twin floors with surprise bait spikes
  private fun buildTwinSplitBridges(meta: ExpansionChallengeMeta, difficulty: DifficultyTier): LevelDefinition {
    val idx = meta.orderIndex
    val v = (idx / 10) % 4
    val twist = ((idx - 38).coerceAtLeast(0) / 10) % 3
    val tiles = mutableListOf<GameTile>()
    val coins = mutableListOf<Coin>()
    val drums = mutableListOf<BounceDrum>()
    val tb = TrapBuilder()
    var tileId = 1
    val nextTile = { tileId++ }
    val floor = floorTypeFor(meta.theme)

    addRow(tiles, nextTile, 0, 6, 13f, floor)
    addRow(tiles, nextTile, 7, 9, 10.6f, TileType.MOSSY_TIMBER)
    addRow(tiles, nextTile, 14, 19 + v, if (twist == 2) 9.6f else 10.2f, TileType.CARVED_STONE) // Upper deck
    addRow(tiles, nextTile, 11, 18 + v, 13.4f, floor)                // Lower deck
    addRow(tiles, nextTile, 20 + v, 25 + v, 13f, floor)
    addCheckpoint(tiles, nextTile, (22.5f + v))
    if (twist == 1) {
      drums.add(BounceDrum(id = 1, x = (18.2f + v) * T, y = 13.4f * T - 22f, launchVy = -15.6f))
      addRow(tiles, nextTile, 27 + v, 29 + v, 9.6f, TileType.MOSSY_TIMBER)
    }
    addRow(tiles, nextTile, 31 + v, 37 + v, 13f, floor)
    addRow(tiles, nextTile, 41 + v, 50 + v, 13f, floor)

    val exitX = (47f + v) * T
    val exitY = 10f * T
    addSunGate(tiles, nextTile, exitX, exitY)

    addCoin(coins, 1, 15.5f, if (twist == 2) 8.2f else 8.8f)
    addCoin(coins, 2, 14.5f, 12.0f)
    addCoin(coins, 3, 28.0f + v, if (twist == 1) 8.2f else 11.4f)
    addCoin(coins, 4, 39.0f + v, 11.0f)

    val cat1 = surpriseCategory(idx, 1)
    val cat2 = surpriseCategory(idx, 2)
    val cat3 = surpriseCategory(idx, 3)

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.SPLITTING_TWIN_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 10.0f * T,
            initialY = 10.2f * T,
            width = 3.6f * T,
            height = T,
            moveSpeed = speedFor(idx, 4.9f),
            cycleFrames = 140,
            telegraphDurationFrames = telegraphFor(idx, cat1, 14),
            behaviorCategory = cat1,
            killsPlayer = false,
            quipPopup = quipOrNull(idx, cat1, false, "SPLIT!")
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.PHANTOM_LEDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 16.5f * T,
            initialY = 10.2f * T,
            width = 2.0f * T,
            height = T,
            cycleFrames = 90,
            telegraphDurationFrames = telegraphFor(idx, cat2, 12),
            behaviorCategory = cat2,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 14.0f * T,
            initialY = 13.4f * T - 32f,
            width = 48f,
            height = 32f,
            triggerMinX = 13.0f * T,
            triggerMaxX = 15.4f * T,
            triggerMinY = 11.4f * T,
            triggerMaxY = 13.8f * T,
            telegraphDurationFrames = telegraphFor(idx, cat3, 15),
            activeDurationFrames = 80,
            behaviorCategory = cat3,
            killsPlayer = true,
            quipPopup = quipOrNull(idx, cat3, true, "GREED TRAP!")
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.SLIDING_FLOOR_SHIFT,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = (26.5f + v) * T,
            initialY = 13f * T,
            width = 3.5f * T,
            height = T,
            targetX = (28.0f + v) * T,
            targetY = 11.8f * T,
            moveSpeed = speedFor(idx, 5.2f),
            cycleFrames = 130,
            telegraphDurationFrames = telegraphFor(idx, cat1, 14),
            behaviorCategory = cat1,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.TEMP_VANISH_REFORM_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = (37.5f + v) * T,
            initialY = 13f * T,
            width = 3.0f * T,
            height = T,
            cycleFrames = 75,
            telegraphDurationFrames = telegraphFor(idx, cat2, 12),
            behaviorCategory = cat2,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.FAKE_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = (33.5f + v) * T,
            initialY = 10f * T,
            width = 2f * T,
            height = 3f * T,
            triggerMinX = (32.0f + v) * T,
            triggerMaxX = (37.0f + v) * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 12,
            activeDurationFrames = 60,
            behaviorCategory = TrapBehaviorCategory.TELEGRAPHED,
            killsPlayer = true,
            quipPopup = "DECOY GATE!"
        )
    )

    if (twist == 2) {
      tb.add(
          TrollTrap(
              id = tb.nextId(),
              type = TrollTrapType.MIRROR_CURSE_TOTEM,
              triggerType = TrapTriggerType.ON_PROXIMITY_X,
              initialX = (32.0f + v) * T,
              initialY = 11.5f * T,
              width = 40f,
              height = 64f,
              triggerMinX = (31.2f + v) * T,
              triggerMaxX = (33.0f + v) * T,
              triggerMinY = 10.0f * T,
              triggerMaxY = 13.5f * T,
              effectDurationFrames = 95,
              telegraphDurationFrames = telegraphFor(idx, cat1, 14),
              behaviorCategory = cat1,
              killsPlayer = false,
              quipPopup = quipOrNull(idx, cat1, false, "MIND FLIP!")
          )
      )
    }

    return LevelDefinition(
        worldId = 1,
        levelId = idx,
        title = meta.title,
        subtitle = meta.subtitle,
        description = "Take the high timber or brave the split bridge—neither stays put.",
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
        parTimeSeconds = 50 + (v * 3),
        difficulty = difficulty,
        enableForestSetPieces = true,
        isTrollLevel = true,
        minCamY = -96f,
        maxCamY = 240f
    )
  }

  // Archetype 2: Vertical Double-Jump Canopy Ascent & Bounce Drum Shortcuts
  private fun buildDoubleJumpCanopy(meta: ExpansionChallengeMeta, difficulty: DifficultyTier): LevelDefinition {
    val idx = meta.orderIndex
    val v = (idx / 10) % 4
    val twist = ((idx - 39).coerceAtLeast(0) / 10) % 3
    val tiles = mutableListOf<GameTile>()
    val coins = mutableListOf<Coin>()
    val drums = mutableListOf<BounceDrum>()
    val critters = mutableListOf<ForestCritter>()
    val tb = TrapBuilder()
    var tileId = 1
    val nextTile = { tileId++ }
    val floor = floorTypeFor(meta.theme)

    addRow(tiles, nextTile, 0, 7, 13f, floor)
    addRow(tiles, nextTile, 9, 13, 10.2f, TileType.MOSSY_TIMBER)
    addRow(tiles, nextTile, 15, 20, if (twist == 1) 7.2f else 7.6f, TileType.CARVED_STONE) // Requires double jump or drum
    addRow(tiles, nextTile, 22, 27 + v, 10.6f, TileType.MOSSY_TIMBER)
    addRow(tiles, nextTile, 24, 30 + v, 13f, floor)
    addCheckpoint(tiles, nextTile, (28.0f + v))
    if (twist == 1) {
      addRow(tiles, nextTile, 33 + v, 35 + v, 8.8f, TileType.MOSSY_TIMBER)
    }
    addRow(tiles, nextTile, 36 + v, 42 + v, 11.4f, TileType.CARVED_STONE)
    addRow(tiles, nextTile, 44 + v, 53 + v, 13f, floor)

    drums.add(BounceDrum(id = 1, x = 6.2f * T, y = 13f * T - 22f, launchVy = -16.8f))
    if (twist == 2) {
      critters.add(
          ForestCritter(
              id = 1,
              x = (38.0f + v) * T,
              y = 11.4f * T - 26f,
              baseY = 11.4f * T - 26f,
              minX = (36.4f + v) * T,
              maxX = (41.6f + v) * T,
              vx = 2.1f,
              behavior = CritterBehavior.PATROL
          )
      )
    }

    val exitX = (49.5f + v) * T
    val exitY = 10f * T
    addSunGate(tiles, nextTile, exitX, exitY)

    addCoin(coins, 1, 11.0f, 8.8f)
    addCoin(coins, 2, 17.5f, if (twist == 1) 5.8f else 6.2f)
    addCoin(coins, 3, if (twist == 1) 34.0f + v else 33.0f + v, if (twist == 1) 7.4f else 9.8f)
    addCoin(coins, 4, 39.0f + v, 9.8f)

    val cat1 = surpriseCategory(idx, 1)
    val cat2 = surpriseCategory(idx, 2)
    val cat3 = surpriseCategory(idx, 3)

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.EVASIVE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 11.0f * T,
            initialY = 10.2f * T,
            width = 2.4f * T,
            height = T,
            targetX = 13.0f * T,
            targetY = 9.0f * T,
            moveSpeed = speedFor(idx, 4.9f),
            telegraphDurationFrames = telegraphFor(idx, cat1, 10),
            behaviorCategory = cat1,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 17.5f * T,
            initialY = 4.2f * T,
            width = 50f,
            height = 50f,
            triggerMinX = 16.4f * T,
            triggerMaxX = 18.8f * T,
            triggerMinY = 4.0f * T,
            triggerMaxY = 8.4f * T,
            targetX = 17.5f * T,
            targetY = 7.6f * T - 50f,
            moveSpeed = speedFor(idx, 9.2f),
            telegraphDurationFrames = telegraphFor(idx, cat2, 12),
            behaviorCategory = cat2,
            killsPlayer = true,
            quipPopup = quipOrNull(idx, cat2, true, "SKY FALL!")
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.LAUNCH_BETRAYAL_PAD,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = (25.0f + v) * T,
            initialY = 13f * T - 20f,
            width = 52f,
            height = 20f,
            launchImpulseVy = -14.8f,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            maxActivations = 3,
            quipPopup = "DOUBLE UP!"
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.REVERSING_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = (31.5f + v) * T,
            initialY = 12.0f * T,
            width = 3.2f * T,
            height = 0.8f * T,
            targetX = (35.5f + v) * T,
            targetY = 11.4f * T,
            moveSpeed = speedFor(idx, 5.2f),
            maxActivations = 3,
            telegraphDurationFrames = telegraphFor(idx, cat1, 10),
            behaviorCategory = cat1,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.TRAILING_SPIKE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = (45.0f + v) * T,
            initialY = 13f * T - 32f,
            width = 40f,
            height = 32f,
            triggerMinX = (44.0f + v) * T,
            triggerMaxX = (48.5f + v) * T,
            triggerMinY = 10.5f * T,
            triggerMaxY = 13.5f * T,
            targetX = (51.0f + v) * T,
            targetY = 13f * T - 32f,
            moveSpeed = pursuitSpeedFor(idx, 4.1f),
            activeDurationFrames = 260,
            telegraphDurationFrames = telegraphFor(idx, cat3, 12),
            behaviorCategory = cat3,
            killsPlayer = true,
            quipPopup = quipOrNull(idx, cat3, true, "DOUBLE-JUMP IT!")
        )
    )

    if (twist == 2) {
      tb.add(
          TrollTrap(
              id = tb.nextId(),
              type = TrollTrapType.SPLITTING_TWIN_FLOOR,
              triggerType = TrapTriggerType.ON_STEP,
              initialX = (42.5f + v) * T,
              initialY = 12.2f * T,
              width = 2.6f * T,
              height = T,
              moveSpeed = speedFor(idx, 4.9f),
              cycleFrames = 130,
              telegraphDurationFrames = telegraphFor(idx, cat1, 12),
              behaviorCategory = cat1,
              killsPlayer = false
          )
      )
    }

    return LevelDefinition(
        worldId = 1,
        levelId = idx,
        title = meta.title,
        subtitle = meta.subtitle,
        description = "Chain bounce drums and double jumps across high stone canopies.",
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
        minCamY = -120f,
        maxCamY = 240f
    )
  }

  // Archetype 3: Opening Ambush Walls releasing Reversing Saws + Betrayal Switch Locked Gate
  private fun buildAmbushWallVault(meta: ExpansionChallengeMeta, difficulty: DifficultyTier): LevelDefinition {
    val idx = meta.orderIndex
    val v = (idx / 10) % 4
    val twist = ((idx - 40).coerceAtLeast(0) / 10) % 3
    val tiles = mutableListOf<GameTile>()
    val coins = mutableListOf<Coin>()
    val drums = mutableListOf<BounceDrum>()
    val tb = TrapBuilder()
    var tileId = 1
    val nextTile = { tileId++ }
    val floor = floorTypeFor(meta.theme)

    addRow(tiles, nextTile, 0, 17, 13f, floor)
    addRow(tiles, nextTile, 10, 14, 9.6f, TileType.CARVED_STONE) // Optional upper bypass
    addRow(tiles, nextTile, 21, 29 + v, 13f, floor)
    addCheckpoint(tiles, nextTile, 24.5f)
    if (twist == 1) {
      drums.add(BounceDrum(id = 1, x = 8.8f * T, y = 13f * T - 22f, launchVy = -15.6f))
    }
    addRow(tiles, nextTile, 31 + v, 43 + v, 13f, floor)
    addRow(tiles, nextTile, 45 + v, 53 + v, 13f, floor)

    val exitX = (49.5f + v) * T
    val exitY = 10f * T
    addSunGate(tiles, nextTile, exitX, exitY)

    addCoin(coins, 1, 12.0f, 8.2f)
    addCoin(coins, 2, 19.0f, if (twist == 1) 10.2f else 11.4f)
    addCoin(coins, 3, 35.0f + v, 11.4f)
    addCoin(coins, 4, 41.0f + v, if (twist == 2) 9.8f else 11.4f)

    val cat1 = surpriseCategory(idx, 1)
    val cat2 = surpriseCategory(idx, 2)

    // Trap 1: Opening ambush wall slides upward on approach, immediately firing Trap 2 (Reversing Saw)!
    val openWallId = tb.nextId()
    val sawId = tb.nextId()
    tb.add(
        TrollTrap(
            id = openWallId,
            type = TrollTrapType.OPENING_AMBUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 11.5f * T,
            initialY = 11.0f * T,
            width = 1.2f * T,
            height = 2.0f * T,
            triggerMinX = 8.4f * T,
            triggerMaxX = 11.4f * T,
            triggerMinY = 10.2f * T,
            triggerMaxY = 13.5f * T,
            targetX = 11.5f * T,
            targetY = 8.2f * T,
            moveSpeed = speedFor(idx, 6.2f),
            telegraphDurationFrames = telegraphFor(idx, cat1, 10),
            behaviorCategory = cat1,
            killsPlayer = false,
            linkedTrapId = sawId,
            quipPopup = quipOrNull(idx, cat1, false, "DOOR'S OPEN!")
        )
    )

    tb.add(
        TrollTrap(
            id = sawId,
            type = TrollTrapType.REVERSING_HAZARD,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = openWallId,
            initialX = 14.5f * T,
            initialY = 13f * T - 34f,
            width = 34f,
            height = 34f,
            targetX = 8.0f * T,
            targetY = 13f * T - 34f,
            moveSpeed = speedFor(idx, 5.4f),
            activeDurationFrames = 300,
            telegraphDurationFrames = telegraphFor(idx, cat2, 10),
            behaviorCategory = cat2,
            killsPlayer = true,
            quipPopup = quipOrNull(idx, cat2, true, "AMBUSH SAW!")
        )
    )

    // Mid chasm sliding deck
    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.SLIDING_FLOOR_SHIFT,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 18.0f * T,
            initialY = 13f * T,
            width = 2.8f * T,
            height = T,
            targetX = 19.6f * T,
            targetY = 13f * T,
            moveSpeed = speedFor(idx, 5.2f),
            cycleFrames = 130,
            telegraphDurationFrames = telegraphFor(idx, cat1, 12),
            behaviorCategory = cat1,
            killsPlayer = false
        )
    )

    // Switch opens Rune Gate (never ON_COIN_COLLECTED so NO_COINS mastery works cleanly!)
    val switchId = tb.nextId()
    val gateId = tb.nextId()
    tb.add(
        TrollTrap(
            id = switchId,
            type = TrollTrapType.BETRAYAL_SWITCH,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = (32.5f + v) * T,
            initialY = 12.2f * T,
            width = 36f,
            height = 38f,
            triggerMinX = (31.8f + v) * T,
            triggerMaxX = (33.8f + v) * T,
            triggerMinY = 11.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            linkedTrapId = gateId,
            quipPopup = "SEAL BROKEN!"
        )
    )

    tb.add(
        TrollTrap(
            id = gateId,
            type = TrollTrapType.LOCKED_RUNE_GATE,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = switchId,
            initialX = (38.5f + v) * T,
            initialY = 10.6f * T,
            width = 44f,
            height = 2.4f * T,
            targetX = (38.5f + v) * T,
            targetY = 6.8f * T,
            moveSpeed = speedFor(idx, 6.4f),
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            quipPopup = "SANCTUM OPEN!"
        )
    )

    // Exit slam cage prank right before Sun Gate
    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.EXIT_SLAM_CAGE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = exitX - 0.5f * T,
            initialY = exitY,
            width = 3.0f * T,
            height = 3.0f * T,
            triggerMinX = exitX - 2.0f * T,
            triggerMaxX = exitX + 0.2f * T,
            triggerMinY = exitY - T,
            triggerMaxY = exitY + 3.2f * T,
            effectDurationFrames = 54,
            telegraphDurationFrames = telegraphFor(idx, cat2, 14),
            behaviorCategory = cat2,
            killsPlayer = false,
            quipPopup = quipOrNull(idx, cat2, false, "NOPE—WAIT!")
        )
    )

    if (twist == 2) {
      tb.add(
          TrollTrap(
              id = tb.nextId(),
              type = TrollTrapType.TEMP_VANISH_REFORM_FLOOR,
              triggerType = TrapTriggerType.ON_STEP,
              initialX = (43.4f + v) * T,
              initialY = 12.6f * T,
              width = 2.6f * T,
              height = T,
              cycleFrames = 75,
              telegraphDurationFrames = telegraphFor(idx, cat1, 12),
              behaviorCategory = cat1,
              killsPlayer = false
          )
      )
    }

    return LevelDefinition(
        worldId = 1,
        levelId = idx,
        title = meta.title,
        subtitle = meta.subtitle,
        description = "Watch opening walls—whatever was locked behind them wants out.",
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

  // Archetype 4: Reversing Conveyor Belts, Wind Gusts & Relocating Exit Gate
  private fun buildConveyorGaleRun(meta: ExpansionChallengeMeta, difficulty: DifficultyTier): LevelDefinition {
    val idx = meta.orderIndex
    val v = (idx / 10) % 4
    val twist = ((idx - 41).coerceAtLeast(0) / 10) % 3
    val tiles = mutableListOf<GameTile>()
    val coins = mutableListOf<Coin>()
    val drums = mutableListOf<BounceDrum>()
    val tb = TrapBuilder()
    var tileId = 1
    val nextTile = { tileId++ }
    val floor = floorTypeFor(meta.theme)

    addRow(tiles, nextTile, 0, 7, 13f, floor)
    addRow(tiles, nextTile, 13, 18, 13f, floor)
    if (twist == 1) {
      drums.add(BounceDrum(id = 1, x = 16.8f * T, y = 13f * T - 22f, launchVy = -15.8f))
      addRow(tiles, nextTile, 19, 21, 9.6f, TileType.CARVED_STONE)
    }
    addRow(tiles, nextTile, 23, 28 + v, 13f, floor)
    addCheckpoint(tiles, nextTile, 25.5f)
    addRow(tiles, nextTile, 35 + v, 41 + v, 13f, floor)
    addRow(tiles, nextTile, 43 + v, 53 + v, 13f, floor)

    val initialExitX = (47.5f + v) * T
    val exitY = 10f * T
    val relocatedExitX = (50.8f + v) * T
    addSunGate(tiles, nextTile, initialExitX, exitY)

    addCoin(coins, 1, 10.5f, 11.2f)
    addCoin(coins, 2, 20.5f, if (twist == 1) 8.2f else 10.6f)
    addCoin(coins, 3, 31.5f + v, 10.8f)
    addCoin(coins, 4, 42.0f + v, 11.2f)

    val cat1 = surpriseCategory(idx, 1)
    val cat2 = surpriseCategory(idx, 2)

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.REVERSE_CONVEYOR_BELT,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 8.0f * T,
            initialY = 13f * T,
            width = 4.6f * T,
            height = T,
            cycleFrames = 120,
            gustForceX = 2.0f,
            gustFlipIntervalFrames = 120,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.TELEGRAPHED,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.GUST_REVERSAL_ZONE,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 18.5f * T,
            initialY = 7.5f * T,
            width = 4.5f * T,
            height = 5.5f * T,
            cycleFrames = 120,
            gustForceX = -0.34f,
            gustFlipIntervalFrames = 120,
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
            initialX = (29.5f + v) * T,
            initialY = 12.6f * T,
            width = 3.2f * T,
            height = 0.8f * T,
            targetX = (33.5f + v) * T,
            targetY = 12.6f * T,
            moveSpeed = speedFor(idx, 5.2f),
            maxActivations = 3,
            telegraphDurationFrames = telegraphFor(idx, cat1, 10),
            behaviorCategory = cat1,
            killsPlayer = false
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.MOVING_SPIKE,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = (36.0f + v) * T,
            initialY = 13f * T - 28f,
            width = 36f,
            height = 28f,
            targetX = (40.0f + v) * T,
            targetY = 13f * T - 28f,
            cycleFrames = 120,
            moveSpeed = speedFor(idx, 4.8f),
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.TELEGRAPHED,
            killsPlayer = true
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.RELOCATING_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = initialExitX,
            initialY = exitY,
            width = 2f * T,
            height = 3f * T,
            triggerMinX = initialExitX - 1.5f * T,
            triggerMaxX = initialExitX + 2.5f * T,
            triggerMinY = exitY - T,
            triggerMaxY = exitY + 3.2f * T,
            targetX = relocatedExitX,
            targetY = exitY,
            linkedDoorTargetX = relocatedExitX,
            linkedDoorTargetY = exitY,
            telegraphDurationFrames = 0,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            killsPlayer = false,
            quipPopup = "TOO SLOW!"
        )
    )

    tb.add(
        TrollTrap(
            id = tb.nextId(),
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = (49.2f + v) * T,
            initialY = 13f * T - 32f,
            width = 44f,
            height = 32f,
            triggerMinX = (48.4f + v) * T,
            triggerMaxX = (50.2f + v) * T,
            triggerMinY = 11.2f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = telegraphFor(idx, cat2, 15),
            activeDurationFrames = 72,
            behaviorCategory = cat2,
            killsPlayer = true,
            quipPopup = quipOrNull(idx, cat2, true, "DOUBLE-JUMP!")
        )
    )

    if (twist == 2) {
      tb.add(
          TrollTrap(
              id = tb.nextId(),
              type = TrollTrapType.PHANTOM_LEDGE,
              triggerType = TrapTriggerType.ON_STEP,
              initialX = 19.6f * T,
              initialY = 10.2f * T,
              width = 2.2f * T,
              height = T,
              cycleFrames = 85,
              telegraphDurationFrames = telegraphFor(idx, cat1, 12),
              behaviorCategory = cat1,
              killsPlayer = false
          )
      )
    }

    return LevelDefinition(
        worldId = 1,
        levelId = idx,
        title = meta.title,
        subtitle = meta.subtitle,
        description = "Ride shifting belts and shifting gusts—even the Sun Gate won't sit still.",
        theme = meta.theme,
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        bounceDrums = drums,
        trollTraps = tb.traps,
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = initialExitX,
        exitY = exitY,
        parTimeSeconds = 50 + (v * 3),
        difficulty = difficulty,
        enableForestSetPieces = true,
        isTrollLevel = true,
        minCamY = -96f,
        maxCamY = 240f
    )
  }
}
