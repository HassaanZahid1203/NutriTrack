# NutriTrack

NutriTrack is a mobile health and nutrition tracking application built per the
Software Requirements Specification (Version 1.0, 10 March 2026).

It lets users log meals, track calories and macronutrients, monitor a daily
**rule-based** Health Score (no AI/ML, per SRS §2.5), and run "what-if"
simulations of dietary changes.

## Tech stack

| Layer | Technology | SRS reference |
| ----- | ---------- | ------------- |
| Platform | Android 9+ (minSdk 28, targetSdk 34) | §2.4 |
| Language | Java | §2.4 |
| UI | Material Design components | §3.1 |
| Auth | Firebase Authentication (email/pw + Google) | §3.3, REQ-1, REQ-2 |
| Cloud DB | Firebase Firestore | §3.3 |
| Local DB | SQLite (food catalogue, seeded with Pakistani foods) | §2.5, §3.3 |
| Charts | MPAndroidChart 3.1.0 | §3.3 |
| Comms | HTTPS (default Firebase transport) | §3.4 |

## Screen → mockup map

| Screen | File | Mockup page |
| ------ | ---- | ----------- |
| Splash | `SplashActivity.java` | – |
| Login / Register | `auth/LoginActivity.java` + `activity_login.xml` | 1, 2 |
| Dashboard | `ui/home/HomeFragment.java` + `fragment_home.xml` | 3 |
| Today's Meals | `ui/meals/MealsFragment.java` + `fragment_meals.xml` | 4 |
| Log Food | `ui/log/LogFoodFragment.java` + `fragment_log_food.xml` | 5 |
| Trends / Simulator | `ui/trends/TrendsFragment.java` + `fragment_trends.xml` | 6 |
| Profile | `ui/profile/ProfileFragment.java` + `fragment_profile.xml` | 7 |

## SRS requirement → implementation map

| Requirement | Implementation |
| ----------- | -------------- |
| REQ-1 (email/pw register) | `LoginActivity.registerWithEmail()` |
| REQ-2 (Firebase Auth) | `LoginActivity` uses `FirebaseAuth` |
| REQ-3 (sign out) | `ProfileFragment.btnSignOut` calls `FirebaseAuth.signOut()` |
| REQ-4 (select food) | `LogFoodFragment.onFoodTap()` |
| REQ-5 (auto-calculate calories) | `MealEntry` ctor multiplies food kcal × quantity |
| REQ-6 (store meal history) | `FirestoreManager.addMealEntry()` writes to `users/{uid}/meals` |
| REQ-7 (daily health score) | `HealthScoreCalculator.compute()` |
| REQ-8 (score 0-100) | All component scores `clamp(0..100)` |
| REQ-9 (simulate meal options) | `TrendsFragment` portion + swap sliders |
| REQ-10 (estimate macro change) | `TrendsFragment.recompute()` |
| REQ-11 (display projected outcomes) | `TrendsFragment` projected score card |
| BR-1 (manual meal logging) | App has no auto-detection / camera — only `LogFoodFragment` |
| BR-2 (score 0-100) | enforced by `HealthScoreCalculator` |
| BR-3 (predefined nutrition) | All food data in `FoodDatabaseHelper.seedFoods()` |

The Health Score formula follows §4.3 exactly:

```
Health Score = Protein Balance   × 0.30
             + Calorie Discipline × 0.30
             + Fat Moderation     × 0.20
             + Consistency Score  × 0.20
```

## Local food database

Seeded on first launch by `FoodDatabaseHelper.onCreate()`. Per SRS §1.4, the
catalogue includes Pakistani items: Chicken Biryani, Daal Chawal,
Chapati / Roti, Aloo Paratha, Chicken Karahi, Beef Nihari, Seekh Kebab,
Samosa, Lassi, Mango, Chai, Naan — alongside Western basics.

Macro values are per 100 g (or per piece / per 100 ml where indicated).

## Building the app

### Prerequisites

* Android Studio Hedgehog (2023.1.1) or newer
* Android SDK 34
* JDK 17

### One-time Firebase setup

The project compiles without Firebase, but Auth and meal sync only work after
the steps below.

1. Go to <https://console.firebase.google.com> and create a project named
   "NutriTrack".
2. **Authentication → Sign-in method**: enable **Email/Password** and
   **Google**.
3. **Build → Firestore Database → Create database** (start in test mode for
   development).
4. **Project settings → Your apps → Android** → register an Android app with
   package name `com.nutritrack.app`.
5. Download `google-services.json` and drop it into the `app/` directory.
6. **Project settings → Your apps → Web SDK config**: copy the *Web client ID*
   and replace the value of `default_web_client_id` in
   `app/src/main/res/values/strings.xml`. (This is what enables the
   "Continue with Google" button.)

### Run

```sh
./gradlew :app:installDebug
```

…or just open the project in Android Studio and press Run. (Android Studio
will fetch the Gradle wrapper jar automatically the first time you sync;
alternatively run `gradle wrapper --gradle-version 8.4` in the project root if
you have a system Gradle installed.)

## Project structure

```
app/src/main
├── AndroidManifest.xml
├── java/com/nutritrack/app
│   ├── NutriTrackApplication.java       Initializes Firebase
│   ├── SplashActivity.java              Routes to Login or Main
│   ├── MainActivity.java                Hosts bottom nav + 5 fragments
│   ├── auth/LoginActivity.java          Email/pw + Google sign-in (REQ-1, REQ-2)
│   ├── adapters/                        RecyclerView adapters
│   ├── database/
│   │   ├── FoodDatabaseHelper.java      SQLite + Pakistani food seed (BR-3)
│   │   └── FirestoreManager.java        Cloud meal sync (§3.3)
│   ├── models/                          POJOs (User, Food, MealEntry, DailyTotals)
│   ├── ui/
│   │   ├── home/HomeFragment.java       Dashboard + weekly chart
│   │   ├── meals/MealsFragment.java     Today's meals overview
│   │   ├── log/LogFoodFragment.java     Meal logging (REQ-4..6)
│   │   ├── trends/TrendsFragment.java   What-If simulator (REQ-9..11)
│   │   ├── profile/ProfileFragment.java Profile + settings + sign-out (REQ-3)
│   │   └── widgets/RingProgressView.java Custom donut progress view
│   └── utils/
│       ├── HealthScoreCalculator.java   Rule-based score per §4.3
│       └── DateUtils.java
└── res/                                 Layouts, drawables, values, menus, color states
```

## Notes for graders / reviewers

* **No AI/ML used.** The Health Score is a transparent weighted formula — see
  `HealthScoreCalculator.java`. Per SRS §2.5 and §1.4 this is intentional.
* **Wellness insights only.** Per SRS §5.2 the app does not provide medical
  advice.
* **Food catalogue is shared and admin-maintained.** Per SRS §2.3.3 admin users
  maintain a single shared food database. Foods live in the Firestore `foods`
  collection. The first time any user opens the app and the collection is
  empty, `FoodRepository.seedIfEmpty()` populates it with the 20 baseline
  items including the Pakistani meals required by §1.4. After that, admins
  add/edit/delete via the in-app panel.
* **Per-user daily target.** The dashboard, today's meals, and profile screens
  all read the user's `dailyCalorieTarget` from the Firestore user document
  (default 2000 on first registration). `UserRepository` caches the profile
  in-memory.
* The day streak (used in the Health Score consistency component) is still a
  fixed 5 until streak tracking is added — flagged in code with a `streak
  placeholder` comment.
* `default_web_client_id` is a placeholder so the project compiles before
  Firebase is wired. Replace it as described above to enable Google Sign-In.

## Admin panel (SRS §2.3.3)

The admin panel is reached from **Profile → Manage Food Database**. The row is
hidden by default and only appears for users whose Firestore `users/{uid}`
document has `isAdmin: true`.

### Granting admin to your account

1. Sign up in the app normally
2. Open Firebase Console → Firestore Database
3. Navigate to `users` → your UID
4. Click **Add field**, name `isAdmin`, type `boolean`, value `true`
5. Sign out and back in to the app (`UserRepository` cache rebuilds at login)
6. Open Profile — "Manage Food Database" is now visible

### What admins can do

* Browse the entire food catalogue with search
* **Add** a new food (name, emoji, calories per unit, macros, unit)
* **Edit** any food by tapping it
* **Delete** a food via the trash icon (with confirmation)

Changes propagate to all users the next time their app loads the catalogue.

### Firestore security rules

The admin panel works whether or not you've set up rules, but for production
you should restrict writes to admins server-side. Paste this into
**Firebase Console → Firestore Database → Rules**:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isAuthed() { return request.auth != null; }
    function isAdmin() {
      return isAuthed() &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.isAdmin == true;
    }

    // Users: each user manages their own profile and meals
    match /users/{uid} {
      allow read: if isAuthed() && request.auth.uid == uid;
      allow write: if isAuthed() && request.auth.uid == uid;

      match /meals/{mealId} {
        allow read, write: if isAuthed() && request.auth.uid == uid;
      }
    }

    // Foods: anyone signed in can read, only admins can write
    match /foods/{foodId} {
      allow read: if isAuthed();
      allow write: if isAdmin();
    }
  }
}
```

Click **Publish**. After this, non-admin users get a permission-denied error
if they somehow try to write to `foods` directly.
