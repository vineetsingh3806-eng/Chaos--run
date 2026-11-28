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

    // 1. Launchpad (0..3) -> Split Bridge (4..5) -> Fake Door Gauntlet (6..13, Y=13) with step stone (7..8.5, Y=11.4)
    addRow(0, 3, 13f, TileType.CARVED_STONE)
    addRow(6, 13, 13f, TileType.CARVED_STONE)
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

    // 4. Fleeing Sun Gate Plaza (32..35 solid -> 36 disappearing -> 38..47 solid, Y=13)
    addRow(32, 35, 13f, TileType.CARVED_STONE)
    addRow(38, 47, 13f, TileType.CARVED_STONE)

    val initialExitX = 34.0f * T
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
        Coin(id = 1, x = 4.8f * T, y = 10.5f * T),
        Coin(id = 2, x = 10.6f * T, y = 8.6f * T),
        Coin(id = 3, x = 27.0f * T, y = 8.6f * T),
        Coin(id = 4, x = 38.6f * T, y = 11.5f * T),
        Coin(id = 5, x = 42.8f * T, y = 11.5f * T)
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
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
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
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
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
            telegraphDurationFrames = 0,
            effectDurationFrames = 55,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
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
            telegraphDurationFrames = 6,
            targetX = 39.2f * T,
            targetY = 10.0f * T,
            secondaryX = 44.0f * T,
            secondaryY = 10.0f * T,
            maxActivations = 2,
            killsPlayer = false,
            quipPopup = "OVER HERE!"
        ),
        // Combo: Slam Cage #3 triggers an instant ceiling keystone just outside the exit bar around cage release
        TrollTrap(
            id = 5,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 3,
            chainDelayFrames = 44,
            initialX = 29.5f * T,
            initialY = 6.2f * T,
            width = 50f,
            height = 52f,
            telegraphDurationFrames = 0,
            targetX = 29.5f * T,
            targetY = 13f * T - 52f,
            moveSpeed = 13.5f,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "FREEDOM DROP!"
        ),
        // Mid-plaza disappearing floor between Sun Gate initial perch and first hop
        TrollTrap(
            id = 6,
            type = TrollTrapType.DISAPPEARING_FLOOR,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 36.0f * T,
            initialY = 13f * T,
            width = 1.6f * T,
            height = T,
            telegraphDurationFrames = 0,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "MIND THE PLAZA!"
        ),
        // Combo: Fleeing Sun Gate #4 wakes an instant pursuit spike mid-chase
        TrollTrap(
            id = 7,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 4,
            chainDelayFrames = 16,
            initialX = 41.5f * T,
            initialY = 13f * T - 30f,
            width = 44f,
            height = 30f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 65,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "EYES DOWN!"
        ),
        // Opening Split Bridge breaks up the long intro sprint
        TrollTrap(
            id = 8,
            type = TrollTrapType.SPLIT_BRIDGE,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 4.0f * T,
            initialY = 13f * T,
            width = 2.0f * T,
            height = T,
            telegraphDurationFrames = 12,
            killsPlayer = false,
            quipPopup = "SPLIT START!"
        ),
        // Coin #1 above Split Bridge triggers a spike right at the Fake Exit step approach!
        TrollTrap(
            id = 9,
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
            quipPopup = "DOOR TAX!"
        ),
        // Coin #2 above Fake Exit #1 triggers a ceiling slab over the exit lane
        TrollTrap(
            id = 10,
            type = TrollTrapType.FALLING_CEILING_BLOCK,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 2,
            chainDelayFrames = 2,
            initialX = 12.2f * T,
            initialY = 5.8f * T,
            width = 48f,
            height = 48f,
            targetX = 12.2f * T,
            targetY = 13f * T - 48f,
            moveSpeed = 14.0f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "ROOF COLLAPSE!"
        ),
        // Delayed betrayal switch right of Checkpoint Bell arms a vertical beam across the Slam Cage exit
        TrollTrap(
            id = 11,
            type = TrollTrapType.BETRAYAL_SWITCH,
            triggerType = TrapTriggerType.ON_STEP,
            initialX = 22.2f * T,
            initialY = 13f * T - 22f,
            width = 44f,
            height = 22f,
            telegraphDurationFrames = 0,
            linkedTrapId = 12,
            killsPlayer = false,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "DOOR MECHANISM?"
        ),
        TrollTrap(
            id = 12,
            type = TrollTrapType.REVERSING_HAZARD,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 11,
            chainDelayFrames = 24,
            initialX = 30.8f * T,
            initialY = 13f * T - 38f,
            width = 38f,
            height = 38f,
            targetX = 27.6f * T,
            targetY = 13f * T - 38f,
            moveSpeed = 5.2f,
            telegraphDurationFrames = 0,
            activeDurationFrames = 160,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "CAGE AMBUSH!"
        ),
        // Grabbing final Coin #5 near the fleeing Sun Gate triggers a reverse Moving Crush Wall from the right
        TrollTrap(
            id = 13,
            type = TrollTrapType.MOVING_CRUSH_WALL,
            triggerType = TrapTriggerType.ON_COIN_COLLECTED,
            triggerCoinId = 5,
            chainDelayFrames = 0,
            initialX = 46.2f * T,
            initialY = 13f * T - 76f,
            width = T,
            height = 76f,
            targetX = 42.0f * T,
            targetY = 13f * T - 76f,
            moveSpeed = 6.2f,
            telegraphDurationFrames = 0,
            killsPlayer = true,
            behaviorCategory = TrapBehaviorCategory.SURPRISE,
            quipPopup = "EXIT AMBUSH!"
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
