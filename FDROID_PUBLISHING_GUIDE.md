# 📱 F-Droid & Open-Source App Publishing Guide

This guide details how to publish **OmniDesk** to **F-Droid**, **IzzyOnDroid**, and other developer-friendly open-source app stores.

---

## 🚀 Recommended Developer-Friendly Options Overview

| Platform | Review Time | Hosting | Auto-Updates | Best For |
| :--- | :--- | :--- | :--- | :--- |
| **IzzyOnDroid** *(Top Pick)* | **24–48 Hours** | Your GitHub Releases | ✅ Yes (Automatic) | Quickest & easiest F-Droid integration |
| **Official F-Droid** | 2–8 Weeks | F-Droid Server (Builds from source) | ✅ Yes | Official F-Droid directory |
| **Obtainium** | **Instant** | Your GitHub Releases | ✅ Yes | Direct GitHub-to-phone auto-updates |
| **Accrescent** | 1–3 Days | Accrescent Web Console | ✅ Yes | Modern, secure APK store |

---

## 🥇 Method 1: IzzyOnDroid (Fastest & Most Developer-Friendly F-Droid Repo)

**Why IzzyOnDroid?**
- It is the official "stepping stone" to F-Droid used by thousands of developers.
- Built directly into modern F-Droid clients (**Droid-ify**, **Neo Store**, and official **F-Droid**).
- Izzy's bot pulls your signed APK directly from your GitHub Releases tags automatically whenever you release a new version.
- Approval usually takes **less than 48 hours**.

### Step 1: Push your project to GitHub
1. Create a public repository on GitHub (e.g., `https://github.com/YOUR_USERNAME/omnidesk`).
2. Push this project code to your repository:
   ```bash
   git init
   git add .
   git commit -m "Initial commit of OmniDesk"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/omnidesk.git
   git push -u origin main
   ```

### Step 2: Create a Release Tag (v1.0.0)
Push a git tag or click **Releases -> Create a new release** on GitHub:
```bash
git tag v1.0.0
git push origin v1.0.0
```
*Our included `.github/workflows/release.yml` will automatically build and attach `OmniDesk.apk` to your GitHub Release!*

### Step 3: Submit to IzzyOnDroid
1. Go to the IzzyOnDroid Inclusion repository:
   👉 **https://gitlab.com/IzzyOnDroid/repo/-/issues/new**
2. Choose the **"App Inclusion"** issue template.
3. Fill in these details:
   - **App Name:** OmniDesk
   - **Package ID:** `com.aistudio.applet.jkysvd`
   - **Repository URL:** `https://github.com/YOUR_USERNAME/omnidesk`
   - **License:** Apache-2.0
   - **Release Asset:** `OmniDesk.apk`
4. Click **Submit Issue**. Within 1–2 days, the maintainer will verify your APK and your app will be live on F-Droid!

---

## 🥈 Method 2: Official Main F-Droid Repository (`fdroiddata`)

To be listed directly inside the default official F-Droid catalog:

### Step 1: Fork the F-Droid metadata repository
1. Sign up on **GitLab.com**.
2. Fork the repository: **https://gitlab.com/fdroid/fdroiddata**

### Step 2: Add your App Metadata File
Create a new file in `metadata/com.aistudio.applet.jkysvd.yml` with the following content:

```yaml
Categories:
  - System
  - Connectivity
License: Apache-2.0
AuthorName: OmniDesk Team
SourceCode: https://github.com/YOUR_USERNAME/omnidesk
IssueTracker: https://github.com/YOUR_USERNAME/omnidesk/issues

AutoName: OmniDesk
Summary: Universal PC remote control for touchpad, keyboard, media, and tools

Description: |-
  OmniDesk transforms your Android phone into an ultra-low-latency wireless
  remote control for your Windows, macOS, or Linux computer.

  Features:
  * Precision multi-touch touchpad with adjustable sensitivity
  * Full desktop keyboard with function keys and shortcuts
  * Volume and playback media controls
  * System telemetry (CPU, RAM, Battery) and power actions
  * Dual-stick analog gamepad
  * Slide presenter with timer
  * Local Wi-Fi discovery, QR scanner, and zero-latency USB cable support

RepoType: git
Repo: https://github.com/YOUR_USERNAME/omnidesk.git

Builds:
  - versionName: '1.0'
    versionCode: 1
    commit: v1.0.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
```

### Step 3: Open a Merge Request
1. Submit your Merge Request to `fdroid/fdroiddata`.
2. The F-Droid automated test runner (`fdroid checkupdates`) will test-build your repository from source.
3. Once reviewed by the F-Droid team, your app will be added to the official repository.

---

## 🥉 Method 3: Obtainium (Direct Open-Source Distribution)

**Obtainium** is an open-source Android app used by privacy enthusiasts to install and automatically receive updates directly from developer GitHub Releases without needing any third-party app store approval.

- Users simply enter: `https://github.com/YOUR_USERNAME/omnidesk`
- Obtainium checks for new tags, downloads the latest APK from your Releases, and notifies the user to update.

---

## 📋 Checklist for F-Droid Compliance (All Done for You!)

- [x] **Open Source License:** Added `LICENSE` (Apache 2.0).
- [x] **Fastlane Metadata:** Added title, short, and full descriptions in `/fastlane/metadata/android/en-US/`.
- [x] **Automated CI/CD Workflow:** Added `.github/workflows/release.yml` to automatically build APKs upon release tags.
- [x] **Zero Tracking / No Ads:** The app communicates solely over local Wi-Fi / USB sockets with zero tracking trackers or third-party ads.
