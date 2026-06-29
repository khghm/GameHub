# Backgammon (Unity Version)
This is the Unity-based implementation of Backgammon for GameHub!

## How to Open in Unity
1. Download and install **Unity 2022.3 LTS** (Long Term Support)
2. Open Unity Hub
3. Click "Add Project from Disk"
4. Navigate to `GameHub/games/backgammon-unity` and select the folder

## Project Structure
```
Assets/
├── Scripts/
│   ├── BackgammonState.cs     # Game state data structures
│   ├── CheckerController.cs   # Individual checker behavior
│   ├── DieController.cs       # Individual die behavior
│   └── BackgammonGameManager.cs  # Core game management
├── Prefabs/                   # Prefabs for checkers, dice, board
├── Sprites/                   # Board, checker, and dice sprites
├── Animations/                # Animator controllers and clips
└── Resources/                 # Runtime resources
```

## Setup Instructions (Required!)
1. **Board & Points**:
   - Import your backgammon board image into `Assets/Sprites`
   - Place the board sprite as a GameObject in the scene
   - Create **24 empty GameObjects** as "Point Spawn Points" and assign them to `BackgammonGameManager.pointSpawnPoints` in the Inspector
   - Add spawn points for bar and bear off areas too

2. **Checker Prefabs**:
   - Create 2 prefabs (white & black checkers)
   - Add `SpriteRenderer`, `Rigidbody2D` (set to Kinematic by default), and `CircleCollider2D`
   - Attach `CheckerController.cs` script
   - Assign the prefabs to `BackgammonGameManager.whiteCheckerPrefab` and `blackCheckerPrefab`

3. **Dice**:
   - Import your dice images into `Assets/Sprites` (1-6)
   - Create a die prefab with `SpriteRenderer`, `Animator`, and `CircleCollider2D`
   - Attach `DieController.cs` script
   - Assign dice sprites to the `diceSprites` array in `DieController`

4. **UI**:
   - Create UI buttons for "Roll Dice" and "End Turn"
   - Hook up their `OnClick` events to `BackgammonGameManager.RollDice`

## Communication with GameHub
To connect this Unity game with the main GameHub app, you can use:
- **Unity as a Library**: Export the Unity project as an Android Library (AAR) and integrate it into GameHub's host module
- **Message Passing**: Use `UnitySendMessage` (Unity → Android) and `UnityPlayer.UnitySendMessage` (Android → Unity) for two-way communication
