# MUSIC & AUDIO LICENSES — CHAOS RUN

**Project:** CHAOS RUN  
**Audio Architecture:** Real-Time Mathematical Procedural Indian Chiptune & Instrumental Synthesis  
**Audio Driver:** Native Android `android.media.AudioTrack` (16-bit PCM Mono @ 22,050 Hz)  
**License Status:** 100% Original Algorithmic Composition & Synthesis (No External Recordings)  

---

## 1. Overview & Synthesis Approach

CHAOS RUN does not bundle or stream any pre-recorded audio files (.mp3, .wav, .ogg, .aac, .flac). 

Instead, 100% of background music and sound effects are generated in real time on a background thread using mathematical wave synthesis formulas running directly inside the application (`ProceduralAudioEngine.kt`).

### High-Fidelity Synthesis Improvements:
1. **Zero Sample Noise**: Completely eliminates random noise generation per sample. All percussion, pads, plucks, and flute leads use pure mathematical oscillators (sine, band-limited triangle, and damped resonant harmonics).
2. **Authentic Indian 1990s Instrumental Textures**:
   - **Bansuri (Flute)**: Sine fundamental + warm 2nd harmonic + 5.2 Hz vibrato LFO and soft breath envelope.
   - **Sitar / Sarod**: Additive harmonics with fast exponential pluck decay and metallic resonance.
   - **Tanpura / Keyboard Strings**: Dual detuned oscillators with slow chorus movement providing warm harmonic body.
   - **Bayan & Dayan (Tabla / Dholak)**: Resonant pitch-swept sine wave for deep bayan bass kicks and tuned chimes for daytime accents.
3. **Coherent Musical Progressions**: 64-step structured phrases across 5 distinct world raga modes, avoiding repetitive or annoying 16-step loops.
4. **Smooth Track Transitions**: True 1.2-second linear crossfading between worlds and menus without clicks or abrupt volume jumps.
5. **Zero Copyright Liabilities**: Eliminates reliance on third-party commercial audio samples, movie recordings, or royalty-free asset packs, substantially mitigating copyright infringement claims.

---

## 2. Track Catalog & Modal Architecture Across 50 Levels (5 Worlds)

| Track Title | In-Game Context | Musical Thaat / Raga Mode | Rhythm / Tala Pattern | Synthesis Method |
| :--- | :--- | :--- | :--- | :--- |
| **Track 1: Dhol Forest Run** | World 1: Green Forest (Levels 1–10) | *Bilawal* (C, D, E, F, G, A, B) | *Keherwa Tala* (8-beat syncopated dhol rhythm) | Warm bassline + bansuri flute lead + bayan dholak kick + soft rim click. |
| **Track 2: Twilight Canopy** | World 2: Dark Jungle (Levels 11–20) | *Khamaj* (C, D, E, F, G, A, Bb) | *Dadra Tala* (6-beat light swing) | Sitar/sarod plucks with metallic harmonics + resonant bayan bass groove + mystical tanpura drone. |
| **Track 3: Molten Rush** | World 3: Lava Valley (Levels 21–30) | *Bhupali* (C, D, E, G, A pentatonic) | Fast 140 BPM *Teental* | High-energy driving 1990s adventure pulse with dynamic dholak rhythm and melodic synth arpeggios. |
| **Track 4: Glacier Serenade** | World 4: Frozen Mountain (Levels 31–40) | *Yaman / Bhairavi* | Ambient meditative float | Dual-channel harmonized chord pads with crystalline bells and gentle sub-bass pulse. |
| **Track 5: Obsidian Tension** | World 5: Haunted Castle (Levels 41–50) | *Bhairav* (C, Db, E, F, G, Ab, B) | Suspenseful staccato pulses | Minor-second intervals creating 1990s action thriller tension, moody sub-bass, and dramatic lead. |

---

## 3. Sound Effects (SFX) Algorithmic Specifications

All sound effects are synthesized into memory buffers at startup using native math:

- **Jump (`playJump`)**: Linear frequency chirp from 180 Hz to 520 Hz over 130 ms with linear amplitude decay.
- **Land (`playLand`)**: 100 Hz to 45 Hz low-frequency resonant sine drop over 80 ms.
- **Coin Collect (`playCoin`)**: Dual-tone chime arpeggio (B5 @ 988 Hz for 90 ms followed by E6 @ 1319 Hz for 90 ms) with harmonic clarity.
- **Hazard Warning (`playWarning`)**: 440 Hz pulse alert with 18 Hz tremolo sine modulation over 120 ms.
- **Platform Crumble (`playCrumble`)**: Filtered descending low-frequency rumble (80 Hz -> 30 Hz) with exponential decay over 280 ms.
- **Floor Disappear (`playDisappear`)**: Crystalline descending chirp (720 Hz -> 240 Hz) with soft resonant ping over 160 ms.
- **Checkpoint Activated (`playCheckpoint`)**: Harmonic 4-note chord chime (C5, E5, G5, C6) with 500 ms exponential fade.
- **Player Death (`playDeath`)**: Melodic minor descending sweep (380 Hz down to 90 Hz) over 420 ms.
- **Level Complete Fanfare (`playLevelComplete`)**: Classical 5-note Indian celebratory fanfare (Sa-Re-Ga-Pa-TarSa: 261.63 Hz, 293.66 Hz, 329.63 Hz, 392.00 Hz, 523.25 Hz).

---

## 4. Certification & Third-Party Audit

- **Third-Party Music Bundled:** None.
- **Commercial Sample Packs Used:** None.
- **Bollywood / Commercial Film Soundtracks:** Strictly prohibited and zero instances present.
- **Audio Output Audit:** Verified pure 16-bit PCM procedural stream at 22,050 Hz sampling rate via standard Android OS APIs.
