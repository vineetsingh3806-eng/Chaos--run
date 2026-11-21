package com.example.data

/**
 * Challenge 09: "The Fake Finish"
 *
 * Central Mechanics: EXIT_SLAM_CAGE (vaultable iron cage around finish path that unlocks
 * deterministically without trapping the player) & FLEEING_SUN_GATE (multi-hop runaway exit
 * gate that stops permanently after bounded hops).
 */
object Challenge09FakeFinish {
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

    // 1. Launchpad & Fake Door Gauntlet (0..13, Y=13) with step stone (7..8.5, Y=11.4)
    addRow(0, 13, 13f, TileType.CARVED_STONE)
    addRow(7, 8, 11.4f, TileType.CARVED_STONE)

    // 2. Checkpoint Sanctuary (15..23, Y=13)
    addRow(15, 23, 13f, TileType.CARVED_STONE)
    tiles.add(
        GameTile(
            id = nextId++,
            x = 20.5f * T,
            y = 12f * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )

    // 3. Slam Cage Corridor (24..31, Y=13)
    addRow(24, 31, 13f, TileType.CARVED_STONE)

    // 4. Fleeing Sun Gate Plaza (32..49, Y=13)
    addRow(32, 49, 13f, TileType.CARVED_STONE)

    val initialExitX = 34.5f * T
    val initialExitY = 10f * T
    tiles.add(
        GameTile(
            id = nextId++,
            x = initialExitX,
            y = initialExitY,
            width = T * 2f,
            height = T * 3f,
            type = TileType.SUN_GATE,
            isSolid = false
        )
    )

    val coins = listOf(
        Coin(id = 1, x = 4.5f * T, y = 11.5f * T),
        Coin(id = 2, x = 10.6f * T, y = 8.6f * T),
        Coin(id = 3, x = 27.0f * T, y = 8.6f * T),
        Coin(id = 4, x = 39.2f * T, y = 11.5f * T),
        Coin(id = 5, x = 44.0f * T, y = 11.5f * T)
    )

    val trollTraps = listOf(
        TrollTrap(
            id = 1,
            type = TrollTrapType.FAKE_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 9.6f * T,
            initialY = 10.0f * T,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 8.8f * T,
            triggerMaxX = 12.0f * T,
            triggerMinY = 10.2f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 12,
            killsPlayer = true,
            quipPopup = "DECOY GATE!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.FAKE_CHECKPOINT,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 16.5f * T,
            initialY = 12.0f * T,
            width = T,
            height = T,
            triggerMinX = 15.8f * T,
            triggerMaxX = 17.8f * T,
            triggerMinY = 11.2f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 12,
            killsPlayer = true,
            quipPopup = "NOT THIS BELL!"
        ),
        TrollTrap(
            id = 3,
            type = TrollTrapType.EXIT_SLAM_CAGE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 25.6f * T,
            initialY = 10.0f * T,
            width = 2.8f * T,
            height = 3f * T,
            triggerMinX = 25.0f * T,
            triggerMaxX = 28.8f * T,
            triggerMinY = 10.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 12,
            effectDurationFrames = 55,
            killsPlayer = false,
            quipPopup = "CAGED!"
        ),
        TrollTrap(
            id = 4,
            type = TrollTrapType.FLEEING_SUN_GATE,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = initialExitX,
            initialY = initialExitY,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = initialExitX - 0.8f * T,
            triggerMaxX = initialExitX + 2.4f * T,
            triggerMinY = initialExitY - 0.5f * T,
            triggerMaxY = initialExitY + 3.2f * T,
            telegraphDurationFrames = 8,
            targetX = 40.5f * T,
            targetY = 10.0f * T,
            secondaryX = 45.5f * T,
            secondaryY = 10.0f * T,
            maxActivations = 2,
            killsPlayer = false,
            quipPopup = "OVER HERE!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 9,
        title = "The Fake Finish",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = initialExitX,
        exitY = initialExitY,
        parTimeSeconds = 30,
        difficulty = difficulty,
        subtitle = "Slam Cages & Fleeing Gates",
        description = "Vault slam cages or wait for their release, dodge decoy shrines, and corner the fleeing Sun Gate!",
        theme = LevelTheme.RUINS_OF_LOST_KING,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
