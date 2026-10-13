package com.example.asalaga1.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.example.asalaga1.R;
import com.example.asalaga1.ui.auth.LoginActivity;
import com.example.asalaga1.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminMainActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        session = new SessionManager(this);


        BottomNavigationView navView = findViewById(R.id.admin_bottom_nav);
        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_admin_home) {
                selectedFragment = new AdminHomeFragment();
            } else if (itemId == R.id.nav_admin_venues) {
                selectedFragment = new AdminVenuesFragment();
            } else if (itemId == R.id.nav_admin_users) {
                selectedFragment = new AdminUsersFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.admin_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.admin_container, new AdminHomeFragment())
                    .commit();
        }
    }

}
