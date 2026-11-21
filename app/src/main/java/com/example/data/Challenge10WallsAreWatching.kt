package com.example.data

/**
 * Challenge 10: "Walls Are Watching"
 *
 * Central Mechanics: PINCER_WALLS (synchronized left/right crushing jaws preserving a
 * readable central safe pocket) & STALKER_CRUSH_WALL (telegraphed horizontal pursuit monolith).
 */
object Challenge10WallsAreWatching {
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

    // 1. Launchpad + Pincer Chamber #1 (0..16, Y=13)
    addRow(0, 16, 13f, TileType.CARVED_STONE)

    // 2. Stalker Hallway #1 (17..26, Y=13) with raised alcove (20.5..22.5, Y=10.6)
    addRow(17, 26, 13f, TileType.CARVED_STONE)
    tiles.add(GameTile(id = nextId++, x = 20.5f * T, y = 10.6f * T, width = 2.0f * T, height = 0.6f * T, type = TileType.CARVED_STONE))

    // 3. Checkpoint Sanctuary (27..31, Y=13)
    addRow(27, 31, 13f, TileType.CARVED_STONE)
    tiles.add(
        GameTile(
            id = nextId++,
            x = 28.5f * T,
            y = 12f * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )

    // 4. Pincer Chamber #2 + Stalker Dash Exit (32..50, Y=13)
    addRow(32, 50, 13f, TileType.CARVED_STONE)
    val exitX = 48.0f * T
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
        Coin(id = 1, x = 4.5f * T, y = 11.5f * T),
        Coin(id = 2, x = 10.2f * T, y = 11.4f * T),
        Coin(id = 3, x = 21.5f * T, y = 9.2f * T),
        Coin(id = 4, x = 35.1f * T, y = 11.4f * T),
        Coin(id = 5, x = 44.0f * T, y = 11.5f * T)
    )

    val trollTraps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.PINCER_WALLS,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 8.5f * T,
            initialY = 13f * T - 84f,
            width = 3.4f * T,
            height = 84f,
            triggerMinX = 7.8f * T,
            triggerMaxX = 12.2f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            minSafeGapWidth = 60f,
            telegraphDurationFrames = 12,
            moveSpeed = 5.0f,
            cycleFrames = 110,
            killsPlayer = true,
            quipPopup = "HOLD CENTER!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 25.4f * T,
            initialY = 13f * T - 84f,
            width = T,
            height = 84f,
            triggerMinX = 19.6f * T,
            triggerMaxX = 22.4f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 12,
            targetX = 18.2f * T,
            targetY = 13f * T - 84f,
            moveSpeed = 4.5f,
            activeDurationFrames = 220,
            killsPlayer = true,
            quipPopup = "WALL ON THE MOVE!"
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.PINCER_WALLS,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 33.5f * T,
            initialY = 13f * T - 84f,
            width = 3.2f * T,
            height = 84f,
            triggerMinX = 32.8f * T,
            triggerMaxX = 37.0f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            minSafeGapWidth = 56f,
            telegraphDurationFrames = 12,
            moveSpeed = 5.2f,
            cycleFrames = 100,
            killsPlayer = true,
            quipPopup = "THREAD THE GAP!"
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.STALKER_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 38.0f * T,
            initialY = 13f * T - 84f,
            width = T,
            height = 84f,
            triggerMinX = 40.4f * T,
            triggerMaxX = 42.6f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 12,
            targetX = 46.6f * T,
            targetY = 13f * T - 84f,
            moveSpeed = 4.8f,
            activeDurationFrames = 240,
            killsPlayer = true,
            quipPopup = "DASH!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 10,
        title = "Walls Are Watching",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 30,
        difficulty = difficulty,
        subtitle = "Pincer Jaws & Stalker Walls",
        description = "Stand center-pocket inside closing pincer jaws and dash or hop above encroaching stalker monoliths.",
        theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
