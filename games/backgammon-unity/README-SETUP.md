# راهنمای تنظیم پروژه تخته‌نرد Unity

## ۱. باز کردن پروژه در Unity
1. Unity Hub را باز کنید
2. روی **Add project from disk** کلیک کنید
3. پوشه‌ی `GameHub/games/backgammon-unity` را انتخاب کنید
4. Unity نسخه 2022.3 LTS را برای باز کردن پروژه انتخاب کنید

## ۲. آماده‌سازی صحنه
### 2.1 ایجاد صحنه جدید
- روی **File → New Scene** کلیک کنید
- صحنه را با نام `MainScene` ذخیره کنید

### 2.2 اضافه کردن تخته
1. در `Project` پوشه `Assets/Sprites` را باز کنید
2. تصویر `backgammon.jpg` را به `Hierarchy` بکشید
3. این GameObject را به `Board` تغییر نام دهید
4. در `Inspector`:
   - `Position (X, Y, Z)` را روی (0, 0, 0) قرار دهید
   - `Scale (X, Y, Z)` را روی (1, 1, 1) قرار دهید
   - `Sorting Layer` را روی `Background` قرار دهید

### 2.3 ساخت نقاط (Points)
برای هر نقطه از 1 تا 24:
1. یک GameObject خالی بسازید و نامش را به `Point_1` (تا `Point_24`) تغییر دهید
2. این GameObject را در جای مناسب روی تخته قرار دهید
3. کامپوننت `PointController.cs` را به آن اضافه کنید
4. فیلد `pointIndex` را به شماره‌ی نقطه تنظیم کنید
5. همه‌ی این 24 GameObject را به `BackgammonGameManager` در فیلد `pointSpawnPoints` در `Inspector` اضافه کنید

### 2.4 ساخت پرفاب مهره
#### سفید
1. یک GameObject خالی بسازید و نامش را به `WhiteChecker` تغییر دهید
2. کامپوننت‌های زیر را اضافه کنید:
   - `Sprite Renderer`: تصویر `Sprites/white.png` را انتخاب کنید
   - `Circle Collider 2D`: `Is Trigger` را فعال کنید
   - `Rigidbody 2D`: `Gravity Scale` را 0 قرار دهید و `Body Type` را `Dynamic` کنید
   - `CheckerController.cs` را اضافه کنید
3. این GameObject را به پوشه `Assets/Prefabs` بکشید تا به پرفاب تبدیل شود
4. GameObject صحنه را حذف کنید

#### سیاه
مراحل بالا را برای `BlackChecker` تکرار کنید، با این تفاوت که تصویر `black.png` را انتخاب کنید

### 2.5 ساخت پرفاب تاس
1. GameObject خالی بسازید و نامش را `Die` بگذارید
2. کامپوننت‌های زیر را اضافه کنید:
   - `Sprite Renderer`
   - `Circle Collider 2D`
   - `DieController.cs`
   - `Animator` (اختیاری، برای انیمیشن)
3. به `Assets/Prefabs` بکشید و از صحنه حذف کنید

### 2.6 ساخت Game Manager
1. GameObject خالی بسازید و نامش را `GameManager` بگذارید
2. کامپوننت `BackgammonGameManager.cs` را به آن اضافه کنید
3. فیلدهای زیر را در `Inspector` پر کنید:
   - `boardRenderer`: `SpriteRenderer` GameObject `Board` را drag کنید
   - `whiteCheckerPrefab`: پرفاب `WhiteChecker` را بکشید
   - `blackCheckerPrefab`: پرفاب `BlackChecker` را بکشید
   - `pointSpawnPoints`: تمام نقاط (Point_1 تا Point_24) را به این لیست اضافه کنید
   - `die1` و `die2`: بعد از ساخت UI پر می‌شوند
   - `rollDiceButton`: بعد از ساخت UI اضافه می‌شود
   - `endTurnButton`: بعد از ساخت UI اضافه می‌شود
   - `statusText`, `whiteBorneOffText`, `blackBorneOffText`: بعد از ساخت UI پر می‌شوند

### 2.7 ساخت UI
1. روی `GameObject → UI → Canvas` کلیک کنید
2. روی `Canvas` راست‌کلیک کنید و `UI → Button` را انتخاب کنید؛ نامش را `RollDiceButton` بگذارید
3. دکمه‌ی دوم بسازید و نامش را `EndTurnButton` بگذارید
4. سه `Text` بسازید:
   - `StatusText`: برای نمایش وضعیت نوبت
   - `WhiteBorneOffText`: برای نمایش تعداد مهره‌های بیرون رفته‌ی سفید
   - `BlackBorneOffText`: برای نمایش تعداد مهره‌های بیرون رفته‌ی سیاه
5. دو `Text` برای نمایش تاس‌ها بسازید: `Die1Text` و `Die2Text`
6. همه‌ی این UIها را در `BackgammonGameManager` در `Inspector` در فیلدهای مربوطه قرار دهید
7. برای `OnClick` دکمه‌های `RollDiceButton` و `EndTurnButton` به ترتیب توابع `RollDice` و (تابع نوبت) از `BackgammonGameManager` را انتخاب کنید

## ۳. تست بازی
1. در `BackgammonGameManager.cs` تابع `Start()` را به صورت زیر اضافه کنید (اختیاری، برای شروع خودکار):
```csharp
void Start()
{
    StartNewGame("Player1", "Player2");
}
```
2. روی دکمه‌ی `Play` کلیک کنید!

## ۴. نکات مهم
- تصاویر در `Assets/Sprites` باید `Texture Type`شان روی `Sprite (2D and UI)` تنظیم شده باشد
- برای بهتر شدن انیمیشن‌ها از `DOTween` یا `LeanTween` استفاده کنید
- برای صدا، فایل‌های audio را در `Assets/Audio` قرار دهید و از `AudioSource` استفاده کنید
