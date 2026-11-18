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

## Scope and limits

- The Android client is untrusted. Authorization must be enforced by Firebase Authentication, Firestore/Storage Security Rules, App Check, and server-side code.
- Firebase client configuration and Firebase API keys are not server credentials. Never put service-account keys, private signing keys, OAuth client secrets, access tokens, or passwords in the repository or APK.
- Public source code and Git history can be copied. Do not store confidential source, production data, user exports, or operational secrets in this public repository.
- A security fix is not considered deployed until its corresponding Firebase rules, Functions, or client release have been deployed and verified.
