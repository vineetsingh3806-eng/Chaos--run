package com.example.data

/**
 * Curated metadata for Phase 11 expansion challenges (ch_37 through ch_200).
 * Every title is unique across all 200 challenges and paired with a fitting theme and subtitle.
 */
internal data class ExpansionChallengeMeta(
    val orderIndex: Int,
    val title: String,
    val subtitle: String,
    val theme: LevelTheme,
    val archetype: Int
) {
  val challengeId: String
    get() = "ch_${orderIndex.toString().padStart(2, '0')}"

  val challengeSeed: Long
    get() = 1000L + orderIndex
}

internal object ChaosPackCatalogData {
  private val themes = arrayOf(
      LevelTheme.WHISPERING_WOODS,
      LevelTheme.HOLLOW_OF_ECHOES,
      LevelTheme.RUINS_OF_LOST_KING,
      LevelTheme.MONSOON_RUN,
      LevelTheme.TEMPLE_OF_LAST_LIGHT
  )

  private val chapterPrefixes = arrayOf(
      "Sliding", "Twin-Split", "Reversing", "Ambush", "Mirage",
      "Echoing", "Sunlit", "Phantom", "Monsoon", "Clockwork",
      "Canopy", "Ember", "Runaway", "Saffron", "Relentless",
      "Betrayal", "Skyward"
  )

  private val chapterNouns = arrayOf(
      "Bridge", "Crossing", "Deck", "Terrace", "Vault",
      "Alcove", "Causeway", "Chamber", "Ridge", "Sanctum",
      "Bastion", "Overlook", "Pass", "Gorge", "Corridor",
      "Spire", "Labyrinth", "Rampart", "Trail", "Citadel"
  )

  private val curatedSubtitles = arrayOf(
      "Sliding Decks & Surprise Reversals",
      "Split Bridges & Twin-Jump Traps",
      "Double Jump Above the Ambush",
      "Opening Walls & Reversing Saws",
      "Fading Stone & Fake Landings",
      "Gale Gusts & Shifting Conveyors",
      "False Shrines & Mirage Gates",
      "Bounce Drums & Canopy Ambush",
      "Cascading Coin & Wall Triggers",
      "Zero-Warning Final Approach"
  )

  private val specialOverrides: Map<Int, Pair<String, String>> = mapOf(
      37 to ("Slip-Slide Clearing" to "First Taste of Sliding Floors"),
      38 to ("Split in the Woods" to "Floors Part Beneath Your Boots"),
      39 to ("Boomerang Blade" to "Reversing Saws & Low Beams"),
      40 to ("The Opening Wall" to "Stone Jaws Hide Moving Hazards"),
      50 to ("Half-Century Hustle" to "Milestone 50 — Five Tricks Collide"),
      75 to ("Double-Tap Canopy" to "Air Jump Over Sliding Traps"),
      81 to ("Skyline Switchback" to "Double-Jump & Reversing Ledges"),
      100 to ("Centurion Trial" to "Challenge 100 — Grand Midpoint Gauntlet"),
      121 to ("Monsoon Dominoes" to "One Coin Awakens Three Surprises"),
      150 to ("Temple of Reversals" to "Milestone 150 — Trust No Direction"),
      161 to ("Silent Ambush" to "Zero-Warning Shifting Vaults"),
      180 to ("Storm of Betrayal" to "Monsoon Gusts & Fleeing Gates"),
      199 to ("Penultimate Prank" to "One Step Short of Chaos Crown"),
      200 to ("Crown of Chaos" to "Challenge 200 — Ultimate Grand Finale")
  )

  val expansionMeta: List<ExpansionChallengeMeta> = (37..200).map { index ->
    val offset = index - 37
    val special = specialOverrides[index]
    val title = special?.first ?: run {
      val prefix = chapterPrefixes[(offset * 3 + offset / 17) % chapterPrefixes.size]
      val noun = chapterNouns[(offset + (offset / 20) * 7) % chapterNouns.size]
      val actTag = when (index) {
        in 37..80 -> "I"
        in 81..120 -> "II"
        in 121..160 -> "III"
        else -> "IV"
      }
      "$prefix $noun $actTag-${(offset % 9) + 1}"
    }
    val archetype = offset % 10
    val subtitle = special?.second ?: curatedSubtitles[archetype]
    val theme = themes[(offset + archetype / 2) % themes.size]
    ExpansionChallengeMeta(
        orderIndex = index,
        title = title,
        subtitle = subtitle,
        theme = theme,
        archetype = archetype
    )
  }

  private val metaByIndex: Map<Int, ExpansionChallengeMeta> =
      expansionMeta.associateBy { it.orderIndex }

  fun metaFor(orderIndex: Int): ExpansionChallengeMeta {
    return metaByIndex[orderIndex] ?: error("Unknown expansion orderIndex=$orderIndex")
  }
}
