package com.example.data

/**
 * Curated challenge catalog (ch_01–ch_12): preserves ch_01–ch_05 mapped onto World 1
 * levels and adds 7 handcrafted Phase 4 challenges (ch_06–ch_12) showcasing all
 * 13 new trap mechanics and 2 event-driven trigger behaviours.
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
      ),
      ChallengeDefinition(
          challengeId = "ch_06",
          orderIndex = 6,
          title = "The Cracking Road",
          subtitle = "Split Bridges & Phantom Ledges",
          theme = LevelTheme.WHISPERING_WOODS,
          challengeSeed = 1006L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> Challenge06CrackingRoad.build(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_07",
          orderIndex = 7,
          title = "Tilted Trouble",
          subtitle = "Seesaw Planks & Trailing Spikes",
          theme = LevelTheme.HOLLOW_OF_ECHOES,
          challengeSeed = 1007L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> Challenge07TiltedTrouble.build(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_08",
          orderIndex = 8,
          title = "The Runaway Floor",
          subtitle = "Evasive Platforms & Yo-Yo Lifts",
          theme = LevelTheme.MONSOON_RUN,
          challengeSeed = 1008L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> Challenge08RunawayFloor.build(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_09",
          orderIndex = 9,
          title = "The Fake Finish",
          subtitle = "Slam Cages & Fleeing Gates",
          theme = LevelTheme.RUINS_OF_LOST_KING,
          challengeSeed = 1009L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> Challenge09FakeFinish.build(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_10",
          orderIndex = 10,
          title = "Walls Are Watching",
          subtitle = "Pincer Jaws & Stalker Walls",
          theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
          challengeSeed = 1010L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> Challenge10WallsAreWatching.build(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_11",
          orderIndex = 11,
          title = "Trust Nothing",
          subtitle = "Betrayal Switches & Coin Traps",
          theme = LevelTheme.RUINS_OF_LOST_KING,
          challengeSeed = 1011L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> Challenge11TrustNothing.build(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_12",
          orderIndex = 12,
          title = "Mirror Mayhem",
          subtitle = "Shifting Gusts & Mirror Totems",
          theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
          challengeSeed = 1012L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> Challenge12MirrorMayhem.build(difficulty) }
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
