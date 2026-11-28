package com.example.data

/**
 * Factory creating all 164 Phase 11 expansion challenges (ch_37..ch_200).
 */
object ChaosPackFactory {
  fun buildChallenge(orderIndex: Int, difficulty: DifficultyTier = DifficultyTier.NORMAL): LevelDefinition {
    val meta = ChaosPackCatalogData.metaFor(orderIndex)
    return if (meta.archetype in 0..4) {
      ChaosPackArchetypesA.buildArchetype(meta, difficulty)
    } else {
      ChaosPackArchetypesB.buildArchetype(meta, difficulty)
    }
  }

  internal fun expansionCatalogEntries(configVersion: Int): List<ChallengeDefinition> {
    return ChaosPackCatalogData.expansionMeta.map { meta ->
      ChallengeDefinition(
          challengeId = meta.challengeId,
          orderIndex = meta.orderIndex,
          title = meta.title,
          subtitle = meta.subtitle,
          theme = meta.theme,
          challengeSeed = meta.challengeSeed,
          configVersion = configVersion,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> buildChallenge(meta.orderIndex, difficulty) }
      )
    }
  }
}
