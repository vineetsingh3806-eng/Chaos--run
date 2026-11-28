package com.example.data

/**
 * Challenge 07: "Tilted Trouble"
 *
 * Central Mechanics: TILTING_PLANK (weight-balanced seesaw bridges)
 * & TRAILING_SPIKE (bounded speed pursuit spike with overhead escape ledges).
 */
object Challenge07TiltedTrouble {
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

    // 1. Opening launchpad (0..4, Y=13) + disappearing edge (5) -> Pit (6..9) bridged by Tilting Plank #1
    addRow(0, 4, 13f, TileType.CARVED_STONE)

    // 2. Chase terrace #1 (10..18, Y=13) with raised safe perch (13.5..15.2, Y=11.3)
    addRow(10, 18, 13f, TileType.CARVED_STONE)
    tiles.add(GameTile(id = nextId++, x = 13.5f * T, y = 11.3f * T, width = 1.8f * T, height = 0.6f * T, type = TileType.CARVED_STONE))

    // Pit (19..22) bridged by Tilting Plank #2

    // 3. Checkpoint sanctuary (23..28, Y=13)
    addRow(23, 28, 13f, TileType.CARVED_STONE)
    tiles.add(
        GameTile(
            id = nextId++,
            x = 24.5f * T,
            y = 12f * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )

    // 4. Pursuit runway #2 (29..40, Y=13) with overhead alcove (33..35, Y=11.2)
    addRow(29, 40, 13f, TileType.CARVED_STONE)
    tiles.add(GameTile(id = nextId++, x = 33.0f * T, y = 11.2f * T, width = 2.0f * T, height = 0.6f * T, type = TileType.CARVED_STONE))

    // Pit (41..44) bridged by Tilting Plank #3

    // 5. Exit sanctum (45..53, Y=13)
    addRow(45, 53, 13f, TileType.CARVED_STONE)
    val exitX = 47.6f * T
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
        Coin(id = 1, x = 3.5f * T, y = 11.5f * T),
        Coin(id = 2, x = 14.3f * T, y = 9.8f * T),
        Coin(id = 3, x = 20.9f * T, y = 10.8f * T),
        Coin(id = 4, x = 34.0f * T, y = 9.6f * T),
        Coin(id = 5, x = 46.0f * T, y = 10.5f * T)
    )

    val trollTraps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.TILTING_PLANK,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 6.0f * T,
            initialY = 12.8f * T,
            width = 3.6f * T,
            height = 18f,
            maxTiltDegrees = 20f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.TRAILING_SPIKE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 10.2f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            triggerMinX = 11.4f * T,
            triggerMaxX = 13.8f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 6,
            targetX = 17.5f * T,
            targetY = 13f * T - 30f,
            moveSpeed = 5.3f,
            activeDurationFrames = 240,
            killsPlayer = true,
            quipPopup = "RIGHT BEHIND YOU!"
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.TILTING_PLANK,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 19.0f * T,
            initialY = 12.6f * T,
            width = 3.8f * T,
            height = 18f,
            maxTiltDegrees = 22f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.TRAILING_SPIKE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 29.2f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            triggerMinX = 30.4f * T,
            triggerMaxX = 33.0f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 6,
            targetX = 38.6f * T,
            targetY = 13f * T - 30f,
            moveSpeed = 5.5f,
            activeDurationFrames = 260,
            killsPlayer = true,
            quipPopup = "RUN FASTER!"
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.TILTING_PLANK,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 41.0f * T,
            initialY = 12.5f * T,
            width = 3.5f * T,
            height = 18f,
            maxTiltDegrees = 20f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        // Combo: Trailing Spike #4 pursuit wakes an instant ceiling keystone ahead on the runway
        TrollTrap(
            id = 6,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 4,
            chainDelayFrames = 24,
            initialX = 36.4f * T,
            initialY = 6.5f * T,
            width = 50f,
            height = 52f,
            telegraphDurationFrames = 0,
            targetX = 36.4f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 13.5f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "CUT OFF!"
        ),
        // Relocating Sun Gate finale: hops past the ceiling drop onto 51.0T!
        TrollTrap(
            id = 7,
            type = TrollTrapType.RELOCATING_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = exitX,
            initialY = exitY,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 46.6f * T,
            triggerMaxX = 49.6f * T,
            triggerMinY = 8.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            linkedDoorTargetX = 51.0f * T,
            linkedDoorTargetY = exitY,
            killsPlayer = false,
            quipPopup = "TILTED GATE!"
        ),
        // Opening launch edge drops cleanly before Plank #1
        TrollTrap(
            id = 8,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 5.0f * T,
            initialY = 13f * T,
            width = T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SLIPPED!"
        ),
        // Coin #2 perch reward triggers a lower floor spike as you drop down!
        TrollTrap(
            id = 9,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 2,
            chainDelayFrames = 8,
            initialX = 16.0f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "PERCH TRAP!"
        ),
        // Coin #5 jump bait drops a ceiling keystone right in front of the fleeing gate!
        TrollTrap(
            id = 10,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            chainDelayFrames = 0,
            initialX = 48.6f * T,
            initialY = 5.6f * T,
            width = 50f,
            height = 50f,
            telegraphDurationFrames = 0,
            targetX = 48.6f * T,
            targetY = 13f * T - 50f,
            moveSpeed = 14.5f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "GATE CRUSHER!"
        ),
        // Early Coin #1 triggers an immediate floor spike pop at the opening stone edge
        TrollTrap(
            id = 11,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 1,
            chainDelayFrames = 2,
            initialX = 4.0f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 60,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "GREEDY START!"
        ),
        // Mid-jump ambush above Tilting Plank #2 when vaulting toward Coin #3
        TrollTrap(
            id = 12,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_JUMP_ABOVE,
            initialX = 20.4f * T,
            initialY = 5.4f * T,
            width = 48f,
            height = 48f,
            triggerMinX = 19.6f * T,
            triggerMaxX = 22.0f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 12.2f * T,
            targetX = 20.4f * T,
            targetY = 12.6f * T - 48f,
            moveSpeed = 13.5f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "AIR AMBUSH!"
        ),
        // Hidden Betrayal Switch past Checkpoint Bell silently arms a delayed runway spike
        TrollTrap(
            id = 13,
            type = TrollTrapType.BETRAYAL_SWITCH,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 26.5f * T,
            initialY = 13f * T - 22f,
            width = 44f,
            height = 22f,
            telegraphDurationFrames = 0,
            linkedTrapId = 14,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "HARMLESS CLICK?"
        ),
        TrollTrap(
            id = 14,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 13,
            chainDelayFrames = 38,
            initialX = 30.0f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 60,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "LATE BITE!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 7,
        title = "Tilted Trouble",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 30,
        difficulty = difficulty,
        subtitle = "Seesaw Planks & Trailing Spikes",
        description = "Shift your weight across responsive seesaw planks and climb overhead ledges to shake off trailing spikes.",
        theme = LevelTheme.HOLLOW_OF_ECHOES,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
