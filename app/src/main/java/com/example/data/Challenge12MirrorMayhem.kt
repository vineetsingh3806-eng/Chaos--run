package com.example.data

/**
 * Challenge 12: "Mirror Mayhem"
 *
 * Central Mechanics: GUST_REVERSAL_ZONE (periodic horizontal wind flips with pre-flip
 * telegraph tinting) & MIRROR_CURSE_TOTEM (temporary readable left/right control inversion
 * with purple halo and clean reset on death/retry).
 */
object Challenge12MirrorMayhem {
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

    // 1. Launchpad & Gust Arena #1 (0..15, Y=13 with manageable center gap at tile 11)
    addRow(0, 10, 13f, TileType.CARVED_STONE)
    addRow(12, 15, 13f, TileType.CARVED_STONE)

    // 2. Wide Safe Mirror Curse Arena (16..26, Y=13, low central step 21..23, Y=12.0)
    addRow(16, 26, 13f, TileType.CARVED_STONE)
    addRow(21, 23, 12.0f, TileType.CARVED_STONE)

    // 3. Checkpoint Sanctuary (27..30, Y=13)
    addRow(27, 30, 13f, TileType.CARVED_STONE)
    tiles.add(
        GameTile(
            id = nextId++,
            x = 28.0f * T,
            y = 12f * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )

    // 4. Gust Reversal #2 + Final Mirror Curse Dash (31..48, Y=13)
    addRow(31, 48, 13f, TileType.CARVED_STONE)
    val exitX = 45.5f * T
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
        Coin(id = 2, x = 11.0f * T, y = 10.8f * T),
        Coin(id = 3, x = 22.0f * T, y = 10.5f * T),
        Coin(id = 4, x = 35.0f * T, y = 11.2f * T),
        Coin(id = 5, x = 43.0f * T, y = 11.5f * T)
    )

    val trollTraps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.GUST_REVERSAL_ZONE,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 6.5f * T,
            initialY = 8.5f * T,
            width = 8.5f * T,
            height = 4.5f * T,
            cycleFrames = 140,
            gustFlipIntervalFrames = 140,
            gustForceX = -0.30f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.MIRROR_CURSE_TOTEM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 17.5f * T,
            initialY = 13f * T - 64f,
            width = 40f,
            height = 64f,
            triggerMinX = 16.5f * T,
            triggerMaxX = 19.2f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 12,
            effectDurationFrames = 140,
            killsPlayer = false,
            quipPopup = "LEFT IS RIGHT!"
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.GUST_REVERSAL_ZONE,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 31.5f * T,
            initialY = 8.5f * T,
            width = 7.5f * T,
            height = 4.5f * T,
            cycleFrames = 150,
            gustFlipIntervalFrames = 150,
            gustForceX = 0.32f,
            telegraphDurationFrames = 0,
            killsPlayer = false
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.MIRROR_CURSE_TOTEM,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 40.0f * T,
            initialY = 13f * T - 64f,
            width = 40f,
            height = 64f,
            triggerMinX = 39.0f * T,
            triggerMaxX = 41.5f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 12,
            effectDurationFrames = 120,
            killsPlayer = false,
            quipPopup = "MIRRORED!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 12,
        title = "Mirror Mayhem",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 30,
        difficulty = difficulty,
        subtitle = "Shifting Gusts & Mirror Totems",
        description = "Watch wind chevrons flip direction and adapt when violet totems temporarily mirror left and right controls.",
        theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
