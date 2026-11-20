package com.example.data

data class World1LevelMeta(
    val levelId: Int,
    val title: String,
    val subtitle: String,
    val mechanicTag: String
)

/**
 * World 1 ("The Forest Is Watching") Level Catalog & Factory (1-1 through 1-5).
 */
object World1Levels {
  const val TILE_SIZE = 48f

  val catalog: List<World1LevelMeta> = listOf(
      World1LevelMeta(1, "The Easy Start", "Wait... That Floor Moved?", "Disappearing Floor • Crumble • Pop Spikes • Fake Safe"),
      World1LevelMeta(2, "The Fake Safe Path", "Trust Nothing You See", "Fake Safe Tiles • Moving Spikes • Fake Bell"),
      World1LevelMeta(3, "The Exit Is Lying", "So Close Yet So Far", "Decoy Exit • Relocating Gate • Crush Walls"),
      World1LevelMeta(4, "The Floor Has Plans", "Timing & Betrayal", "Blink Platforms • Crumble Chains • Ambush Ceilings"),
      World1LevelMeta(5, "Almost There", "One Final Troll", "Full Trap Gauntlet • Last-Second Exit Gag")
  )

  private val cachedRelicIds: Map<Int, List<String>> by lazy {
    (1..5).associateWith { id -> createLevel(id).relics.map { it.id } }
  }

  fun relicIdsForLevel(levelId: Int): List<String> = cachedRelicIds[levelId].orEmpty()

  fun createLevel(levelId: Int, difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    return when (levelId) {
      1 -> createLevel1(difficulty)
      2 -> Level12FakeSafePath.build(difficulty)
      3 -> Level13ExitIsLying.build(difficulty)
      4 -> Level14FloorHasPlans.build(difficulty)
      5 -> Level15AlmostThere.build(difficulty)
      else -> createLevel1(difficulty)
    }
  }

  fun createLevel1(difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
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

    // 1. Safe warm-up runway (Tiles 0 - 11, Y = 13) — lets players relax and trust the floor
    addPlatform(0, 11, 13, TileType.MOSSY_TIMBER)

    // (Tiles 12 - 13 are DISAPPEARING_FLOOR TrollTraps, Tile 14 is open pit gap)

    // 2. Solid stone landing clearing (Tiles 15 - 22, Y = 13) — hosts Gag 2 Surprise Spikes at Tile 18
    addPlatform(15, 22, 13, TileType.CARVED_STONE)

    // (Tiles 23 - 24 = DELAYED_CRUMBLE wooden span over Gap 2 — trembles then drops!)

    // 3. Coin-bait avenue (Tiles 25 - 31, Y = 13) — hosts Gag 3 Falling Ceiling Block at Tile 28
    addPlatform(25, 31, 13, TileType.MOSSY_TIMBER)

    // (Tiles 32 - 33 = FAKE_SAFE_PLATFORM bridge over Gap 3 — sprouts spikes on contact!)

    // 4. First threshold of exit door (Tiles 34 - 37, Y = 13)
    addPlatform(34, 37, 13, TileType.CARVED_STONE)

    // (Tiles 38 - 39 are surprise DISAPPEARING_FLOOR TrollTraps right behind the hopping exit door!)

    // 5. Final landing platform for relocated Exit Door (Tiles 40 - 46, Y = 13)
    addPlatform(40, 46, 13, TileType.CARVED_STONE)

    // Exit Door initially sits invitingly at Tile 36 (hops to Tile 42.5 on approach)
    val initialExitX = 36f * TILE_SIZE
    val initialExitY = 10f * TILE_SIZE
    tiles.add(
        GameTile(
            id = nextId++,
            x = initialExitX,
            y = initialExitY,
            width = TILE_SIZE * 2f,
            height = TILE_SIZE * 3f,
            type = TileType.SUN_GATE,
            isSolid = false
        )
    )

    val coins = listOf(
        Coin(id = 1, x = 5.5f * TILE_SIZE, y = 11.5f * TILE_SIZE),  // Safe intro coin 1
        Coin(id = 2, x = 8.5f * TILE_SIZE, y = 11.5f * TILE_SIZE),  // Safe intro coin 2
        Coin(id = 3, x = 28.0f * TILE_SIZE, y = 11.5f * TILE_SIZE), // Coin-bait directly under ceiling thwomp!
        Coin(id = 4, x = 38.5f * TILE_SIZE, y = 10.5f * TILE_SIZE)  // Arc over surprise pit after door hop
    )

    val reactionLag = when (difficulty) {
      DifficultyTier.NORMAL -> 0
      DifficultyTier.HARD -> -1
      DifficultyTier.NIGHTMARE -> -2
    }

    val trollTraps = listOf(
        // Gag 1: Friendly bridge tiles 12-13 vanish right on footfall!
        TrollTrap(
            id = 1,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 12f * TILE_SIZE,
            initialY = 13f * TILE_SIZE,
            width = TILE_SIZE,
            height = TILE_SIZE,
            telegraphDurationFrames = (4 + reactionLag).coerceAtLeast(2),
            killsPlayer = false,
            quipPopup = "BYE FLOOR!"
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 13f * TILE_SIZE,
            initialY = 13f * TILE_SIZE,
            width = TILE_SIZE,
            height = TILE_SIZE,
            telegraphDurationFrames = (4 + reactionLag).coerceAtLeast(2),
            killsPlayer = false
        ),
        // Gag 2: Hidden floor seam spikes pop up right ahead of Aarav at Tile 18
        TrollTrap(
            id = 3,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 18f * TILE_SIZE,
            initialY = 13f * TILE_SIZE - 32f,
            width = 56f,
            height = 32f,
            triggerMinX = 17.0f * TILE_SIZE,
            triggerMaxX = 19.3f * TILE_SIZE,
            triggerMinY = 9f * TILE_SIZE,
            triggerMaxY = 13.5f * TILE_SIZE,
            telegraphDurationFrames = (12 + reactionLag).coerceAtLeast(12),
            activeDurationFrames = 0,
            killsPlayer = true,
            quipPopup = "SURPRISE!"
        ),
        // Gag 2b: Delayed Collapsing Plank bridge over Gap 2 (Tiles 23-24) — shakes & cracks before dropping!
        TrollTrap(
            id = 8,
            type = TrollTrapType.DELAYED_CRUMBLE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 23f * TILE_SIZE,
            initialY = 13f * TILE_SIZE,
            width = TILE_SIZE * 2f,
            height = TILE_SIZE,
            telegraphDurationFrames = (26 + reactionLag * 4).coerceAtLeast(16),
            killsPlayer = false,
            quipPopup = "CRUMBLING!"
        ),
        // Gag 3: Falling ceiling block above shiny bait Coin #3
        TrollTrap(
            id = 4,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 27.4f * TILE_SIZE,
            initialY = 5.5f * TILE_SIZE,
            width = 56f,
            height = 56f,
            triggerMinX = 26.5f * TILE_SIZE,
            triggerMaxX = 28.9f * TILE_SIZE,
            triggerMinY = 8f * TILE_SIZE,
            triggerMaxY = 13.5f * TILE_SIZE,
            telegraphDurationFrames = (8 + reactionLag).coerceAtLeast(4),
            targetY = 13f * TILE_SIZE - 56f,
            moveSpeed = 14f,
            killsPlayer = true,
            quipPopup = "BAITED!"
        ),
        // Gag 3b: Fake Safe Platform spanning Gap 3 (Tiles 32-33) — sprouts lethal spikes if trusted!
        TrollTrap(
            id = 9,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 32f * TILE_SIZE,
            initialY = 13f * TILE_SIZE,
            width = TILE_SIZE * 2f,
            height = TILE_SIZE,
            telegraphDurationFrames = (12 + reactionLag).coerceAtLeast(12),
            killsPlayer = true,
            quipPopup = "TRICKED!"
        ),
        // Gag 4: Surprise pit right after the exit door hops away!
        TrollTrap(
            id = 5,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 38f * TILE_SIZE,
            initialY = 13f * TILE_SIZE,
            width = TILE_SIZE,
            height = TILE_SIZE,
            telegraphDurationFrames = 3,
            killsPlayer = false
        ),
        TrollTrap(
            id = 6,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 39f * TILE_SIZE,
            initialY = 13f * TILE_SIZE,
            width = TILE_SIZE,
            height = TILE_SIZE,
            telegraphDurationFrames = 3,
            killsPlayer = false
        ),
        // Gag 4b: Cheeky exit door leaps from Tile 36 to Tile 42.5 when Aarav reaches it!
        TrollTrap(
            id = 7,
            type = TrollTrapType.RELOCATING_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = initialExitX,
            initialY = initialExitY,
            width = TILE_SIZE * 2f,
            height = TILE_SIZE * 3f,
            triggerMinX = initialExitX - 48f,
            triggerMaxX = initialExitX + TILE_SIZE * 2.5f,
            triggerMinY = 5f * TILE_SIZE,
            triggerMaxY = 13.5f * TILE_SIZE,
            telegraphDurationFrames = 0,
            linkedDoorTargetX = 42.5f * TILE_SIZE,
            linkedDoorTargetY = initialExitY,
            killsPlayer = false,
            quipPopup = "WAIT FOR ME!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 1,
        title = "The Easy Start",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2 * TILE_SIZE,
        spawnY = 11 * TILE_SIZE,
        exitX = initialExitX,
        exitY = initialExitY,
        parTimeSeconds = 20,
        difficulty = difficulty,
        subtitle = "Wait... That Floor Moved?",
        description = "An innocent warm-up trail hiding vanishing planks, pop-up spikes, coin bait, and a runaway door.",
        theme = LevelTheme.WHISPERING_WOODS,
        enableForestSetPieces = false,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
