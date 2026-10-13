package com.example.asalaga1.ui.owner;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.Booking;
import com.example.asalaga1.model.Court;
import com.example.asalaga1.model.User;
import com.example.asalaga1.model.Venue;
import com.example.asalaga1.util.ImageHelper;
import com.example.asalaga1.util.SessionManager;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OwnerBookingsFragment extends Fragment {

    private RecyclerView rvBookings;
    private TextView tvEmptyState;
    private AppRepository repository;
    private SessionManager session;
    private List<Booking> bookingList = new ArrayList<>();
    private OwnerBookingAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owner_bookings, container, false);

        repository = new AppRepository(requireContext());
        session = new SessionManager(requireContext());

        rvBookings = view.findViewById(R.id.rv_owner_bookings);
        tvEmptyState = view.findViewById(R.id.tv_owner_bookings_empty);

        rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OwnerBookingAdapter(bookingList, repository, new OwnerBookingAdapter.OnBookingDecisionListener() {
            @Override
            public void onAcceptBooking(Booking booking) {
                updateStatus(booking.getId(), "CONFIRMED", "Pesanan berhasil diterima!");
            }

            @Override
            public void onRejectBooking(Booking booking) {
                updateStatus(booking.getId(), "CANCELLED", "Pesanan berhasil ditolak.");
            }
        });
        rvBookings.setAdapter(adapter);

        loadBookings();

        return view;
    }

    private void loadBookings() {
        int ownerId = session.getUserId();
        repository.getExecutor().execute(() -> {
            List<Booking> bookings = repository.getBookingDao().getBookingsByOwner(ownerId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    bookingList.clear();
                    bookingList.addAll(bookings);
                    adapter.notifyDataSetChanged();

                    if (bookingList.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        rvBookings.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        rvBookings.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void updateStatus(int bookingId, String status, String successMessage) {
        repository.getExecutor().execute(() -> {
            repository.getBookingDao().updateBookingStatus(bookingId, status);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show();
                    loadBookings();
                });
            }
        });
    }

    // Embedded Adapter for Owner incoming bookings list
    public static class OwnerBookingAdapter extends RecyclerView.Adapter<OwnerBookingAdapter.OwnerBookingViewHolder> {
        private final List<Booking> bookings;
        private final AppRepository repository;
        private final OnBookingDecisionListener listener;

        public interface OnBookingDecisionListener {
            void onAcceptBooking(Booking booking);
            void onRejectBooking(Booking booking);
        }

        public OwnerBookingAdapter(List<Booking> bookings, AppRepository repository, OnBookingDecisionListener listener) {
            this.bookings = bookings;
            this.repository = repository;
            this.listener = listener;
        }

        @NonNull
        @Override
        public OwnerBookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
            return new OwnerBookingViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OwnerBookingViewHolder holder, int position) {
            Booking booking = bookings.get(position);
            holder.bind(booking, repository, listener);
        }

        @Override
        public int getItemCount() {
            return bookings.size();
        }

        public static class OwnerBookingViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvVenue, tvCourt, tvStatus, tvDateTime, tvPrice, tvPaymentStatus, tvCustomerInfo;
            private final Button btnReject, btnAccept, btnViewProof;
            private final View layoutActions;

            public OwnerBookingViewHolder(@NonNull View itemView) {
                super(itemView);
                tvVenue = itemView.findViewById(R.id.tv_item_booking_venue);
                tvCourt = itemView.findViewById(R.id.tv_item_booking_court);
                tvStatus = itemView.findViewById(R.id.tv_item_booking_status);
                tvDateTime = itemView.findViewById(R.id.tv_item_booking_datetime);
                tvPrice = itemView.findViewById(R.id.tv_item_booking_price);
                tvPaymentStatus = itemView.findViewById(R.id.tv_item_booking_payment_status);
                tvCustomerInfo = itemView.findViewById(R.id.tv_item_booking_customer_info);
                btnReject = itemView.findViewById(R.id.btn_booking_cancel);
                btnAccept = itemView.findViewById(R.id.btn_booking_action);
                layoutActions = itemView.findViewById(R.id.layout_booking_actions);
                btnViewProof = itemView.findViewById(R.id.btn_view_proof);
            }

            public void bind(Booking booking, AppRepository repository, OnBookingDecisionListener listener) {
                tvVenue.setText("Loading...");
                tvCourt.setText("Loading...");
                tvCustomerInfo.setVisibility(View.VISIBLE);
                tvCustomerInfo.setText("Loading Penyewa...");

                // Dynamic loads
                repository.getExecutor().execute(() -> {
                    Court court = repository.getCourtDao().getCourtById(booking.getCourtId());
                    if (court != null) {
                        Venue venue = repository.getVenueDao().getVenueById(court.getVenueId());
                        tvCourt.post(() -> tvCourt.setText(court.getName()));
                        if (venue != null) {
                            tvVenue.post(() -> tvVenue.setText(venue.getName()));
                        }
                    }

                    User customer = repository.getUserDao().getUserById(booking.getCustomerId());
                    if (customer != null) {
                        tvCustomerInfo.post(() -> tvCustomerInfo.setText("Penyewa: " + customer.getName() + " (" + customer.getPhone() + ")"));
                    }
                });

                tvDateTime.setText(booking.getBookingDate() + " • " + 
                        String.format(Locale.US, "%02d:00 - %02d:00", booking.getStartTime(), booking.getEndTime()));
                
                NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
                tvPrice.setText(format.format(booking.getTotalPrice()));
                
                // Payment Status & Proof Button
                boolean hasProof = !"Belum diunggah".equals(booking.getPaymentProof());
                tvPaymentStatus.setText("Bukti Pembayaran: " + (hasProof ? "Sudah diunggah" : "Belum diunggah"));
                if (hasProof) {
                    tvPaymentStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_confirmed));
                    btnViewProof.setVisibility(View.VISIBLE);
                    btnViewProof.setOnClickListener(v -> ImageHelper.showImagePreviewDialog(itemView.getContext(), booking.getPaymentProof()));
                } else {
                    tvPaymentStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_cancelled));
                    btnViewProof.setVisibility(View.GONE);
                }

                // Handle PENDING booking
                if ("PENDING".equals(booking.getStatus())) {
                    tvStatus.setBackgroundResource(R.drawable.bg_tag_pending);
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_pending));
                    layoutActions.setVisibility(View.VISIBLE);
                    btnReject.setText("Tolak");
                    btnAccept.setText("Terima & Konfirmasi");
                    btnAccept.setOnClickListener(v -> listener.onAcceptBooking(booking));
                    btnReject.setOnClickListener(v -> listener.onRejectBooking(booking));

                    if ("Belum diunggah".equals(booking.getPaymentProof())) {
                        final Runnable countdownRunnable = new Runnable() {
                            @Override
                            public void run() {
                                long now = System.currentTimeMillis();
                                long expiresAt = booking.getCreatedAt() + 10 * 60 * 1000;
                                long remaining = expiresAt - now;

                                if (remaining > 0) {
                                    long min = remaining / 60000;
                                    long sec = (remaining % 60000) / 1000;
                                    tvStatus.setText(String.format(Locale.US, "PENDING (%02d:%02d)", min, sec));
                                    tvStatus.postDelayed(this, 1000);
                                } else {
                                    tvStatus.setText("EXPIRED");
                                    tvStatus.setBackgroundResource(R.drawable.bg_tag_cancelled);
                                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_cancelled));
                                    layoutActions.setVisibility(View.GONE);

                                    // Cancel in Firestore in background
                                    repository.getExecutor().execute(() -> {
                                        repository.getBookingDao().updateBookingStatus(booking.getId(), "CANCELLED");
                                    });
                                }
                            }
                        };

                        tvStatus.removeCallbacks(null);
                        tvStatus.post(countdownRunnable);
                    } else {
                        tvStatus.removeCallbacks(null);
                        tvStatus.setText("PENDING (Perlu Konfirmasi)");
                    }
                } else {
                    tvStatus.removeCallbacks(null);
                    tvStatus.setText(booking.getStatus());
                    layoutActions.setVisibility(View.GONE);

                    if ("CONFIRMED".equals(booking.getStatus())) {
                        tvStatus.setBackgroundResource(R.drawable.bg_tag_confirmed);
                        tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_confirmed));
                    } else {
                        tvStatus.setBackgroundResource(R.drawable.bg_tag_cancelled);
                        tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_cancelled));
                    }
                }
            }
        }
    }
}
