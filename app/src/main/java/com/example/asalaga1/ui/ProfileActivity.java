package com.example.asalaga1.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.User;
import com.example.asalaga1.ui.auth.LoginActivity;
import com.example.asalaga1.util.SessionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvInitial, tvName, tvEmail, tvPhone, tvRole;
    private ImageButton btnBack;
    private Button btnLogout;
    private SwitchMaterial switchDarkMode;
    private SessionManager session;
    private AppRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = new SessionManager(this);
        repository = new AppRepository(this);

        tvInitial = findViewById(R.id.tv_profile_initial);
        tvName = findViewById(R.id.tv_profile_name);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvPhone = findViewById(R.id.tv_profile_phone);
        tvRole = findViewById(R.id.tv_profile_role);
        btnBack = findViewById(R.id.btn_profile_back);
        btnLogout = findViewById(R.id.btn_logout);
        switchDarkMode = findViewById(R.id.switch_dark_mode);

        // Set switch to current saved preference
        switchDarkMode.setChecked(session.isDarkMode());

        // Listen for dark mode toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            session.setDarkMode(isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        btnBack.setOnClickListener(v -> finish());
        btnLogout.setOnClickListener(v -> handleLogout());

        loadUserData();
    }

    private void loadUserData() {
        int userId = session.getUserId();
        tvName.setText(session.getUserName());
        tvEmail.setText(session.getUserEmail());
        tvRole.setText(session.getUserRole());

        if (!session.getUserName().isEmpty()) {
            tvInitial.setText(String.valueOf(session.getUserName().charAt(0)).toUpperCase());
        }

        repository.getExecutor().execute(() -> {
            User user = repository.getUserDao().getUserById(userId);
            if (user != null) {
                runOnUiThread(() -> {
                    tvPhone.setText(user.getPhone());
                });
            }
        });
    }

    private void handleLogout() {
        session.logout();
        Toast.makeText(this, "Berhasil keluar akun!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
