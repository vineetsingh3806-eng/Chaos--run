package com.example.data

/**
 * World 1 — Level 1-2: "The Fake Safe Path"
 *
 * Central Mechanic: Trust nothing you see — pristine flat stone paths sprout spikes or drop out
 * while sketchy upper crumbling planks carry you safely to the exit. Includes an ON_JUMP_ABOVE
 * ceiling trap over bait Coin #2, a Fake Checkpoint Bell betrayal, and a patrolling Moving Spike.
 */
object Level12FakeSafePath {
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

    // 1. Safe intro runway (Tiles 0 - 9, Y = 13)
    addRow(0, 9, 13f, TileType.CARVED_STONE)

    // (Tiles 10 - 11 = Trapdoor FAKE_SAFE_PLATFORM, Tile 12 = pit gap)

    // 2. Coin-bait landing clearing (Tiles 13 - 23, Y = 13) — hosts greedy Coin #2 + Fake Bell
    addRow(13, 23, 13f, TileType.CARVED_STONE)

    // 3. The Signature Fork (Tiles 24 - 33):
    //    Upper "sketchy" path: stone step (24.5..25.5, Y=11) + 2 crumbling plank bridges (27..28.5 & 30.5..32)
    tiles.add(GameTile(id = nextId++, x = 24.5f * T, y = 11f * T, width = T * 1.5f, height = T * 0.6f, type = TileType.MOSSY_TIMBER))

    //    Lower "safe-looking" wide stone path (Tiles 24 - 33, Y = 13) hides spike/trapdoor tiles + moving spike!
    addRow(27, 29, 13f, TileType.CARVED_STONE)

    // 4. Rejoin clearing (Tiles 34 - 37, Y = 13)
    addRow(34, 37, 13f, TileType.CARVED_STONE)

    // (Tiles 38 - 39 = Disappearing Floor planks)

    // 5. Small mid-chasm island (Tiles 40 - 41, Y = 13)
    addRow(40, 41, 13f, TileType.CARVED_STONE)

    // (Tiles 42 - 43 = Drop Trapdoor FAKE_SAFE_PLATFORM)

    // 6. Final victory terrace (Tiles 44 - 50, Y = 13)
    addRow(44, 50, 13f, TileType.CARVED_STONE)

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
        Coin(id = 1, x = 6.0f * T, y = 11.5f * T),   // Safe intro coin
        Coin(id = 2, x = 15.6f * T, y = 10.6f * T),  // Bait under ON_JUMP_ABOVE ceiling block
        Coin(id = 3, x = 29.8f * T, y = 8.9f * T),   // High reward along sketchy upper crumble route
        Coin(id = 4, x = 46.2f * T, y = 11.5f * T)   // Final jump coin before Sun Gate
    )

    val lag = when (difficulty) {
      DifficultyTier.NORMAL -> 0
      DifficultyTier.HARD -> -1
      DifficultyTier.NIGHTMARE -> -2
    }

    val trollTraps = listOf(
        // Trap 1: Stone-moss trapdoor right after intro runway (falls out on step)
        TrollTrap(
            id = 1,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 10f * T,
            initialY = 13f * T,
            width = T * 2f,
            height = T,
            telegraphDurationFrames = (5 + lag).coerceAtLeast(2),
            killsPlayer = false,
            quipPopup = "TRAPDOOR!"
        ),
        // Trap 2: Hidden ceiling block triggered ONLY when Aarav jumps up for Coin #2
        TrollTrap(
            id = 2,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_JUMP_ABOVE,
            initialX = 15.0f * T,
            initialY = 5.5f * T,
            width = 54f,
            height = 54f,
            triggerMinX = 14.3f * T,
            triggerMaxX = 16.5f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 13.4f * T,
            telegraphDurationFrames = (7 + lag).coerceAtLeast(3),
            targetY = 13f * T - 54f,
            moveSpeed = 14.5f,
            killsPlayer = true,
            quipPopup = "GREEDY!"
        ),
        // Trap 3: Fake Checkpoint Bell on Tile 22 — betrays anyone rushing to save!
        TrollTrap(
            id = 3,
            type = TrollTrapType.FAKE_CHECKPOINT,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 22f * T,
            initialY = 12f * T,
            width = T,
            height = T,
            triggerMinX = 21.6f * T,
            triggerMaxX = 23.1f * T,
            triggerMinY = 11.4f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = (5 + lag).coerceAtLeast(2),
            killsPlayer = true,
            quipPopup = "NO CHECKPOINT!"
        ),
        // Trap 4a: Lower "safe" platform #1 sprouts lethal spikes on footfall
        TrollTrap(
            id = 4,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 24f * T,
            initialY = 13f * T,
            width = T * 3f,
            height = T,
            telegraphDurationFrames = (5 + lag).coerceAtLeast(2),
            killsPlayer = true,
            quipPopup = "NOT SAFE!"
        ),
        // Trap 4b: Oscillating Moving Spike patrolling the lower middle stone deck (26.8 -> 29.3)
        TrollTrap(
            id = 5,
            type = TrollTrapType.MOVING_SPIKE,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 26.8f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            targetX = 29.1f * T,
            targetY = 13f * T - 30f,
            cycleFrames = when (difficulty) {
              DifficultyTier.NORMAL -> 115
              DifficultyTier.HARD -> 98
              DifficultyTier.NIGHTMARE -> 90
            },
            killsPlayer = true,
            quipPopup = "WATCH STEP!"
        ),
        // Trap 4c: Lower "safe" platform #2 also sprouts spikes on contact
        TrollTrap(
            id = 6,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 30f * T,
            initialY = 13f * T,
            width = T * 3f,
            height = T,
            telegraphDurationFrames = (5 + lag).coerceAtLeast(2),
            killsPlayer = true,
            quipPopup = "BAITED!"
        ),
        // Real Path Upper 1: Sketchy delayed crumbling plank (actually safe if you keep moving!)
        TrollTrap(
            id = 7,
            type = TrollTrapType.DELAYED_CRUMBLE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 27.2f * T,
            initialY = 10.4f * T,
            width = T * 1.8f,
            height = T * 0.7f,
            telegraphDurationFrames = (25 + lag * 3).coerceAtLeast(16),
            killsPlayer = false,
            quipPopup = "KEEP MOVING!"
        ),
        // Real Path Upper 2: Second delayed crumbling plank leading back to stone clearing
        TrollTrap(
            id = 8,
            type = TrollTrapType.DELAYED_CRUMBLE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 31.0f * T,
            initialY = 10.4f * T,
            width = T * 1.8f,
            height = T * 0.7f,
            telegraphDurationFrames = (25 + lag * 3).coerceAtLeast(16),
            killsPlayer = false
        ),
        // Trap 5: Disappearing floor tiles (38..39)
        TrollTrap(
            id = 9,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 38f * T,
            initialY = 13f * T,
            width = T,
            height = T,
            telegraphDurationFrames = (4 + lag).coerceAtLeast(2),
            killsPlayer = false,
            quipPopup = "POOF!"
        ),
        TrollTrap(
            id = 10,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 39f * T,
            initialY = 13f * T,
            width = T,
            height = T,
            telegraphDurationFrames = (4 + lag).coerceAtLeast(2),
            killsPlayer = false
        ),
        // Trap 6: Second trapdoor Fake Safe bridge before final terrace (42..43)
        TrollTrap(
            id = 11,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 42f * T,
            initialY = 13f * T,
            width = T * 2f,
            height = T,
            telegraphDurationFrames = (5 + lag).coerceAtLeast(2),
            killsPlayer = false,
            quipPopup = "GOTCHA!"
        ),
        // Trap 7: Final cheek pop-up spike just before the exit gate!
        TrollTrap(
            id = 12,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 45.6f * T,
            initialY = 13f * T - 30f,
            width = 48f,
            height = 30f,
            triggerMinX = 44.7f * T,
            triggerMaxX = 46.8f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = (6 + lag).coerceAtLeast(3),
            killsPlayer = true,
            quipPopup = "ONE MORE!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 2,
        title = "The Fake Safe Path",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 22,
        difficulty = difficulty,
        subtitle = "Trust Nothing You See",
        description = "Smooth flat bridges lie, the checkpoint bell bites, and only the sketchy upper planks hold true.",
        theme = LevelTheme.HOLLOW_OF_ECHOES,
        hasDarknessLantern = false,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
