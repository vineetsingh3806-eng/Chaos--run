package com.example.data

import com.example.engine.TrapFairnessPolicy
import java.util.Calendar
import java.util.Locale

/**
 * Compatibility-verified optional replay modifiers for CHAOS RUN 5.
 *
 * Technical compatibility note:
 * - SPEED_RUN: Enforces a tighter par-time limit (65% of normal parTimeSeconds) with clean attempt reset on timeout.
 * - NO_CHECKPOINT: Leaves shrine checkpoint bells dormant so any death restarts from spawn.
 * - TRAP_RUSH: Applies TrapFairnessPolicy-validated AdaptiveTier.RELENTLESS timing/speed tuning.
 * - LANTERN_MIST: Engages ExpansionAtlasRenderer's pre-allocated radial lantern cutout around the player
 *   (and exit gate / switches) without obscuring close hazards.
 * - Note: Global horizontal X-geometry mirroring ("MIRRORED_CHAOS") is intentionally omitted because
 *   authored set pieces and directional trap trigger zones rely on validated left-to-right world coordinates.
 */
enum class ChaosModifier(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val description: String,
    val accentColorHex: Long
) {
  SPEED_RUN(
      id = "speed_run",
      displayName = "Speed Run",
      shortLabel = "SPEED",
      description = "Beat the tighter speed-par clock before time expires.",
      accentColorHex = 0xFF38BDF8
  ),
  NO_CHECKPOINT(
      id = "no_checkpoint",
      displayName = "No Checkpoint",
      shortLabel = "NO CP",
      description = "Shrine bells remain dormant — every death restarts at spawn.",
      accentColorHex = 0xFFF59E0B
  ),
  TRAP_RUSH(
      id = "trap_rush",
      displayName = "Trap Rush",
      shortLabel = "RUSH",
      description = "Relentless trap speeds & faster cycles (fairness floor enforced).",
      accentColorHex = 0xFFFB7185
  ),
  LANTERN_MIST(
      id = "lantern_mist",
      displayName = "Lantern Mist",
      shortLabel = "MIST",
      description = "Twilight fog narrows sight to your lantern & shrine beacons.",
      accentColorHex = 0xFFA855F7
  );

  companion object {
    fun fromId(rawId: String?): ChaosModifier? {
      if (rawId.isNullOrBlank()) return null
      val normalized = rawId.trim().lowercase(Locale.US)
      return entries.firstOrNull { it.id == normalized || it.name.lowercase(Locale.US) == normalized }
    }

    fun canonicalKey(modifiers: Set<ChaosModifier>): String {
      if (modifiers.isEmpty()) return "standard"
      return entries.filter { modifiers.contains(it) }.joinToString("+") { it.id }
    }

    fun parseKey(rawKey: String?): Set<ChaosModifier> {
      if (rawKey.isNullOrBlank() || rawKey == "standard") return emptySet()
      return rawKey.split('+')
          .mapNotNull { fromId(it) }
          .toSet()
    }

    fun computeSpeedRunLimitMs(parTimeSeconds: Int): Long {
      return (parTimeSeconds.coerceAtLeast(15) * 650L).coerceAtLeast(10_000L)
    }
  }
}

data class ModifierChallengeRecord(
    val challengeId: String,
    val modifierKey: String,
    val bestTimeMs: Long = 0L,
    val bestScore: Int = 0,
    val leastDeaths: Int = -1,
    val clearsCount: Int = 0
)

// --- Mastery Mode (600 optional skill objectives across ch_01..ch_200) ---

enum class MasteryObjectiveKind {
  ZERO_DEATHS,
  SPEED_PAR,
  NO_COINS,
  NO_CHECKPOINT,
  MIN_SCORE
}

data class MasteryObjectiveSpec(
    val id: String,
    val kind: MasteryObjectiveKind,
    val title: String,
    val description: String,
    val targetValue: Long
)

data class ChallengeMasterySpec(
    val challengeId: String,
    val objectives: List<MasteryObjectiveSpec>
)

data class ChallengeMasteryProgress(
    val challengeId: String,
    val completedObjectiveIds: Set<String> = emptySet()
) {
  val masteryStars: Int
    get() = completedObjectiveIds.size
}

data class ChallengeAttemptMetrics(
    val challengeId: String,
    val completed: Boolean,
    val timeMs: Long,
    val deaths: Int,
    val coinsCollected: Int,
    val totalCoins: Int,
    val usedCheckpoint: Boolean,
    val score: Int
)

object MasteryCatalog {
  val specs: List<ChallengeMasterySpec> = ChallengeCatalog.entries.map { def ->
    val level = def.buildLevel()
    val speedTargetMs = (level.parTimeSeconds * 850L).coerceAtLeast(16_000L)
    val zeroDeathsObj = MasteryObjectiveSpec(
        id = "${def.challengeId}_zero_deaths",
        kind = MasteryObjectiveKind.ZERO_DEATHS,
        title = "Untouchable",
        description = "Clear without a single death",
        targetValue = 0L
    )
    val speedObj = MasteryObjectiveSpec(
        id = "${def.challengeId}_speed_par",
        kind = MasteryObjectiveKind.SPEED_PAR,
        title = "Swift Runner",
        description = "Clear in under ${speedTargetMs / 1000L}s",
        targetValue = speedTargetMs
    )
    val thirdObj = if (def.orderIndex <= 36) {
      when (def.challengeId) {
        "ch_04", "ch_07", "ch_09", "ch_12",
        "ch_14", "ch_15", "ch_17", "ch_19", "ch_21", "ch_23",
        "ch_26", "ch_29", "ch_31", "ch_33", "ch_35" -> MasteryObjectiveSpec(
            id = "${def.challengeId}_no_checkpoint",
            kind = MasteryObjectiveKind.NO_CHECKPOINT,
            title = "No Shrine Anchor",
            description = "Clear without binding the checkpoint bell",
            targetValue = 0L
        )
        "ch_05", "ch_10", "ch_18", "ch_22", "ch_24",
        "ch_27", "ch_30", "ch_34", "ch_36" -> MasteryObjectiveSpec(
            id = "${def.challengeId}_min_score",
            kind = MasteryObjectiveKind.MIN_SCORE,
            title = "Treasure Hunter",
            description = "Clear with score 400+",
            targetValue = 400L
        )
        else -> MasteryObjectiveSpec(
            id = "${def.challengeId}_no_coins",
            kind = MasteryObjectiveKind.NO_COINS,
            title = "Ascetic Path",
            description = "Clear with 0 coins collected",
            targetValue = 0L
        )
      }
    } else {
      val hasCoinLockedGate = level.trollTraps.any {
        it.type == TrollTrapType.LOCKED_RUNE_GATE &&
            it.triggerType == TrapTriggerType.ON_COIN_COLLECTED
      }
      val slot = if (hasCoinLockedGate && def.orderIndex % 3 == 2) 0 else def.orderIndex % 3
      when (slot) {
        0 -> MasteryObjectiveSpec(
            id = "${def.challengeId}_no_checkpoint",
            kind = MasteryObjectiveKind.NO_CHECKPOINT,
            title = "No Shrine Anchor",
            description = "Clear without binding the checkpoint bell",
            targetValue = 0L
        )
        1 -> MasteryObjectiveSpec(
            id = "${def.challengeId}_min_score",
            kind = MasteryObjectiveKind.MIN_SCORE,
            title = "Treasure Hunter",
            description = "Clear with score 400+",
            targetValue = 400L
        )
        else -> MasteryObjectiveSpec(
            id = "${def.challengeId}_no_coins",
            kind = MasteryObjectiveKind.NO_COINS,
            title = "Ascetic Path",
            description = "Clear with 0 coins collected",
            targetValue = 0L
        )
      }
    }
    ChallengeMasterySpec(
        challengeId = def.challengeId,
        objectives = listOf(zeroDeathsObj, speedObj, thirdObj)
    )
  }

  private val byId: Map<String, ChallengeMasterySpec> = specs.associateBy { it.challengeId }

  fun specFor(challengeId: String): ChallengeMasterySpec? = byId[challengeId]

  val totalPossibleStars: Int = specs.sumOf { it.objectives.size }

  fun evaluateAttempt(metrics: ChallengeAttemptMetrics): Set<String> {
    if (!metrics.completed || metrics.timeMs <= 0L) return emptySet()
    val spec = byId[metrics.challengeId] ?: return emptySet()
    val earned = LinkedHashSet<String>(3)
    for (obj in spec.objectives) {
      val met = when (obj.kind) {
        MasteryObjectiveKind.ZERO_DEATHS -> metrics.deaths == 0
        MasteryObjectiveKind.SPEED_PAR -> metrics.timeMs in 1L until obj.targetValue
        MasteryObjectiveKind.NO_COINS -> metrics.coinsCollected == 0
        MasteryObjectiveKind.NO_CHECKPOINT -> !metrics.usedCheckpoint
        MasteryObjectiveKind.MIN_SCORE -> metrics.score >= obj.targetValue
      }
      if (met) earned.add(obj.id)
    }
    return earned
  }
}

// --- Offline Deterministic Daily Challenge ---

data class OfflineCalendarDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
  fun toIsoDateKey(): String = "%04d-%02d-%02d".format(Locale.US, year, month, day)

  fun toEpochDayApprox(): Long = year * 366L + month * 31L + day

  fun isValid(): Boolean {
    if (year !in 2020..2100 || month !in 1..12) return false
    val maxDay = when (month) {
      2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
      4, 6, 9, 11 -> 30
      else -> 31
    }
    return day in 1..maxDay
  }

  companion object {
    val SAFE_FALLBACK = OfflineCalendarDate(2026, 1, 1)
    private val DATE_REGEX = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")

    fun todayLocal(): OfflineCalendarDate {
      return try {
        val cal = Calendar.getInstance()
        val candidate = OfflineCalendarDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH)
        )
        if (candidate.isValid()) candidate else SAFE_FALLBACK
      } catch (_: Throwable) {
        SAFE_FALLBACK
      }
    }

    fun parseDateKey(raw: String?): OfflineCalendarDate? {
      if (raw.isNullOrBlank()) return null
      val match = DATE_REGEX.matchEntire(raw.trim()) ?: return null
      val y = match.groupValues[1].toIntOrNull() ?: return null
      val m = match.groupValues[2].toIntOrNull() ?: return null
      val d = match.groupValues[3].toIntOrNull() ?: return null
      val date = OfflineCalendarDate(y, m, d)
      return if (date.isValid()) date else null
    }
  }
}

data class DailyChallengeSpec(
    val dateKey: String,
    val challengeId: String,
    val challengeTitle: String,
    val challengeOrderIndex: Int,
    val theme: LevelTheme,
    val modifiers: Set<ChaosModifier>,
    val adaptiveTier: AdaptiveTier,
    val deterministicSeed: Long,
    val isFallback: Boolean = false
)

object OfflineDailyChallengeGenerator {
  private data class DailyPreset(
      val challengeId: String,
      val modifiers: Set<ChaosModifier>,
      val tier: AdaptiveTier
  )

  // Curated, pre-verified combinations covering all 200 challenges safely
  private val curatedPool: List<DailyPreset> = ChallengeCatalog.entries.map { def ->
    when (def.orderIndex % 6) {
      0 -> DailyPreset(def.challengeId, setOf(ChaosModifier.TRAP_RUSH), AdaptiveTier.RELENTLESS)
      1 -> DailyPreset(def.challengeId, setOf(ChaosModifier.SPEED_RUN), AdaptiveTier.STANDARD)
      2 -> DailyPreset(def.challengeId, setOf(ChaosModifier.LANTERN_MIST), AdaptiveTier.ESCALATED)
      3 -> DailyPreset(def.challengeId, setOf(ChaosModifier.NO_CHECKPOINT), AdaptiveTier.STANDARD)
      4 -> DailyPreset(def.challengeId, setOf(ChaosModifier.SPEED_RUN, ChaosModifier.NO_CHECKPOINT), AdaptiveTier.ESCALATED)
      else -> DailyPreset(def.challengeId, setOf(ChaosModifier.LANTERN_MIST, ChaosModifier.SPEED_RUN), AdaptiveTier.STANDARD)
    }
  }

  private fun mix64(input: Long): Long {
    var z = input + -7046029254386353131L
    z = (z xor (z ushr 30)) * -4658895280553007687L
    z = (z xor (z ushr 27)) * -7723592293110700685L
    return z xor (z ushr 31)
  }

  fun forDate(date: OfflineCalendarDate?): DailyChallengeSpec {
    val safeDate = if (date != null && date.isValid()) date else OfflineCalendarDate.SAFE_FALLBACK
    val usedFallbackDate = (date == null || !date.isValid())
    val compactDate = safeDate.year * 10000L + safeDate.month * 100L + safeDate.day
    val seed = mix64(compactDate xor 0x5A17C4A05L)
    val index = ((seed ushr 1) % curatedPool.size).toInt().coerceIn(0, curatedPool.lastIndex)
    val preset = curatedPool[index]

    val def = ChallengeCatalog.findById(preset.challengeId)
    val resolvedTier = if (preset.modifiers.contains(ChaosModifier.TRAP_RUSH)) {
      AdaptiveTier.RELENTLESS
    } else {
      preset.tier
    }

    if (def == null || !TrapFairnessPolicy.validateChallengeTraps(def, resolvedTier).all { it.isFair }) {
      return fallbackSpec(safeDate.toIsoDateKey(), seed)
    }

    return DailyChallengeSpec(
        dateKey = safeDate.toIsoDateKey(),
        challengeId = def.challengeId,
        challengeTitle = def.title,
        challengeOrderIndex = def.orderIndex,
        theme = def.theme,
        modifiers = preset.modifiers,
        adaptiveTier = resolvedTier,
        deterministicSeed = seed,
        isFallback = usedFallbackDate
    )
  }

  fun fallbackSpec(
      dateKey: String = OfflineCalendarDate.SAFE_FALLBACK.toIsoDateKey(),
      seed: Long = 1001L
  ): DailyChallengeSpec {
    val first = ChallengeCatalog.entries.first()
    return DailyChallengeSpec(
        dateKey = dateKey,
        challengeId = first.challengeId,
        challengeTitle = first.title,
        challengeOrderIndex = first.orderIndex,
        theme = first.theme,
        modifiers = emptySet(),
        adaptiveTier = AdaptiveTier.STANDARD,
        deterministicSeed = seed,
        isFallback = true
    )
  }
}

// --- Offline Cosmetic Rewards & Player Victory Titles ---

enum class CosmeticTrail(
    val id: String,
    val displayName: String,
    val description: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val unlockHint: String
) {
  NONE(
      id = "none",
      displayName = "Classic",
      description = "Clean runner silhouette with no trail.",
      primaryColorHex = 0xFF94A3B8,
      secondaryColorHex = 0xFF64748B,
      unlockHint = "Unlocked by default"
  ),
  EMBER_SPARK(
      id = "ember_spark",
      displayName = "Ember Spark",
      description = "Warm saffron embers trailing behind every jump.",
      primaryColorHex = 0xFFF59E0B,
      secondaryColorHex = 0xFFFB7185,
      unlockHint = "Earn 3 Mastery stars"
  ),
  TEAL_ECHO(
      id = "teal_echo",
      displayName = "Teal Echo",
      description = "Spectral monsoon-teal ribbons on every stride.",
      primaryColorHex = 0xFF2DD4BF,
      secondaryColorHex = 0xFF38BDF8,
      unlockHint = "Complete 1 Daily Challenge"
  ),
  SOLAR_GOLD(
      id = "solar_gold",
      displayName = "Solar Gold",
      description = "Radiant Sun Gate gold dust trail.",
      primaryColorHex = 0xFFFDE047,
      secondaryColorHex = 0xFFF97316,
      unlockHint = "Earn 12 Mastery stars"
  ),
  VOID_VIOLET(
      id = "void_violet",
      displayName = "Void Violet",
      description = "Sanctum violet after-image pulse.",
      primaryColorHex = 0xFFC084FC,
      secondaryColorHex = 0xFF818CF8,
      unlockHint = "Clear 3 Chaos Modifier runs"
  );

  companion object {
    val DEFAULT = NONE
    fun fromId(rawId: String?): CosmeticTrail {
      if (rawId.isNullOrBlank()) return DEFAULT
      val norm = rawId.trim().lowercase(Locale.US)
      return entries.firstOrNull { it.id == norm || it.name.lowercase(Locale.US) == norm } ?: DEFAULT
    }
  }
}

enum class VictoryEffect(
    val id: String,
    val displayName: String,
    val description: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val unlockHint: String
) {
  DEFAULT(
      id = "default",
      displayName = "Sunburst",
      description = "Classic golden Sun Gate celebration sparkles.",
      primaryColorHex = 0xFFFDE047,
      secondaryColorHex = 0xFFF59E0B,
      unlockHint = "Unlocked by default"
  ),
  SAFFRON_FIREWORKS(
      id = "saffron_fireworks",
      displayName = "Festival Fireworks",
      description = "Vibrant saffron & emerald twin-ring burst at the exit gate.",
      primaryColorHex = 0xFF10B981,
      secondaryColorHex = 0xFFF59E0B,
      unlockHint = "Earn 6 Mastery stars or complete all 200 challenges"
  ),
  MONSOON_BURST(
      id = "monsoon_burst",
      displayName = "Monsoon Storm",
      description = "Electric cyan & lightning-white victory surge.",
      primaryColorHex = 0xFF38BDF8,
      secondaryColorHex = 0xFFE0F2FE,
      unlockHint = "Complete 2 Daily Challenges"
  ),
  STARLIGHT_CROWN(
      id = "starlight_crown",
      displayName = "Starlight Crown",
      description = "Royal violet & gold celebration burst.",
      primaryColorHex = 0xFFC084FC,
      secondaryColorHex = 0xFFFDE047,
      unlockHint = "Earn 24 Mastery stars"
  );

  companion object {
    fun fromId(rawId: String?): VictoryEffect {
      if (rawId.isNullOrBlank()) return DEFAULT
      val norm = rawId.trim().lowercase(Locale.US)
      return entries.firstOrNull { it.id == norm || it.name.lowercase(Locale.US) == norm } ?: DEFAULT
    }
  }
}

enum class PlayerTitle(
    val id: String,
    val displayName: String,
    val description: String,
    val accentColorHex: Long,
    val unlockHint: String
) {
  NONE(
      id = "none",
      displayName = "Runner",
      description = "Ready to face the Forest of Traps.",
      accentColorHex = 0xFF94A3B8,
      unlockHint = "Unlocked by default"
  ),
  TRAP_SURVIVOR(
      id = "trap_survivor",
      displayName = "Trap Survivor",
      description = "Learned the tricks and kept coming back.",
      accentColorHex = 0xFF10B981,
      unlockHint = "Earn 3 Mastery stars or finish all 200 challenges"
  ),
  COIN_GREEDY(
      id = "coin_greedy",
      displayName = "Coin Greedy",
      description = "Could not resist a single shiny trap bait.",
      accentColorHex = 0xFFFDE047,
      unlockHint = "Clear a challenge collecting 100% of its coins"
  ),
  NO_MERCY(
      id = "no_mercy",
      displayName = "No Mercy",
      description = "Zero checkpoints, zero excuses.",
      accentColorHex = 0xFFF59E0B,
      unlockHint = "Clear any challenge with No Checkpoint or Trap Rush"
  ),
  SPEED_DEMON(
      id = "speed_demon",
      displayName = "Speed Demon",
      description = "Outran every collapsing plank and chasing spike.",
      accentColorHex = 0xFF38BDF8,
      unlockHint = "Clear Speed Run modifier or 3 Swift Runner stars"
  ),
  DAILY_CHALLENGER(
      id = "daily_challenger",
      displayName = "Daily Seeker",
      description = "Returned for the daily trial.",
      accentColorHex = 0xFF2DD4BF,
      unlockHint = "Complete 1 Daily Challenge"
  ),
  MASTER_OF_CHAOS(
      id = "master_of_chaos",
      displayName = "Master of Chaos",
      description = "Conquered Mastery objectives and Daily trials alike.",
      accentColorHex = 0xFFC084FC,
      unlockHint = "Earn 18 Mastery stars & 1 Daily Challenge clear"
  );

  companion object {
    val DEFAULT = NONE
    fun fromId(rawId: String?): PlayerTitle {
      if (rawId.isNullOrBlank()) return DEFAULT
      val norm = rawId.trim().lowercase(Locale.US)
      return entries.firstOrNull { it.id == norm || it.name.lowercase(Locale.US) == norm } ?: DEFAULT
    }
  }
}

data class RewardInventoryState(
    val unlockedTrails: Set<CosmeticTrail> = setOf(CosmeticTrail.NONE),
    val equippedTrail: CosmeticTrail = CosmeticTrail.NONE,
    val unlockedEffects: Set<VictoryEffect> = setOf(VictoryEffect.DEFAULT),
    val equippedEffect: VictoryEffect = VictoryEffect.DEFAULT,
    val unlockedTitles: Set<PlayerTitle> = setOf(PlayerTitle.NONE),
    val equippedTitle: PlayerTitle = PlayerTitle.NONE
)

data class UnlockedRewardNotification(
    val newTrails: List<CosmeticTrail> = emptyList(),
    val newEffects: List<VictoryEffect> = emptyList(),
    val newTitles: List<PlayerTitle> = emptyList()
) {
  val isEmpty: Boolean
    get() = newTrails.isEmpty() && newEffects.isEmpty() && newTitles.isEmpty()
}

data class Phase4RetentionSnapshot(
    val masteryByChallenge: Map<String, ChallengeMasteryProgress> = emptyMap(),
    val modifierRecords: Map<String, ModifierChallengeRecord> = emptyMap(),
    val dailyCompletedDates: Set<String> = emptySet(),
    val dailyTotalClears: Int = 0,
    val zeroDeathChallenges: Set<String> = emptySet(),
    val coinGreedyEarned: Boolean = false,
    val rewards: RewardInventoryState = RewardInventoryState()
) {
  val totalMasteryStars: Int
    get() = masteryByChallenge.values.sumOf { it.masteryStars }

  val totalModifierClears: Int
    get() = modifierRecords.values.sumOf { it.clearsCount }
}
