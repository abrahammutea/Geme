# Candy Splash Quest — Project & Conversation Log

This repository contains the **Candy Splash Quest** Android 2D Match-3 mobile game (Kotlin + Jetpack Compose + Custom 2D Canvas) along with the conversation summary and architecture notes so development can continue seamlessly in the cloud.

## Conversation Summary

1. **Goal & Brainstorming:**
   - Explored 2D mobile game ideas for Android, focusing on landscape orientation, obstacle action, and a lively, kid-friendly Candy Crush-style Match-3 experience.
2. **Project Scaffolding:**
   - Created native Android application **Candy Splash Quest** (`com.candysplash.quest`) targeting Android SDK 36 (minSdk 24) with Kotlin 2.3 and Jetpack Compose.
3. **Core Match-3 Engine (`CandyGameEngine.kt`):**
   - **6 Expressive Candies:** Strawberry (`Berry`), Lemon (`Sunny`), Blueberry (`Splash`), Apple (`Gummy`), Grape (`Magic`), and Tangerine (`Honey` Gummy Bear).
   - **Special Power-Up Candies:**
     - **Striped Rocket (`STRIPED_HORIZONTAL` / `STRIPED_VERTICAL`):** Created by matching 4 in a line; clears an entire row or column.
     - **3x3 Candy Bomb (`BOMB_3X3`):** Created by L/T-shaped or 5+ cluster matches; explodes a 3x3 area.
     - **Rainbow Star (`RAINBOW_STAR`):** Created by matching 5 in a line; clears all candies of the swapped color on the board.
   - **3 Action Obstacles:**
     - **Golden Jelly Tiles (`hasJelly`):** Cleared by matching candies on top of them.
     - **Frosty Ice Blocks (`iceLayers`):** Overlay on candies; cracked by matching the candy frozen inside.
     - **Crunchy Waffle Crates (`ObstacleType.WAFFLE_CRATE`):** Solid blockers that cannot be swapped; smashed by matching adjacent candies or hitting them with explosions/Lollipop Hammer.
   - **5 Progressive Worlds:**
     1. *Berry Meadow* (6x7 grid, 20 moves, 8 Jelly, 1,000 pts)
     2. *Frosty Ice Peak* (6x7 grid, 22 moves, 8 Ice, 1,600 pts)
     3. *Waffle Woods* (6x8 grid, 24 moves, 6 Waffle Crates + 6 Jelly, 2,200 pts)
     4. *Rainbow Carnival* (7x8 grid, 25 moves, 8 Jelly + 6 Ice + 4 Waffle Crates, 3,000 pts)
     5. *Candy Castle Party* (7x8 grid, 28 moves, 10 Jelly + 8 Ice + 6 Waffle Crates, 4,500 pts)
4. **Custom 2D Canvas Graphics & Animations (`CandyBoardCanvas.kt`):**
   - Custom vector rendering for every candy shape with glossy radial gradients, specular highlights, kawaii eyes, rosy cheeks, smiles, pulsing selection/hint rings, and 8-way confetti particle bursts.
   - Supports both tap-to-swap and touch drag/swipe gestures.
5. **Landscape HUD, Boosters & Synthesized Chimes (`MainScreen.kt`, `CandyGameViewModel.kt`, `CandySoundEffects.kt`):**
   - Adaptive landscape layout (`sensorLandscape` in `AndroidManifest.xml`) with Left Stats Panel (level switcher, moves, score, 3-star progress bar, obstacle counters), Center 2D Board, and Right Boosters Panel (**🔨 Hammer**, **🌈 Rainbow Drop**, **🌀 Shuffle**, **💡 Hint**, **🔄 Reset**).
   - Zero-asset synthesized pentatonic sound effects via Android `AudioTrack` (`CandySoundEffects.kt`) with mute toggle.

## Project Structure

- `app/src/main/kotlin/com/candysplash/quest/game/CandyGameEngine.kt` — Pure Kotlin Match-3 board generator, match resolver, special candy chain reactions, obstacle damage, gravity & refill, and hint finder.
- `app/src/main/kotlin/com/candysplash/quest/game/CandyGameViewModel.kt` — Coroutine-driven cascading combo loop, level progression, and booster state.
- `app/src/main/kotlin/com/candysplash/quest/game/CandySoundEffects.kt` — Procedural PCM audio chimes for pops, combos, special blasts, and level victory.
- `app/src/main/kotlin/com/candysplash/quest/ui/main/CandyBoardCanvas.kt` — Interactive 2D Canvas renderer for candies, obstacles, and particle bursts.
- `app/src/main/kotlin/com/candysplash/quest/ui/main/MainScreen.kt` — Landscape & portrait responsive Compose HUD and victory/out-of-moves dialogs.
- `app/src/test/kotlin/com/candysplash/quest/ui/main/MainScreenViewModelTest.kt` — Unit tests for board generation, obstacle counts, and booster actions.

## Building the APK

```bash
./gradlew testDebugUnitTest assembleDebug
```

The compiled debug APK is output to `app/build/outputs/apk/debug/app-debug.apk` (and copied to `CandySplashQuest.apk`).
