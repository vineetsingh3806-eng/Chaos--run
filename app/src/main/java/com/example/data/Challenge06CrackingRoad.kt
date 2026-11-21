package com.example.data

/**
 * Challenge 06: "The Cracking Road"
 *
 * Central Mechanics: SPLIT_BRIDGE (outer solid wings survive when center cracks open)
 * & PHANTOM_LEDGE (ghosted ledges with rune clues that phase out under step and reform).
 */
object Challenge06CrackingRoad {
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

    // 1. Opening launchpad (0..6, Y=13)
    addRow(0, 6, 13f, TileType.CARVED_STONE)
    // Gap 7..10 bridged by Split Bridge #1

    // 2. Mid landing terrace (11..15, Y=13)
    addRow(11, 15, 13f, TileType.CARVED_STONE)
    // Chasm 16..23 crossed via Phantom Ledges #1 & #2

    // 3. Checkpoint sanctuary (24..28, Y=12.5)
    addRow(24, 28, 12.5f, TileType.CARVED_STONE)
    tiles.add(
        GameTile(
            id = nextId++,
            x = 25.5f * T,
            y = 11.5f * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )

    // Gap 29..33 bridged by Split Bridge #2 -> stone pier (34..36, Y=12.5)
    addRow(34, 36, 12.5f, TileType.CARVED_STONE)
    // Gap 37..39.5 crossed via Phantom Ledge #3

    // 4. Exit sanctum (40..46, Y=13)
    addRow(40, 46, 13f, TileType.CARVED_STONE)
    val exitX = 43.5f * T
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
        Coin(id = 2, x = 13.0f * T, y = 11.5f * T),
        Coin(id = 3, x = 21.4f * T, y = 10.2f * T),
        Coin(id = 4, x = 38.2f * T, y = 10.4f * T),
        Coin(id = 5, x = 41.5f * T, y = 11.5f * T)
    )

    val lag = when (difficulty) {
      DifficultyTier.NORMAL -> 0
      DifficultyTier.HARD -> -1
      DifficultyTier.NIGHTMARE -> -2
    }

    val trollTraps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.SPLIT_BRIDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 7f * T,
            initialY = 13f * T,
            width = 4f * T,
            height = T,
            telegraphDurationFrames = (14 + lag).coerceAtLeast(12),
            killsPlayer = false,
            quipPopup = "MIND THE GAP!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.PHANTOM_LEDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 16.4f * T,
            initialY = 12.4f * T,
            width = 2.6f * T,
            height = 24f,
            telegraphDurationFrames = (12 + lag).coerceAtLeast(10),
            cycleFrames = 110,
            killsPlayer = false,
            quipPopup = "FADED!"
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.PHANTOM_LEDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 20.2f * T,
            initialY = 11.9f * T,
            width = 2.6f * T,
            height = 24f,
            telegraphDurationFrames = (12 + lag).coerceAtLeast(10),
            cycleFrames = 110,
            killsPlayer = false,
            quipPopup = "KEEP MOVING!"
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.SPLIT_BRIDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 29.3f * T,
            initialY = 12.5f * T,
            width = 4.2f * T,
            height = T,
            telegraphDurationFrames = (14 + lag).coerceAtLeast(12),
            killsPlayer = false,
            quipPopup = "SPLIT APART!"
        ),
        TrollTrap(
            id = 5,
            type = TrollTrapType.PHANTOM_LEDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 36.8f * T,
            initialY = 12.1f * T,
            width = 2.6f * T,
            height = 24f,
            telegraphDurationFrames = (12 + lag).coerceAtLeast(10),
            cycleFrames = 110,
            killsPlayer = false
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 6,
        title = "The Cracking Road",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 28,
        difficulty = difficulty,
        subtitle = "Split Bridges & Phantom Ledges",
        description = "Read center seams on wooden spans and leap cleanly off fading phantom ledges before they vanish.",
        theme = LevelTheme.WHISPERING_WOODS,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
