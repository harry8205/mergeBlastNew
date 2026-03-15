# 🎮 Merge Blast — Android Game

A feature-complete clone of the Merge Number Blast game, built with modern Android development best practices.

---

## 📱 Features

| Feature | Status |
|---|---|
| ✅ Merge Blocks Gameplay | Complete |
| ✅ 5×8 Grid with Gravity | Complete |
| ✅ Cascade Merge Engine | Complete |
| ✅ Combo System | Complete |
| ✅ Animated Block Tiles | Complete |
| ✅ Daily Quests (5 quests) | Complete |
| ✅ Daily Rewards (5-day streak) | Complete |
| ✅ Shop System | Complete |
| ✅ Leaderboard (local + mock AI) | Complete |
| ✅ Hammer Powerup | Complete |
| ✅ Shuffle Powerup | Complete |
| ✅ Coins Economy | Complete |
| ✅ Player Level & EXP | Complete |
| ✅ Trophy / Rank System | Complete |
| ✅ Persistent Storage (Room DB) | Complete |
| ✅ Splash Screen | Complete |
| ✅ Compose Navigation | Complete |
| ✅ Starfield Background | Complete |
| ✅ Fullscreen / Immersive Mode | Complete |
| ✅ Haptic Feedback | Complete |

---

## 🏗️ Architecture

```
com.mergeblast/
├── MergeBlastApp.kt            ← Application class (DI root)
│
├── data/
│   ├── models/
│   │   └── GameModels.kt       ← Block, GameState, PlayerData, Quest, ShopItem, etc.
│   └── repository/
│       ├── Database.kt         ← Room DB, DAOs
│       └── GameRepository.kt   ← Single source of truth
│
├── utils/
│   ├── GameEngine.kt           ← Pure game logic (drop, merge, gravity, powerups)
│   └── SoundManager.kt         ← SoundPool + vibration
│
├── viewmodel/
│   └── GameViewModel.kt        ← StateFlows, events, all game actions
│
└── ui/
    ├── MainActivity.kt         ← Compose NavHost, SplashActivity
    ├── game/
    │   └── GameScreen.kt       ← Grid, HUD, blocks, controls, game over
    ├── menu/
    │   └── MenuScreen.kt       ← Main menu, daily reward dialog
    ├── shop/
    │   └── ShopScreen.kt       ← Shop items, purchase flow
    ├── quest/
    │   └── QuestScreen.kt      ← Daily quest list
    └── leaderboard/
        └── LeaderboardScreen.kt ← Podium + ranked list
```

**Stack:**
- **UI:** Jetpack Compose + Material 3
- **Navigation:** Compose Navigation
- **State:** ViewModel + StateFlow + SharedFlow
- **Database:** Room (PlayerData, Leaderboard)
- **Preferences:** DataStore (settings, quests)
- **Animations:** Compose Animation APIs
- **Concurrency:** Kotlin Coroutines

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- JDK 17
- A device or emulator running Android 7.0+ (API 24+)

### Setup

1. **Clone / extract** the project folder.

2. **Set your SDK path:**
   ```bash
   cp local.properties.template local.properties
   # Edit local.properties and set sdk.dir=/your/android/sdk/path
   ```

3. **Open in Android Studio:**
   - File → Open → select the `MergeBlastGame` folder
   - Wait for Gradle sync to complete

4. **Run:**
   - Select a device/emulator
   - Click ▶ Run

---

## 🎮 How to Play

1. **Drop blocks** into any of the 5 columns by tapping a column.
2. **Blocks fall** to the bottom due to gravity.
3. **Identical adjacent blocks** (horizontal or vertical) **automatically merge** into a block with double the value.
4. **Cascade merges** happen automatically — one drop can trigger multiple merges!
5. **Earn coins** from merges and use them for powerups:
   - 🔨 **Hammer** (150 coins) — removes any block from the grid
   - 🔀 **Shuffle** (420 coins) — randomly rearranges all blocks
6. **Game Over** when all columns are full with no valid moves.

### Block Values
`1B → 2B → 4B → 8B → ... → 1KB → 2KB → ... → 1MB → ... → 1T → 2T → ... → 1Q`

---

## 💾 Data Persistence

| Data | Storage |
|---|---|
| Player coins, level, EXP, trophies | Room DB |
| Best scores, highest block | Room DB |
| Leaderboard entries | Room DB |
| Daily quest progress | DataStore |
| Sound / vibration settings | DataStore |
| Daily reward streak | Room DB |

---

## 🎨 Design System

| Element | Value |
|---|---|
| Background | Dark navy gradient `#1A1035 → #0A2545` |
| Block colors | 12-color rotating palette |
| Accent | Teal `#4ECDC4`, Coral `#FF6B6B`, Gold `#FFB300` |
| Typography | ExtraBold for scores/titles, SemiBold for labels |
| Animations | Spring physics, infinite transitions, scale/fade |

---

## 📦 Key Dependencies

```gradle
// Jetpack Compose BOM 2023.10.01
// Navigation Compose 2.7.6
// Room 2.6.1
// DataStore 1.0.0
// Lifecycle / ViewModel 2.7.0
// Lottie Compose 6.2.0
// Kotlin Coroutines 1.7.3
// Material 3
```

---

## 🔧 Customisation Tips

### Change block colors
Edit `BLOCK_COLORS` in `GameScreen.kt`.

### Add more shop items
Add entries to `getShopItems()` in `GameRepository.kt`.

### Change grid size
Edit `ROWS` and `COLS` constants in `GameModels.kt` (`GameState.Companion`).

### Add real ads (AdMob)
Replace the `AdButton` composable and `addCoinsFromAd()` calls with AdMob integration.

---

## 📄 License

This project is provided for educational/portfolio purposes.  
Game concept inspired by Merge Number Blast (Pineapple Studio).
