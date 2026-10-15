# 🌟 Starter "Good First Issues" for Contributors

Looking to make your first contribution to **Shrinko**? Here are 4 curated, well-scoped features and enhancements that are perfect for getting started.

If you want to claim one of these, please comment on the respective issue (or create one using our [Feature Request Template](ISSUE_TEMPLATE/feature_request.md)) so we can assign it to you!

---

### Issue 1: 🎨 App Theme Quick Toggle (System / Light / Dark)
* **Labels:** `good first issue`, `enhancement`, `ui/ux`
* **Difficulty:** Easy
* **Context:**
  Currently, Shrinko follows the Android system theme. Adding an explicit in-app theme selection setting gives users control over their visual preference.
* **Acceptance Criteria:**
  1. Add a "Theme" preference selector in `SettingsScreen` with options:
     - 🌗 System Default (default)
     - ☀️ Always Light
     - 🌙 Always Dark
  2. Persist the user's choice in Jetpack Preferences `DataStore`.
  3. Ensure the theme switches dynamically across all screens without requiring an app restart.
* **Key Codebase Files:**
  - `presentation/settings/SettingsScreen.kt`
  - `presentation/theme/Theme.kt`
  - `data/repository/` (Preferences DataStore)

---

### Issue 2: 🖼️ Interactive Touch-Drag Split Slider for Before/After Comparison
* **Labels:** `good first issue`, `enhancement`, `compose`
* **Difficulty:** Medium
* **Context:**
  When comparing the original image with the compressed result, users love being able to slide a divider back and forth over the same image area to inspect visual artifacts and quality differences.
* **Acceptance Criteria:**
  1. Create a reusable Composable `ComparisonSlider` that overlays both images and clips the top image based on a draggable divider line.
  2. Add smooth touch gesture dragging with a custom draggable handle.
  3. Support pinch-to-zoom synchronously on both images.
* **Key Codebase Files:**
  - `presentation/comparison/ComparisonScreen.kt`
  - `presentation/components/`

---

### Issue 3: 🚀 Support Modern AVIF Image Format (Android 12+)
* **Labels:** `good first issue`, `enhancement`, `core-engine`
* **Difficulty:** Medium
* **Context:**
  AVIF offers superior compression efficiency over JPEG and even WebP at equivalent visual fidelity. Android 12 (API 31)+ provides native AVIF encoding.
* **Acceptance Criteria:**
  1. Add `AVIF` to `OutputFormat` in `domain/model/ImageModels.kt` (guarded for API 31+).
  2. Update the compression engine in `data/` to encode bitmaps using `Bitmap.CompressFormat.AVIF` where supported.
  3. In the UI dropdown, show AVIF with a subtle badge or disable it gracefully on Android versions < 12 with a tooltip.
* **Key Codebase Files:**
  - `domain/model/ImageModels.kt`
  - `data/repository/ImageRepositoryImpl.kt`
  - `presentation/editor/EditorScreen.kt`

---

### Issue 4: 🔔 Batch Compression Status Notification
* **Labels:** `good first issue`, `enhancement`, `notifications`
* **Difficulty:** Medium
* **Context:**
  When users batch-compress 20 or 50 high-resolution photos, it can take 10–20 seconds. If they navigate away from the app, they should see a progress notification.
* **Acceptance Criteria:**
  1. Display a status bar notification with a progress indicator (`X of Y images compressed`).
  2. Upon completion, show a summary notification: `"Shrinko compressed 15 images and saved 42.5 MB!"` with a tap action to open the app's History screen.
  3. Respect `POST_NOTIFICATIONS` runtime permission on Android 13+.
* **Key Codebase Files:**
  - `presentation/batch/BatchViewModel.kt`
  - `AndroidManifest.xml`
