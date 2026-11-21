package com.example.data

/**
 * Challenge 11: "Trust Nothing"
 *
 * Central Mechanics: BETRAYAL_SWITCH, ON_COIN_COLLECTED, & ON_TRAP_ACTIVATED.
 * Demonstrates event-driven trap chaining without recursion leaks or duplicate coin triggers.
 */
object Challenge11TrustNothing {
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

    // 1. Opening & Switch Chain Arena (0..15, Y=13)
    addRow(0, 15, 13f, TileType.CARVED_STONE)

    // 2. Coin Greed Arena (16..24, Y=13)
    addRow(16, 24, 13f, TileType.CARVED_STONE)

    // 3. Checkpoint Sanctuary (25..29, Y=13)
    addRow(25, 29, 13f, TileType.CARVED_STONE)
    tiles.add(
        GameTile(
            id = nextId++,
            x = 26.5f * T,
            y = 12f * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )

    // 4. Phantom Coin Bridge & Final Switch Pursuit (30..50, Y=13)
    addRow(30, 33, 13f, TileType.CARVED_STONE)
    addRow(38, 50, 13f, TileType.CARVED_STONE)
    val exitX = 46.8f * T
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
        Coin(id = 2, x = 19.0f * T, y = 11.2f * T), // Triggers Ceiling Block #4!
        Coin(id = 3, x = 32.2f * T, y = 11.2f * T), // Triggers Phantom Ledge #5!
        Coin(id = 4, x = 35.6f * T, y = 10.2f * T),
        Coin(id = 5, x = 44.5f * T, y = 11.5f * T)
    )

    val trollTraps = listOf(
        // Trap 1: Betrayal Switch -> chains into Trap 2 (Spike Pop)
        TrollTrap(
            id = 1,
            type = TrollTrapType.BETRAYAL_SWITCH,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 9.0f * T,
            initialY = 13f * T - 24f,
            width = 48f,
            height = 24f,
            telegraphDurationFrames = 0,
            linkedTrapId = 2,
            killsPlayer = false,
            quipPopup = "TRAP ARMED!"
        ),
        // Trap 2: Triggered by Trap 1, then chains into Trap 3 (Ceiling Block)
        TrollTrap(
            id = 2,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 1,
            linkedTrapId = 3,
            chainDelayFrames = 8,
            initialX = 11.4f * T,
            initialY = 13f * T - 30f,
            width = 52f,
            height = 30f,
            telegraphDurationFrames = 12,
            activeDurationFrames = 65,
            killsPlayer = true,
            quipPopup = "CHAINED!"
        ),
        // Trap 3: Triggered by Trap 2!
        TrollTrap(
            id = 3,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 2,
            chainDelayFrames = 10,
            initialX = 13.6f * T,
            initialY = 6.0f * T,
            width = 52f,
            height = 54f,
            targetY = 13f * T - 54f,
            telegraphDurationFrames = 12,
            moveSpeed = 12f,
            killsPlayer = true,
            quipPopup = "SECOND DROP!"
        ),
        // Trap 4: Triggered by collecting Coin #2!
        TrollTrap(
            id = 4,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 2,
            chainDelayFrames = 4,
            initialX = 18.5f * T,
            initialY = 6.0f * T,
            width = 52f,
            height = 54f,
            targetY = 13f * T - 54f,
            telegraphDurationFrames = 14,
            moveSpeed = 12.5f,
            killsPlayer = true,
            quipPopup = "GREED TAX!"
        ),
        // Trap 5: Phantom Ledge bridged over pit (34..37), drops when Coin #3 collected
        TrollTrap(
            id = 5,
            type = TrollTrapType.PHANTOM_LEDGE,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 3,
            chainDelayFrames = 6,
            initialX = 34.4f * T,
            initialY = 12.4f * T,
            width = 2.6f * T,
            height = 24f,
            telegraphDurationFrames = 12,
            cycleFrames = 100,
            killsPlayer = false,
            quipPopup = "COIN CURSE!"
        ),
        // Trap 6 -> Trap 7: Second Betrayal Switch triggers Trailing Spike
        TrollTrap(
            id = 6,
            type = TrollTrapType.BETRAYAL_SWITCH,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 39.2f * T,
            initialY = 13f * T - 24f,
            width = 48f,
            height = 24f,
            telegraphDurationFrames = 0,
            linkedTrapId = 7,
            killsPlayer = false,
            quipPopup = "WHY PRESS IT?!"
        ),
        TrollTrap(
            id = 7,
            type = TrollTrapType.TRAILING_SPIKE,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 6,
            chainDelayFrames = 6,
            initialX = 38.2f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 12,
            targetX = 43.2f * T,
            targetY = 13f * T - 30f,
            moveSpeed = 4.6f,
            activeDurationFrames = 220,
            killsPlayer = true,
            quipPopup = "LEVER SURPRISE!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 11,
        title = "Trust Nothing",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = exitX,
        exitY = exitY,
        parTimeSeconds = 30,
        difficulty = difficulty,
        subtitle = "Betrayal Switches & Coin Traps",
        description = "Every tempting coin and floor switch can wake a chained trap down the corridor — stay one step ahead.",
        theme = LevelTheme.RUINS_OF_LOST_KING,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
