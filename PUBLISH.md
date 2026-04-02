# Build & Publish Guide

This library is distributed via **JitPack**. Publishing a new version means:
1. Bumping the version in the relevant files
2. Committing and pushing to the default branch
3. Creating and pushing a Git tag — this triggers GitHub Actions, which builds the AAR and creates a GitHub Release; JitPack then picks up the tag automatically

---

## Step 1 — Bump the version

Update the version string in these three places:

| File | Field | Example |
|---|---|---|
| `photoeditor/build.gradle` | `PUBLISH_VERSION` | `'3.1.8'` |
| `app/build.gradle` | `versionName` | `"3.1.8"` |
| `app/build.gradle` | `versionCode` | increment by 1 |
| `README.md` | JitPack dependency line | `v3.1.8` |

---

## Step 2 — Commit and push

```bash
git add photoeditor/build.gradle app/build.gradle README.md
git commit -m "chore: bump version to 3.1.8"
git push origin <your-branch>
```

Make sure you push to the branch that is set as default on GitHub (currently `default-config`), otherwise JitPack will not resolve the correct code.

---

## Step 3 — Create and push the tag

```bash
git tag v3.1.8
git push origin v3.1.8
```

This triggers the GitHub Actions workflow (`.github/workflows/release.yml`), which:
1. Builds `photoeditor-release.aar`
2. Creates a GitHub Release named `v3.1.8`
3. Attaches the AAR as a downloadable release asset
4. Auto-generates release notes from commits

---

## Step 4 — Verify on JitPack

JitPack builds on first request after the tag is published. Trigger and monitor the build at:

```
https://jitpack.io/#virtualspirit/AndroidPhotoEditor/v3.1.8
```

Once the build turns green, the dependency is available:

```gradle
implementation 'com.github.virtualspirit:AndroidPhotoEditor:v3.1.8'
```

---

## Building locally

To build the release AAR without publishing:

```bash
./gradlew :photoeditor:assembleRelease
```

Output: `photoeditor/build/outputs/aar/photoeditor-release.aar`

To test the AAR in another local project, publish to your local Maven cache:

```bash
./gradlew :photoeditor:publishToMavenLocal
```

Then in the consuming project:

```gradle
repositories { mavenLocal() }
dependencies {
    implementation 'com.virtualspirit:photoeditor:3.1.8'
}
```

---

## Requirements

- **Java 17** — required by the build. Verify with `java -version`.
- **Android SDK** — set via `ANDROID_HOME` or `local.properties`.

---

## Troubleshooting

**GitHub Actions did not trigger**
- The workflow only fires on a tag push matching `v*.*.*`. Make sure you pushed the tag with `git push origin vX.Y.Z`, not just the branch.

**JitPack build fails**
- Check the build log at `https://jitpack.io/#virtualspirit/AndroidPhotoEditor/vX.Y.Z`.
- Ensure the tag exists on GitHub and points to a commit that compiles cleanly.
- Confirm that `PUBLISH_VERSION` in `photoeditor/build.gradle` matches the tag.

**Old version still resolving**
- Gradle caches dependencies aggressively. Run `./gradlew --refresh-dependencies` in the consuming project.
