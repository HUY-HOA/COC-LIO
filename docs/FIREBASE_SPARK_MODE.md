# Firebase Spark mode for COC-LIO

COC-LIO is configured to run without Firebase Blaze billing.

## What remains available

- Google sign-in through Firebase Authentication.
- Minimal account profiles in Cloud Firestore, used for authentication, account maintenance and owner-managed access.
- The owner account list and account status management.
- Local village JSON parsing, history and upgrade tracking on the device.

## Data collection in the current app

The login screen contains one required confirmation for the Terms of Use and Privacy Policy. It does not offer optional data-collection switches.

The Firestore account profile contains the Google UID, email, display name and profile photo, email-verification status, provider ID, preferred display name, sign-in count and account timestamps such as created, updated and last sign-in.

Firebase Analytics, Crashlytics and Performance Monitoring are disabled. The app does not start its device/activity tracker and does not write IP addresses or device identifiers to account profiles. The manifest does not request location, contacts, call logs, SMS or phone-state permissions.

Village JSON, Player Tag, village names and progression history are processed and stored locally. The current app does not upload JSON to Cloud Firestore or sync village data to the cloud. Any legacy JSON snapshots or activity fields from older app versions are not automatically removed by this update; handle deletion separately when needed.

Firestore profile fields reported by the client are not server-verified audit records. The app's owner account list removes legacy `lastIp` and `lastIpCapturedAt` fields when those fields are encountered.

## Features paused on Spark

- Cloud Functions are not deployed or called by the Android app.
- Firebase Storage and APK distribution are not used by the current JSON-focused Android app. Existing Storage objects and legacy Firestore records are not automatically deleted by this code cleanup.
- The owner cannot delete another user's Firebase Authentication account from the Android app. The app can mark that user's COC-LIO Firestore profile as disabled. This blocks access to Firestore-protected COC-LIO features, but does not delete the Authentication account or automatically erase the user's other data.
- The owner account list is based on Firestore profile documents. Without the server-side Auth user-list sync, stale profiles can remain after an Auth account is deleted separately.

## Existing Cloud Storage data

The existing Firebase Storage bucket is not deleted by this configuration change. On the Spark plan, access to the affected Firebase Storage bucket is unavailable. Existing objects may remain stored but cannot be read, uploaded, or deleted through the app until Blaze billing is linked again. Do not assume that switching plans erased those files.

## Deploying the Firestore security rules

The Firebase CLI configuration now targets Firestore rules only. The GitHub Actions workflow **Deploy COC-LIO Firestore Rules** is manual (`workflow_dispatch`) and deploys only:

```sh
firebase deploy --only firestore:rules --project coc-lio-73de9 --non-interactive
```

The old Cloud Functions deploy workflow has been removed. The `functions/` source folder is retained in the repository for possible future use if the project is deliberately upgraded again.

## Free-plan limits

Spark has service-specific free quotas. When a quota is exhausted, the affected service may stop accepting requests until its quota resets or the project is upgraded. Spark does not automatically switch the project to paid billing. Monitor Firestore reads, writes, stored data and network usage to stay within the free limits.
