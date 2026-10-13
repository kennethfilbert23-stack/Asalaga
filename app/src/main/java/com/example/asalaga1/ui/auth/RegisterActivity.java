package com.example.asalaga1.ui.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.User;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword;
    private RadioGroup rgRole;
    private Button btnRegister;
    private TextView tvGoLogin;
    private AppRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        repository = new AppRepository(this);

        etName = findViewById(R.id.et_reg_name);
        etEmail = findViewById(R.id.et_reg_email);
        etPhone = findViewById(R.id.et_reg_phone);
        etPassword = findViewById(R.id.et_reg_password);
        rgRole = findViewById(R.id.rg_reg_role);
        btnRegister = findViewById(R.id.btn_register);
        tvGoLogin = findViewById(R.id.tv_btn_go_login);

        btnRegister.setOnClickListener(v -> handleRegister());
        tvGoLogin.setOnClickListener(v -> finish()); // Go back to login screen
    }

    private void handleRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Harap isi semua kolom!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Sandi minimal harus 6 karakter!", Toast.LENGTH_SHORT).show();
            return;
        }

        int checkedId = rgRole.getCheckedRadioButtonId();
        final String role;
        if (checkedId == R.id.rb_role_owner) {
            role = "OWNER";
        } else {
            role = "CUSTOMER";
        }

        repository.getExecutor().execute(() -> {
            User existing = repository.getUserDao().getUserByEmail(email);
            if (existing != null) {
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "Email sudah terdaftar! Harap gunakan email lain.", Toast.LENGTH_LONG).show();
                });
            } else {
                User newUser = new User(name, email, password, role, phone, System.currentTimeMillis());
                repository.getUserDao().insertUser(newUser);
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "Pendaftaran Berhasil! Silakan login.", Toast.LENGTH_LONG).show();
                    finish(); // Return to Login Screen
                });
            }
        });
    }
}
