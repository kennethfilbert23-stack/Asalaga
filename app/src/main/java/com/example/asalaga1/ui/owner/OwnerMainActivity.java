package com.example.asalaga1.ui.owner;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.asalaga1.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class OwnerMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_main);

        BottomNavigationView navView = findViewById(R.id.owner_bottom_nav);
        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_owner_home) {
                selectedFragment = new OwnerHomeFragment();
            } else if (itemId == R.id.nav_owner_venues) {
                selectedFragment = new OwnerVenuesFragment();
            } else if (itemId == R.id.nav_owner_bookings) {
                selectedFragment = new OwnerBookingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.owner_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.owner_container, new OwnerHomeFragment())
                    .commit();
        }
    }
}
