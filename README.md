# CloudBrowser Android

A simple landscape-first Android browser using the system Android WebView.

## Build an APK
1. Open the **Actions** tab in this repository.
2. Select **Build Android APK** and press **Run workflow**, or push a commit to `main`.
3. When the workflow completes successfully, open the workflow run and download the `CloudBrowser-debug-apk` artifact.
4. Extract the ZIP and install `app-debug.apk` on your Android phone. Android may ask you to allow installation from the app used to open the APK.

## Notes
- This is a WebView-based browser, not a full Chromium replacement. Some websites may refuse to run inside embedded WebViews.
- Google may block account sign-in inside embedded browsers for security reasons. This project does not implement Google OAuth sign-in; do not enter credentials into untrusted pages.
- The build workflow creates a debug APK artifact. It is not a Play Store release build.
