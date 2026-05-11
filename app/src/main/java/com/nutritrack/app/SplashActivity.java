package com.nutritrack.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.nutritrack.app.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean signedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
            Intent next = new Intent(this,
                    signedIn ? MainActivity.class : LoginActivity.class);
            startActivity(next);
            finish();
        }, SPLASH_DELAY_MS);
    }
}
