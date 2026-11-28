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

    // 1. Shortened launch runway (0..6, Y = 13) -> Disappearing planks (7..8) -> perch stone (9)
    addRow(0, 6, 13f, TileType.CARVED_STONE)
    addRow(9, 9, 13f, TileType.CARVED_STONE)

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

    // 6. Final victory terrace (44..47) -> Disappearing Pit (48..49) -> Relocated Sun Gate landing (50..54, Y = 13)
    addRow(44, 47, 13f, TileType.CARVED_STONE)
    addRow(50, 54, 13f, TileType.CARVED_STONE)

    val exitX = 46.2f * T
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
        Coin(id = 1, x = 4.6f * T, y = 10.5f * T),   // Elevated intro jump bait triggering a coin curse spike!
        Coin(id = 2, x = 15.6f * T, y = 10.6f * T),  // Bait under ON_JUMP_ABOVE ceiling block
        Coin(id = 3, x = 29.8f * T, y = 8.9f * T),   // High reward along sketchy upper crumble route
        Coin(id = 4, x = 45.8f * T, y = 10.5f * T)   // Jump bait before the runaway Sun Gate
    )

    val lag = when (difficulty) {
      DifficultyTier.NORMAL -> 0
      DifficultyTier.HARD -> -1
      DifficultyTier.NIGHTMARE -> -2
    }

    val trollTraps = listOf(
        // Trap 0: Early disappearing bridge right after short start (Tiles 7..8)
        TrollTrap(
            id = 13,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 7f * T,
            initialY = 13f * T,
            width = T * 2f,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "TOO TRUSTING!"
        ),
        // Trap 1: Stone-moss trapdoor after perch tile 9 (falls out on step)
        TrollTrap(
            id = 1,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 10f * T,
            initialY = 13f * T,
            width = T * 2f,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "TRAPDOOR!"
        ),
        // Trap 2: Surprise hidden ceiling block triggered ONLY when Aarav jumps up for Coin #2
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
            telegraphDurationFrames = 0,
            targetY = 13f * T - 54f,
            moveSpeed = 15.0f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "GREEDY!"
        ),
        // Trap 2b: Hidden floor spike between Coin #2 and Fake Bell so clearing doesn't feel empty
        TrollTrap(
            id = 14,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 18.6f * T,
            initialY = 13f * T - 30f,
            width = 46f,
            height = 30f,
            triggerMinX = 17.7f * T,
            triggerMaxX = 19.7f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            activeDurationFrames = 70,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "EYES DOWN!"
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
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "NO CHECKPOINT!"
        ),
        // Trap 4a: Lower "safe" platform #1 sprouts lethal spikes immediately on footfall
        TrollTrap(
            id = 4,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 24f * T,
            initialY = 13f * T,
            width = T * 3f,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "NOT SAFE!"
        ),
        // Trap 4b: Faster Oscillating Moving Spike patrolling the lower middle stone deck (26.8 -> 29.1)
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
              DifficultyTier.NORMAL -> 96
              DifficultyTier.HARD -> 90
              DifficultyTier.NIGHTMARE -> 90
            },
            killsPlayer = true,
            quipPopup = "WATCH STEP!"
        ),
        // Trap 4c: Lower "safe" platform #2 also sprouts spikes immediately on contact
        TrollTrap(
            id = 6,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 30f * T,
            initialY = 13f * T,
            width = T * 3f,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "BAITED!"
        ),
        // Real Path Upper 1: Sharper delayed crumbling plank (actually safe if you keep moving!)
        TrollTrap(
            id = 7,
            type = TrollTrapType.DELAYED_CRUMBLE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 27.2f * T,
            initialY = 10.4f * T,
            width = T * 1.8f,
            height = T * 0.7f,
            telegraphDurationFrames = (17 + lag).coerceAtLeast(14),
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
            telegraphDurationFrames = (16 + lag).coerceAtLeast(14),
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
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
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
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
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
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "GOTCHA!"
        ),
        // Trap 7: Runaway Sun Gate leaps past one final disappearing gap (48..49) onto 51.2T!
        TrollTrap(
            id = 12,
            type = TrollTrapType.RELOCATING_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = exitX,
            initialY = exitY,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 45.2f * T,
            triggerMaxX = 48.2f * T,
            triggerMinY = 8.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            linkedDoorTargetX = 51.2f * T,
            linkedDoorTargetY = exitY,
            killsPlayer = false,
            quipPopup = "EVEN THE DOOR?!"
        ),
        // Trap 8: Disappearing planks right behind the runaway Sun Gate (48..49)
        TrollTrap(
            id = 15,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 48f * T,
            initialY = 13f * T,
            width = T * 2f,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "ONE MORE PIT!"
        ),
        // Trap 9: Coin #1 jump bait triggers an early floor spike right ahead (5.6T)
        TrollTrap(
            id = 16,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 1,
            chainDelayFrames = 4,
            initialX = 5.6f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 55,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "COIN CURSE!"
        ),
        // Trap 10: Optional Coin #4 pops a last-second spike at 47.0T while chasing the gate
        TrollTrap(
            id = 17,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 4,
            chainDelayFrames = 2,
            initialX = 47.0f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 55,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "NOT FREE!"
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
