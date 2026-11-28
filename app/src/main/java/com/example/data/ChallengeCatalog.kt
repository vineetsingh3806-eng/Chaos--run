package com.example.data

/**
 * Curated challenge catalog (ch_01–ch_200): preserves ch_01–ch_36 unchanged in IDs/order/seeds
 * and adds 164 handcrafted/varied Phase 11 challenges (ch_37–ch_200).
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
      ),
      // --- Phase 6: Expert Tier (ch_13–ch_16) ---
      ChallengeDefinition(
          challengeId = "ch_13",
          orderIndex = 13,
          title = "Belt & Betrayal",
          subtitle = "Reversing Conveyors & Coin Baits",
          theme = LevelTheme.WHISPERING_WOODS,
          challengeSeed = 1013L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> ExpertChallenges13To16.buildChallenge13(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_14",
          orderIndex = 14,
          title = "Ember Basin Ascent",
          subtitle = "Spring Pads & Rising Ember Basins",
          theme = LevelTheme.HOLLOW_OF_ECHOES,
          challengeSeed = 1014L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> ExpertChallenges13To16.buildChallenge14(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_15",
          orderIndex = 15,
          title = "The Locked Sanctum",
          subtitle = "Sealed Rune Gates & Decoy Levers",
          theme = LevelTheme.RUINS_OF_LOST_KING,
          challengeSeed = 1015L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> ExpertChallenges13To16.buildChallenge15(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_16",
          orderIndex = 16,
          title = "Beam & Gale",
          subtitle = "Rune Sightlines & Shifting Winds",
          theme = LevelTheme.MONSOON_RUN,
          challengeSeed = 1016L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> ExpertChallenges13To16.buildChallenge16(difficulty) }
      ),
      // --- Phase 6: Very Difficult Tier (ch_17–ch_20) ---
      ChallengeDefinition(
          challengeId = "ch_17",
          orderIndex = 17,
          title = "Clockwork Ambush",
          subtitle = "Belts, Pincers & Stalker Timing",
          theme = LevelTheme.RUINS_OF_LOST_KING,
          challengeSeed = 1017L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> VeryDifficultChallenges17To20.buildChallenge17(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_18",
          orderIndex = 18,
          title = "Skyward Mirage",
          subtitle = "Aerial Springs & Fleeing Gates",
          theme = LevelTheme.MONSOON_RUN,
          challengeSeed = 1018L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> VeryDifficultChallenges17To20.buildChallenge18(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_19",
          orderIndex = 19,
          title = "Curse of the Totem",
          subtitle = "Mirror Curses & Beam Chambers",
          theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
          challengeSeed = 1019L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> VeryDifficultChallenges17To20.buildChallenge19(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_20",
          orderIndex = 20,
          title = "Domino Cavern",
          subtitle = "Cascading Chain Reactions",
          theme = LevelTheme.HOLLOW_OF_ECHOES,
          challengeSeed = 1020L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> VeryDifficultChallenges17To20.buildChallenge20(difficulty) }
      ),
      // --- Phase 6: Extreme Challenge Tier (ch_21–ch_24) ---
      ChallengeDefinition(
          challengeId = "ch_21",
          orderIndex = 21,
          title = "The Pursuer's Vault",
          subtitle = "Stalker Pursuit & Cage Vaults",
          theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
          challengeSeed = 1021L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> ExtremeChallenges21To24.buildChallenge21(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_22",
          orderIndex = 22,
          title = "Monsoon Cyclone",
          subtitle = "Cyclone Winds & Blink Gauntlet",
          theme = LevelTheme.MONSOON_RUN,
          challengeSeed = 1022L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> ExtremeChallenges21To24.buildChallenge22(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_23",
          orderIndex = 23,
          title = "Vault of Deception",
          subtitle = "False Bells, Gates & Mirage Doors",
          theme = LevelTheme.RUINS_OF_LOST_KING,
          challengeSeed = 1023L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> ExtremeChallenges21To24.buildChallenge23(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_24",
          orderIndex = 24,
          title = "Trial of the Eternal Sun",
          subtitle = "Grand Finale — Every Trick United",
          theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
          challengeSeed = 1024L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> ExtremeChallenges21To24.buildChallenge24(difficulty) }
      ),
      // --- Phase 7: Nightmare Tier (ch_25–ch_30) ---
      ChallengeDefinition(
          challengeId = "ch_25",
          orderIndex = 25,
          title = "No Time to React",
          subtitle = "Instant Collapses & Ambush Spikes",
          theme = LevelTheme.WHISPERING_WOODS,
          challengeSeed = 1025L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> NightmareChallenges25To28.buildChallenge25(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_26",
          orderIndex = 26,
          title = "The Ceiling Wants You",
          subtitle = "Silent Canopy Slams & Moving Lifts",
          theme = LevelTheme.HOLLOW_OF_ECHOES,
          challengeSeed = 1026L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> NightmareChallenges25To28.buildChallenge26(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_27",
          orderIndex = 27,
          title = "Coin of Betrayal",
          subtitle = "Cursed Gold & Instant Chains",
          theme = LevelTheme.RUINS_OF_LOST_KING,
          challengeSeed = 1027L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> NightmareChallenges25To28.buildChallenge27(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_28",
          orderIndex = 28,
          title = "The Moving Nightmare",
          subtitle = "Instant Stalkers & Collapsing Belts",
          theme = LevelTheme.MONSOON_RUN,
          challengeSeed = 1028L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> NightmareChallenges25To28.buildChallenge28(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_29",
          orderIndex = 29,
          title = "Nothing Is Safe",
          subtitle = "Rigged Ledges & False Shrines",
          theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
          challengeSeed = 1029L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> NightmareBrutalChallenges29To32.buildChallenge29(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_30",
          orderIndex = 30,
          title = "The Fake Exit Returns",
          subtitle = "Mirage Portals & Fleeing Sanctums",
          theme = LevelTheme.RUINS_OF_LOST_KING,
          challengeSeed = 1030L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> NightmareBrutalChallenges29To32.buildChallenge30(difficulty) }
      ),
      // --- Phase 7: Brutal Tier (ch_31–ch_36) ---
      ChallengeDefinition(
          challengeId = "ch_31",
          orderIndex = 31,
          title = "Chain Reaction",
          subtitle = "Instant Cascading Detonations",
          theme = LevelTheme.HOLLOW_OF_ECHOES,
          challengeSeed = 1031L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> NightmareBrutalChallenges29To32.buildChallenge31(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_32",
          orderIndex = 32,
          title = "The Trap Corridor",
          subtitle = "Relentless Back-to-Back Gauntlet",
          theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
          challengeSeed = 1032L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> NightmareBrutalChallenges29To32.buildChallenge32(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_33",
          orderIndex = 33,
          title = "Above and Below",
          subtitle = "Dual Canopy & Cavern Gauntlets",
          theme = LevelTheme.WHISPERING_WOODS,
          challengeSeed = 1033L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> BrutalChallenges33To36.buildChallenge33(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_34",
          orderIndex = 34,
          title = "The Unforgiving Tower",
          subtitle = "Skyward Springs & Canopy Slams",
          theme = LevelTheme.MONSOON_RUN,
          challengeSeed = 1034L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> BrutalChallenges33To36.buildChallenge34(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_35",
          orderIndex = 35,
          title = "Survival Instinct",
          subtitle = "Zero-Warning Multi-Hazard Trial",
          theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
          challengeSeed = 1035L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> BrutalChallenges33To36.buildChallenge35(difficulty) }
      ),
      ChallengeDefinition(
          challengeId = "ch_36",
          orderIndex = 36,
          title = "The Last Laugh",
          subtitle = "Brutal Finale — No Warnings Left",
          theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
          challengeSeed = 1036L,
          configVersion = CONFIG_VERSION,
          baseDifficulty = DifficultyTier.NORMAL,
          levelFactory = { difficulty -> BrutalChallenges33To36.buildChallenge36(difficulty) }
      )
  ) + ChaosPackFactory.expansionCatalogEntries(CONFIG_VERSION)

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
