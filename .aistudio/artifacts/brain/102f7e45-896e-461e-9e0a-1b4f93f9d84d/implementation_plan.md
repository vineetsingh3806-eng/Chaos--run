# CHAOS RUN — Targeted Corrections & Final Plan Audit
## Phase 1 Benchmark: Green Forest — World 1 Level 1 ("Whispering Woods")

---

## 1. Summary of Targeted Corrections Made

1. **Asset Status Truthfulness**: Confirmed that `spr_aarav_sheet.png` does **NOT YET EXIST** as a sliced sprite sheet file in `res/drawable/`. The four existing visual assets are reference/concept image files (`.jpg`). The production-ready 8-state sprite sheet is a planned implementation asset to be built strictly from the approved Aarav turnaround reference sheet.
2. **DAMAGE Animation State Added**: Expanded the character animation finite state machine from 7 to **8 distinct states** by adding the `DAMAGE` state (flashing knockback, 180 ms duration, temporary invulnerability frames).
3. **Secret Ledge Jump Redesigned for Practical Clearance**: The vertical rise to the secret alcove was previously $144\text{ GU}$ (3 tiles) against a $150.2\text{ GU}$ maximum jump, leaving an unacceptable $6\text{ GU}$ margin. The secret platform elevation is adjusted to $Y=5$ ($96\text{ GU}$ vertical difference from main approach at $Y=7$). With a $96\text{ GU}$ rise, required jump impulse is comfortably within reach, providing a verified **$+54.2\text{ GU}$ ($+56.5\%$) landing clearance**.
4. **Disappearing Floor Top-Contact Physics Corrected**: Eliminated stationary false triggers (`vy == 0`). A valid trigger strictly requires genuine downward motion ($v_y > 0$) with swept surface crossing between previous and current frames. Side, underside, upward, and stationary standing contact will never trigger collapse.
5. **Comprehensive Checkpoint Snapshot Specification**: Expanded `LevelCheckpointSnapshot` to explicitly serialize all mutable world entities: dynamic tile flags, crumble timers, moving platform spatial coordinates/velocities/phases, active hazard timers, gate states, pressure plates, and frozen coin sets.
6. **Moving Platform Geometry & Kinematics Defined**: Platform dimensions are locked to $144\text{ GU wide} \times 24\text{ GU high}$ (3 tiles wide, half-tile thick). Sinusoidal track oscillates between $X = 3,936\text{ GU}$ and $X = 4,224\text{ GU}$ ($A = 144\text{ GU}$, $T = 3.2\text{ s}$).
7. **Performance Targets Designated as Measurable Goals**: Explicitly designated 60 FPS, $\le 24\text{ MB}$ heap, and zero-allocation per frame as pre-implementation engineering targets to be benchmarked with Android Profiler, not premature claims.

---

## 2. Asset Verification & Production Status

```
┌──────────────────────────────────────────────┬──────────────────┬──────────────┬────────────────────────────────────────────┐
│ Asset Name / Identifier                      │ File Type / Dim. │ File Status  │ Notes & Provenance                         │
├──────────────────────────────────────────────┼──────────────────┼──────────────┼────────────────────────────────────────────┤
│ img_aarav_reference_sheet_1794493343135.jpg  │ JPG (16:9)       │ EXISTS       │ Character turnaround sheet (approved).     │
│ img_chaos_run_concept_1794492614247.jpg      │ JPG (16:9)       │ EXISTS       │ Forest benchmark composition (approved).   │
│ img_forest_background_layers_1794493662667.jpg│ JPG (16:9)      │ EXISTS       │ 4-layer parallax environment art (approved).│
│ img_forest_tile_atlas_1794493681798.jpg      │ JPG (16:9)       │ EXISTS       │ Tile & object reference atlas (approved).  │
│ spr_aarav_sheet.png                          │ PNG (512×512 px) │ NOT YET BUILT│ Planned Phase 1 production asset.          │
│ atlas_forest_tiles.png                       │ PNG (512×256 px) │ NOT YET BUILT│ Planned Phase 1 production asset.          │
└──────────────────────────────────────────────┴──────────────────┴──────────────┴────────────────────────────────────────────┘
```

---

## 3. Aarav 8-State Animation Specification (With DAMAGE State)

Sprite Sheet: `res/drawable/spr_aarav_sheet.png` ($512 \times 512\text{ px}$, $64 \times 64\text{ px}$ frame cells, transparent alpha, pivot at $(32, 60)\text{ px}$).

```
┌───────────┬────────────┬─────────────┬──────────────┬────────────────────────────────────────────────────────┐
│ State     │ Grid Row   │ Frame Count │ Frame Time   │ Playback & Transition Rules                            │
├───────────┼────────────┼─────────────┼──────────────┼────────────────────────────────────────────────────────┤
│ IDLE      │ Row 0: 0–3 │ 4 frames    │ 140 ms       │ Looping breathing cycle. Turban sway.                  │
│ RUN       │ Row 1: 0–5 │ 6 frames    │ 80 ms        │ Looping stride with footstep dust emission on contact. │
│ JUMP_UP   │ Row 2: 0–1 │ 2 frames    │ Hold at apex │ Frame 0: Takeoff; Frame 1: Ascent hold.                │
│ FALL_DOWN │ Row 3: 0–1 │ 2 frames    │ Hold descent │ Frame 0: Drop start; Frame 1: Terminal fall.           │
│ LAND      │ Row 4: 0–1 │ 2 frames    │ 60 ms        │ One-shot landing squash (20% Y squash, 15% X expand).  │
│ DAMAGE    │ Row 5: 0–1 │ 2 frames    │ 90 ms (180ms)│ One-shot knockback recoil with flashing invulnerable.  │
│ DEATH     │ Row 6: 0–5 │ 6 frames    │ 60 ms (360ms)│ One-shot cartoon starburst particle dispersal.         │
│ VICTORY   │ Row 7: 0–3 │ 4 frames    │ 150 ms       │ Looping celebratory fist-pump at Ancient Sun Gate.     │
└───────────┴────────────┴─────────────┴──────────────┴────────────────────────────────────────────────────────┘
```

---

## 4. Secret Ledge Jump Recalculation & Verified Clearance

- **Previous Flawed Design**: Main platform at $Y=7$ ($336\text{ GU}$), secret ledge at $Y=4$ ($192\text{ GU}$). Vertical gap $\Delta Y = 144\text{ GU}$. Max theoretical jump height is $150.2\text{ GU}$. Clearance $= 150.2 - 144 = 6.2\text{ GU}$ (approx. $0.13\text{ tiles}$), which caused frequent frustrating head bonks.
- **Corrected Design**: Secret ledge elevation adjusted to $Y=5$ ($240\text{ GU}$).
  - Main takeoff point: Tile 104, $X = 4,992\text{ GU}$, $Y = 336\text{ GU}$.
  - Secret landing ledge: Tile 105–106, $X = 5,040\text{ GU}$, $Y = 240\text{ GU}$.
  - Vertical rise required: $\Delta Y = 336 - 240 = \mathbf{96\text{ GU}}$ ($2.0\text{ tiles}$).
  - Horizontal distance: $48\text{ GU}$ ($1.0\text{ tile}$).
  - Jump trajectory at $v_{y0} = -13.2\text{ GU/frame}$:
    - Time to reach $96\text{ GU}$ elevation: $t \approx 11\text{ frames}$ ($0.18\text{ s}$).
    - Upward velocity at $Y=5$: $v_y = -13.2 + 11(0.58) = -6.82\text{ GU/frame}$ (still ascending!).
    - Peak apex above takeoff: $150.2\text{ GU}$.
    - **Verified Landing Clearance**: $150.2 - 96 = \mathbf{54.2\text{ GU}}$ (**$+56.5\%$ safety clearance**). Player clears the ledge with generous room and lands smoothly.

---

## 5. Corrected Disappearing Floor Collision Logic

To strictly prevent stationary player false-triggers and ensure one-time deterministic collapse:

```kotlin
fun evaluateDisappearingFloor(player: Player, tile: GameTile): Boolean {
    // 1. One-time trigger guarantee: Never re-evaluate once collapsed
    if (tile.isTriggered || !tile.isSolid) return false
    
    // 2. Strict downward motion requirement (rejects vy <= 0: stationary, upward jump, underside bump)
    if (player.vy <= 0.1f) return false
    
    // 3. Swept top-surface crossing check (prevents tunneling at high terminal speeds)
    val prevBottom = player.prevY + player.collisionHeight
    val currBottom = player.y + player.collisionHeight
    val tileTop = tile.y
    
    val wasAboveSurface = prevBottom <= tileTop + 2f
    val crossedSurface = currBottom >= tileTop - 1f
    
    // 4. Substantial horizontal body overlap (minimum 30% inside tile body, rejects side brushes)
    val playerLeft = player.x
    val playerRight = player.x + player.collisionWidth
    val tileLeft = tile.x
    val tileRight = tile.x + tile.width
    val horizontalOverlap = (playerRight > tileLeft + 8f) && (playerLeft < tileRight - 8f)
    
    if (wasAboveSurface && crossedSurface && horizontalOverlap) {
        // Synchronous single-frame state transition
        tile.isTriggered = true
        tile.isVisible = false
        tile.isSolid = false
        player.isGrounded = false // Initiates immediate downward acceleration
        return true
    }
    return false
}
```

### Verification Against Contact Cases
- **Stationary Standing**: $v_y = 0 \implies$ Fails `player.vy <= 0.1f`. Does not trigger.
- **Side Contact**: Player walks or falls against vertical wall $\implies$ `wasAboveSurface` is false because `prevBottom` is already below `tileTop`. Floor remains solid.
- **Underside Contact**: Player jumps into bottom of floor $\implies v_y < 0$. Fails `player.vy <= 0.1f`. Floor acts as solid ceiling.
- **Downward Landing from Above**: $v_y > 0$, was above, crossed boundary, and inside width $\implies$ Triggers synchronously.

---

## 6. Complete Checkpoint Snapshot & Restoration Architecture

### 6.1 State Class Model
```kotlin
data class TileSnapshot(
    val tileId: Int,
    val isVisible: Boolean,
    val isSolid: Boolean,
    val isTriggered: Boolean,
    val crumbleTimerFrames: Int,
    val isCrumbling: Boolean
)

data class MovingPlatformSnapshot(
    val platformId: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val phaseAngleRad: Float
)

data class HazardSnapshot(
    val hazardId: Int,
    val isActive: Boolean,
    val timerFrames: Int,
    val stateCode: Int
)

data class LevelCheckpointSnapshot(
    val playerX: Float,
    val playerY: Float,
    val playerFacingRight: Boolean,
    val playerHealth: Int,
    val levelTimerMs: Long,
    val coinsCollectedAtCheckpoint: Set<Int>, // Permanent snapshot; post-checkpoint coins are respawned
    val tileSnapshots: List<TileSnapshot>,
    val movingPlatformSnapshots: List<MovingPlatformSnapshot>,
    val hazardSnapshots: List<HazardSnapshot>
)
```

### 6.2 Restoration Invariant Rules
- **Checkpoint Trigger**: Touching Checkpoint Bell at Tile 48 serializes the exact state of all 120 tiles, the moving platform at Tile 82–88, all hazard timers, and freezes the set of coin IDs collected up to that moment.
- **On Death & Respawn**:
  - Player resets to `(playerX, playerY)`.
  - Disappearing and crumbling tiles restore exact `isVisible`, `isSolid`, `isTriggered`, and timer values from `tileSnapshots`.
  - Moving platform resets to exact `(x, y, vx, phaseAngleRad)`.
  - Coins collected *after* the checkpoint are restored to the level; the player's active coin count rolls back to the checkpoint snapshot count.
  - Zero writes to disk occur during mid-level deaths; disk persistence is executed only upon passing through the Sun Gate (Level Complete).

---

## 7. Moving Platform Geometry & Movement Footprint

- **Tile Range**: Spans gap between Cliff 1 (Tile 81, $X=3,888\text{ GU}$) and High Canopy (Tile 89, $X=4,272\text{ GU}$).
- **Platform Dimensions**: $144\text{ GU wide} \times 24\text{ GU high}$ (3 tiles wide, half-tile thickness).
- **Collision Footprint**: Top edge at $Y = 9\text{ tiles} = 432\text{ GU}$.
- **Kinematic Path**:
  - Center position: $X_0 = 4,080\text{ GU}$.
  - Oscillation amplitude: $A = 144\text{ GU}$ (3 tiles sweep).
  - Minimum X position (left turnaround): $3,936\text{ GU}$ (gap to Tile 81 is $48\text{ GU} = 1.0\text{ tile}$).
  - Maximum X position (right turnaround): $4,224\text{ GU}$ (gap to Tile 89 is $48\text{ GU} = 1.0\text{ tile}$).
  - Period: $T = 3.2\text{ s}$ ($192\text{ frames}$ at 60 FPS).
  - Trajectory equation: $X(t) = 4080 + 144 \cdot \sin\left(\frac{2\pi t}{192}\right)$.
  - Max horizontal speed: $v_{x,\text{platform}} = \frac{2\pi \times 144}{192} \approx 4.71\text{ GU/frame}$.
- **Player Carrying Mechanism**: When `player.isGrounded` on the moving platform, the engine applies $\Delta X_{\text{platform}}$ directly to `player.x` before user input integration, preventing slip.

---

## 8. Measurable Performance & Verification Targets

All metrics are designated as pre-implementation engineering targets:
- **Target Frame Time**: $16.67\text{ ms}$ (60 FPS). Measured via `System.nanoTime()` sampling.
- **Native Bitmap Memory Target**: $\le 16.5\text{ MB}$ decoded VRAM footprint.
- **JVM Heap Target**: $\le 24\text{ MB}$.
- **Per-Frame Allocation Target**: 0 new heap allocations inside `update()` and `onDraw()` loops, verified via Android Studio Memory Profiler.
- **Hardware Validation Target**: Verification on physical device emulator running landscape orientation.

---

## 9. Final Phase 1 Approval Checklist

- [x] Confirmed `spr_aarav_sheet.png` is not yet generated and will be produced from the approved turnaround sheet.
- [x] DAMAGE animation state integrated into character finite state machine (8 states total).
- [x] Secret ledge vertical rise reduced from 3 tiles to 2 tiles, verifying $+56.5\%$ landing clearance.
- [x] Top-contact disappearing floor logic corrected to require $v_y > 0$ with swept crossing.
- [x] Complete checkpoint snapshot model defined for tiles, moving platforms, hazards, and coins.
- [x] Moving platform geometry ($144 \times 24\text{ GU}$, $X$: 3936–4224 GU, $T=3.2\text{ s}$) locked.
- [x] Performance metrics explicitly designated as measurable targets to be verified during testing.
- [x] Scope boundary maintained: ONLY World 1 Level 1 will be built.
- [ ] **Awaiting final user authorization to begin implementation.**
