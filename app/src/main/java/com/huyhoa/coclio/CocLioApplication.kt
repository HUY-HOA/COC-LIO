package com.huyhoa.coclio

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Initializes Firebase App Check before activities and Firebase-backed features start.
 *
 * Play Integrity supports both Play-distributed and sideloaded Android applications.
 * Enforcement must remain disabled until the release signing certificate is registered
 * in Firebase App Check and valid traffic has been checked in the metrics dashboard.
 */
class CocLioApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
    }
}
