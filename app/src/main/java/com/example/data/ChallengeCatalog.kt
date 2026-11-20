package com.example.data

/**
 * Initial curated challenge catalog (ch_01–ch_05) mapping cleanly onto the existing
 * five playable World 1 levels while preserving World1Levels and all level layouts.
 */
object ChallengeCatalog {
  const val CONFIG_VERSION = 1

  val entries: List<ChallengeDefinition> = listOf(
      ChallengeDefinition(
          challengeId = "ch_01",
          orderIndex = 1,
          title = "The Easy Start",
          subtitle = "Wait... That Floor Moved?",
          theme = LevelTheme.WHISPERING_WOODS,
          challengeSeed = 1001L,
          configVersion = CONFIG_VERSION,
          legacyWorldId = 1,
          legacyLevelId = 1,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> World1Levels.createLevel(1, difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_02",
          orderIndex = 2,
          title = "The Fake Safe Path",
          subtitle = "Trust Nothing You See",
          theme = LevelTheme.HOLLOW_OF_ECHOES,
          challengeSeed = 1002L,
          configVersion = CONFIG_VERSION,
          legacyWorldId = 1,
          legacyLevelId = 2,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> World1Levels.createLevel(2, difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_03",
          orderIndex = 3,
          title = "The Exit Is Lying",
          subtitle = "So Close Yet So Far",
          theme = LevelTheme.RUINS_OF_LOST_KING,
          challengeSeed = 1003L,
          configVersion = CONFIG_VERSION,
          legacyWorldId = 1,
          legacyLevelId = 3,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> World1Levels.createLevel(3, difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_04",
          orderIndex = 4,
          title = "The Floor Has Plans",
          subtitle = "Timing & Betrayal",
          theme = LevelTheme.MONSOON_RUN,
          challengeSeed = 1004L,
          configVersion = CONFIG_VERSION,
          legacyWorldId = 1,
          legacyLevelId = 4,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> World1Levels.createLevel(4, difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_05",
          orderIndex = 5,
          title = "Almost There",
          subtitle = "One Final Troll",
          theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
          challengeSeed = 1005L,
          configVersion = CONFIG_VERSION,
          legacyWorldId = 1,
          legacyLevelId = 5,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> World1Levels.createLevel(5, difficulty) }
      )
  )

  private val byId: Map<String, ChallengeDefinition> = entries.associateBy { it.challengeId }

  fun findById(challengeId: String): ChallengeDefinition? = byId[challengeId]

  fun findByLegacyLevel(worldId: Int, levelId: Int): ChallengeDefinition? {
    return entries.firstOrNull { it.legacyWorldId == worldId && it.legacyLevelId == levelId }
  }

  fun createLockedConfig(
      challengeId: String,
      adaptiveTier: AdaptiveTier = AdaptiveTier.STANDARD
  ): LockedChallengeConfig? {
    return byId[challengeId]?.lockConfig(adaptiveTier)
  }

  fun fromLegacyLevel(
      worldId: Int,
      levelId: Int,
      adaptiveTier: AdaptiveTier = AdaptiveTier.STANDARD
  ): LockedChallengeConfig? {
    return findByLegacyLevel(worldId, levelId)?.lockConfig(adaptiveTier)
  }
}
