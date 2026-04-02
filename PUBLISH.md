# Publishing Guide

This document explains how to publish a new version of the `photoeditor` library — both automatically via GitHub Actions and manually from your local machine.

---

## Files to update before publishing

| File | Field | Example |
|---|---|---|
| `photoeditor/build.gradle` | `PUBLISH_VERSION` | `'3.1.7'` |
| `app/build.gradle` | `versionName` | `"3.1.7"` |
| `app/build.gradle` | `versionCode` | increment by 1 |
| `README.md` | JitPack dependency line | `v3.1.7` |

---

## Option 1 — Automatic via GitHub Actions (recommended)

The workflow in `.github/workflows/release.yml` triggers automatically when a Git tag matching `v*.*.*` is pushed. It builds the release AAR and uploads it to GitHub Releases.

### Steps

```bash
# 1. Make sure all version fields above are updated and committed
git add photoeditor/build.gradle app/build.gradle README.md
git commit -m "chore: bump version to 3.1.7"

# 2. Push the commit
git push origin main

# 3. Create and push the version tag
git tag v3.1.7
git push origin v3.1.7
```

GitHub Actions will then:
1. Build `photoeditor-release.aar`
2. Create a GitHub Release named `v3.1.7`
3. Attach the AAR as a release asset
4. Auto-generate release notes from merged PRs/commits

After the release is created, JitPack will automatically pick up the new tag and make it available as:

```gradle
implementation 'com.github.virtualspirit:AndroidPhotoEditor:v3.1.7'
```

---

## Option 2 — Manual from local machine

Use this when you need to publish without pushing a tag, or when CI is unavailable.

### Prerequisites

#### 1. Java 17
The build requires Java 17. Verify with:
```bash
java -version
```
If needed, install via [Adoptium](https://adoptium.net/) or SDKMAN:
```bash
sdk install java 17-zulu
sdk use java 17-zulu
```

#### 2. GPG signing key
The library must be signed before publishing to Maven Central.

```bash
# List your keys
gpg --list-secret-keys --keyid-format SHORT

# Export the secret keyring (replace KEY_ID with your 8-char key ID)
gpg --export-secret-keys KEY_ID > ~/.gnupg/secring.gpg
```

#### 3. Sonatype (OSSRH) account
You need an account at [https://issues.sonatype.org](https://issues.sonatype.org) with write access to the `com.virtualspirit` group.

#### 4. Configure `local.properties`
Add the following to `local.properties` in the project root (this file is git-ignored):

```properties
# GPG signing
signing.keyId=XXXXXXXX          # last 8 chars of your GPG key ID
signing.password=your_gpg_passphrase
signing.secretKeyRingFile=/Users/yourname/.gnupg/secring.gpg

# Sonatype OSSRH credentials
ossrhUsername=your_sonatype_username
ossrhPassword=your_sonatype_password

# Sonatype staging profile ID
# Find it at: https://s01.oss.sonatype.org → Staging Profiles → select com.virtualspirit → copy the ID from the URL
sonatypeStagingProfileId=XXXXXXXXXXXXXXXX
```

---

### Build and publish steps

#### Step 1 — Build the release AAR
```bash
./gradlew :photoeditor:assembleRelease
```
Output: `photoeditor/build/outputs/aar/photoeditor-release.aar`

#### Step 2 — Publish to Sonatype staging repository
```bash
./gradlew :photoeditor:publishReleasePublicationToSonatypeRepository
```
This uploads the signed AAR + sources JAR to a new staging repository at `s01.oss.sonatype.org`.

#### Step 3 — Close the staging repository
```bash
./gradlew closeAndReleaseRepository
```
This closes the staging repo (runs validation checks) and releases it to Maven Central. The artifact will appear on Maven Central within 10–30 minutes.

#### Step 4 — (Optional) Publish to local Maven for testing
```bash
./gradlew :photoeditor:publishToMavenLocal
```
Then reference it in another local project:
```gradle
repositories { mavenLocal() }
dependencies {
    implementation 'com.virtualspirit:photoeditor:3.1.7'
}
```

#### Step 5 — Tag the release on Git
```bash
git tag v3.1.7
git push origin v3.1.7
```
This creates the GitHub Release entry and makes the tag available on JitPack.

---

## Verifying the release

| Channel | URL |
|---|---|
| GitHub Release | `https://github.com/virtualspirit/AndroidPhotoEditor/releases/tag/v3.1.7` |
| JitPack | `https://jitpack.io/#virtualspirit/AndroidPhotoEditor/v3.1.7` |
| Maven Central | `https://central.sonatype.com/artifact/com.virtualspirit/photoeditor/3.1.7` |

---

## Troubleshooting

**`Signature validation failed`**
- Make sure `signing.secretKeyRingFile` points to the correct `.gpg` file.
- Verify `signing.keyId` is exactly 8 characters (the short form).

**`401 Unauthorized` on Sonatype**
- Double-check `ossrhUsername` and `ossrhPassword` in `local.properties`.

**`Repository is already released`**
- The version already exists on Maven Central. You cannot overwrite a published version — bump the version number.

**JitPack build fails**
- Check the build log at `https://jitpack.io/#virtualspirit/AndroidPhotoEditor/v3.1.7`.
- Ensure the tag exists on GitHub before triggering a JitPack build.
