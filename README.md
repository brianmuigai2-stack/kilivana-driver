# Kilivana Driver 🚚

The Android app for Kilivana's delivery drivers: the people who carry fresh produce and farm inputs from farmers to buyers across Kenya. A driver logs in, picks up jobs, follows real road routes from where they are to the farmer and on to the buyer, and keeps track of their deliveries and notifications.

> **Status:** UI-complete prototype running on sample data. There is no backend yet. See [What's real and what's a placeholder](#whats-real-and-whats-a-placeholder).

## Features

- **Splash screen** with an animated background (slow photo zoom, a sweep of sunlight and floating pollen).
- **Login** with phone number and password. There is no sign-up: driver accounts are created by a Kilivana admin, so the screen points new or locked-out drivers to their admin.
- **Home** with a greeting header, today's schedule, quick actions and recent activity. The bell shows a red dot while notifications are unread.
- **Jobs** with Available / Accepted / Completed tabs and a card per job (route, times, cargo, payout).
- **Job details** with Accept or Decline (with confirmation), Start Trip for accepted jobs, and a share button.
- **En Route** with the live GPS position, real road routes from the driver to the farmer and from the farmer to the buyer, clearly labelled **You / Farmer / Buyer** pins, and a Picked Up → In Transit → Delivered stepper.
- **Map** tab with a live OpenStreetMap map, a "you are here" dot, a recenter button, and handling for denied permission and GPS switched off.
- **Delivery history** with All / Completed / Cancelled filters.
- **Notifications** inbox with unread highlighting, filters and "Mark all read".
- **Profile** with rating, vehicle and payment details, and log out.
- **Settings** with notification toggles, language picker, dark mode toggle and app version.

### Navigation

```
Splash → Login → Main (Home · Jobs · Map · More)

Jobs → Job Details → Accept Job → En Route → Delivered → Completed
Home → History
Home → Notifications (bell)
More (Profile) → Settings
```

## What's real and what's a placeholder

| Area | Status |
|---|---|
| Screens and navigation | Real |
| GPS location | Real (Fused Location Provider) |
| Map tiles | Real (OpenStreetMap) |
| Road routes | Real (OSRM public demo server), with a straight-line fallback and an on-screen note if it fails |
| Login | **Placeholder.** Any phone with 9+ digits and a password of 4+ characters gets in |
| Jobs, history, notifications, profile | **Sample data** inside the ViewModels, marked with `TODO` |
| Dark mode | Toggle is stored, but there is no dark theme yet |
| Language | Picker is stored, but the app isn't translated yet |
| Push notifications | Not implemented |
| Settings persistence | In memory only, so it resets when the app restarts |

## Tech stack

- **Kotlin** and **Jetpack Compose** with **Material 3**
- **MVVM**: a `ViewModel` per feature exposing a `StateFlow` of one `UiState`
- **osmdroid** (OpenStreetMap) for maps
- **Google Play Services Location** for GPS
- **OSRM** public API for road routes (plain `HttpURLConnection`, no extra networking library)
- **material-icons-extended** for icons

## Getting started

**You need:** a recent Android Studio (with its bundled JDK) and a device or emulator. The min and target SDK are set in `app/build.gradle.kts`.

1. Open the project in Android Studio and let Gradle sync.
2. Run the `app` configuration on a device or emulator.
3. On the login screen, use any phone number with 9+ digits and any password with 4+ characters.

Or build from the terminal:

```bash
./gradlew assembleDebug
```

There are **no API keys** to set up.

### Testing location on an emulator

- Use a system image labelled **Google APIs** or **Play Store**.
- Open the emulator's **Extended controls** (`...`), go to **Location**, and set a point near Nairobi or Thika. The default position is in California, where the job routes can't be drawn.
- Routes and map tiles need internet access.

## Project structure

```
app/src/main/java/com/example/kilivana_driver/
├── MainActivity.kt              # Creates the ViewModels, handles status bar style
├── data/
│   ├── model/                   # Job, Driver, DeliveryRecord, AppNotification
│   └── routing/RouteRepository.kt   # Fetches road routes from OSRM
└── ui/
    ├── components/              # KilivanaButton, KilivanaTextField,
    │                            # KilivanaBottomBar, KilivanaStatusBarScrim
    ├── theme/                   # Color.kt, Theme.kt
    └── screens/
        ├── splash/
        ├── login/               # LoginScreen, LoginViewModel
        ├── main/                # MainScreen: tabs + navigation between screens
        ├── dashboard/           # Home
        ├── jobs/                # JobsScreen, JobDetailsScreen, JobRouteScreen
        ├── map/                 # MapScreen, UserLocation helpers
        ├── history/
        ├── notifications/
        ├── profile/
        └── settings/
app/src/main/res/drawable/       # kilivana_logo.jpg, splashscreen.jpg
```

## Conventions

- **State:** each screen gets one `UiState`. ViewModels own the state and screens just render it.
- **Navigation:** for now it's plain state in `MainScreen.kt` (no Navigation library). System back is handled with `BackHandler`.
- **Status bar:** every screen except the map screens starts with `KilivanaStatusBarScrim()`, a green strip behind the status bar. `MainScreen` tells `MainActivity` when to switch the status bar icons between white and dark.
- **Colors** come from `ui/theme/Color.kt`. Avoid hardcoding hex values in screens.
- **Sample data** lives in the ViewModels and is marked `TODO`, so it's easy to find and replace with API calls.

## Maps and routing: read before release

Both services used here are free public servers meant for development and light use:

- **OpenStreetMap tiles** require an identifying User-Agent. The value is set in `MapScreen.kt` and `JobRouteScreen.kt` (`OSM_USER_AGENT`) and in `RouteRepository.kt` (`USER_AGENT`). Keep the three in sync, and add a contact address or website before shipping.
- **The OSRM demo server** (`router.project-osrm.org`) isn't meant for production traffic.

For a real launch, point the map at a paid tile provider (or Google Maps) and the routing at a paid or self-hosted service. The routing base URL is a single constant in `RouteRepository.kt`.

## Roadmap

- [ ] Delivery Confirmation screen after "Mark as Delivered"
- [ ] Real authentication with secure token storage
- [ ] Connect jobs, history, profile and notifications to the backend API
- [ ] Push notifications (Firebase Cloud Messaging)
- [ ] Background location while on a trip (foreground service)
- [ ] Tapping a notification opens the related job
- [ ] Dark theme
- [ ] Kiswahili translation
- [ ] Persist settings (DataStore)
- [ ] Driver photo loading (Coil)
- [ ] Replace the `com.example` package name and applicationId
- [ ] Release signing and ProGuard rules

## Design

The screens follow the Figma design, with the exception of **Settings** and **Notifications**, which had no Figma frames and were designed in the app's own style.
