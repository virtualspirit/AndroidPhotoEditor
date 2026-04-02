# Deployment & Release Guide — AndroidPhotoEditor

## Overview

AndroidPhotoEditor is distributed via **JitPack**. There is no manual publish step — JitPack builds and serves the library automatically when a Git tag is pushed to GitHub.

```
developer pushes tag → GitHub Actions builds AAR → GitHub Release created → JitPack serves the tag
```

---

## Branching

| Branch | Purpose |
|---|---|
| `default-config` | Main development branch — PRs merge here |
| `master` | Legacy / stable |

All version tags should point to commits on `default-config`.

---

## Versioning

This project uses **Semantic Versioning** (`MAJOR.MINOR.PATCH`).

| Change type | Version bump | Example |
|---|---|---|
| Bug fix | PATCH | `3.1.7` → `3.1.8` |
| New feature, backward-compatible | MINOR | `3.1.8` → `3.2.0` |
| Breaking change | MAJOR | `3.x.x` → `4.0.0` |

---

## Release Steps

### 1. Update version fields

| File | Field |
|---|---|
| `photoeditor/build.gradle` | `PUBLISH_VERSION` |
| `app/build.gradle` | `versionName` |
| `app/build.gradle` | `versionCode` (increment by 1) |
| `README.md` | JitPack dependency line |

### 2. Commit and push

```bash
git add photoeditor/build.gradle app/build.gradle README.md
git commit -m "chore: bump version to X.Y.Z"
git push origin default-config
```

### 3. Create and push the tag

```bash
git tag vX.Y.Z
git push origin vX.Y.Z
```

This triggers the GitHub Actions workflow automatically.

---

## GitHub Actions Workflow

**File**: `.github/workflows/release.yml`
**Trigger**: push of a tag matching `v*.*.*`

### What it does

1. Checks out the code
2. Sets up Java 17
3. Caches Gradle dependencies
4. Runs `./gradlew :photoeditor:assembleRelease`
5. Creates a GitHub Release named `vX.Y.Z`
6. Attaches `photoeditor-release.aar` as a release asset
7. Auto-generates release notes from commits

### Workflow file summary

```
on:
  push:
    tags:
      - 'v*.*.*'
```

---

## JitPack

JitPack builds on the **first request** after a tag is published. To trigger and monitor the build:

```
https://jitpack.io/#virtualspirit/AndroidPhotoEditor/vX.Y.Z
```

Once the build is green, the dependency is live:

```gradle
implementation 'com.github.virtualspirit:AndroidPhotoEditor:vX.Y.Z'
```

---

## Building Locally

```bash
# Build release AAR
./gradlew :photoeditor:assembleRelease
# Output: photoeditor/build/outputs/aar/photoeditor-release.aar

# Publish to local Maven cache for testing
./gradlew :photoeditor:publishToMavenLocal
```

To consume from local Maven in another project:

```gradle
repositories { mavenLocal() }
dependencies {
    implementation 'com.virtualspirit:photoeditor:X.Y.Z'
}
```

---

## Requirements

- **Java 17** — verify with `java -version`
- **Android SDK** — set via `ANDROID_HOME` or `local.properties`

---

## Troubleshooting

**Workflow did not trigger**
Tag was not pushed. Run `git push origin vX.Y.Z`.

**JitPack build fails**
Check `https://jitpack.io/#virtualspirit/AndroidPhotoEditor/vX.Y.Z` for the build log.
Confirm `PUBLISH_VERSION` in `photoeditor/build.gradle` matches the tag name.

**Gradle still resolves old version**
Run `./gradlew --refresh-dependencies` in the consuming project.
