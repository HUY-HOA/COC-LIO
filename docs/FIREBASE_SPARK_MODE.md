# Firebase Spark mode for COC-LIO

COC-LIO is configured to run without Firebase Blaze billing.

## What remains available

- Google sign-in through Firebase Authentication.
- User profiles and consent choices in Cloud Firestore.
- Community text messages.
- Community JSON sharing and receiving through Cloud Firestore.
- The owner account list, profile details, and consent-based activity/device fields written directly by the app.
- Local village data and other on-device features.

Firestore activity/device fields are client-reported. They are not server-verified audit records. IP addresses are not collected in Spark mode. Activity reporting runs only after the user has opted in.

## Features paused on Spark

- Cloud Functions are not deployed or called by the Android app.
- Automatic push notifications for community messages and app releases are paused. Firebase Cloud Messaging itself remains in the project, but the server-side notification triggers are not available.
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
