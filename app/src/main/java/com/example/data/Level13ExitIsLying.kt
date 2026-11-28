package com.example.data

/**
 * World 1 — Level 1-3: "The Exit Is Lying"
 *
 * Central Mechanic: Decoy Fake Exit Door near the beginning plus a chained multi-hop
 * Relocating Exit Door that flees upward and onward, guarded by an ambush Moving Crush Wall.
 */
object Level13ExitIsLying {
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

    // 1. Shortened launch (0..4) -> Disappearing Floor planks (5..6) -> Decoy exit arena (7..17, Y = 13)
    addRow(0, 4, 13f, TileType.CARVED_STONE)
    addRow(7, 17, 13f, TileType.CARVED_STONE)
    // Raised stone step (10..11, Y=11.5) giving clean jump clearance OVER the decoy Fake Exit Door at Tile 13
    addRow(10, 11, 11.5f, TileType.CARVED_STONE)

    // 2. Crusher corridor (Tiles 19 - 29, Y = 13) after a crisp 1-tile pit gap (Tile 18)
    addRow(19, 29, 13f, TileType.CARVED_STONE)

    // (Tile 30 = pit, Tiles 31 - 32 = DELAYED_CRUMBLE bridge, Tile 33 = pit)

    // 3. First Exit Door Threshold (Tiles 34 - 42, Y = 13)
    addRow(34, 42, 13f, TileType.CARVED_STONE)

    // Stepping slab (42.3..43.3, Y = 11.5) up toward Upper Sky Terrace
    tiles.add(GameTile(id = nextId++, x = 42.3f * T, y = 11.5f * T, width = T * 1.4f, height = T * 0.7f, type = TileType.CARVED_STONE))

    // 4. Upper Sky Terrace (Tiles 44 - 48, Y = 10.0) — hosts Hop #2 of the runaway Sun Gate
    addRow(44, 48, 10.0f, TileType.CARVED_STONE)

    // (Tile 49 = DISAPPEARING_FLOOR drop pad at Y = 11.5 guarding final descent)

    // 5. Final Real Exit Terrace (Tiles 50 - 56, Y = 13)
    addRow(50, 56, 13f, TileType.CARVED_STONE)

    // Initial placement of the real SUN_GATE on Lower Platform (Tile 39.5, Y = 10)
    // It hops -> (45.5*T, 7.0*T) on Upper Terrace -> (52.5*T, 10.0*T) on Final Terrace!
    val initialExitX = 39.5f * T
    val initialExitY = 10.0f * T
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
    // Fair mid-stage checkpoint after surviving the crusher corridor & delayed crumble
    tiles.add(
        GameTile(
            id = nextId++,
            x = 35.0f * T,
            y = 12.0f * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )

    val coins = listOf(
        Coin(id = 1, x = 3.6f * T, y = 10.4f * T),    // Jump bait coin arming a surprise ahead on landing!
        Coin(id = 2, x = 14.0f * T, y = 8.9f * T),    // Floats above Fake Exit Door to clue jumping over
        Coin(id = 3, x = 42.9f * T, y = 10.0f * T),   // Step reward toward sky terrace
        Coin(id = 4, x = 49.5f * T, y = 8.8f * T)     // Leap coin into final gate arena
    )

    val lag = when (difficulty) {
      DifficultyTier.NORMAL -> 0
      DifficultyTier.HARD -> -1
      DifficultyTier.NIGHTMARE -> -2
    }

    val trollTraps = listOf(
        // Trap 0: Opening Disappearing Floor planks (5..6) — breaks up the long warm-up run
        TrollTrap(
            id = 9,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 5f * T,
            initialY = 13f * T,
            width = T * 2f,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "WAKE UP!"
        ),
        // Trap 1: Lethal FAKE_EXIT_DOOR baiting players right at Tile 13!
        TrollTrap(
            id = 1,
            type = TrollTrapType.FAKE_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 13.0f * T,
            initialY = 10.0f * T,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 12.2f * T,
            triggerMaxX = 15.2f * T,
            triggerMinY = 10.3f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "TOO EARLY!"
        ),
        // Trap 2: Faster Sliding Moving Crush Wall slamming left across Tiles 28 -> 22
        TrollTrap(
            id = 2,
            type = TrollTrapType.MOVING_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 28.0f * T,
            initialY = 13f * T - 80f,
            width = T,
            height = 80f,
            triggerMinX = 22.4f * T,
            triggerMaxX = 25.2f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = (6 + lag).coerceAtLeast(6),
            targetX = 22.0f * T,
            targetY = 13f * T - 80f,
            moveSpeed = if (difficulty == DifficultyTier.NIGHTMARE) 11.2f else 10.2f,
            killsPlayer = true,
            quipPopup = "PERSONAL SPACE!"
        ),
        // Trap 3: Cheeky Surprise Spike hidden halfway down the crusher corridor (Tile 26)
        TrollTrap(
            id = 3,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 26.0f * T,
            initialY = 13f * T - 30f,
            width = 48f,
            height = 30f,
            triggerMinX = 25.0f * T,
            triggerMaxX = 27.2f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SURPRISE!"
        ),
        // Trap 4: Tighter Delayed Crumble bridge over Gap (Tiles 31 - 32)
        TrollTrap(
            id = 4,
            type = TrollTrapType.DELAYED_CRUMBLE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 31f * T,
            initialY = 13f * T,
            width = T * 2f,
            height = T,
            telegraphDurationFrames = (16 + lag).coerceAtLeast(14),
            killsPlayer = false,
            quipPopup = "KEEP RUNNING!"
        ),
        // Trap 4b: Surprise Ceiling Block before Initial Sun Gate (Tile 37.6) while player rushes!
        TrollTrap(
            id = 10,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 37.6f * T,
            initialY = 6.0f * T,
            width = 50f,
            height = 50f,
            triggerMinX = 36.8f * T,
            triggerMaxX = 38.6f * T,
            triggerMinY = 6.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            targetY = 13f * T - 50f,
            moveSpeed = 14.5f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "DOOR GUARD!"
        ),
        // Trap 5: Runaway Exit Hop #1 — leaps upward onto Upper Sky Terrace!
        TrollTrap(
            id = 5,
            type = TrollTrapType.RELOCATING_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = initialExitX,
            initialY = initialExitY,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 38.4f * T,
            triggerMaxX = 41.8f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            linkedDoorTargetX = 45.5f * T,
            linkedDoorTargetY = 7.0f * T,
            killsPlayer = false,
            quipPopup = "UP HERE!"
        ),
        // Trap 6: Runaway Exit Hop #2 — leaps again from Upper Terrace down to Final Terrace!
        TrollTrap(
            id = 6,
            type = TrollTrapType.RELOCATING_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 45.5f * T,
            initialY = 7.0f * T,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 44.6f * T,
            triggerMaxX = 48.0f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 10.5f * T,
            telegraphDurationFrames = 0,
            linkedDoorTargetX = 52.5f * T,
            linkedDoorTargetY = 10.0f * T,
            killsPlayer = false,
            quipPopup = "PSYCH! LAST TIME!"
        ),
        // Trap 7: Disappearing stepping block between Sky Terrace and Final Terrace
        TrollTrap(
            id = 7,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 49f * T,
            initialY = 11.5f * T,
            width = T,
            height = T * 0.7f,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        // Trap 8: Final Betrayal Switch right in front of the resting Sun Gate triggers a ceiling slam!
        TrollTrap(
            id = 8,
            type = TrollTrapType.BETRAYAL_SWITCH,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 51.0f * T,
            initialY = 13f * T - 22f,
            width = 44f,
            height = 22f,
            telegraphDurationFrames = 0,
            linkedTrapId = 11,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "FINALLY?!"
        ),
        // Trap 9: Chained finale Ceiling Slam (Tile 51.8) fired instantaneously by Trap #8!
        TrollTrap(
            id = 11,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 8,
            chainDelayFrames = 0,
            initialX = 51.8f * T,
            initialY = 6.2f * T,
            width = 50f,
            height = 50f,
            telegraphDurationFrames = 0,
            targetY = 13f * T - 50f,
            moveSpeed = 14.5f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "ONE LAST SLAM!"
        ),
        // Trap 10: Greedy early Coin #1 activates a temporary floor spike right after Gap 5..6!
        TrollTrap(
            id = 12,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 1,
            chainDelayFrames = 4,
            initialX = 7.4f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 55,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "TAX COLLECTED!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 3,
        title = "The Exit Is Lying",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = initialExitX,
        exitY = initialExitY,
        parTimeSeconds = 24,
        difficulty = difficulty,
        subtitle = "So Close Yet So Far",
        description = "Beware fake Sun Gates with sharp teeth, sliding stone walls, and an exit that refuses to stand still.",
        theme = LevelTheme.RUINS_OF_LOST_KING,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
