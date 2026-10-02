# Coursey Android App

**Kotlin** · Jetpack Compose (Material 3) · MVVM + Clean Architecture · Coroutines / Flow · SQLite (`SQLiteOpenHelper`) · Navigation Compose

---

## 1. Architecture

I chose Clean Architecture with MVVM to keep the application modular, testable, and maintainable. The UI, business logic, and data layers are separated, making it easier to modify or scale individual parts without affecting the rest of the application.

## 2. Offline Support

The application uses a local SQLite database (learning_dashboard.db) with courses and lessons tables and foreign keys enabled. Remote data is mapped through the data and domain layers before being stored in a single transaction. When a lesson is completed, both the lesson status and the course progress are updated atomically to keep the data consistent. The local data source exposes Flows with change notifications instead of polling, and stateIn(WhileSubscribed(5_000)) stops the query when no screen is visible. This allows the application to display cached content quickly, update automatically when data changes, and restore course progress when the app is reopened offline.

## 3. Security

Current state: none. Authentication is a mock comparing against a JSON constant, and the session sits in unencrypted SharedPreferences. This is development scaffolding.

Production design:

| Concern | Approach |
|---|---|
| Token storage | `EncryptedSharedPreferences` (AndroidX Security), keyed by a Keystore-held AES key — never plain `SharedPreferences` |
| Token lifetime | Short-lived access token in memory only; refresh token persisted Keystore-protected |
| Silent refresh | An OkHttp `Authenticator` refreshes and retries once per request |
| Transport | HTTPS only, `usesCleartextTraffic="false"`, certificate pinning on the API host |
| Protocol | OAuth 2.0 Authorization Code + PKCE, replacing the credential check |
| Logging | Auth headers and PII redacted from release logs |

## 4. Scale — 1M users, hundreds of courses

Right now the app saves every course into one local database and loads the whole catalog into memory at once. That works for three courses but breaks at thousands.

First, I'd load courses in small batches as the user scrolls, so the app only ever holds a screenful — Paging 3 does that in Android.

Second, I'd move off my hand-written SQL to Room. Right now if I rename a column and forget one query, the app still builds and crashes when someone opens that screen. Room makes that a build error, and it also handles database upgrades, which is what quietly breaks apps a year after release.

Third, I'd stop re-downloading data that hasn't changed — the server replies "nothing new" instead of resending everything, and for my own progress it only sends what changed since the last sync.

And fourth, if several screens want fresh data at once, make one request and share it. Plus error logging and crash reporting before real traffic arrives, so a break finds me instead of hiding.

## 5. Second Platform — iOS/macOS

I would implement the iOS/macOS version using **Swift and SwiftUI**. The existing **domain layer** can remain conceptually the same because it contains platform-independent entities, business rules, and use cases. The **data layer** would be adapted to iOS using technologies such as **SwiftData/Core Data** for local storage, **URLSession** for networking, and **Keychain** for securely storing authentication tokens. The **presentation layer** would be rebuilt using SwiftUI while maintaining the same screens, navigation flow, and application behaviour. Platform-specific components such as Android lifecycle handling, SQLite implementation, and Android storage would be replaced with their equivalent iOS/macOS frameworks.
