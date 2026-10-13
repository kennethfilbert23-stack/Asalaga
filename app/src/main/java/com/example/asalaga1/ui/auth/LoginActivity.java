package com.example.asalaga1.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.User;
import com.example.asalaga1.ui.admin.AdminMainActivity;
import com.example.asalaga1.ui.customer.CustomerMainActivity;
import com.example.asalaga1.ui.owner.OwnerMainActivity;
import com.example.asalaga1.util.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoRegister;
    private AppRepository repository;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        repository = new AppRepository(this);
        session = new SessionManager(this);

        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoRegister = findViewById(R.id.tv_btn_go_register);

        btnLogin.setOnClickListener(v -> handleLogin());
        tvGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Harap isi semua kolom!", Toast.LENGTH_SHORT).show();
            return;
        }

        repository.getExecutor().execute(() -> {
            User user = repository.getUserDao().getUserByEmail(email);
            runOnUiThread(() -> {
                if (user != null && user.getPassword().equals(password)) {
                    // Save login session
                    session.createLoginSession(user.getId(), user.getName(), user.getEmail(), user.getRole());
                    
                    Toast.makeText(this, "Login Berhasil! Selamat Datang " + user.getName(), Toast.LENGTH_SHORT).show();
                    
                    // Redirect
                    Intent intent;
                    if ("ADMIN".equals(user.getRole())) {
                        intent = new Intent(LoginActivity.this, AdminMainActivity.class);
                    } else if ("OWNER".equals(user.getRole())) {
                        intent = new Intent(LoginActivity.this, OwnerMainActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, CustomerMainActivity.class);
                    }
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Email atau Password salah!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
