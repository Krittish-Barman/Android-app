# 📱 Daily Tracker

**One offline Android app for your habits, money, study time and tasks, with "what's next" notifications and automatic expense tracking from bank SMS.**

No account. No server. No ads. **No INTERNET permission**, so the app cannot upload your data anywhere. Everything stays on your phone.

![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-green)
![Language](https://img.shields.io/badge/language-Java-orange)
![UI](https://img.shields.io/badge/UI-XML%20Views%20%2B%20Material%203-blue)
![Database](https://img.shields.io/badge/database-Room-lightgrey)

---

## 📸 Screenshots

| Home | Money | Focus | Stats |
|:----:|:-----:|:-----:|:-----:|
| <img src="screenshots/home.jpg" width="200"/> | <img src="screenshots/money.jpg" width="200"/> | <img src="screenshots/focus.jpg" width="200"/> | <img src="screenshots/stats.jpg" width="200"/> |
| Today's dashboard with "Up next" | Budget bar and expenses | Focus timer with subjects | Study time and habit completion |

------|--------|-------|-------|-------|
| `screenshots/home.png` | `screenshots/habits.png` | `screenshots/money.png` | `screenshots/focus.png` | `screenshots/stats.png` |

**Demo:** `screenshots/demo.gif`

---

## ✨ Features

### 🏠 Home
- Today's dashboard: progress ring, habits, top 3 tasks, money spent, study time
- **"Up next" card** shows the nearest habit or task with a Done button

### ✅ Habits
- Daily, chosen weekdays, or "X times per week"
- **Streaks** (a skipped day keeps your streak, a missed day resets it)
- 12-week **heatmap**. Tap any day to fix a forgotten tick
- Optional target (for example 20 min) and a reminder time

### 💸 Money
- Expense, income and transfer with a fast **number pad**
- Categories, monthly budget bar ("70% used · ₹1,800 left"), filters
- Swipe to delete with **Undo**
- **Automatic expenses from SMS** (see below)

### ⏱️ Focus timer
- 25/5, 50/10 or custom sessions, with subjects (DSA, Java, English...)
- Keeps the right time with the **screen off or app closed**
- Optional rule: leaving the app for 10+ seconds marks the session "interrupted"
- Daily goal and session history

### 🔔 Smart notifications
- "Next: Gym at 6:00 PM (in 15 min)" with **Done** and **Snooze** buttons
- Morning plan, evening check, streak at risk, budget alert (80% and 100%), weekly summary
- Quiet hours and a switch for each type
- Alarms are set again after a phone restart

### 📊 Stats
- Study minutes chart, habit completion %, spending by category, best study time

### 🔒 Privacy and safety
- Fingerprint / screen lock
- Light and dark theme
- Only SMS from senders **you** allow are read, and OTP and offer messages are ignored

---

## 📥 Option 1: Install the app (no coding needed)

1. Go to the **[Releases](https://github.com/Krittish-Barman/Android-app/releases)** page of this repository.
2. Download `DailyTracker-v1.0.apk` to your phone.
3. Open the file. If Android asks, allow **"Install unknown apps"** for your browser or file manager.
4. Open **Daily Tracker** and follow the 3-step welcome screen.

> This is a personal app installed from an APK. It is not on the Play Store.

---

## 🛠️ Option 2: Build it yourself (for developers)

**You need:** [Android Studio](https://developer.android.com/studio) (latest stable) and an Android phone with USB debugging (recommended) or an emulator.

### Step by step

1. **Download the code**
   ```bash
   git clone https://github.com/Krittish-Barman/Android-app.git
   cd Android-app
   ```
   (or click **Code → Download ZIP** on GitHub and unzip it)
2. Open Android Studio → **File → Open** → choose the project folder.
3. Wait for **Gradle sync** to finish (the first time it downloads libraries, so you need internet once).
4. Connect your phone and press the green **Run ▶** button.

**Minimum Android version:** 8.0 (API 26).

> If you get a build error, try **Build → Clean Project**, then **Build → Rebuild Project**. Still stuck? Open an issue and paste the error text.

### Run the tests
```bash
./gradlew test
```
Unit tests cover the **SMS parser**, the **streak calculator** and **money formatting**.

---

## 💬 How automatic expense tracking works

Your bank and UPI apps already send an SMS for every payment. This app reads those messages **on your phone** and creates a *pending* entry that you confirm with one tap.

```
SMS arrives → sender allowed? → skip OTP/offers → read amount & merchant
→ duplicate check → guess category → save as PENDING → you Confirm / Edit / Ignore
```

### Turn it on
1. Open **Settings → Money → Auto-detect expenses from SMS** and allow the permission.
2. Open **Allowed SMS senders** and add your bank's sender ids, separated by commas (for example `HDFCBK, SBIUPI`).
   *Tip: open one bank SMS on your phone and look at the sender name.*
3. New payments appear in **Money → Review**.
4. Use **Import old SMS** to test with the last 30 to 90 days.

### If Android blocks the SMS permission
On Android 13+ a sideloaded app may need one extra step:
**Settings → Apps → Daily Tracker → ⋮ (three dots) → Allow restricted settings**, then try again.

### Good to know
- No parser is 100% correct. That is why you review every entry first.
- Banks write SMS differently. If one is read wrongly, add it to `SmsParserTest.java` and adjust the regex in `SmsParser.java`.
- The raw SMS text is deleted after you confirm or ignore an entry.

---

## 🔐 Privacy

| What | How |
|------|-----|
| Internet | The app does **not** request the INTERNET permission |
| Data storage | Local database on your phone only |
| SMS | Only allowed senders are read, processed on the device |
| Account / login | None |
| Ads / tracking | None |

---

## ⚙️ Permissions

| Permission | Why |
|------------|-----|
| `POST_NOTIFICATIONS` | Show reminders |
| `SCHEDULE_EXACT_ALARM` | Exact "Next up" and focus-timer times |
| `RECEIVE_BOOT_COMPLETED` | Set alarms again after restart |
| `RECEIVE_SMS`, `READ_SMS` | Optional auto-detect expenses |
| `USE_BIOMETRIC` | Fingerprint lock |
| `VIBRATE` | Small vibration when you tick a habit |

---

## 🔋 Reminders not on time?

Some phones (Xiaomi, Oppo, Vivo, Realme...) stop background apps.
1. App info → **Battery → Unrestricted**
2. Turn on **Auto-start** for Daily Tracker
3. Allow **Alarms & reminders**

The app has a **Settings → Battery help** button that opens these screens.

---

## 🧱 How the code is organised

```
app/src/main/java/com/krittish/dailytracker/
├── ui/            Screens (Fragments) + ViewModels: home, habits, money, focus, tasks, stats, settings, onboarding
├── data/
│   ├── db/        Room database: entities and DAOs
│   ├── repo/      Repositories (the UI never touches the database directly)
│   └── prefs/     Settings (SharedPreferences)
├── domain/        Pure logic: StreakCalculator, BudgetCalculator, UpNextProvider
├── sms/           SmsReceiver, SmsParser (pure Java), SmsProcessor, SmsImporter
├── notifications/ NotificationScheduler (owns all alarms and work), receivers, WorkManager worker
├── focus/         FocusTimerManager
└── util/          MoneyFormat (paise), DateUtils, AppExecutors, constants
```

**Architecture in one line:** Fragment → ViewModel → Repository → Room (LiveData flows back up).

**Tech stack:** Java · XML Views + ViewBinding · Material 3 · Navigation component (one Activity, many Fragments) · Room + LiveData · WorkManager + AlarmManager · BiometricPrompt · JUnit

### Design choices worth knowing
- **Money is stored in paise** (`long`), so there are no decimal rounding errors.
- **Streaks are calculated from logs**, not stored as a counter, so editing a past day never breaks them.
- **The focus timer saves its end time** and uses an alarm, so it stays correct with the screen off.
- **Duplicate SMS** are blocked with a unique reference number (or a hash of amount, type, account, minute and merchant).
- **Database work runs on a background executor**, and screens only read LiveData.

---

## 🗺️ Roadmap

- [x] Habits, money, home dashboard
- [x] Focus timer, tasks, notifications
- [x] Automatic expenses from SMS
- [x] Stats, budget alerts, fingerprint lock, dark mode
- [ ] Export / import backup
- [ ] Recurring monthly entries and per-category budgets
- [ ] Home screen widget
- [ ] English word of the day with pronunciation and quiz

---

## 🤝 Contributing

Ideas and bug reports are welcome. Open an [Issue](https://github.com/Krittish-Barman/Android-app/issues), or send a **Pull Request**. When sharing SMS samples, always remove names, account numbers and balances.

---

## ⚠️ Disclaimer

This is a personal project for learning and daily use. Always check your entries against your real bank statement. The Play Store restricts SMS permissions, so this app is meant for sideloading (personal APK) only.

---

## 👤 Author

**Krittish Barman**, MCA student, Kolkata
GitHub: [@Krittish-Barman](https://github.com/Krittish-Barman) · Project: [Android-app](https://github.com/Krittish-Barman/Android-app)

If you like the project, please give it a ⭐

---

## 📄 License

Choose a license when you create the repo (the MIT License is a common simple choice) and add a `LICENSE` file.
