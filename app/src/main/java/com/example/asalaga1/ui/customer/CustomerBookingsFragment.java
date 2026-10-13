package com.example.asalaga1.ui.customer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.Booking;
import com.example.asalaga1.model.Court;
import com.example.asalaga1.model.Venue;
import com.bumptech.glide.Glide;
import com.example.asalaga1.util.ImageHelper;
import com.example.asalaga1.util.SessionManager;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerBookingsFragment extends Fragment {

    private RecyclerView rvBookings;
    private TextView tvEmptyState;
    private AppRepository repository;
    private SessionManager session;
    private List<Booking> bookingList = new ArrayList<>();
    private BookingAdapter adapter;

    // Google Drive Apps Script Upload fields
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private android.net.Uri selectedImageUri;
    private ImageView imgDialogPreview;
    private Button btnDialogSubmit;
    private View layoutUploadProgress;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Register image picker activity result launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    android.net.Uri uri = result.getData().getData();
                    if (uri != null) {
                        selectedImageUri = uri;
                        if (imgDialogPreview != null) {
                            Glide.with(this)
                                 .load(uri)
                                 .into(imgDialogPreview);
                        }
                        if (btnDialogSubmit != null) {
                            btnDialogSubmit.setEnabled(true);
                        }
                    }
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_bookings, container, false);

        repository = new AppRepository(requireContext());
        session = new SessionManager(requireContext());

        rvBookings = view.findViewById(R.id.rv_customer_bookings);
        tvEmptyState = view.findViewById(R.id.tv_bookings_empty_state);

        rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BookingAdapter(bookingList, repository, new BookingAdapter.OnBookingActionListener() {
            @Override
            public void onUploadProof(Booking booking) {
                showUploadSimulationDialog(booking);
            }

            @Override
            public void onCancelBooking(Booking booking) {
                showCancelConfirmDialog(booking);
            }
        });
        rvBookings.setAdapter(adapter);

        loadBookings();

        return view;
    }

    private void loadBookings() {
        int customerId = session.getUserId();
        repository.getExecutor().execute(() -> {
            List<Booking> bookings = repository.getBookingDao().getBookingsByCustomer(customerId);
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

    private void showUploadSimulationDialog(Booking booking) {
        selectedImageUri = null;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_upload_payment, null);
        TextView tvTotal = dialogView.findViewById(R.id.tv_upload_total);
        imgDialogPreview = dialogView.findViewById(R.id.img_upload_preview);
        btnDialogSubmit = dialogView.findViewById(R.id.btn_upload_submit);
        btnDialogSubmit.setEnabled(false); // Disable submit button until image is chosen
        Button btnSelect = dialogView.findViewById(R.id.btn_select_image);
        layoutUploadProgress = dialogView.findViewById(R.id.layout_upload_progress);

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        tvTotal.setText(format.format(booking.getTotalPrice()));

        // Default placeholder for upload preview: QR Code image
        String qrUrl = ImageHelper.getImageUrl("1E3HZjVCbOYhGMinxKsOqR7oU_U1YoMOF");
        Glide.with(this)
             .load(qrUrl)
             .placeholder(R.drawable.ic_booking)
             .error(R.drawable.ic_booking)
             .into(imgDialogPreview);

        btnSelect.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnDialogSubmit.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImageAndConfirm(dialog, booking);
            }
        });

        dialog.show();
    }

    private void uploadImageAndConfirm(AlertDialog dialog, Booking booking) {
        if (layoutUploadProgress != null) {
            layoutUploadProgress.setVisibility(View.VISIBLE);
        }
        btnDialogSubmit.setEnabled(false);

        repository.getExecutor().execute(() -> {
            try {
                String base64Image = getBase64FromUri(selectedImageUri);
                String fileName = "payment_" + booking.getId() + "_" + System.currentTimeMillis() + ".jpg";

                // Apps Script Web App URL provided by the user
                String webAppUrl = "https://script.google.com/macros/s/AKfycby5QPgLpGosFCQl0TLuHNxCZX0uW91IsHLDvNAjX8a9-i9Pqj4736VJ2vz1gJCW5utXNw/exec";
                
                // Network POST Request
                java.net.URL url = new java.net.URL(webAppUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(60000);

                org.json.JSONObject jsonParam = new org.json.JSONObject();
                jsonParam.put("imageBytes", base64Image);
                jsonParam.put("fileName", fileName);
                jsonParam.put("folderType", "PAYMENT");

                try (java.io.OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int status = conn.getResponseCode();
                
                // Follow 302/303 redirect for Google Apps Script Web App
                if (status == java.net.HttpURLConnection.HTTP_MOVED_TEMP 
                    || status == java.net.HttpURLConnection.HTTP_MOVED_PERM
                    || status == 307 || status == 308) {
                    
                    String redirectUrl = conn.getHeaderField("Location");
                    if (redirectUrl != null) {
                        java.net.URL nextUrl = new java.net.URL(redirectUrl);
                        conn = (java.net.HttpURLConnection) nextUrl.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("Accept", "application/json");
                        conn.setConnectTimeout(60000);
                        conn.setReadTimeout(60000);
                        status = conn.getResponseCode();
                    }
                }

                if (status == java.net.HttpURLConnection.HTTP_OK) {
                    String responseString = readStream(conn.getInputStream());
                    org.json.JSONObject responseJson = new org.json.JSONObject(responseString);
                    
                    if ("success".equals(responseJson.getString("status"))) {
                        String imageUrl = responseJson.getString("imageUrl");

                        // Update the paymentProof link to Google Drive link in Firestore
                        repository.getBookingDao().updatePaymentProof(booking.getId(), imageUrl);

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Bukti transfer berhasil diunggah ke Google Drive Anda!", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                                loadBookings();
                            });
                        }
                    } else {
                        throw new Exception(responseJson.optString("message", "Apps Script upload failed."));
                    }
                } else {
                    throw new Exception("HTTP server error code: " + status);
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Gagal unggah: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        if (layoutUploadProgress != null) {
                            layoutUploadProgress.setVisibility(View.GONE);
                        }
                        btnDialogSubmit.setEnabled(true);
                    });
                }
            }
        });
    }

    private String getBase64FromUri(android.net.Uri uri) throws Exception {
        java.io.InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        byte[] imageBytes = byteBuffer.toByteArray();
        return android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
    }

    private String readStream(java.io.InputStream in) throws Exception {
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    private void showCancelConfirmDialog(Booking booking) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Batalkan Pesanan")
                .setMessage("Apakah Anda yakin ingin membatalkan sewa lapangan ini?")
                .setPositiveButton("Ya, Batalkan", (dialog, which) -> {
                    repository.getExecutor().execute(() -> {
                        repository.getBookingDao().updateBookingStatus(booking.getId(), "CANCELLED");
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Pesanan berhasil dibatalkan.", Toast.LENGTH_SHORT).show();
                                loadBookings();
                            });
                        }
                    });
                })
                .setNegativeButton("Kembali", null)
                .show();
    }

    // Inside Class BookingAdapter
    public static class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {
        private final List<Booking> bookings;
        private final AppRepository repository;
        private final OnBookingActionListener listener;

        public interface OnBookingActionListener {
            void onUploadProof(Booking booking);
            void onCancelBooking(Booking booking);
        }

        public BookingAdapter(List<Booking> bookings, AppRepository repository, OnBookingActionListener listener) {
            this.bookings = bookings;
            this.repository = repository;
            this.listener = listener;
        }

        @NonNull
        @Override
        public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
            return new BookingViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
            Booking booking = bookings.get(position);
            holder.bind(booking, repository, listener);
        }

        @Override
        public int getItemCount() {
            return bookings.size();
        }

        public static class BookingViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvVenue, tvCourt, tvStatus, tvDateTime, tvPrice, tvPaymentStatus, tvCustomerInfo;
            private final Button btnCancel, btnAction, btnViewProof;

            public BookingViewHolder(@NonNull View itemView) {
                super(itemView);
                tvVenue = itemView.findViewById(R.id.tv_item_booking_venue);
                tvCourt = itemView.findViewById(R.id.tv_item_booking_court);
                tvStatus = itemView.findViewById(R.id.tv_item_booking_status);
                tvDateTime = itemView.findViewById(R.id.tv_item_booking_datetime);
                tvPrice = itemView.findViewById(R.id.tv_item_booking_price);
                tvPaymentStatus = itemView.findViewById(R.id.tv_item_booking_payment_status);
                tvCustomerInfo = itemView.findViewById(R.id.tv_item_booking_customer_info);
                btnCancel = itemView.findViewById(R.id.btn_booking_cancel);
                btnAction = itemView.findViewById(R.id.btn_booking_action);
                btnViewProof = itemView.findViewById(R.id.btn_view_proof);
            }

            public void bind(Booking booking, AppRepository repository, OnBookingActionListener listener) {
                tvVenue.setText("Loading...");
                tvCourt.setText("Loading...");

                // Dynamic fetch of court & venue
                repository.getExecutor().execute(() -> {
                    Court court = repository.getCourtDao().getCourtById(booking.getCourtId());
                    if (court != null) {
                        Venue venue = repository.getVenueDao().getVenueById(court.getVenueId());
                        tvCourt.post(() -> tvCourt.setText(court.getName()));
                        if (venue != null) {
                            tvVenue.post(() -> tvVenue.setText(venue.getName()));
                        }
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

                // Handle PENDING booking with live countdown timer
                if ("PENDING".equals(booking.getStatus()) && "Belum diunggah".equals(booking.getPaymentProof())) {
                    tvStatus.setBackgroundResource(R.drawable.bg_tag_pending);
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_pending));
                    btnCancel.setVisibility(View.VISIBLE);
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setText("Upload Bukti");
                    btnAction.setOnClickListener(v -> listener.onUploadProof(booking));

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
                                btnCancel.setVisibility(View.GONE);
                                btnAction.setVisibility(View.GONE);

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
                    btnAction.setVisibility(View.GONE);

                    if ("CONFIRMED".equals(booking.getStatus())) {
                        tvStatus.setText(booking.getStatus());
                        tvStatus.setBackgroundResource(R.drawable.bg_tag_confirmed);
                        tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_confirmed));
                        btnCancel.setVisibility(View.GONE);
                    } else if ("PENDING".equals(booking.getStatus())) {
                        tvStatus.setText("PENDING (Perlu Konfirmasi)");
                        tvStatus.setBackgroundResource(R.drawable.bg_tag_pending);
                        tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_pending));
                        btnCancel.setVisibility(View.GONE);
                    } else {
                        tvStatus.setText(booking.getStatus());
                        tvStatus.setBackgroundResource(R.drawable.bg_tag_cancelled);
                        tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_cancelled));
                        btnCancel.setVisibility(View.GONE);
                    }
                }

                btnCancel.setOnClickListener(v -> listener.onCancelBooking(booking));
            }
        }
    }
}
