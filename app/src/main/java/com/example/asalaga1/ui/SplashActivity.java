package com.example.asalaga1.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.asalaga1.R;
import com.example.asalaga1.ui.admin.AdminMainActivity;
import com.example.asalaga1.ui.auth.LoginActivity;
import com.example.asalaga1.ui.customer.CustomerMainActivity;
import com.example.asalaga1.ui.owner.OwnerMainActivity;
import com.example.asalaga1.util.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved theme preference BEFORE super.onCreate
        SessionManager session = new SessionManager(this);
        if (session.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (session.isLoggedIn()) {
                String role = session.getUserRole();
                Intent intent;
                if ("ADMIN".equals(role)) {
                    intent = new Intent(SplashActivity.this, AdminMainActivity.class);
                } else if ("OWNER".equals(role)) {
                    intent = new Intent(SplashActivity.this, OwnerMainActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, CustomerMainActivity.class);
                }
                startActivity(intent);
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, 1500); // delay 1.5 seconds
    }
}

