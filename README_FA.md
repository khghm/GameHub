
# 🎮 GameHub - پلتفرم بازی‌های چندنفره آنلاین

**GameHub** یک پلتفرم بازی‌های چندنفره آنلاین توسعه‌یافته با Kotlin Multiplatform است که شامل کلاینت اندروید (Compose)، سرور Ktor و 21 بازی مختلف است.

---

## 📁 ساختار پروژه

```
GameHub/
├── .github/                # CI/CD workflows
├── .idea/                  # فایل‌های IDE
├── .kotlin/                # لاگ‌های خطای Kotlin
├── gamehub-admin/          # پنل ادمین (React + TypeScript + Vite)
├── games/                  # ماژول‌های بازی‌ها (21 بازی)
│   ├── abalone/
│   ├── backgammon/
│   ├── backgammon-unity/  # نسخه Unity Backgammon
│   ├── baltazar/
│   ├── battleship/
│   ├── blokus/
│   ├── bridge/
│   ├── checkers/
│   ├── chess/
│   ├── connectfour/
│   ├── esmofamil/
│   ├── farkle/
│   ├── hex/
│   ├── ludo/
│   ├── match-monster/
│   ├── monopoly/
│   ├── nard/
│   ├── othello/
│   ├── soccer-striker/
│   ├── spades-baloot/
│   ├── tictactoe/
│   ├── uno/
│   └── yahtzee/
├── gradle/                 # Wrapper و نسخه‌های وابستگی‌ها
├── host/                   # کلاینت اندروید (Compose)
├── server/                 # سرور Ktor
└── shared/                 # ماژول اشتراکی (Multiplatform)
```

---

## 🧩 ماژول‌های پروژه

### 1. `:shared`
ماژول اشتراکی Kotlin Multiplatform که کدهای مشترک بین کلاینت و سرور را دارد.

#### زیرماژول‌ها:
- `graphics/`: موتور گرافیکی سفارشی
  - `math/`: توابع ریاضی، بردارها، ماتریس‌ها
  - `physics/`: موتور فیزیک ساده
  - `rendering/`: سیستم رندرینگ
  - `resources/`: مدیریت منابع (Asset Loader, Texture Cache)
  - `sprite/`: سیستم اسپریت
  - `vfx/`: افکت‌های بصری (Shader, Lighting)
- `networking/`: کلاس‌های شبکه اشتراکی (WsMessage, GameNetworkClient)
- `state/`: مدیریت استیت
- `rating/`: سیستم ریتینگ
- `idempotency/`: مدیریت تکرار درخواست‌ها
- `lock/`: مدیریت قفل‌ها (Redlock)
- `registry/`: رجیستری بازی‌ها
- `engine/`: موتور بازی‌های اسنپ‌شات

### 2. `:host`
کلاینت اندروید توسعه‌یافته با Jetpack Compose.

#### زیرماژول‌ها:
- `ui/`: کامپوننت‌های UI و صفحات
  - `components/`: کامپوننت‌های قابل استفاده مجدد
  - `navigation/`: NavGraph برای مسیریابی
  - `screens/`: صفحات مختلف (Login, Lobby, GameScreen و غیره)
  - `theme/`: تم و استایل‌های برنامه
- `network/`: کلاینت‌های API (Auth, Game, Matchmaking و غیره)
- `statemanager/`: مدیریت استیت (LobbyStore, GameplayStore)
- `viewmodel/`: ViewModel‌ها
- `ai/`: موتور هوش مصنوعی برای بازی‌ها
- `notifications/`: مدیریت اعلان‌ها
- `secure/`: ذخیره‌سازی امن (AndroidX Security)
- `time/`: همگام‌سازی زمان

#### فایل‌های مهم:
- `MainActivity.kt`: اکتیویتی اصلی
- `GameHubApp.kt`: کلاس Application
- `AndroidManifest.xml`: مانیفست اندروید

### 3. `:server`
سرور Ktor با معماری میکروسرویس‌ها (در واقع یک مونولیتیک که به صورت ماژولار توسعه‌یافته).

#### زیرماژول‌ها و سرویس‌ها:
- `admin/`: سرویس‌های ادمین (AdminService, RbacService, MetricsService, ReportService)
- `anticheat/`: سیستم ضد تقلب (AntiCheatService, SpeedHackDetector, MacroDetector, LagSwitchDetector, CollusionDetector, ShadowPoolManager)
- `bot/`: مدیریت بات‌ها (CentralBotManager, BotRotationScheduler, BotProfileRepository)
- `cache/`: مدیریت کش (CircuitBreakerCacheProvider, SessionCache, PresenceCache)
- `clan/`: سرویس کلن
- `completion/`: سرویس اتمام بازی
- `di/`: تزریق وابستگی (Koin)
- `domain/`: مدل‌های دیتابیس (Exposed ORM)
- `economy/`: اقتصاد درون‌بازی (EconomyService, ShopService, MarketDataCollector)
- `featureflags/`: مدیریت فیچر فلگ‌ها
- `lobby/`: لابی‌های کاستوم
- `matchmaking/`: سیستم جورچین (MatchmakingService)
- `modules/`: ماژول‌های Ktor (WebSocketHandler, AuthModule, FriendsModule, PartyModule و غیره)
- `notifications/`: سرویس اعلان‌ها
- `persistence/`: مدیریت دیتابیس (DatabaseFactory, Exposed ORM, Flyway)
- `rating/`: سیستم ریتینگ (RatingService, Elo/TrueSkill)
- `repository/`: ریپازیتوری‌ها
- `replay/`: سیستم ریمپ (ReplayService)
- `security/`: امنیت (JwtService, RateLimiter, TokenBlacklist, AntiReplayFilter)
- `session/`: مدیریت سشن‌های بازی
- `settings/`: تنظیمات کاربر
- `society/`: سرویس جوامع
- `wal/`: Write-Ahead Log برای ریزاقدامات (Event Sourcing)

#### فایل‌های مهم:
- `Application.kt`: نقطه ورودی سرور
- `docker-compose.yml`: فایل Docker Compose برای اجرا
- `Dockerfile`: فایل Docker Image

### 4. `:games/*`
هر بازی یک ماژول مستقل Kotlin Multiplatform است که شامل:
- `commonMain/`: کدهای مشترک (Engine, State, UI)
- `server/`: کدهای سرور
- `android/`: کدهای اندروید (در صورت نیاز)

#### لیست بازی‌ها:
1. Tic-Tac-Toe (دامنه دریا)
2. Uno (یونو)
3. Connect Four (چهار به چپ)
4. Ludo (لودو)
5. Monopoly (مونوپلی)
6. Chess (شطرنج)
7. Farkle (فارکل)
8. Esmo Famil (اسمو فامیل)
9. Backgammon (تخته‌نرد)
10. Abalone (آبالون)
11. Spades-Baloot (اسپیدز-بلوت)
12. Othello (اتلو)
13. Baltazar (بالتازار)
14. Bridge (بریج)
15. Checkers (چکرز)
16. Blokus (بلوکوس)
17. Yahtzee (یازی)
18. Nard (نرد)
19. Hex (هگز)
20. Battleship (کشتی‌ها)
21. Match-Monster (مچ مانستر)
22. Soccer Striker (فوتبال)

### 5. `gamehub-admin/`
پنل ادمین توسعه‌یافته با React + TypeScript + Vite + Tailwind CSS.

#### صفحات پنل ادمین:
- `Login`: صفحه ورود
- `Dashboard`: داشبورد اصلی
- `Users`: مدیریت کاربران
- `CheatAttempts`: گزارش‌های تقلب
- `Reports`: گزارش‌های کاربران
- `AuditLog`: لاگ حسابرسی
- `FeatureFlags`: مدیریت فیچر فلگ‌ها
- `ShadowPool`: مدیریت استخر بات‌های سایه‌ای
- `Monitoring`: مانیتورینگ سرور

---

## 🛠️ تکنولوژی‌ها و وابستگی‌ها

### کلاینت (Android):
- **زبان**: Kotlin 2.1.0
- **UI**: Jetpack Compose (BOM 2024.10.00)
- **شبکه**: Ktor Client 3.0.1 (OkHttp, WebSockets, Serialization)
- **سریال‌سازی**: Kotlinx Serialization 1.7.3
- **هم‌رویی**: Kotlinx Coroutines 1.9.0
- **امنیت**: AndroidX Security Crypto 1.0.0
- **نظیرسازی**: Navigation Compose 2.8.4
- **MinSdk**: 24 (Android 7.0)
- **CompileSdk**: 35
- **JDK**: 21

### سرور (Ktor/JVM):
- **زبان**: Kotlin 2.1.0
- **فریم‌ورک**: Ktor 3.0.1 (Netty Engine)
- **تزریق وابستگی**: Koin 4.1.1
- **ORM**: Exposed 0.52.0
- **دیتابیس**: PostgreSQL, H2 (توسعه)
- **مایگریشن**: Flyway 10.17.1
- **کش**: Redis (Lettuce 6.4.1)
- **امنیت**: JWT (JJWT 0.12.6), BCrypt
- **متریکس**: Micrometer + Prometheus
- **تحمل خطا**: Resilience4j 2.2.0
- **JDK**: 21

### پنل ادمین:
- **فریم‌ورک**: React 18
- **زبان**: TypeScript 5
- **بیلد**: Vite 5
- **استایل**: Tailwind CSS 3
- **HTTP**: Fetch API

---

## 🚀 راه‌اندازی پروژه

### پیش‌نیازها:
1. JDK 21 (LTS)
2. Android Studio Hedgehog | 2024.1.1 یا بالاتر
3. Docker و Docker Compose (برای سرور)
4. Node.js 20+ (برای پنل ادمین)
5. Gradle 8.11.1 (در پروژه Wrapper وجود دارد)

### مرحله 1: رفع مشکلات اولیه
قبل از همه، فایل `settings.gradle.kts` را ویرایش کنید و ریپازیتوری‌های استاندارد را فعال کنید:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()  // فعال کنید
        mavenCentral()  // فعال کنید
        maven { url = uri("https://maven.myket.ir") }
        maven { url = uri("https://maven.myket.id") }
        maven { url = uri("https://jitpack.io") }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()  // فعال کنید
        mavenCentral()  // فعال کنید
        maven { url = uri("https://maven.myket.ir") }
        maven { url = uri("https://maven.myket.id") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.google.com") }
    }
}
```

### مرحله 2: کلاینت اندروید
1. پروژه را در Android Studio باز کنید
2. Sync Gradle را انجام دهید
3. ماژول `:host` را انتخاب کنید
4. روی دکمه Run کلیک کنید یا از دستور زیر استفاده کنید:
   ```bash
   ./gradlew :host:assembleDebug
   ```

### مرحله 3: سرور Ktor
#### روش A: با Docker Compose (توصیه‌شده)
```bash
cd server
docker-compose up -d
```

#### روش B: بدون Docker
1. یک دیتابیس PostgreSQL ایجاد کنید
2. متغیرهای محیطی را تنظیم کنید (برای اطلاعات بیشتر به `server/build.gradle.kts` و `server/src/main/kotlin/com/gamehub/server/di/AppModule.kt` مراجعه کنید)
3. سرور را اجرا کنید:
   ```bash
   ./gradlew :server:run
   ```

سرور روی پورت `8080` اجرا می‌شود.
- Metrics: `http://localhost:8080/metrics`
- Admin API: `http://localhost:8080/api/admin/*`

### مرحله 4: پنل ادمین
```bash
cd gamehub-admin
npm install
npm run dev
```

پنل روی `http://localhost:5173` اجرا می‌شود.
- نام کاربری پیش‌فرض: `admin`
- رمز عبور پیش‌فرض: `admin123`

---

## 🔧 پیکربندی کلاینت

IP سرور در `host/build.gradle.kts` به صورت hard-coded تنظیم شده است. برای تغییر آن:

```kotlin
// host/build.gradle.kts
android {
    defaultConfig {
        // ...
        buildConfigField("String", "SERVER_IP", "\"192.168.1.100\"") // IP خود را وارد کنید
    }
}
```

یا بهتر است از یک فایل `local.properties` استفاده کنید:
```properties
# local.properties
server.ip=192.168.1.100
```

---

## 📊 ویژگی‌های کلیدی

### 🔐 امنیت
- **احراز هویت**: JWT Token
- **رمزگذاری**: BCrypt برای پسوردها
- **ضد تکرار درخواست**: Idempotency Manager
- **محدودیت درخواست**: Rate Limiter
- **محرمانه‌سازی**: Anti-Replay Filter
- **هویت‌سنجی دستگاه**: Device Attestation

### 🤖 ضد تقلب (Anti-Cheat)
- **Speed Hack Detection**: تشخیص حرکت‌های بسیار سریع
- **Macro Detection**: تشخیص الگوهای تکراری (ماکرو)
- **Lag Switch Detection**: تشخیص نوسان شدید تاخیر
- **Collusion Detection**: تشخیص تبانی بین کاربران
- **Shadow Pool**: استخر بات‌های سایه‌ای برای آزمایش کاربران مشکوک
- **Time Attestation**: تأیید زمان کلاینت

### 👥 ویژگی‌های اجتماعی
- دوستان (Friends)
- پارت‌ها (Parties)
- کلن‌ها (Clans)
- جوامع (Societies)
- چت (Chat)
- اعلان‌ها (Notifications)

### 🏆 سیستم ریتینگ و لیدربرد
- الگوریتم Elo/TrueSkill
- لیدربرد برای هر بازی
- تاریخچه بازی‌ها (Match History)

### 🧠 بات‌ها (Bots)
- بات‌های با سختی‌های مختلف (1-10)
- Shadow Bots برای ضد تقلب
- Bot Strategy Registry
- Bot Rotation Scheduler

### 🎯 سیستم فیچر فلگ
- فعال/غیرفعال کردن فیچرها بدون دیپلوی مجدد
- محیط‌های مختلف (Production, Staging, Development)

### 📈 مانیتورینگ و لاگ
- Micrometer Metrics + Prometheus
- Audit Log برای عملیات ادمین
- Write-Ahead Log (Event Sourcing)
- لاگ‌های بازی (Game Event Log)

---

## 📚 مستندات بیشتر

- [موتور گرافیکی - README_FA](GRAPHICS_ENGINE_README_FA.md)
- [پنل ادمین - README](gamehub-admin/README.md)
- [Backgammon Unity - README](games/backgammon-unity/README.md)

---

## 🤝 مشارکت

قبل از مشارکت، لطفاً:
1. یک Issue برای مشکل یا فیچر جدید ایجاد کنید
2. یک Branch از `main` بسازید
3. تغییرات خود را Commit کنید
4. یک Pull Request ایجاد کنید

---

## 📄 لایسنس

این پروژه تحت لایسنس [MIT](LICENSE) منتشر شده است.

---

## 📞 تماس

برای سوال یا مشکل، لطفاً یک Issue ایجاد کنید.

---

## 🙌 یادداشت‌ها مهم

### مشکلات شناخته‌شده:
1. **ریپازیتوری‌ها در settings.gradle.kts**: فعال نیستند (باید باز کنید)
2. **خطای Kotlin Compiler در Soccer Physics Engine**: خطای `Expected expression 'FirPropertyAccessExpressionImpl' to be resolved` (احتمالاً با نسخه پایدارتر Kotlin حل می‌شود)
3. **فایل‌های JAR در games/*/build/libs**: نباید در git باشند (باید به .gitignore اضافه شوند)
4. **IP سرور hard-coded**: باید از متغیر محیطی یا local.properties استفاده شود
5. **فایل temp_SoccerPhysics.kt**: باید حذف شود

### توصیه‌ها:
- از Kotlin 1.9.x یا یک نسخه پایدارتر 2.x استفاده کنید
- برای بهبود عملکرد، از Redis در تولید استفاده کنید
- رمزهای عبور و کلیدها را در متغیرهای محیطی ذخیره کنید، نه در کد
- فایل `local.properties` را در .gitignore نگه دارید
