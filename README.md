# App Open Counter

Android Kotlin app for counting how many times apps are opened each day. V1.0 stores all data locally and does not connect to any server.

## Tech Stack

- Kotlin
- Jetpack Compose
- Room local database
- MVVM + Repository structure
- Gradle Kotlin DSL
- minSdk 26
- targetSdk 36

All usage statistics are stored locally in Room. The app does not include any remote API client or data upload path.

## Build

This repository is prepared for CI builds with GitHub Actions. The current local environment may not have Android SDK or Gradle installed, so cloud CI is the preferred build path for now.

```bash
gradle build
```

When a Gradle Wrapper is generated later in an Android-ready environment, the equivalent command will be:

```bash
./gradlew build
```
