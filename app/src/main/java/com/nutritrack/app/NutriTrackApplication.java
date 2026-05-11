package com.nutritrack.app;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class NutriTrackApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
