package com.example.engine

import com.example.data.AdaptiveTier
import com.example.data.ChallengeDefinition
import com.example.data.TrapBehaviorCategory
import com.example.data.TrapTriggerType
import com.example.data.TrollTrap
import com.example.data.TrollTrapType
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class TrapFairnessClass {
  SURPRISE_TRAP,
  STANDARD_LETHAL_FLOOR,
  QUALIFIED_STATIC_CLUE_LETHAL,
  CONTINUOUS_VISIBLE_HAZARD,
  NON_LETHAL_PLATFORM_TRAP,
  INSTANT_RELOCATE_GAG
}

data class TrapClueQualification(
    val hasStaticVisualClue: Boolean,
    val hasMinimumReactionSpacing: Boolean,
    val hasEscapeOpportunity: Boolean
) {
  val isQualified: Boolean
    get() = hasStaticVisualClue && hasMinimumReactionSpacing && hasEscapeOpportunity
}

data class TrapFairnessValidation(
    val trapId: Int,
    val trapType: TrollTrapType,
    val fairnessClass: TrapFairnessClass,
    val tier: AdaptiveTier,
    val requiredMinTelegraphFrames: Int,
    val effectiveTelegraphFrames: Int,
    val warningOccursBeforeActiveHitbox: Boolean,
    val qualification: TrapClueQualification,
    val isFair: Boolean,
    val reason: String
)

/**
 * Central deterministic trap fairness policy and adaptive tuning validator.
 *
 * Ensures:
 * - Standard lethal floor traps provide at least [STANDARD_LETHAL_FLOOR_MIN_FRAMES] (12) frames
 *   of readable pre-hitbox warning (12-18 recommended).
 * - Qualified 6-11 frame warnings are permitted ONLY when a clear static visual clue, >= 1.5x
 *   reaction spacing, and a genuine escape route exist.
 * - All warning windows occur strictly BEFORE lethal hitboxes activate.
 * - Adaptive tiers may extend warning windows ([AdaptiveTier.ASSISTED]) and adjust movement/cycle
 *   speed fairly, while never shortening warning windows below the fairness floor.
 */
object TrapFairnessPolicy {
  const val STANDARD_LETHAL_FLOOR_MIN_FRAMES = 12
  const val RECOMMENDED_WARNING_MIN_FRAMES = 12
  const val RECOMMENDED_WARNING_MAX_FRAMES = 18
  const val QUALIFIED_SHORT_WARNING_MIN_FRAMES = 6
  const val MIN_REACTION_SPACING_MULTIPLIER = 1.5f
  const val MIN_BLINK_CYCLE_FRAMES = 120
  const val MIN_OSC_SPIKE_CYCLE_FRAMES = 90
  const val MIN_YO_YO_CYCLE_FRAMES = 100
  const val MIN_GUST_CYCLE_FRAMES = 90
  const val MIN_MOVING_TRAP_SPEED = 4.5f
  const val MIN_PURSUIT_TRAP_SPEED = 3.2f
  const val MAX_PURSUIT_TRAP_SPEED = 5.6f
  const val MIN_CRUMBLE_TELEGRAPH_FRAMES = 14
  const val MIN_DISAPPEARING_FLOOR_FRAMES = 2
  const val MIN_SPLIT_BRIDGE_TELEGRAPH_FRAMES = 12
  const val MIN_PHANTOM_LEDGE_TELEGRAPH_FRAMES = 10
  const val MIN_SLAM_CAGE_TELEGRAPH_FRAMES = 12
  const val MIN_MIRROR_CURSE_TELEGRAPH_FRAMES = 12
  const val MIN_FLEEING_GATE_TELEGRAPH_FRAMES = 6
  const val MIN_BEAM_CYCLE_FRAMES = 100
  const val MAX_CONVEYOR_SPEED = 2.8f

  fun classify(
      trap: TrollTrap,
      clueOverride: TrapClueQualification? = null
  ): TrapFairnessClass {
    if (trap.behaviorCategory == TrapBehaviorCategory.SURPRISE) {
      return TrapFairnessClass.SURPRISE_TRAP
    }
    if (trap.type == TrollTrapType.RELOCATING_EXIT_DOOR && !trap.killsPlayer) {
      return TrapFairnessClass.INSTANT_RELOCATE_GAG
    }
    if ((trap.type == TrollTrapType.MOVING_SPIKE &&
            trap.triggerType == TrapTriggerType.ON_TIMER_CYCLE) ||
        trap.type == TrollTrapType.YO_YO_PLATFORM) {
      return TrapFairnessClass.CONTINUOUS_VISIBLE_HAZARD
    }
    if (!trap.killsPlayer) {
      return TrapFairnessClass.NON_LETHAL_PLATFORM_TRAP
    }
    return when (trap.type) {
      TrollTrapType.SURPRISE_SPIKE_POP,
      TrollTrapType.FAKE_SAFE_PLATFORM,
      TrollTrapType.LAUNCH_BETRAYAL_PAD -> TrapFairnessClass.STANDARD_LETHAL_FLOOR

      TrollTrapType.FALLING_CEILING_BLOCK,
      TrollTrapType.MOVING_CRUSH_WALL,
      TrollTrapType.MOVING_SPIKE,
      TrollTrapType.REVERSING_HAZARD,
      TrollTrapType.FAKE_EXIT_DOOR,
      TrollTrapType.FAKE_CHECKPOINT,
      TrollTrapType.TRAILING_SPIKE,
      TrollTrapType.STALKER_CRUSH_WALL,
      TrollTrapType.PINCER_WALLS,
      TrollTrapType.RISING_EMBER_SURGE,
      TrollTrapType.TELEGRAPHED_RUNE_BEAM -> {
        val qual = clueOverride ?: inferQualification(trap)
        if (qual.isQualified) {
          TrapFairnessClass.QUALIFIED_STATIC_CLUE_LETHAL
        } else {
          TrapFairnessClass.STANDARD_LETHAL_FLOOR
        }
      }

      else -> TrapFairnessClass.STANDARD_LETHAL_FLOOR
    }
  }

  fun inferQualification(trap: TrollTrap): TrapClueQualification {
    val staticClue = when (trap.type) {
      TrollTrapType.FALLING_CEILING_BLOCK,
      TrollTrapType.MOVING_CRUSH_WALL,
      TrollTrapType.MOVING_SPIKE,
      TrollTrapType.FAKE_EXIT_DOOR,
      TrollTrapType.FAKE_CHECKPOINT,
      TrollTrapType.TRAILING_SPIKE,
      TrollTrapType.STALKER_CRUSH_WALL,
      TrollTrapType.PINCER_WALLS,
      TrollTrapType.SPLIT_BRIDGE,
      TrollTrapType.PHANTOM_LEDGE,
      TrollTrapType.EXIT_SLAM_CAGE,
      TrollTrapType.MIRROR_CURSE_TOTEM,
      TrollTrapType.BETRAYAL_SWITCH,
      TrollTrapType.GUST_REVERSAL_ZONE,
      TrollTrapType.REVERSE_CONVEYOR_BELT,
      TrollTrapType.LAUNCH_BETRAYAL_PAD,
      TrollTrapType.RISING_EMBER_SURGE,
      TrollTrapType.TELEGRAPHED_RUNE_BEAM,
      TrollTrapType.LOCKED_RUNE_GATE,
      TrollTrapType.SLIDING_FLOOR_SHIFT,
      TrollTrapType.SPLITTING_TWIN_FLOOR,
      TrollTrapType.REVERSING_PLATFORM,
      TrollTrapType.OPENING_AMBUSH_WALL,
      TrollTrapType.REVERSING_HAZARD,
      TrollTrapType.TEMP_VANISH_REFORM_FLOOR -> true
      else -> false
    }
    val triggerSpanX = (trap.triggerMaxX - trap.triggerMinX).coerceAtLeast(0f)
    val travelDist = hypot(
        (trap.targetX - trap.initialX).toDouble(),
        (trap.targetY - trap.initialY).toDouble()
    ).toFloat()
    val hasReactionSpacing = when (trap.type) {
      TrollTrapType.FALLING_CEILING_BLOCK ->
        abs(trap.targetY - trap.initialY) >= trap.height * MIN_REACTION_SPACING_MULTIPLIER ||
            triggerSpanX >= trap.width * MIN_REACTION_SPACING_MULTIPLIER ||
            trap.triggerType == TrapTriggerType.ON_COIN_COLLECTED ||
            trap.triggerType == TrapTriggerType.ON_TRAP_ACTIVATED
      TrollTrapType.MOVING_CRUSH_WALL,
      TrollTrapType.MOVING_SPIKE,
      TrollTrapType.REVERSING_HAZARD,
      TrollTrapType.TRAILING_SPIKE,
      TrollTrapType.STALKER_CRUSH_WALL ->
        travelDist >= trap.width * MIN_REACTION_SPACING_MULTIPLIER ||
            triggerSpanX >= trap.width * MIN_REACTION_SPACING_MULTIPLIER ||
            trap.triggerType == TrapTriggerType.ON_COIN_COLLECTED ||
            trap.triggerType == TrapTriggerType.ON_TRAP_ACTIVATED
      TrollTrapType.PINCER_WALLS ->
        trap.minSafeGapWidth >= 44f && trap.width >= trap.minSafeGapWidth + 64f
      TrollTrapType.RISING_EMBER_SURGE ->
        abs(trap.targetY - trap.initialY) >= 24f && trap.width >= 48f
      TrollTrapType.TELEGRAPHED_RUNE_BEAM ->
        trap.width >= 24f && trap.height >= 16f
      TrollTrapType.FAKE_EXIT_DOOR,
      TrollTrapType.FAKE_CHECKPOINT ->
        triggerSpanX >= trap.width * MIN_REACTION_SPACING_MULTIPLIER - 0.5f
      else -> staticClue
    }
    val hasEscape = staticClue && (trap.triggerType != TrapTriggerType.ON_STEP || !trap.killsPlayer)
    return TrapClueQualification(
        hasStaticVisualClue = staticClue,
        hasMinimumReactionSpacing = hasReactionSpacing,
        hasEscapeOpportunity = hasEscape
    )
  }

  fun minimumFairTelegraphFrames(
      trap: TrollTrap,
      clueOverride: TrapClueQualification? = null
  ): Int {
    return when (classify(trap, clueOverride)) {
      TrapFairnessClass.SURPRISE_TRAP -> 0
      TrapFairnessClass.STANDARD_LETHAL_FLOOR -> STANDARD_LETHAL_FLOOR_MIN_FRAMES
      TrapFairnessClass.QUALIFIED_STATIC_CLUE_LETHAL -> QUALIFIED_SHORT_WARNING_MIN_FRAMES
      TrapFairnessClass.CONTINUOUS_VISIBLE_HAZARD -> 0
      TrapFairnessClass.INSTANT_RELOCATE_GAG -> 0
      TrapFairnessClass.NON_LETHAL_PLATFORM_TRAP -> when (trap.type) {
        TrollTrapType.DELAYED_CRUMBLE -> MIN_CRUMBLE_TELEGRAPH_FRAMES
        TrollTrapType.DISAPPEARING_FLOOR -> MIN_DISAPPEARING_FLOOR_FRAMES
        TrollTrapType.SPLIT_BRIDGE -> MIN_SPLIT_BRIDGE_TELEGRAPH_FRAMES
        TrollTrapType.PHANTOM_LEDGE -> MIN_PHANTOM_LEDGE_TELEGRAPH_FRAMES
        TrollTrapType.EXIT_SLAM_CAGE -> MIN_SLAM_CAGE_TELEGRAPH_FRAMES
        TrollTrapType.MIRROR_CURSE_TOTEM -> MIN_MIRROR_CURSE_TELEGRAPH_FRAMES
        TrollTrapType.FLEEING_SUN_GATE -> MIN_FLEEING_GATE_TELEGRAPH_FRAMES
        else -> 0
      }
    }
  }

  fun resolveEffectiveTelegraphFrames(
      trap: TrollTrap,
      tier: AdaptiveTier,
      clueOverride: TrapClueQualification? = null
  ): Int {
    val fairnessClass = classify(trap, clueOverride)
    val minFloor = minimumFairTelegraphFrames(trap, clueOverride)

    if (fairnessClass == TrapFairnessClass.SURPRISE_TRAP ||
        fairnessClass == TrapFairnessClass.INSTANT_RELOCATE_GAG ||
        fairnessClass == TrapFairnessClass.CONTINUOUS_VISIBLE_HAZARD ||
        trap.type == TrollTrapType.TIMED_BLINK_PLATFORM) {
      return trap.telegraphDurationFrames.coerceAtLeast(minFloor)
    }

    val baseline = maxOf(trap.telegraphDurationFrames, minFloor)

    if (!trap.killsPlayer) {
      val assistedBonus = when (trap.type) {
        TrollTrapType.DELAYED_CRUMBLE,
        TrollTrapType.SPLIT_BRIDGE,
        TrollTrapType.PHANTOM_LEDGE,
        TrollTrapType.EXIT_SLAM_CAGE,
        TrollTrapType.MIRROR_CURSE_TOTEM -> 4
        TrollTrapType.TILTING_PLANK,
        TrollTrapType.BETRAYAL_SWITCH,
        TrollTrapType.GUST_REVERSAL_ZONE,
        TrollTrapType.REVERSE_CONVEYOR_BELT,
        TrollTrapType.LOCKED_RUNE_GATE -> 0
        else -> 2
      }
      return when (tier) {
        AdaptiveTier.ASSISTED -> baseline + assistedBonus
        AdaptiveTier.STANDARD -> baseline
        AdaptiveTier.ESCALATED, AdaptiveTier.RELENTLESS -> baseline.coerceAtLeast(minFloor)
      }
    }

    val adjusted = baseline + tier.telegraphBonusFrames
    return maxOf(adjusted, minFloor)
  }

  fun resolveEffectiveMoveSpeed(trap: TrollTrap, tier: AdaptiveTier): Float {
    val scaled = trap.moveSpeed * tier.speedScale
    return when (trap.type) {
      TrollTrapType.FALLING_CEILING_BLOCK,
      TrollTrapType.MOVING_CRUSH_WALL,
      TrollTrapType.MOVING_SPIKE,
      TrollTrapType.PINCER_WALLS,
      TrollTrapType.EVASIVE_PLATFORM,
      TrollTrapType.LOCKED_RUNE_GATE,
      TrollTrapType.SLIDING_FLOOR_SHIFT,
      TrollTrapType.REVERSING_PLATFORM,
      TrollTrapType.OPENING_AMBUSH_WALL,
      TrollTrapType.REVERSING_HAZARD -> scaled.coerceAtLeast(MIN_MOVING_TRAP_SPEED)
      TrollTrapType.TRAILING_SPIKE,
      TrollTrapType.STALKER_CRUSH_WALL,
      TrollTrapType.RISING_EMBER_SURGE ->
        scaled.coerceIn(MIN_PURSUIT_TRAP_SPEED, MAX_PURSUIT_TRAP_SPEED)
      else -> trap.moveSpeed
    }
  }

  fun resolveEffectiveCycleFrames(trap: TrollTrap, tier: AdaptiveTier): Int {
    if (trap.cycleFrames <= 0 &&
        trap.type != TrollTrapType.GUST_REVERSAL_ZONE &&
        trap.type != TrollTrapType.REVERSE_CONVEYOR_BELT) {
      return trap.cycleFrames
    }
    return when (trap.type) {
      TrollTrapType.TIMED_BLINK_PLATFORM -> {
        val scale = when (tier) {
          AdaptiveTier.ASSISTED -> 1.15f
          AdaptiveTier.STANDARD -> 1.00f
          AdaptiveTier.ESCALATED -> 0.94f
          AdaptiveTier.RELENTLESS -> 0.90f
        }
        (trap.cycleFrames * scale).roundToInt().coerceAtLeast(MIN_BLINK_CYCLE_FRAMES)
      }
      TrollTrapType.MOVING_SPIKE -> {
        val scale = when (tier) {
          AdaptiveTier.ASSISTED -> 1.18f
          AdaptiveTier.STANDARD -> 1.00f
          AdaptiveTier.ESCALATED -> 0.92f
          AdaptiveTier.RELENTLESS -> 0.86f
        }
        (trap.cycleFrames * scale).roundToInt().coerceAtLeast(MIN_OSC_SPIKE_CYCLE_FRAMES)
      }
      TrollTrapType.YO_YO_PLATFORM -> {
        val scale = when (tier) {
          AdaptiveTier.ASSISTED -> 1.16f
          AdaptiveTier.STANDARD -> 1.00f
          AdaptiveTier.ESCALATED -> 0.94f
          AdaptiveTier.RELENTLESS -> 0.88f
        }
        (trap.cycleFrames * scale).roundToInt().coerceAtLeast(MIN_YO_YO_CYCLE_FRAMES)
      }
      TrollTrapType.GUST_REVERSAL_ZONE,
      TrollTrapType.REVERSE_CONVEYOR_BELT -> {
        val base = if (trap.cycleFrames > 0) trap.cycleFrames else trap.gustFlipIntervalFrames
        val scale = when (tier) {
          AdaptiveTier.ASSISTED -> 1.20f
          AdaptiveTier.STANDARD -> 1.00f
          AdaptiveTier.ESCALATED -> 0.95f
          AdaptiveTier.RELENTLESS -> 0.90f
        }
        (base * scale).roundToInt().coerceAtLeast(MIN_GUST_CYCLE_FRAMES)
      }
      TrollTrapType.TELEGRAPHED_RUNE_BEAM,
      TrollTrapType.RISING_EMBER_SURGE -> {
        val scale = when (tier) {
          AdaptiveTier.ASSISTED -> 1.18f
          AdaptiveTier.STANDARD -> 1.00f
          AdaptiveTier.ESCALATED -> 0.94f
          AdaptiveTier.RELENTLESS -> 0.88f
        }
        (trap.cycleFrames * scale).roundToInt().coerceAtLeast(MIN_BEAM_CYCLE_FRAMES)
      }
      else -> trap.cycleFrames
    }
  }

  fun applyTierTuningInPlace(traps: List<TrollTrap>, tier: AdaptiveTier) {
    for (trap in traps) {
      trap.applyLockedTuning(
          telegraphFrames = resolveEffectiveTelegraphFrames(trap, tier),
          moveSpeedScaled = resolveEffectiveMoveSpeed(trap, tier),
          cycleFramesScaled = resolveEffectiveCycleFrames(trap, tier)
      )
    }
  }

  fun validateTrap(
      trap: TrollTrap,
      tier: AdaptiveTier = AdaptiveTier.STANDARD,
      clueOverride: TrapClueQualification? = null
  ): TrapFairnessValidation {
    val fairnessClass = classify(trap, clueOverride)
    val qualification = clueOverride ?: inferQualification(trap)
    val minRequired = minimumFairTelegraphFrames(trap, clueOverride)
    val effective = resolveEffectiveTelegraphFrames(trap, tier, clueOverride)
    val warningBeforeActive = when (fairnessClass) {
      TrapFairnessClass.SURPRISE_TRAP ->
        effective >= 0 &&
            (trap.type != TrollTrapType.EXIT_SLAM_CAGE || trap.effectDurationFrames >= 30) &&
            (trap.type != TrollTrapType.PHANTOM_LEDGE || trap.cycleFrames == 0 || trap.cycleFrames >= 60)
      TrapFairnessClass.STANDARD_LETHAL_FLOOR,
      TrapFairnessClass.QUALIFIED_STATIC_CLUE_LETHAL -> effective >= minRequired && effective > 0
      TrapFairnessClass.CONTINUOUS_VISIBLE_HAZARD -> when (trap.type) {
        TrollTrapType.YO_YO_PLATFORM -> trap.cycleFrames >= MIN_YO_YO_CYCLE_FRAMES
        else -> trap.cycleFrames >= MIN_OSC_SPIKE_CYCLE_FRAMES
      }
      TrapFairnessClass.INSTANT_RELOCATE_GAG -> true
      TrapFairnessClass.NON_LETHAL_PLATFORM_TRAP -> effective >= minRequired
    }
    val meetsAuthoredFloor = if (trap.killsPlayer &&
        fairnessClass != TrapFairnessClass.CONTINUOUS_VISIBLE_HAZARD &&
        fairnessClass != TrapFairnessClass.SURPRISE_TRAP) {
      trap.telegraphDurationFrames >= minRequired && effective >= minRequired
    } else if (fairnessClass == TrapFairnessClass.SURPRISE_TRAP) {
      trap.telegraphDurationFrames >= 0 && effective >= 0
    } else {
      effective >= minRequired
    }
    val hasValidTriggerLinks =
        trap.triggerSourceTrapId != trap.id &&
            trap.linkedTrapId != trap.id &&
            trap.chainDelayFrames >= 0
    val pursuitCapped = when (trap.type) {
      TrollTrapType.TRAILING_SPIKE,
      TrollTrapType.STALKER_CRUSH_WALL,
      TrollTrapType.RISING_EMBER_SURGE ->
        resolveEffectiveMoveSpeed(trap, tier) <= MAX_PURSUIT_TRAP_SPEED
      TrollTrapType.PINCER_WALLS -> trap.minSafeGapWidth >= 44f
      TrollTrapType.GUST_REVERSAL_ZONE ->
        resolveEffectiveCycleFrames(trap, tier) >= MIN_GUST_CYCLE_FRAMES
      TrollTrapType.REVERSE_CONVEYOR_BELT ->
        resolveEffectiveCycleFrames(trap, tier) >= MIN_GUST_CYCLE_FRAMES &&
            abs(trap.gustForceX) <= MAX_CONVEYOR_SPEED
      TrollTrapType.TELEGRAPHED_RUNE_BEAM ->
        trap.cycleFrames <= 0 || resolveEffectiveCycleFrames(trap, tier) >= MIN_BEAM_CYCLE_FRAMES
      else -> true
    }

    val isFair = warningBeforeActive && meetsAuthoredFloor && hasValidTriggerLinks && pursuitCapped
    val reason = when {
      !hasValidTriggerLinks -> "Invalid self-referential or negative chain trigger link"
      !pursuitCapped -> "Pursuit/zone fairness bound violated for ${trap.type}"
      !meetsAuthoredFloor ->
        "Telegraph ${trap.telegraphDurationFrames}f / effective ${effective}f < required min ${minRequired}f for $fairnessClass"
      !warningBeforeActive ->
        "Warning window does not precede hitbox activation"
      else -> "OK ($fairnessClass, ${effective}f >= ${minRequired}f)"
    }
    return TrapFairnessValidation(
        trapId = trap.id,
        trapType = trap.type,
        fairnessClass = fairnessClass,
        tier = tier,
        requiredMinTelegraphFrames = minRequired,
        effectiveTelegraphFrames = effective,
        warningOccursBeforeActiveHitbox = warningBeforeActive,
        qualification = qualification,
        isFair = isFair,
        reason = reason
    )
  }

  fun findCyclicChainTrapIds(traps: List<TrollTrap>): Set<Int> {
    val byId = traps.associateBy { it.id }
    val adjacency = HashMap<Int, MutableSet<Int>>(traps.size)
    for (t in traps) {
      adjacency[t.id] = mutableSetOf()
    }
    for (t in traps) {
      val linked = t.linkedTrapId
      if (linked != null && byId.containsKey(linked)) {
        adjacency[t.id]?.add(linked)
      }
      if (t.triggerType == TrapTriggerType.ON_TRAP_ACTIVATED) {
        val src = t.triggerSourceTrapId
        if (src != null && byId.containsKey(src)) {
          adjacency[src]?.add(t.id)
        }
      }
    }
    val cyclicIds = mutableSetOf<Int>()
    // 0 = unvisited, 1 = visiting, 2 = done
    val state = HashMap<Int, Int>(traps.size)
    val stack = mutableListOf<Int>()

    fun dfs(nodeId: Int) {
      state[nodeId] = 1
      stack.add(nodeId)
      for (next in adjacency[nodeId].orEmpty()) {
        when (state[next] ?: 0) {
          0 -> dfs(next)
          1 -> {
            val idx = stack.indexOf(next)
            if (idx >= 0) {
              for (k in idx until stack.size) {
                cyclicIds.add(stack[k])
              }
            }
          }
        }
      }
      stack.removeAt(stack.size - 1)
      state[nodeId] = 2
    }

    for (t in traps) {
      if ((state[t.id] ?: 0) == 0) {
        dfs(t.id)
      }
    }
    return cyclicIds
  }

  fun validateChallengeTraps(
      challengeDef: ChallengeDefinition,
      tier: AdaptiveTier = AdaptiveTier.STANDARD
  ): List<TrapFairnessValidation> {
    val level = challengeDef.buildLevel()
    val trapIds = level.trollTraps.map { it.id }.toSet()
    val coinIds = level.coins.map { it.id }.toSet()
    val cyclicIds = findCyclicChainTrapIds(level.trollTraps)
    return level.trollTraps.map { trap ->
      val base = validateTrap(trap, tier)
      val missingSource = trap.triggerType == TrapTriggerType.ON_TRAP_ACTIVATED &&
          trap.triggerSourceTrapId != null &&
          !trapIds.contains(trap.triggerSourceTrapId)
      val missingLinked = trap.linkedTrapId != null && !trapIds.contains(trap.linkedTrapId)
      val missingCoin = trap.triggerType == TrapTriggerType.ON_COIN_COLLECTED &&
          trap.triggerCoinId != null &&
          !coinIds.contains(trap.triggerCoinId)
      val inCycle = cyclicIds.contains(trap.id)
      if (missingSource || missingLinked || missingCoin || inCycle) {
        val reason = when {
          inCycle -> "Trap ${trap.id} participates in a cyclic ON_TRAP_ACTIVATED chain"
          missingSource -> "Trap ${trap.id} references missing triggerSourceTrapId=${trap.triggerSourceTrapId}"
          missingLinked -> "Trap ${trap.id} references missing linkedTrapId=${trap.linkedTrapId}"
          else -> "Trap ${trap.id} references missing triggerCoinId=${trap.triggerCoinId}"
        }
        base.copy(isFair = false, reason = reason)
      } else {
        base
      }
    }
  }
}
