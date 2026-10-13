package com.example.asalaga1.ui.owner;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.Court;
import com.example.asalaga1.model.Venue;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.bumptech.glide.Glide;
import com.example.asalaga1.util.ImageHelper;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OwnerCourtsActivity extends AppCompatActivity {

    private RecyclerView rvCourts;
    private TextView tvEmptyState;
    private FloatingActionButton fabAdd;

    // Venue editing views
    private EditText etVenueAddress;
    private EditText etVenueDesc;
    private Button btnSaveVenueDetails;
    private ImageView imgVenuePhotoHeader;
    private View layoutVenueUploadProgress;

    // Photo Upload helpers
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private String uploadMode = ""; // "VENUE", "COURT", or "COURT_EXISTING"
    private String selectedCourtImageUrl = ""; // stores uploaded court photo URL
    private int updatingCourtId = -1; // stores ID of the court currently being updated

    // Dialog court image views
    private ImageView imgDialogCourtPreview;
    private View layoutDialogCourtProgress;
    private Button btnDialogCourtSelect;

    private AppRepository repository;
    private int venueId;
    private String venueName;
    private List<Court> courtList = new ArrayList<>();
    private OwnerCourtAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_courts);

        venueId = getIntent().getIntExtra("VENUE_ID", -1);
        venueName = getIntent().getStringExtra("VENUE_NAME");
        if (venueId == -1) {
            Toast.makeText(this, "Venue tidak teridentifikasi!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repository = new AppRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar_owner_courts);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Kelola Venue - " + (venueName != null ? venueName : ""));
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvCourts = findViewById(R.id.rv_owner_courts);
        tvEmptyState = findViewById(R.id.tv_owner_courts_empty);
        fabAdd = findViewById(R.id.fab_add_court);

        // Venue editing layouts
        etVenueAddress = findViewById(R.id.et_venue_address);
        etVenueDesc = findViewById(R.id.et_venue_desc);
        btnSaveVenueDetails = findViewById(R.id.btn_save_venue_details);
        imgVenuePhotoHeader = findViewById(R.id.img_venue_photo_header);
        layoutVenueUploadProgress = findViewById(R.id.layout_venue_upload_progress);

        rvCourts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OwnerCourtAdapter(courtList, this::toggleCourtStatus, this::onCourtImageClick);
        rvCourts.setAdapter(adapter);

        // Initialize ActivityResultLauncher for image picking
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri uri = result.getData().getData();
                    if (uri != null) {
                        if ("VENUE".equals(uploadMode)) {
                            uploadVenuePhoto(uri);
                        } else if ("COURT".equals(uploadMode)) {
                            uploadCourtPhoto(uri);
                        } else if ("COURT_EXISTING".equals(uploadMode)) {
                            uploadExistingCourtPhoto(uri, updatingCourtId);
                        }
                    }
                }
            }
        );

        findViewById(R.id.btn_change_venue_photo).setOnClickListener(v -> {
            uploadMode = "VENUE";
            selectImage();
        });

        fabAdd.setOnClickListener(v -> showAddCourtDialog());
        btnSaveVenueDetails.setOnClickListener(v -> saveVenueDetails());

        loadVenueDetails();
        loadCourts();
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadVenuePhoto(android.net.Uri uri) {
        layoutVenueUploadProgress.setVisibility(View.VISIBLE);

        repository.getExecutor().execute(() -> {
            try {
                String base64Image = getBase64FromUri(uri);
                String fileName = "venue_" + venueId + "_" + System.currentTimeMillis() + ".jpg";

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
                jsonParam.put("folderType", "VENUE");

                try (java.io.OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int status = conn.getResponseCode();
                
                // Follow redirects
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

                        // Update in Firestore
                        repository.getVenueDao().updateVenueImagePath(venueId, imageUrl);

                        runOnUiThread(() -> {
                            Toast.makeText(this, "Foto venue berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                            layoutVenueUploadProgress.setVisibility(View.GONE);
                            Glide.with(this)
                                 .load(imageUrl)
                                 .placeholder(R.drawable.bg_gradient_blue)
                                 .into(imgVenuePhotoHeader);
                        });
                    } else {
                        throw new Exception(responseJson.optString("message", "Upload failed"));
                    }
                } else {
                    throw new Exception("HTTP status: " + status);
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Gagal unggah: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    layoutVenueUploadProgress.setVisibility(View.GONE);
                });
            }
        });
    }

    private void uploadCourtPhoto(android.net.Uri uri) {
        layoutDialogCourtProgress.setVisibility(View.VISIBLE);
        if (btnDialogCourtSelect != null) btnDialogCourtSelect.setEnabled(false);
        if (imgDialogCourtPreview != null) imgDialogCourtPreview.setEnabled(false);

        repository.getExecutor().execute(() -> {
            try {
                String base64Image = getBase64FromUri(uri);
                String fileName = "court_" + venueId + "_" + System.currentTimeMillis() + ".jpg";

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
                jsonParam.put("folderType", "COURT");

                try (java.io.OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int status = conn.getResponseCode();
                
                // Follow redirects
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
                        selectedCourtImageUrl = imageUrl;

                        runOnUiThread(() -> {
                            Toast.makeText(this, "Foto lapangan berhasil diunggah!", Toast.LENGTH_SHORT).show();
                            layoutDialogCourtProgress.setVisibility(View.GONE);
                            if (btnDialogCourtSelect != null) btnDialogCourtSelect.setEnabled(true);
                            if (imgDialogCourtPreview != null) imgDialogCourtPreview.setEnabled(true);
                            Glide.with(this)
                                 .load(imageUrl)
                                 .placeholder(R.drawable.bg_gradient_blue)
                                 .into(imgDialogCourtPreview);
                        });
                    } else {
                        throw new Exception(responseJson.optString("message", "Upload failed"));
                    }
                } else {
                    throw new Exception("HTTP status: " + status);
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Gagal unggah: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    layoutDialogCourtProgress.setVisibility(View.GONE);
                    if (btnDialogCourtSelect != null) btnDialogCourtSelect.setEnabled(true);
                    if (imgDialogCourtPreview != null) imgDialogCourtPreview.setEnabled(true);
                });
            }
        });
    }

    private void onCourtImageClick(Court court) {
        uploadMode = "COURT_EXISTING";
        updatingCourtId = court.getId();
        selectImage();
    }

    private void uploadExistingCourtPhoto(android.net.Uri uri, final int courtId) {
        final android.app.AlertDialog progressDialog = new android.app.AlertDialog.Builder(this)
                .setView(new android.widget.ProgressBar(this))
                .setMessage("Mengunggah foto lapangan...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        repository.getExecutor().execute(() -> {
            try {
                String base64Image = getBase64FromUri(uri);
                String fileName = "court_existing_" + courtId + "_" + System.currentTimeMillis() + ".jpg";

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
                jsonParam.put("folderType", "COURT");

                try (java.io.OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int status = conn.getResponseCode();
                
                // Follow redirects
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

                        // Update in Firestore
                        repository.getCourtDao().updateCourtImagePath(courtId, imageUrl);

                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Foto lapangan berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                            loadCourts();
                        });
                    } else {
                        throw new Exception(responseJson.optString("message", "Upload failed"));
                    }
                } else {
                    throw new Exception("HTTP status: " + status);
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Gagal unggah: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String getBase64FromUri(android.net.Uri uri) throws Exception {
        java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
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

    private void loadVenueDetails() {
        repository.getExecutor().execute(() -> {
            Venue venue = repository.getVenueDao().getVenueById(venueId);
            if (venue != null) {
                runOnUiThread(() -> {
                    etVenueAddress.setText(venue.getAddress());
                    etVenueDesc.setText(venue.getDescription());
                    
                    String imgId = venue.getImagePath();
                    String imgUrl = ImageHelper.getImageUrl(imgId);
                    
                    Glide.with(this)
                         .load(imgUrl)
                         .placeholder(R.drawable.bg_gradient_blue)
                         .into(imgVenuePhotoHeader);
                });
            }
        });
    }

    private void saveVenueDetails() {
        String address = etVenueAddress.getText().toString().trim();
        String desc = etVenueDesc.getText().toString().trim();

        if (address.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Semua rincian venue wajib diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        repository.getExecutor().execute(() -> {
            repository.getVenueDao().updateVenueDetails(venueId, address, desc);
            runOnUiThread(() -> {
                Toast.makeText(this, "Detail venue berhasil disimpan!", Toast.LENGTH_SHORT).show();
                loadVenueDetails();
            });
        });
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

    private void toggleCourtStatus(Court court) {
        String newStatus = "ACTIVE".equals(court.getStatus()) ? "MAINTENANCE" : "ACTIVE";
        repository.getExecutor().execute(() -> {
            repository.getCourtDao().updateCourtStatus(court.getId(), newStatus);
            runOnUiThread(() -> {
                Toast.makeText(OwnerCourtsActivity.this, "Status lapangan berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                loadCourts();
            });
        });
    }

    private void showAddCourtDialog() {
        selectedCourtImageUrl = ""; // reset court image URL

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_court, null);
        EditText etName = dialogView.findViewById(R.id.et_dialog_court_name);
        Spinner spSport = dialogView.findViewById(R.id.sp_dialog_court_sport);
        EditText etPrice = dialogView.findViewById(R.id.et_dialog_court_price);
        
        imgDialogCourtPreview = dialogView.findViewById(R.id.img_dialog_court_preview);
        layoutDialogCourtProgress = dialogView.findViewById(R.id.layout_dialog_court_progress);
        btnDialogCourtSelect = dialogView.findViewById(R.id.btn_dialog_court_select_image);
        btnDialogCourtSelect.setVisibility(View.GONE); // Hide button completely!

        // Preload default mockup image inside the dialog
        Glide.with(this)
             .load(R.drawable.bg_gradient_blue)
             .into(imgDialogCourtPreview);

        imgDialogCourtPreview.setOnClickListener(v -> {
            uploadMode = "COURT";
            selectImage();
        });

        String[] sports = {"Futsal", "Badminton", "Basketball", "Tennis"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sports);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSport.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Tambah Lapangan Baru")
                .setView(dialogView)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String sportType = sports[spSport.getSelectedItemPosition()];
                    String priceStr = etPrice.getText().toString().trim();

                    if (name.isEmpty() || priceStr.isEmpty()) {
                        Toast.makeText(OwnerCourtsActivity.this, "Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double price = Double.parseDouble(priceStr);
                    repository.getExecutor().execute(() -> {
                        // Use 6-parameter constructor to set the uploaded Google Drive image URL
                        String imagePath = selectedCourtImageUrl.isEmpty() ? "venue_placeholder" : selectedCourtImageUrl;
                        Court newCourt = new Court(venueId, name, sportType, price, "ACTIVE", imagePath);
                        repository.getCourtDao().insertCourt(newCourt);
                        runOnUiThread(() -> {
                            Toast.makeText(OwnerCourtsActivity.this, "Lapangan berhasil ditambahkan!", Toast.LENGTH_SHORT).show();
                            loadCourts();
                        });
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // Adaptor untuk Lapangan sisi Owner
    public static class OwnerCourtAdapter extends RecyclerView.Adapter<OwnerCourtAdapter.OwnerCourtViewHolder> {
        private final List<Court> courts;
        private final OnToggleStatusListener listener;
        private final OnCourtImageClickListener imageListener;

        public interface OnToggleStatusListener {
            void onToggleStatus(Court court);
        }

        public interface OnCourtImageClickListener {
            void onCourtImageClick(Court court);
        }

        public OwnerCourtAdapter(List<Court> courts, OnToggleStatusListener listener, OnCourtImageClickListener imageListener) {
            this.courts = courts;
            this.listener = listener;
            this.imageListener = imageListener;
        }

        @NonNull
        @Override
        public OwnerCourtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_court, parent, false);
            return new OwnerCourtViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OwnerCourtViewHolder holder, int position) {
            Court court = courts.get(position);
            holder.bind(court, listener, imageListener);
        }

        @Override
        public int getItemCount() {
            return courts.size();
        }

        public static class OwnerCourtViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName, tvSport, tvStatus, tvPrice;
            private final Button btnAction;
            private final android.widget.ImageView imgCourt;

            public OwnerCourtViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_item_court_name);
                tvSport = itemView.findViewById(R.id.tv_item_court_sport);
                tvStatus = itemView.findViewById(R.id.tv_item_court_status);
                tvPrice = itemView.findViewById(R.id.tv_item_court_price);
                btnAction = itemView.findViewById(R.id.btn_court_action);
                imgCourt = itemView.findViewById(R.id.img_item_court);
            }

            public void bind(Court court, OnToggleStatusListener listener, OnCourtImageClickListener imageListener) {
                tvName.setText(court.getName());
                
                String icon = "🏸 ";
                if ("Futsal".equalsIgnoreCase(court.getSportType())) icon = "⚽ ";
                else if ("Basketball".equalsIgnoreCase(court.getSportType())) icon = "🏀 ";
                else if ("Tennis".equalsIgnoreCase(court.getSportType())) icon = "🎾 ";
                
                tvSport.setText(icon + court.getSportType());
                tvStatus.setText(court.getStatus());

                String imgId = court.getImagePath();
                String imgUrl = ImageHelper.getImageUrl(imgId);

                Glide.with(itemView.getContext())
                     .load(imgUrl)
                     .placeholder(R.drawable.bg_gradient_blue)
                     .error(R.drawable.bg_gradient_blue)
                     .into(imgCourt);

                imgCourt.setOnClickListener(v -> imageListener.onCourtImageClick(court));

                NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
                tvPrice.setText(format.format(court.getPricePerHour()) + " / Jam");

                if ("MAINTENANCE".equalsIgnoreCase(court.getStatus())) {
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_cancelled));
                    btnAction.setText("Aktifkan");
                    btnAction.setBackgroundResource(R.drawable.bg_button_blue); // standard capsule blue
                } else {
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_confirmed));
                    btnAction.setText("Tutup");
                    btnAction.setBackgroundResource(R.drawable.bg_tag_cancelled); // show a warning red outline
                }

                btnAction.setOnClickListener(v -> listener.onToggleStatus(court));
            }
        }
    }
}
