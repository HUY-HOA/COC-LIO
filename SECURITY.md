# Security policy

## Reporting a vulnerability

Do not report credentials, exploit details, personal data, or unpublished vulnerability details in public Issues or Discussions.

Use GitHub's private vulnerability reporting for this repository:
1. Open the repository's Security tab.
2. Select Report a vulnerability.
3. Include the affected version, reproduction steps, impact, and a safe proof of concept.

If private vulnerability reporting is unavailable, contact the maintainer through the support address on the official COC-LIO website. Do not attach passwords, signing keys, OAuth secrets, Firebase service-account files, or user data.

## Supported code

Security fixes are applied to the main branch. Releases should use the latest reviewed source and a newly validated signed APK.

## Current Firebase deployment boundary

- `app/google-services.json` is Firebase client configuration. Its project identifiers and Firebase client API key are public identifiers, not server credentials. Do not replace it with a service-account JSON file. Authorization remains the responsibility of Firebase Authentication, Firestore Rules and App Check.
- Play Integrity App Check initialization is present in the Android app. Do not enable Firebase App Check enforcement until the actual stable Release signing certificate is registered and legitimate sideloaded devices appear in App Check metrics.
- The current project is kept on Firebase Spark. Firestore Rules are deployed by the repository workflow. Cloud Storage and Cloud Functions are not used by the current app and are not deployed on Spark; `storage.rules` is retained only as a restrictive future reference and is not active.
- The Firebase API key's API allowlist and any application restrictions must be checked in Google Cloud Console. Do not delete or replace the key without verifying the Android app configuration and required Firebase API allowlist.

## Scope and limits

- The Android client is untrusted. Authorization must be enforced by Firebase Authentication, Firestore/Storage Security Rules, App Check, and server-side code.
- Firebase client configuration and Firebase API keys are not server credentials. Never put service-account keys, private signing keys, OAuth client secrets, access tokens, or passwords in the repository or APK.
- Public source code and Git history can be copied. Do not store confidential source, production data, user exports, or operational secrets in this public repository.
- A security fix is not considered deployed until its corresponding Firebase rules, Functions, or client release have been deployed and verified.
