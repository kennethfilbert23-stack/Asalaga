package com.example.asalaga1.ui.customer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.Court;
import com.example.asalaga1.model.Venue;
import com.example.asalaga1.util.SessionManager;
import java.util.ArrayList;
import java.util.List;

public class VenueDetailsActivity extends AppCompatActivity {

    private TextView tvName, tvAddress, tvDesc;
    private RecyclerView rvCourts;
    private TextView tvEmptyState;
    private AppRepository repository;
    private int venueId;
    private List<Court> courtList = new ArrayList<>();
    private CourtAdapter adapter;
    private SessionManager session;
    private ImageView imgVenue;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venue_details);

        venueId = getIntent().getIntExtra("VENUE_ID", -1);
        if (venueId == -1) {
            Toast.makeText(this, "Venue tidak ditemukan!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repository = new AppRepository(this);
        session = new SessionManager(this);
        isAdmin = "ADMIN".equals(session.getUserRole());

        Toolbar toolbar = findViewById(R.id.toolbar_venue);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvName = findViewById(R.id.tv_detail_venue_name);
        tvAddress = findViewById(R.id.tv_detail_venue_address);
        tvDesc = findViewById(R.id.tv_detail_venue_desc);
        rvCourts = findViewById(R.id.rv_detail_courts);
        tvEmptyState = findViewById(R.id.tv_detail_empty_state);
        imgVenue = findViewById(R.id.img_detail_venue);

        if (isAdmin) {
            imgVenue.setOnClickListener(v -> showEditVenueImageDialog());
        }

        rvCourts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourtAdapter(courtList, isAdmin, court -> {
            Intent intent = new Intent(VenueDetailsActivity.this, BookingActivity.class);
            intent.putExtra("COURT_ID", court.getId());
            startActivity(intent);
        }, court -> {
            showEditCourtImageDialog(court);
        });
        rvCourts.setAdapter(adapter);

        loadVenueDetails();
        loadCourts();
    }

    private void loadVenueDetails() {
        repository.getExecutor().execute(() -> {
            Venue venue = repository.getVenueDao().getVenueById(venueId);
            runOnUiThread(() -> {
                if (venue != null) {
                    tvName.setText(venue.getName());
                    tvAddress.setText("📍 " + venue.getAddress());
                    tvDesc.setText(venue.getDescription());

                    String imgUrl = com.example.asalaga1.util.ImageHelper.getImageUrl(venue.getImagePath());

                    Glide.with(VenueDetailsActivity.this)
                         .load(imgUrl)
                         .placeholder(R.drawable.bg_gradient_blue)
                         .error(R.drawable.bg_gradient_blue)
                         .into(imgVenue);
                }
            });
        });
    }

    private void showEditVenueImageDialog() {
        repository.getExecutor().execute(() -> {
            Venue venue = repository.getVenueDao().getVenueById(venueId);
            if (venue != null) {
                runOnUiThread(() -> {
                    EditText etImageId = new EditText(this);
                    etImageId.setHint("Masukkan Google Drive Image ID");
                    String currentId = venue.getImagePath();
                    if (currentId != null && !"venue_placeholder".equals(currentId)) {
                        etImageId.setText(currentId);
                    }

                    // Add padding to EditText to look nice in dialog
                    int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
                    etImageId.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

                    new AlertDialog.Builder(this)
                            .setTitle("Ubah ID Gambar Venue")
                            .setMessage("Masukkan ID gambar Google Drive. Kosongkan untuk menggunakan default.")
                            .setView(etImageId)
                            .setPositiveButton("Simpan", (dialog, which) -> {
                                String newId = etImageId.getText().toString().trim();
                                repository.getExecutor().execute(() -> {
                                    repository.getVenueDao().updateVenueImagePath(venueId, newId);
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, "Gambar venue berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                                        loadVenueDetails();
                                    });
                                });
                            })
                            .setNegativeButton("Batal", null)
                            .show();
                });
            }
        });
    }

    private void showEditCourtImageDialog(Court court) {
        EditText etImageId = new EditText(this);
        etImageId.setHint("Masukkan Google Drive Image ID");
        String currentId = court.getImagePath();
        if (currentId != null) {
            etImageId.setText(currentId);
        }

        // Add padding to EditText to look nice in dialog
        int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
        etImageId.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        new AlertDialog.Builder(this)
                .setTitle("Ubah ID Gambar Lapangan")
                .setMessage("Masukkan ID gambar Google Drive untuk lapangan '" + court.getName() + "'. Kosongkan untuk menggunakan default.")
                .setView(etImageId)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newId = etImageId.getText().toString().trim();
                    repository.getExecutor().execute(() -> {
                        repository.getCourtDao().updateCourtImagePath(court.getId(), newId);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Gambar lapangan berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                            loadCourts();
                        });
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void loadCourts() {
        repository.getExecutor().execute(() -> {
            List<Court> courts = repository.getCourtDao().getCourtsByVenue(venueId);
            runOnUiThread(() -> {
                courtList.clear();
                courtList.addAll(courts);
                adapter.notifyDataSetChanged();

                if (courtList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvCourts.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvCourts.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    // Inside Class CourtAdapter
    public static class CourtAdapter extends RecyclerView.Adapter<CourtAdapter.CourtViewHolder> {
        private final List<Court> courts;
        private final boolean isAdmin;
        private final OnCourtClickListener listener;
        private final OnEditCourtImageListener editListener;

        public interface OnCourtClickListener {
            void onCourtClick(Court court);
        }

        public interface OnEditCourtImageListener {
            void onEditCourtImage(Court court);
        }

        public CourtAdapter(List<Court> courts, boolean isAdmin, OnCourtClickListener listener, OnEditCourtImageListener editListener) {
            this.courts = courts;
            this.isAdmin = isAdmin;
            this.listener = listener;
            this.editListener = editListener;
        }

        @NonNull
        @Override
        public CourtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_court, parent, false);
            return new CourtViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CourtViewHolder holder, int position) {
            Court court = courts.get(position);
            holder.bind(court, isAdmin, listener, editListener);
        }

        @Override
        public int getItemCount() {
            return courts.size();
        }

        public static class CourtViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName, tvSport, tvStatus, tvPrice;
            private final Button btnAction;
            private final ImageView imgCourt;

            public CourtViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_item_court_name);
                tvSport = itemView.findViewById(R.id.tv_item_court_sport);
                tvStatus = itemView.findViewById(R.id.tv_item_court_status);
                tvPrice = itemView.findViewById(R.id.tv_item_court_price);
                btnAction = itemView.findViewById(R.id.btn_court_action);
                imgCourt = itemView.findViewById(R.id.img_item_court);
            }

            public void bind(Court court, boolean isAdmin, OnCourtClickListener listener, OnEditCourtImageListener editListener) {
                tvName.setText(court.getName());
                
                String icon = "🏸 ";
                if ("Futsal".equalsIgnoreCase(court.getSportType())) icon = "⚽ ";
                else if ("Basketball".equalsIgnoreCase(court.getSportType())) icon = "🏀 ";
                else if ("Tennis".equalsIgnoreCase(court.getSportType())) icon = "🎾 ";
                
                tvSport.setText(icon + court.getSportType());
                tvStatus.setText(court.getStatus());
                
                java.text.NumberFormat format = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("in", "ID"));
                tvPrice.setText(format.format(court.getPricePerHour()) + " / Jam");

                String imgUrl = com.example.asalaga1.util.ImageHelper.getImageUrl(court.getImagePath());

                Glide.with(itemView.getContext())
                     .load(imgUrl)
                     .placeholder(R.drawable.bg_gradient_blue)
                     .error(R.drawable.bg_gradient_blue)
                     .into(imgCourt);

                if (isAdmin) {
                    btnAction.setEnabled(true);
                    btnAction.setText("Edit Gambar");
                    btnAction.setAlpha(1.0f);
                    btnAction.setOnClickListener(v -> editListener.onEditCourtImage(court));
                } else {
                    if ("MAINTENANCE".equalsIgnoreCase(court.getStatus())) {
                        tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_cancelled));
                        btnAction.setEnabled(false);
                        btnAction.setText("Tutup");
                        btnAction.setAlpha(0.5f);
                    } else {
                        tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_confirmed));
                        btnAction.setEnabled(true);
                        btnAction.setText("Pesan");
                        btnAction.setAlpha(1.0f);
                        btnAction.setOnClickListener(v -> listener.onCourtClick(court));
                    }
                }
            }
        }
    }
}
