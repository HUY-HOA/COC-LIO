# COC-LIO remote configuration

This folder contains the JSON value used by the app's Firebase Remote Config parameter.

## One-time Firebase setup

1. Open the Firebase project `coc-lio-73de9`.
2. Open Remote Config.
3. Add a parameter named `coclio_app_config_json`.
4. Set its default value to the complete contents of `app.json` as a JSON string.
5. Publish the changes.

The Android app includes the same safe defaults locally. Once the first APK containing the remote configuration client is installed, it fetches the published parameter and checks for changes about every 65 seconds while the app is in the foreground.

## Updating later

Edit `app.json`, then replace the value of `coclio_app_config_json` in Firebase Remote Config with the updated JSON and publish it. No APK rebuild is needed for supported configuration changes.

Supported remote values are limited to theme colors, LED speed multiplier, and Vietnamese/English text overrides used by the app's translated text component. Values are validated and clamped in the app. This document does not provide a mechanism for downloading or executing code.

Do not place passwords, API secrets, personal data, or executable code in this configuration.
