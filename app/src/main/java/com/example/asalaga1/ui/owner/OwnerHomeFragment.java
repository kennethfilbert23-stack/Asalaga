package com.example.asalaga1.ui.owner;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.Booking;
import com.example.asalaga1.model.Venue;
import com.example.asalaga1.ui.ProfileActivity;
import com.example.asalaga1.util.SessionManager;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OwnerHomeFragment extends Fragment {

    private TextView tvOwnerName;
    private TextView tvEarnings;
    private TextView tvVenuesCount;
    private TextView tvPendingCount;
    private ImageButton btnProfile;

    private AppRepository repository;
    private SessionManager session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owner_home, container, false);

        repository = new AppRepository(requireContext());
        session = new SessionManager(requireContext());

        tvOwnerName = view.findViewById(R.id.tv_owner_name);
        tvEarnings = view.findViewById(R.id.tv_owner_total_earnings);
        tvVenuesCount = view.findViewById(R.id.tv_owner_venues_count);
        tvPendingCount = view.findViewById(R.id.tv_owner_pending_bookings);
        btnProfile = view.findViewById(R.id.btn_owner_logout);

        tvOwnerName.setText(session.getUserName());

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ProfileActivity.class));
        });

        loadStatistics();

        return view;
    }

    private void loadStatistics() {
        int ownerId = session.getUserId();
        repository.getExecutor().execute(() -> {
            // Fetch owned venues
            List<Venue> venues = repository.getVenueDao().getVenuesByOwner(ownerId);
            int venueCount = venues.size();

            // Fetch incoming bookings and calculate confirmed earnings and pending counts
            List<Booking> bookings = repository.getBookingDao().getBookingsByOwner(ownerId);
            double totalEarnings = 0;
            int pendingBookings = 0;

            for (Booking b : bookings) {
                if ("CONFIRMED".equals(b.getStatus())) {
                    totalEarnings += b.getTotalPrice();
                } else if ("PENDING".equals(b.getStatus())) {
                    pendingBookings++;
                }
            }

            final double finalEarnings = totalEarnings;
            final int finalPending = pendingBookings;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvVenuesCount.setText(String.valueOf(venueCount));
                    tvPendingCount.setText(String.valueOf(finalPending));

                    NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
                    tvEarnings.setText(format.format(finalEarnings));
                });
            }
        });
    }
}
