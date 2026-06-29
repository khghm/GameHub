
# 🎮 GameHub - Online Multiplayer Game Platform

**GameHub** is an online multiplayer game platform developed with Kotlin Multiplatform, including an Android client (Compose), Ktor server, and 21 different games.

- Persian (فارسی) README: [README_FA.md](README_FA.md)
- Graphics Engine README (Persian): [GRAPHICS_ENGINE_README_FA.md](GRAPHICS_ENGINE_README_FA.md)

---

## 📁 Project Structure

```
GameHub/
├── .github/                # CI/CD workflows
├── gamehub-admin/          # Admin panel (React + TypeScript + Vite)
├── games/                  # Game modules (21 games)
├── gradle/                 # Wrapper and dependency versions
├── host/                   # Android client (Compose)
├── server/                 # Ktor server
└── shared/                 # Shared multiplatform module
```

---

## 🧩 Project Modules

### 1. `:shared`
Shared Kotlin Multiplatform module with common code between client and server.
- Graphics engine (math, physics, rendering, sprites, VFX)
- Networking classes
- State management
- Rating system
- Idempotency and locking

### 2. `:host`
Android client developed with Jetpack Compose.
- Compose UI components
- API clients
- State management
- ViewModels
- AI engine
- Notifications
- Secure storage
- Time synchronization

### 3. `:server`
Ktor server with modular architecture.
- Admin services
- Anti-cheat system
- Bot management
- Caching
- Clan/society systems
- Economy system
- Feature flags
- Matchmaking
- JWT security
- Session management
- PostgreSQL + Exposed ORM + Flyway
- Redis caching
- Micrometer + Prometheus metrics

### 4. `:games/*`
Each game is an independent Kotlin Multiplatform module with common, server, and (optional) Android code.

**Games list:**
Tic-Tac-Toe, Uno, Connect Four, Ludo, Monopoly, Chess, Farkle, Esmo Famil, Backgammon, Abalone, Spades-Baloot, Othello, Baltazar, Bridge, Checkers, Blokus, Yahtzee, Nard, Hex, Battleship, Match-Monster, Soccer Striker.

### 5. `gamehub-admin/`
Admin panel built with React + TypeScript + Vite + Tailwind CSS.

---

## 🚀 Quick Start

### Prerequisites
- JDK 21
- Android Studio Hedgehog | 2024.1.1+
- Docker & Docker Compose (for server)
- Node.js 20+ (for admin panel)

### 1. Fix Initial Issues
First, enable standard repositories in `settings.gradle.kts`:
```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.myket.ir") }
        maven { url = uri("https://maven.myket.id") }
        maven { url = uri("https://jitpack.io") }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.myket.ir") }
        maven { url = uri("https://maven.myket.id") }
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Android Client
```bash
./gradlew :host:assembleDebug
```

### 3. Server
```bash
cd server
docker-compose up -d
```
Server runs on `http://localhost:8080`

### 4. Admin Panel
```bash
cd gamehub-admin
npm install
npm run dev
```
Admin panel runs on `http://localhost:5173`
- Default username: `admin`
- Default password: `admin123`

---

## 🛠️ Tech Stack

### Client (Android)
- Kotlin 2.1.0
- Jetpack Compose
- Ktor Client 3.0.1
- Kotlinx Coroutines 1.9.0
- Kotlinx Serialization 1.7.3
- AndroidX Security

### Server (Ktor/JVM)
- Kotlin 2.1.0
- Ktor 3.0.1
- Koin 4.1.1 (DI)
- Exposed 0.52.0 (ORM)
- PostgreSQL + Flyway
- Redis (Lettuce)
- JWT (JJWT 0.12.6)
- Micrometer + Prometheus
- Resilience4j 2.2.0

### Admin Panel
- React 18
- TypeScript 5
- Vite 5
- Tailwind CSS 3

---

## 📊 Key Features

- 🔐 JWT Authentication
- 🤖 Advanced Anti-Cheat System (Speed Hack, Macro, Lag Switch, Collusion Detection)
- 👥 Social Features (Friends, Parties, Clans, Chat)
- 🏆 Rating System (Elo/TrueSkill) & Leaderboards
- 🧠 AI Bots with multiple difficulty levels
- 🎯 Feature Flags
- 📈 Monitoring & Metrics (Prometheus)
- 📝 Audit Logs & Event Sourcing

---

## 📄 License

This project is licensed under the MIT License.
