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

    // 1. Opening launchpad (0..5, Y=13) -> Pit (6..9) bridged by Tilting Plank #1
    addRow(0, 5, 13f, TileType.CARVED_STONE)

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

    // 5. Exit sanctum (45..50, Y=13)
    addRow(45, 50, 13f, TileType.CARVED_STONE)
    val exitX = 47.5f * T
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
        Coin(id = 1, x = 4.0f * T, y = 11.5f * T),
        Coin(id = 2, x = 14.3f * T, y = 9.8f * T),
        Coin(id = 3, x = 20.9f * T, y = 10.8f * T),
        Coin(id = 4, x = 34.0f * T, y = 9.6f * T),
        Coin(id = 5, x = 46.0f * T, y = 11.5f * T)
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
            triggerMinX = 12.0f * T,
            triggerMaxX = 14.0f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 12,
            targetX = 17.0f * T,
            targetY = 13f * T - 30f,
            moveSpeed = 4.5f,
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
            triggerMinX = 31.0f * T,
            triggerMaxX = 33.2f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 12,
            targetX = 38.2f * T,
            targetY = 13f * T - 30f,
            moveSpeed = 4.7f,
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
