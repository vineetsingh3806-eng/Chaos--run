# ASSET ORIGIN RECORD — CHAOS RUN

This document records the origin, creation methodology, and provenance of every visual and acoustic asset in CHAOS RUN.

---

## 1. Visual Character Asset: "Aarav the Seeker"

- **Character Name:** Aarav the Seeker
- **Creator / Implementation:** Original procedural pixel-art vector renderer in `ChaosGameRenderer.kt`
- **Design Elements:**
  - Headwear: Saffron explorer turban with brass-rimmed goggles
  - Attire: Teal explorer utility vest with belt pouches and brass buckle over linen tunic
  - Accessory: Dynamically waving ochre traveler's scarf with sine-wave wind physics
  - Lower: Khaki exploration trousers with brown travel boots
- **Animation States:** 8 distinct states (Idle, Run, Jump, Fall, Land, Death, Respawn, Victory) rendered via native Android Canvas routines.
- **Provenance:** 100% original design created specifically for this project. Does not duplicate, trace, or imitate any existing commercial game characters.

---

## 2. World Environments & Tile Assets

- **World 1 — Green Forest:** Procedural grass cap fringes, timber plank bridges with grain cracks, mossy stone platforms.
- **World 2 — Dark Jungle:** Decaying ancient vine stone blocks, luminescent cyan mushrooms, moving moss branches.
- **World 3 — Lava Valley:** Obsidian basalt crags with red-orange heat cracks, rising molten magma wave crests.
- **World 4 — Frozen Mountain:** High-friction snow caps, translucent cyan ice ledges, crystalline icicles with dust crumbs.
- **World 5 — Haunted Castle:** Weathered gothic slate cobblestones, purple ambient masonry, iron portcullises, ornate gold exit arches.
- **Provenance:** All environment blocks are drawn procedurally in code via Android Canvas (`Paint`, `Path`, `RectF`, `LinearGradient`). Zero external tile sheets or pirated sprite packs.

---

## 3. Launcher Icon & Branding

- **Launcher Foreground:** Generated specifically for CHAOS RUN using Google AI Studio image generation with strict custom pixel-art adventure parameters, centered within a 66dp safe zone on a solid `#1A2E26` background.
- **Launcher Fallbacks:** Converted into standard RGBA PNG formats for all 5 Android density folders (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) with inscribed circular masking for `ic_launcher_round.png`.
- **Branding Typography:** Custom styled in Jetpack Compose using modern bold typography tokens.

---

## 4. Acoustic Assets

- **Sound Engine:** `ProceduralAudioEngine.kt`
- **Sample Files:** 0 external sound files. All audio is computed in real time using native mathematical oscillators.
