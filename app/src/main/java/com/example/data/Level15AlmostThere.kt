package com.example.data

/**
 * World 1 — Level 1-5: "Almost There"
 *
 * Central Mechanic: World 1 Troll Finale — chains every mechanic from 1-1..1-4 (disappearing
 * planks, proximity ambush spike, delayed crumble, timed blink platform, moving crush wall,
 * fake checkpoint bell, jump-above ceiling block, spiked fake safe platform) into a multi-part
 * Final Surprise: Fake Exit Door -> Relocating Sun Gate up to the High Sanctum -> Final Spike Pop!
 */
object Level15AlmostThere {
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

    // 1. Opening launchpad (Tiles 0 - 6, Y = 13) -> Disappearing planks (7 - 8) -> Ambush terrace (9 - 14)
    addRow(0, 6, 13f, TileType.CARVED_STONE)
    addRow(9, 14, 13f, TileType.CARVED_STONE)

    // (Tile 15 = pit, Tiles 16 - 17 = DELAYED_CRUMBLE, Tile 18 - 19 = pit, Tiles 20 - 22 = TIMED_BLINK_PLATFORM)

    // 2. Crusher & Fake Bell Sanctum (Tiles 23 - 30, Y = 13)
    addRow(23, 30, 13f, TileType.CARVED_STONE)

    // 3. Ceiling Bait Bridge (Tiles 32 - 36, Y = 13) after 1-tile pit (Tile 31)
    addRow(32, 36, 13f, TileType.CARVED_STONE)

    // (Tile 37 = pit, Tiles 38 - 39 = Spiked FAKE_SAFE_PLATFORM, Tiles 40 - 41 = DELAYED_CRUMBLE, Tile 42 = pit)

    // 4. Lower Finale Bait Arena (Tiles 43 - 53, Y = 13)
    addRow(43, 53, 13f, TileType.CARVED_STONE)
    // Raised jump step (43..44, Y = 11.5) to clear the Fake Exit Door cleanly
    addRow(43, 44, 11.5f, TileType.CARVED_STONE)

    // Stepping stone (54.5..55.5, Y = 11.2) leading up to the elevated High Sanctum
    tiles.add(GameTile(id = nextId++, x = 54.5f * T, y = 11.2f * T, width = T * 1.4f, height = T * 0.7f, type = TileType.CARVED_STONE))

    // 5. High Finale Sanctum (Tiles 57 - 58, 59 Disappearing Floor, 60 - 64, Y = 9.5) — destination of relocated gate!
    addRow(57, 58, 9.5f, TileType.CARVED_STONE)
    addRow(60, 64, 9.5f, TileType.CARVED_STONE)

    // True SUN_GATE initially sits on Lower Finale Arena (Tile 51.5, Y = 10) before hopping up!
    val initialExitX = 51.5f * T
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
    // Real mid-stage checkpoint rewarding players who survive the Crusher & Fake Bell
    tiles.add(
        GameTile(
            id = nextId++,
            x = 32.2f * T,
            y = 12.0f * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )

    val coins = listOf(
        Coin(id = 1, x = 4.5f * T, y = 10.4f * T),    // Opening jump bait coin triggering early tax spike
        Coin(id = 2, x = 21.2f * T, y = 10.0f * T),   // Over Timed Blink Platform
        Coin(id = 3, x = 34.4f * T, y = 10.6f * T),   // Bait under ON_JUMP_ABOVE ceiling block
        Coin(id = 4, x = 38.8f * T, y = 10.4f * T),   // High arc above spiked fake safe bridge
        Coin(id = 5, x = 58.9f * T, y = 7.4f * T)     // Reward over the final surprise spike!
    )

    val lag = when (difficulty) {
      DifficultyTier.NORMAL -> 0
      DifficultyTier.HARD -> -1
      DifficultyTier.NIGHTMARE -> -2
    }

    val trollTraps = listOf(
        // 1. Disappearing planks at opening bridge (7 - 8)
        TrollTrap(
            id = 1,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 7f * T,
            initialY = 13f * T,
            width = T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        TrollTrap(
            id = 2,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 8f * T,
            initialY = 13f * T,
            width = T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE
        ),
        // 2. Proximity ambush Moving Spike charging left across Tiles 13.5 -> 9.6
        TrollTrap(
            id = 3,
            type = TrollTrapType.MOVING_SPIKE,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 13.5f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            triggerMinX = 9.8f * T,
            triggerMaxX = 12.6f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 6,
            targetX = 9.6f * T,
            targetY = 13f * T - 30f,
            moveSpeed = if (difficulty == DifficultyTier.NIGHTMARE) 11.0f else 10.0f,
            killsPlayer = true,
            quipPopup = "INCOMING!"
        ),
        // 3. Delayed Crumble bridge (Tiles 16 - 17, Y = 12.2)
        TrollTrap(
            id = 4,
            type = TrollTrapType.DELAYED_CRUMBLE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 16f * T,
            initialY = 12.2f * T,
            width = T * 2f,
            height = T * 0.8f,
            telegraphDurationFrames = (15 + lag).coerceAtLeast(14),
            killsPlayer = false
        ),
        // 4. Faster Timed Blink Platform over mid-gauntlet chasm (20.0..22.5, Y = 12.0)
        TrollTrap(
            id = 5,
            type = TrollTrapType.TIMED_BLINK_PLATFORM,
            triggerType = TrapTriggerType.ON_TIMER_CYCLE,
            initialX = 20.0f * T,
            initialY = 12.0f * T,
            width = T * 2.5f,
            height = 28f,
            cycleFrames = when (difficulty) {
              DifficultyTier.NORMAL -> 122
              DifficultyTier.HARD -> 120
              DifficultyTier.NIGHTMARE -> 120
            },
            phaseOffsetFrames = 0,
            killsPlayer = false
        ),
        // 5. Moving Crush Wall rushing left across Tiles 28.5 -> 24.5
        TrollTrap(
            id = 6,
            type = TrollTrapType.MOVING_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 28.5f * T,
            initialY = 13f * T - 80f,
            width = T,
            height = 80f,
            triggerMinX = 24.8f * T,
            triggerMaxX = 27.6f * T,
            triggerMinY = 9.0f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 6,
            targetX = 24.5f * T,
            targetY = 13f * T - 80f,
            moveSpeed = if (difficulty == DifficultyTier.NIGHTMARE) 11.2f else 10.5f,
            killsPlayer = true,
            quipPopup = "NO RUSH!"
        ),
        // 6. Fake Checkpoint Bell right at edge of Crusher Terrace (Tile 29.8)
        TrollTrap(
            id = 7,
            type = TrollTrapType.FAKE_CHECKPOINT,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 29.8f * T,
            initialY = 12f * T,
            width = T,
            height = T,
            triggerMinX = 29.4f * T,
            triggerMaxX = 30.9f * T,
            triggerMinY = 11.4f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "NICE TRY!"
        ),
        // 7. Instant ON_JUMP_ABOVE Ceiling Thwomp over Coin #3 (Tile 34.4)
        TrollTrap(
            id = 8,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_JUMP_ABOVE,
            initialX = 33.8f * T,
            initialY = 5.5f * T,
            width = 54f,
            height = 54f,
            triggerMinX = 33.2f * T,
            triggerMaxX = 35.4f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 13.4f * T,
            telegraphDurationFrames = 0,
            targetY = 13f * T - 54f,
            moveSpeed = 15.5f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "GREED CHECK!"
        ),
        // 8. Spiked Fake Safe Platform (38..39, Y=13) — jump straight past to crumbling planks!
        TrollTrap(
            id = 9,
            type = TrollTrapType.FAKE_SAFE_PLATFORM,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 38f * T,
            initialY = 13f * T,
            width = T * 1.8f,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "SPIKED!"
        ),
        // 9. Delayed Crumble recovery span (40.2..41.8, Y=13)
        TrollTrap(
            id = 10,
            type = TrollTrapType.DELAYED_CRUMBLE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 40.2f * T,
            initialY = 13f * T,
            width = T * 1.6f,
            height = T,
            telegraphDurationFrames = (15 + lag).coerceAtLeast(14),
            killsPlayer = false
        ),
        // 10. FINAL SURPRISE PART A: Lethal Fake Exit Door at Tile 46 (jump over from Y=11.5 step!)
        TrollTrap(
            id = 11,
            type = TrollTrapType.FAKE_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = 46.0f * T,
            initialY = 10.0f * T,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 45.2f * T,
            triggerMaxX = 48.2f * T,
            triggerMinY = 10.3f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "ALMOST... FAKE!"
        ),
        // 10b. Ambush Ceiling Block right past Fake Exit Door before Lower Gate!
        TrollTrap(
            id = 14,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 49.0f * T,
            initialY = 6.0f * T,
            width = 50f,
            height = 50f,
            triggerMinX = 48.2f * T,
            triggerMaxX = 50.2f * T,
            triggerMinY = 6.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            targetY = 13f * T - 50f,
            moveSpeed = 14.8f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "EYES UP!"
        ),
        // 11. FINAL SURPRISE PART B: Relocating Exit Door leaps from Tile 51.5 -> High Sanctum Tile 60.5!
        TrollTrap(
            id = 12,
            type = TrollTrapType.RELOCATING_EXIT_DOOR,
            triggerType = TrapTriggerType.ON_APPROACH_EXIT,
            initialX = initialExitX,
            initialY = initialExitY,
            width = T * 2f,
            height = T * 3f,
            triggerMinX = 50.6f * T,
            triggerMaxX = 54.0f * T,
            triggerMinY = 9.5f * T,
            triggerMaxY = 13.5f * T,
            telegraphDurationFrames = 0,
            linkedDoorTargetX = 60.5f * T,
            linkedDoorTargetY = 6.5f * T,
            linkedTrapId = 16,
            killsPlayer = false,
            quipPopup = "ONE LAST CLIMB!"
        ),
        // 12. FINAL SURPRISE PART C: Cheeky pop-up spike right on the High Sanctum threshold!
        TrollTrap(
            id = 13,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 58.4f * T,
            initialY = 9.5f * T - 30f,
            width = 50f,
            height = 30f,
            triggerMinX = 57.5f * T,
            triggerMaxX = 59.8f * T,
            triggerMinY = 6.0f * T,
            triggerMaxY = 10.0f * T,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "LAST TROLL!"
        ),
        // 13. Early Coin #1 jump bait pops a floor spike right after it (Tile 5.6)
        TrollTrap(
            id = 15,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 1,
            chainDelayFrames = 2,
            initialX = 5.6f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 55,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "EARLY TAX!"
        ),
        // 14. Chained Disappearing Tile at High Sanctum (Tile 59) drops out when Sun Gate flees up!
        TrollTrap(
            id = 16,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 12,
            chainDelayFrames = 0,
            initialX = 59.0f * T,
            initialY = 9.5f * T,
            width = T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "MIND THE STEP!"
        )
    )

    return LevelDefinition(
        worldId = 1,
        levelId = 5,
        title = "Almost There",
        tiles = tiles,
        coins = coins,
        movingPlatforms = emptyList(),
        spawnX = 2f * T,
        spawnY = 11f * T,
        exitX = initialExitX,
        exitY = initialExitY,
        parTimeSeconds = 30,
        difficulty = difficulty,
        subtitle = "One Final Troll",
        description = "Every World 1 trick collides in a fast final gauntlet — beware fake doors, hopping gates, and one last spike!",
        theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
        trollTraps = trollTraps,
        isTrollLevel = true
    )
  }
}
