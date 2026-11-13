# Firebase Spark mode for COC-LIO

COC-LIO is configured to run without Firebase Blaze billing.

## What remains available

- Google sign-in through Firebase Authentication.
- User profiles and consent choices in Cloud Firestore.
- Community text messages.
- Community JSON sharing and receiving through Cloud Firestore.
- The owner account list, profile details, and consent-based activity/device fields written directly by the app.
- Local village data and other on-device features.

Firestore activity/device fields are client-reported. They are not server-verified audit records. The app does not collect IP addresses. When the owner opens the account list, any legacy `lastIp` and `lastIpCapturedAt` fields still present in user profiles are deleted. Activity reporting runs only after the user has opted in. The current Android manifest requests Internet, network-state, and notification permissions only; it does not request location, contacts, call logs, SMS, or phone-state access.

## Features paused on Spark

- Cloud Functions are not deployed or called by the Android app.
- Server-pushed notifications through Firebase Cloud Messaging remain paused because Cloud Functions are not deployed and FCM auto-initialization is disabled. When users enable the community or app-update switches in COC-LIO settings, the Android app checks the signed-in user's readable Firestore collections with WorkManager in the background. The scheduled interval is 15 minutes, but Android battery optimization, Doze, connectivity, or manufacturer background limits can delay checks. This is periodic polling, not instant push delivery, and it does not register or store FCM tokens.
- Community message notifications use Android's conversation-bubble API on Android 11 and later. When the OS and user allow bubbles, a new message can open a floating, expandable community chat with live Firestore updates and text replies. Users must allow notification bubbles for COC-LIO or promote a community notification to a bubble in Android's notification UI. On Spark, the incoming message check still uses the 15-minute WorkManager polling schedule, so bubble delivery is not instant. Devices that do not support or allow notification bubbles continue to show ordinary notifications.
- Uploading or downloading community images, attachments, and APK releases through Firebase Storage is paused.
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

Spark has service-specific free quotas. When a quota is exhausted, the affected service may stop accepting requests until its quota resets or the project is upgraded. Spark does not automatically switch the project to paid billing. Monitor Firestore reads, writes, stored data, and network usage to stay within the free limits.


## Expanded opt-in collection (consent version 6)

The Android app only sends optional device/activity telemetry when the user accepts the corresponding choice. The fields are device manufacturer/model/brand/product/codename, up to four supported CPU ABIs, Android version/API level, app version, locale, time zone, screen pixel dimensions/density, broad network transport type, app-open count, timestamps, and foreground duration. Network data is limited to the transport category; no IP or hardware identifier is added.

A separate "Đồng bộ dữ liệu Clash of Clans lên tài khoản" choice controls cloud sync of JSON that the user explicitly imports. When enabled, COC-LIO writes the latest raw JSON snapshot per Player Tag to the user's Firestore subcollection and updates a small summary in the user profile. It does not connect to the game process or read the game account in the background.

The synchronization is subject to Spark Firestore limits: 1 GiB total stored data, 20,000 document writes/day, 50,000 document reads/day, and 10 GiB/month network egress. The app keeps only one cloud snapshot per Player Tag, but each different tag creates another document. Users should disable sync or request deletion when they no longer need the cloud copy.
