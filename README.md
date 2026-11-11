# Kilivana Driver

This Android app supports the Kilivana delivery workflow for drivers and the inspector workflow used by field staff. It combines a driver dashboard, live route guidance, profile management, and a role-based app shell so the same codebase can serve both user types.

> Status: the app is being developed against the live Kilivana backend with a local development fallback and a Render backup. Driver-side profile features and jobs use the backend; delivery history is derived from jobs, while notifications use sample data.

## Overview

Kilivana connects farmers, buyers, and field staff. In this app:

- Drivers can sign in, view their job queue, accept deliveries, navigate to pickup and drop-off points, and review their profile and delivery activity.
- Inspectors can switch into a dedicated inspector login flow and access a separate welcome shell without entering the driver dashboard.
- The app keeps session state, role-based navigation, and settings in memory or through local persistence mechanisms while the backend is being completed.

## Features

### Driver experience

- Splash screen with branded background and app launch flow.
- Driver login flow with email address and password input.
- Dashboard with greeting, recent activity, quick actions, and notification state.
- Jobs list with Available, Accepted, and Completed views.
- Job detail flow with accept or decline options and trip lifecycle actions.
- En route experience showing device location, route guidance, pickup and delivery status, and labelled points for the driver, farmer, and buyer.
- Map tab with Mapbox Standard map tiles, current location marker, and recenter behavior.
- History, notifications, profile, and settings screens.
- Profile photo upload, crop, and delete support using UCrop and Coil.

### Inspector experience

- Separate inspector login screen and password visibility toggle.
- Forgot-password flow for the inspector account path.
- Role-based routing so inspector users never enter the driver dashboard.
- Dedicated inspector shell/welcome screen after successful authentication.

### Real and placeholder areas

| Area | Status |
| --- | --- |
| Screens and navigation | Real |
| GPS location | Real via Google Play Services Location |
| Map tiles | Real via Mapbox Standard |
| Road routes | Real via Mapbox Directions API with a fallback |
| Driver login | Backend authentication via `AuthRepository.login` |
| Inspector login | Backend authentication via `AuthRepository.login`, with an inspector role check |
| Profile read/update/image upload | Real |
| Driver photo on profile/dashboard | Real |
| Jobs and history data | Jobs loaded from the backend via `JobsRepository`; history derived from completed and cancelled jobs |
| Notifications data | Sample data in `NotificationsViewModel` |
| Dark mode | Toggle exists, theme not fully implemented |
| Language switching | UI state exists, translations not complete |
| Push notifications | Not implemented |
| Settings persistence | Partial/in-memory state |

## Tech stack

- Kotlin with Jetpack Compose and Material 3
- MVVM architecture with one `UiState` per feature
- Retrofit + OkHttp + kotlinx.serialization for API calls
- DataStore and security storage for local preferences/session handling
- Mapbox Maps and Mapbox Directions API for map tiles and routes
- Google Play Services Location for GPS tracking
- UCrop for image cropping before upload
- Coil for image loading
- Material icons extended for app icons

## Getting started

### Prerequisites

- Android Studio with the bundled JDK
- An Android emulator or a physical device
- Internet access for map tiles and route requests

### Required local config

This project reads the Mapbox token from `local.properties` in the project root:

```properties
mapbox.access.token=YOUR_MAPBOX_ACCESS_TOKEN
```

If the file does not exist, create it using the Android project root as the location.

### Run the app

From Android Studio:

1. Open the project.
2. Let Gradle sync finish.
3. Select the `app` configuration.
4. Run on an emulator or device.

From the terminal:

```bash
./gradlew assembleDebug
```

### Emulator notes

- Use a Google Play image or a device image with Google APIs enabled.
- Set the emulator location to Nairobi or Thika for realistic route behaviour.
- Route generation and maps need connectivity to the internet.

## Project structure

```text
app/src/main/java/com/example/kilivana_driver/
├── MainActivity.kt                     # Hosts the app shell, role-based navigation, and image picker
├── KilivanaApp.kt                      # App-level composition entry point
├── data/
│   ├── model/                          # Job, driver, profile, auth, and related models
│   ├── network/                        # API clients, repositories, session storage, auth interceptors
│   └── routing/RouteRepository.kt     # Route generation and Mapbox handling
├── notifications/
│   └── NotificationHelper.kt            # Notification channel setup and Android notifications
├── tracking/
├── ui/
│   ├── components/                     # Shared UI pieces and design system primitives
│   ├── theme/                          # Colors and theme definitions
│   └── screens/
│       ├── dashboard/
│       ├── history/
│       ├── inspector/
│       ├── jobs/
│       ├── login/
│       ├── main/
│       ├── map/
│       ├── notifications/
│       ├── profile/
│       ├── proof/
│       ├── settings/
│       └── splash/
└── res/
    └── drawable/                       # App illustration assets and logos
```

## Backend and network behaviour

The app is configured to talk to a Kilivana backend through a development host and failover flow:

- `BASE_URL` is the primary ngrok tunnel used during local development.
- `FALLBACK_BASE_URL` is a hosted Render deployment used as a backup.
- `LOCAL_BASE_URL` and `LAN_BASE_URL` are provided for machine-local testing.

The API layer includes:

- `ngrokHeaderInterceptor` to skip ngrok browser warnings.
- `HostFailoverInterceptor` to retry on alternate hosts when the current one fails.
- `AuthInterceptor` and `TokenAuthenticator` to attach and refresh bearer tokens centrally.
- `ProfileApi`/`ProfileRepository` for profile data and image workflows.

## Development notes

- State is managed with feature-specific `UiState` objects and `ViewModel` ownership.
- Navigation is currently handled in a lightweight app state flow rather than a full Compose navigation graph.
- The app uses a green status-bar scrim in several screens to match the design system.
- Notifications use sample data pending backend integration.

## Roadmap

- [ ] Delivery confirmation workflow after marking trips as delivered
- [x] Backend authentication and encrypted storage for remembered tokens
- [ ] Connect notifications to the backend API
- [ ] Push notifications via Firebase Cloud Messaging
- [ ] Background location while a trip is active
- [ ] Deep-link notifications to related jobs
- [ ] Full dark theme support
- [ ] Kiswahili localization
- [ ] Persistent settings via DataStore
- [ ] Replace the `com.example` package name and applicationId
- [ ] Release signing and production ProGuard configuration

## Design

The UI follows the Kilivana product design direction. Some areas such as settings and notifications were designed directly in-app where no Figma source existed.
