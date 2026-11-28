package com.example.data

/**
 * Shared geometry and trap builder utilities for Phase 11 expansion challenges (ch_37..ch_200).
 * Guarantees safe spawn areas, reachable SUN_GATE exits, zero tall vertical spike-gate barriers,
 * and deterministic per-challenge variation across 10 archetypes.
 */
internal object ChaosPackCommon {
  const val T = World1Levels.TILE_SIZE

  class TrapBuilder {
    private var idSeq = 1
    val traps = mutableListOf<TrollTrap>()

    fun nextId(): Int = idSeq++

    fun add(trap: TrollTrap): TrollTrap {
      traps.add(trap)
      return trap
    }
  }

  fun addRow(
      tiles: MutableList<GameTile>,
      nextId: () -> Int,
      startTileX: Int,
      endTileX: Int,
      tileY: Float,
      type: TileType
  ) {
    if (endTileX < startTileX) return
    for (tx in startTileX..endTileX) {
      tiles.add(
          GameTile(
              id = nextId(),
              x = tx * T,
              y = tileY * T,
              width = T,
              height = T,
              type = type
          )
      )
    }
  }

  fun addWallBlock(
      tiles: MutableList<GameTile>,
      nextId: () -> Int,
      tileX: Float,
      topTileY: Float,
      tilesTall: Int,
      type: TileType = TileType.CARVED_STONE
  ) {
    for (i in 0 until tilesTall) {
      tiles.add(
          GameTile(
              id = nextId(),
              x = tileX * T,
              y = (topTileY + i) * T,
              width = T,
              height = T,
              type = type
          )
      )
    }
  }

  fun addCheckpoint(
      tiles: MutableList<GameTile>,
      nextId: () -> Int,
      tileX: Float,
      floorTileY: Float = 13f
  ) {
    tiles.add(
        GameTile(
            id = nextId(),
            x = tileX * T,
            y = (floorTileY - 1f) * T,
            width = T,
            height = T,
            type = TileType.CHECKPOINT_BELL,
            isSolid = false
        )
    )
  }

  fun addSunGate(
      tiles: MutableList<GameTile>,
      nextId: () -> Int,
      exitX: Float,
      exitY: Float
  ) {
    tiles.add(
        GameTile(
            id = nextId(),
            x = exitX,
            y = exitY,
            width = T * 2f,
            height = T * 3f,
            type = TileType.SUN_GATE,
            isSolid = false
        )
    )
  }

  fun addCoin(coins: MutableList<Coin>, id: Int, tileX: Float, tileY: Float) {
    coins.add(Coin(id = id, x = tileX * T, y = tileY * T))
  }

  fun floorTypeFor(theme: LevelTheme): TileType = when (theme) {
    LevelTheme.WHISPERING_WOODS -> TileType.MOSSY_TIMBER
    LevelTheme.HOLLOW_OF_ECHOES -> TileType.CARVED_STONE
    LevelTheme.RUINS_OF_LOST_KING -> TileType.CARVED_STONE
    LevelTheme.MONSOON_RUN -> TileType.SLIPPERY_STONE
    LevelTheme.TEMPLE_OF_LAST_LIGHT -> TileType.CARVED_STONE
  }

  /**
   * Resolves behavior category & telegraph frames so early/mid expansion mixes readable clues
   * with zero-warning surprise ambushes, while nightmare tier (ch_161..ch_200) enforces
   * zero-telegraph & null quips on lethal SURPRISE traps.
   */
  fun surpriseCategory(orderIndex: Int, slot: Int): TrapBehaviorCategory {
    return when {
      orderIndex >= 161 -> TrapBehaviorCategory.SURPRISE
      (orderIndex + slot) % 2 == 0 -> TrapBehaviorCategory.SURPRISE
      else -> TrapBehaviorCategory.TELEGRAPHED
    }
  }

  fun telegraphFor(
      orderIndex: Int,
      category: TrapBehaviorCategory,
      normalTelegraph: Int
  ): Int {
    if (category == TrapBehaviorCategory.SURPRISE) return 0
    return if (orderIndex >= 121) normalTelegraph.coerceAtLeast(14) else normalTelegraph.coerceAtLeast(16)
  }

  fun quipOrNull(orderIndex: Int, category: TrapBehaviorCategory, isLethal: Boolean, text: String): String? {
    if (category == TrapBehaviorCategory.SURPRISE && isLethal) return null
    return text
  }

  fun speedFor(orderIndex: Int, base: Float): Float {
    val tierScale = when {
      orderIndex <= 80 -> 1.0f
      orderIndex <= 120 -> 1.06f
      orderIndex <= 160 -> 1.12f
      else -> 1.18f
    }
    return (base * tierScale).coerceIn(4.6f, 9.8f)
  }

  fun pursuitSpeedFor(orderIndex: Int, base: Float): Float {
    val tierScale = when {
      orderIndex <= 80 -> 0.92f
      orderIndex <= 120 -> 0.98f
      orderIndex <= 160 -> 1.03f
      else -> 1.08f
    }
    return (base * tierScale).coerceIn(3.4f, 4.6f)
  }
}
