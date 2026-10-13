package com.example.asalaga1.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.Booking;
import com.example.asalaga1.model.User;
import com.example.asalaga1.model.Venue;
import com.example.asalaga1.ui.ProfileActivity;
import com.example.asalaga1.util.SessionManager;
import java.util.List;

public class AdminHomeFragment extends Fragment {

    private TextView tvAdminName;
    private TextView tvBookingsCount;
    private TextView tvUsersCount;
    private TextView tvPendingVenuesCount;
    private ImageButton btnProfile;

    private AppRepository repository;
    private SessionManager session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);

        repository = new AppRepository(requireContext());
        session = new SessionManager(requireContext());

        tvAdminName = view.findViewById(R.id.tv_admin_name);
        tvBookingsCount = view.findViewById(R.id.tv_admin_bookings_count);
        tvUsersCount = view.findViewById(R.id.tv_admin_users_count);
        tvPendingVenuesCount = view.findViewById(R.id.tv_admin_pending_venues_count);
        btnProfile = view.findViewById(R.id.btn_admin_profile);

        tvAdminName.setText(session.getUserName());

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ProfileActivity.class));
        });

        loadAdminStats();

        return view;
    }

    private void loadAdminStats() {
        repository.getExecutor().execute(() -> {
            // Bookings count
            List<Booking> bookings = repository.getBookingDao().getAllBookings();
            int totalBookings = bookings.size();

            // Users count (except Admin)
            List<User> users = repository.getUserDao().getAllUsersExceptAdmin();
            int totalUsers = users.size();

            // Pending venues count
            List<Venue> pendingVenues = repository.getVenueDao().getPendingVenues();
            int totalPending = pendingVenues.size();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvBookingsCount.setText(totalBookings + " Pesanan");
                    tvUsersCount.setText(String.valueOf(totalUsers));
                    tvPendingVenuesCount.setText(totalPending + " Venue");
                });
            }
        });
    }
}
