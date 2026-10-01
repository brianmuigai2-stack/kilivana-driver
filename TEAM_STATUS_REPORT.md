# Kilivana Driver Android App: Team Status Report

**Assessment date:** 1 October 2026  
**Overall status:** UI-complete prototype with selected backend integrations; not production-ready.

## Executive Summary

The app has a substantial Kotlin/Jetpack Compose interface and working code paths for location, maps, route lookup, login/session handling, and driver-profile operations. Several core delivery workflows are still demo-only: jobs, delivery history, and in-app notifications use sample data, and accepting/completing a job does not update a backend. A backend health-check action exists, but this review did not confirm that the live backend is reachable. The current automated tests do not exercise these feature flows.

## Libraries and Platform

| Library / platform | Declared version | Purpose and observed use |
|---|---:|---|
| Kotlin / Android Gradle Plugin | Kotlin 2.0.21 / AGP 8.13.2 | Android app build and Kotlin implementation. |
| Jetpack Compose, Material 3, Compose BOM | BOM 2024.09.00 | Main UI, theme, screen composition, and Material components. |
| AndroidX Core KTX, Lifecycle Runtime, Activity Compose | Core KTX declared as 1.10.1 and 1.13.1; Lifecycle 2.6.1; Activity Compose 1.8.0 | Android/Kotlin helpers, ViewModel lifecycle flows, Compose activity integration. Core KTX is declared twice at different versions and should be consolidated. |
| Google Play Services Location | 21.3.0 | Fused location provider for device GPS. |
| osmdroid | 6.1.18 | OpenStreetMap tiles, map views, markers, and route display. |
| Retrofit, Kotlin Serialization converter, OkHttp logging interceptor, kotlinx.serialization JSON | Retrofit 2.11.0; converter 1.0.0; OkHttp 4.12.0; serialization 1.7.3 | REST API client, JSON serialization, HTTP interception/logging. |
| Coil Compose | 2.7.0 | Loads profile images in profile views. Other image slots still use placeholders. |
| Preferences DataStore and AndroidX Security Crypto | DataStore 1.1.1; Security Crypto 1.1.0-alpha06 | Persists remembered-session metadata and encrypts stored access/refresh tokens. Security Crypto is an alpha dependency. |
| Material Icons Extended | Catalog 1.6.8 plus a second versionless declaration | Compose icons. The same artifact appears twice in Gradle declarations; consolidate it. |
| JUnit, AndroidX Test JUnit, Espresso, Compose UI test | JUnit 4.13.2; AndroidX JUnit 1.1.5; Espresso 3.5.1; Compose BOM-managed | Test dependencies are present, but the current test files are generated examples rather than feature coverage. |

**Android configuration:** min SDK 24, target/compile SDK 36, Java/Kotlin target 11. Release minification is disabled.

## Implemented and Wired

- **UI and navigation:** Splash/login/main screens and feature screens are implemented in Compose; navigation is managed by screen state in the app rather than a Navigation library.
- **Location and map:** Location permission declarations, fused location updates, OpenStreetMap map views, markers, and recentering logic are present.
- **Road routing:** OSRM route requests are implemented with a straight-line fallback and failure logging. The public demo endpoint is for development/light use, not production traffic.
- **Authentication:** Login calls the backend, stores an in-memory session, attaches bearer tokens, and includes refresh-token handling. “Remember me” stores session metadata in DataStore and tokens through Android Keystore-backed encrypted preferences.
- **Driver profile:** Backend calls are wired for loading profile/images and uploading/deleting profile images. Coil renders profile images.
- **Local notification test:** Android notification channels and a test-notification action exist; the action respects the in-app push/sound toggles and OS permission.
- **Settings theme toggle:** The dark-mode setting is wired into the Compose theme during the current process.

These are source-level implementation findings, not confirmation from a device run or successful live API session.

## Incomplete or Not Yet Working as Production Features

- **Jobs and delivery state:** Jobs are hard-coded sample records. Accept and complete actions only mutate local ViewModel state; there are TODOs for backend calls. Decline is also not sent to the backend, and sample delivery OTPs are embedded in the app.
- **Delivery history:** Records are loaded from sample data, not an API.
- **Notifications:** Inbox items are sample data and read state is in memory. Firebase Cloud Messaging or another push-delivery service is not integrated. The current test notification is local only.
- **Settings persistence and localization:** Notification, sound, dark-mode, and language choices reset when the app process restarts. Choosing Kiswahili does not translate the app. DataStore is currently used for remembered authentication, not these settings.
- **Profile completeness:** Some displayed statistics remain placeholders (rating and completed-delivery count are set to zero). The driver model currently derives its `paymentMethod` display value from `availabilityStatus`, which should be verified/corrected.
- **Support/account actions:** Password reset, contact support, bank details, help, legal/about links, and several profile actions are TODOs.
- **Backend/service availability:** The API base URL is a hard-coded free ngrok tunnel and can change or go offline. The OSRM demo service and public OSM tile servers have availability, usage-policy, and production-scale limitations.
- **Release readiness:** The application ID is still `com.example.kilivana_driver`; release signing is not configured in the reviewed Gradle file; minification is disabled. Debug HTTP body logging includes request headers, so bearer tokens may appear in development Logcat. Redact `Authorization` before sharing debug logs and confirm logging is absent from release builds.

## Verification and Confidence

- **Automated tests:** Only the default arithmetic unit test and package-name instrumentation test are present. They do not validate login, token refresh, API parsing, GPS, routes, jobs, or notifications.
- **Build/test result:** A Gradle unit-test run was attempted in this workspace, but the terminal did not provide a definitive final pass/fail marker. Treat build and test status as **unverified** until `./gradlew testDebugUnitTest assembleDebug` completes successfully.
- **Runtime behavior:** No emulator/device run or live backend health-check result was captured during this review. GPS permissions, map tile loading, OSRM responses, login, profile upload, and Android notification behavior still need hands-on validation.

## Recommended Team Priorities

1. Confirm a stable staging API URL, then validate authentication and profile calls on a device; add tests for error parsing, session restore, and token refresh.
2. Connect jobs, accept/decline/complete actions, history, and notifications to backend endpoints; remove hard-coded OTPs and sample production-facing records.
3. Persist settings, implement localization or remove the non-functional language option, and define actual push-notification delivery.
4. Replace public demo routing/tile dependencies with production-appropriate services and review their usage policies.
5. Resolve duplicate Gradle dependency declarations, review the alpha security dependency and debug log redaction, then configure package ID, release signing, and release build checks.

## Evidence Reviewed

Gradle dependency declarations and version catalog; app manifest; README; authentication, session, profile, routing, location, notification, settings, jobs, and navigation source; and the existing local/instrumented test files.