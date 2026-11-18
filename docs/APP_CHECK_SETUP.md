# COC-LIO — Firebase App Check for sideloaded APKs

The Android client initializes Firebase App Check with the Play Integrity provider in CocLioApplication. App Check tokens are sent by supported Firebase SDKs, but Firebase services do not reject requests until enforcement is enabled in Firebase Console.

## Required Firebase setup

1. In the Google Play Console, open the app's App integrity section and link the Play Integrity API to the same Google Cloud/Firebase project used by COC-LIO.
2. In Firebase Console, open Security → App Check → Apps and register the Android app com.huyhoa.coclio with the Play Integrity provider.
3. Register the SHA-256 fingerprint of the actual Release signing certificate. Use the same stable certificate used to sign APKs distributed to users.
4. Because COC-LIO distributes APKs outside Google Play, configure the advanced Play Integrity settings for external distribution:
   - Do not require PLAY_RECOGNIZED.
   - Do not require LICENSED.
   - Require the MEETS_DEVICE_INTEGRITY device verdict according to the intended security policy.
5. Distribute an APK containing the App Check initialization. Review App Check metrics for Authentication, Firestore, Storage and Functions before enforcement.
6. Enable enforcement for each Firebase product only after legitimate sideloaded installs produce valid App Check traffic. If valid users are missing, do not enable enforcement yet.

## Important operating notes

- Play Integrity supports apps distributed through Google Play and outside Google Play. The external-distribution settings must be chosen deliberately; the default Play-recognized requirement can reject sideloaded users.
- The emulator/debug environment may not produce a valid production attestation. Keep enforcement disabled until the Release APK has been verified on real supported devices.
- App Check is an additional abuse-prevention layer, not a replacement for Authentication or Security Rules.
- Never commit App Check debug tokens, service-account credentials, or signing material.
- Firebase Console and Play Console setup cannot be completed by changing this repository alone.
