package com.example.data

/**
 * World 1 Level 1 ("Whispering Woods") Benchmark Level Definition.
 * Strict Phase 1 scope: Only Level 1 is implemented.
 */
object World1Levels {
  const val TILE_SIZE = 48f

  fun createLevel1(): LevelDefinition {
    val tiles = mutableListOf<GameTile>()
    var nextId = 1

    fun addPlatform(startTileX: Int, endTileX: Int, tileY: Int, type: TileType) {
      for (tx in startTileX..endTileX) {
        tiles.add(
            GameTile(
                id = nextId++,
                x = tx * TILE_SIZE,
                y = tileY * TILE_SIZE,
                width = TILE_SIZE,
                height = TILE_SIZE,
                type = type
            )
        )
      }
    }

    // Beat 1: Intro Plains (Tiles 0 - 11, Y = 13)
    addPlatform(0, 11, 13, TileType.MOSSY_TIMBER)

    // Beat 1: Gap 1 (Tiles 12 - 14 is void pit)

    // Beat 1: Landing terrace (Tiles 15 - 21, Y = 13)
    addPlatform(15, 21, 13, TileType.MOSSY_TIMBER)

    // Beat 2: Gap 2 (Tiles 22 - 24 is river gap)

    // Beat 2: Raised river pier (Tiles 25 - 26, Y = 12)
    addPlatform(25, 26, 12, TileType.CARVED_STONE)

    // Beat 2: Trembling crumbling bridge (Tiles 27 - 29, Y = 12)
    addPlatform(27, 29, 12, TileType.CRUMBLING_WOOD)

    // Beat 2: Safe clearing (Tiles 30 - 44, Y = 13)
    addPlatform(30, 44, 13, TileType.MOSSY_TIMBER)

    // Beat 3: Ravine Gap 3 (Tiles 45 - 47 is void pit)

    // Beat 3: Shrine terrace (Tiles 48 - 53, Y = 13)
    addPlatform(48, 53, 13, TileType.CARVED_STONE)

    // Beat 3: Checkpoint bell tile object at Tile 48 (elevated above ground)
    tiles.add(
        GameTile(
            id = nextId++,
            x = 48 * TILE_SIZE,
            y = 11 * TILE_SIZE,
            width = TILE_SIZE * 1.5f,
            height = TILE_SIZE * 2f,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )

    // Beat 3: Raised terrace (Tiles 54 - 56, Y = 11)
    addPlatform(54, 56, 11, TileType.CARVED_STONE)

    // Beat 3: Deceptive Step: Instant Disappearing Floor (Tiles 57 - 58, Y = 11)
    addPlatform(57, 58, 11, TileType.DISAPPEARING_FLOOR)

    // Beat 3: Safe lower basin (Tiles 59 - 64, Y = 13)
    addPlatform(59, 64, 13, TileType.MOSSY_TIMBER)

    // Beat 4: Ascending pathway (Tiles 65 - 72, Y = 12)
    addPlatform(65, 72, 12, TileType.MOSSY_TIMBER)

    // Beat 4: Ground bramble spikes in pit (Tiles 73 - 75, Y = 14)
    addPlatform(73, 75, 14, TileType.BRAMBLE_SPIKES)

    // Beat 4: High cliff ledge (Tiles 76 - 81, Y = 10)
    addPlatform(76, 81, 10, TileType.CARVED_STONE)

    // Beat 4: Moving Platform is added via movingPlatforms list (Tiles 82 - 88)

    // Beat 4: High canopy landing (Tiles 89 - 95, Y = 8)
    addPlatform(89, 95, 8, TileType.MOSSY_TIMBER)

    // Beat 5: Overlook ruin terrace (Tiles 96 - 103, Y = 7)
    addPlatform(96, 103, 7, TileType.CARVED_STONE)

    // Beat 5: Secret High Alcove (Tiles 104 - 106, Y = 5) - Verified +56.5% jump clearance
    addPlatform(104, 106, 5, TileType.CARVED_STONE)

    // Beat 5: Sun Gate approach terrace (Tiles 107 - 113, Y = 7)
    addPlatform(107, 113, 7, TileType.CARVED_STONE)

    // Beat 5: Ancient Sun Gate base (Tiles 114 - 119, Y = 7)
    addPlatform(114, 119, 7, TileType.CARVED_STONE)

    // Ancient Sun Gate portal entity (at Tile 115, Y = 4 to 6)
    tiles.add(
        GameTile(
            id = nextId++,
            x = 115 * TILE_SIZE,
            y = 4 * TILE_SIZE,
            width = TILE_SIZE * 2f,
            height = TILE_SIZE * 3f,
            type = TileType.SUN_GATE,
            isSolid = false
        )
    )

    // 10 Collectible Golden Star Coins
    val coins = listOf(
        Coin(id = 1, x = 12.5f * TILE_SIZE, y = 11f * TILE_SIZE), // Guidance arc over Gap 1
        Coin(id = 2, x = 13.5f * TILE_SIZE, y = 10.5f * TILE_SIZE),
        Coin(id = 3, x = 14.5f * TILE_SIZE, y = 11f * TILE_SIZE),
        Coin(id = 4, x = 34f * TILE_SIZE, y = 11.5f * TILE_SIZE), // Riverside clearing
        Coin(id = 5, x = 40f * TILE_SIZE, y = 11.5f * TILE_SIZE),
        Coin(id = 6, x = 50f * TILE_SIZE, y = 11.5f * TILE_SIZE), // Shrine terrace near checkpoint
        Coin(id = 7, x = 68f * TILE_SIZE, y = 10.5f * TILE_SIZE), // Ascending path
        Coin(id = 8, x = 92f * TILE_SIZE, y = 6.5f * TILE_SIZE),  // High canopy
        Coin(id = 9, x = 104.5f * TILE_SIZE, y = 3.8f * TILE_SIZE), // Secret Alcove Coin 1
        Coin(id = 10, x = 105.5f * TILE_SIZE, y = 3.8f * TILE_SIZE) // Secret Alcove Coin 2
    )

    // Moving Platform (Tiles 82 - 88 at Y = 9)
    val movingPlatforms = listOf(
        MovingPlatform(
            id = 101,
            x = 4080f, // Center position
            y = 9 * TILE_SIZE, // Y = 432f
            width = 3 * TILE_SIZE, // 144f
            height = 24f,
            minX = 3936f,
            maxX = 4224f,
            centerX = 4080f,
            amplitude = 144f,
            periodFrames = 192
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 1,
        title = "Whispering Woods",
        tiles = tiles,
        coins = coins,
        movingPlatforms = movingPlatforms,
        spawnX = 2 * TILE_SIZE,
        spawnY = 11 * TILE_SIZE,
        exitX = 115 * TILE_SIZE,
        exitY = 4 * TILE_SIZE,
        parTimeSeconds = 60
    )
  }
}
