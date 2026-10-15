# Contributing to Shrinko

Thank you for your interest in contributing to **Shrinko - Image Compressor & Optimizer**! We love community contributions and want to make the onboarding process as transparent, smooth, and friendly as possible.

Shrinko is an actively maintained open-source project by **[Cheatpot](https://github.com/AbhinavMalviya58)** (Lead Maintainer: [Abhinav Malviya](https://github.com/AbhinavMalviya58)). Because this project has a live production app on the **Google Play Store**, we follow strict code quality and branching standards to protect production stability.

---

## 🛡️ Critical Rule: Dual-Branch Strategy (GitFlow)

> ⚠️ **IMPORTANT**: **Never open a Pull Request directly against the `main` branch!**

* **`main`**: The production release branch. Code here is deployed directly to end-users on the Google Play Store. It is protected and updated only by maintainers when releasing new versions.
* **`develop`**: The primary open-source development branch. **All PRs must be branched from and targeted against `develop`.**

```
feature branch (feat/...) ──► PR into develop ──► Tested & Audited ──► Release PR into main
```

---

## 🚀 How to Contribute

### 1. Find or Propose an Issue
* Browse existing [GitHub Issues](https://github.com/AbhinavMalviya58/Shrinko-Image-Compressor/issues) or check our [Good First Issues](.github/GOOD_FIRST_ISSUES.md).
* If you want to propose a new feature or fix a bug, please open an Issue first using our Issue Templates so we can discuss the design before you write code.

### 2. Fork and Create a Feature Branch
Always base your feature branch on `develop`:
```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/Shrinko-Image-Compressor.git
cd Shrinko-Image-Compressor

# Ensure you have the latest develop branch
git checkout develop
git pull origin develop

# Create your feature branch
git checkout -b feat/your-feature-name
```

### Branch Naming Conventions:
* `feat/short-description` (e.g., `feat/avif-support`)
* `fix/short-description` (e.g., `fix/exif-orientation-rotation`)
* `refactor/short-description` (e.g., `refactor/history-viewmodel`)
* `docs/short-description` (e.g., `docs/update-setup-guide`)

---

## 💻 Development & Code Standards

### 1. Kotlin & Jetpack Compose Best Practices
* Follow official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide).
* Keep Composable functions focused and decoupled.
* Preserve Unidirectional Data Flow (UDF) — Composables receive state and emit events to ViewModels.
* Heavy image processing operations must run on background dispatchers (`Dispatchers.Default` or `Dispatchers.IO`), never on the Main thread.

### 2. Commit Message Guidelines
We follow [Conventional Commits](https://www.conventionalcommits.org/):
* `feat: add AVIF format encoding support`
* `fix: prevent OutOfMemoryError on images larger than 48MP`
* `docs: clarify local build instructions in README`
* `refactor: extract bitmap compression logic into dedicated usecase`
* `test: add unit tests for compression ratio calculator`

### 3. Local Verification Before Pushing
Always verify that the app compiles and unit tests pass locally before pushing:
```bash
# On Linux / macOS
./gradlew lintDebug testDebugUnitTest assembleDebug

# On Windows (PowerShell)
.\gradlew.bat lintDebug testDebugUnitTest assembleDebug
```

---

## 📬 Submitting a Pull Request (PR)

1. Push your branch to your GitHub fork:
   ```bash
   git push origin feat/your-feature-name
   ```
2. Navigate to [Shrinko Pull Requests](https://github.com/AbhinavMalviya58/Shrinko-Image-Compressor/pulls).
3. Click **New Pull Request**.
4. Set the **base branch to `develop`** (NOT `main`).
5. Fill out the provided **Pull Request Template**:
   * Explain what problem this PR solves.
   * Provide before/after screenshots or screen recordings for any UI changes.
   * Confirm that your code has been tested on a device or emulator.
6. Submit the PR! Our automated CI will run checks, and maintainers will review your submission promptly.

---

## 💬 Community & Questions

Need help or want to bounce off an idea?
* Open a [Discussion](https://github.com/AbhinavMalviya58/Shrinko-Image-Compressor/discussions) or create an Issue.
* Connect with the maintainer: [Abhinav Malviya](https://github.com/AbhinavMalviya58).

Thank you for helping make Shrinko the best offline image optimizer on Android!
