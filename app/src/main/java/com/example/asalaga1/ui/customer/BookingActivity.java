package com.example.asalaga1.ui.customer;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.Booking;
import com.example.asalaga1.model.Court;
import com.example.asalaga1.util.SessionManager;
import com.example.asalaga1.util.ImageHelper;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private TextView tvCourtName, tvCourtPrice;
    private TextView tvSelectedDate;
    private Button btnPickDate;
    private RecyclerView rvTimeSlots;
    private Button btnConfirm;

    private AppRepository repository;
    private SessionManager session;
    private int courtId;
    private Court court;
    private String selectedDate = "";
    private double pricePerHour = 0;

    private final List<Integer> operationalHours = new ArrayList<>();
    private final List<Integer> bookedHours = new ArrayList<>();
    private final List<Integer> selectedHours = new ArrayList<>();
    private TimeSlotAdapter timeSlotAdapter;

    private static class ContiguousBlock {
        int startHour;
        int endHour;

        ContiguousBlock(int startHour, int endHour) {
            this.startHour = startHour;
            this.endHour = endHour;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        courtId = getIntent().getIntExtra("COURT_ID", -1);
        if (courtId == -1) {
            Toast.makeText(this, "Lapangan tidak ditemukan!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repository = new AppRepository(this);
        session = new SessionManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar_booking);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvCourtName = findViewById(R.id.tv_booking_court_name);
        tvCourtPrice = findViewById(R.id.tv_booking_court_price);
        tvSelectedDate = findViewById(R.id.tv_booking_date);
        btnPickDate = findViewById(R.id.btn_pick_date);
        rvTimeSlots = findViewById(R.id.rv_time_slots);
        btnConfirm = findViewById(R.id.btn_confirm_booking);

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnConfirm.setOnClickListener(v -> saveBooking());

        setupHoursGrid();
        loadCourtDetails();
    }

    private void setupHoursGrid() {
        // Operational hours: 07:00 to 21:00 (which covers up to 22:00)
        for (int i = 7; i <= 21; i++) {
            operationalHours.add(i);
        }

        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 3));
        timeSlotAdapter = new TimeSlotAdapter(operationalHours, bookedHours);
        rvTimeSlots.setAdapter(timeSlotAdapter);

        calculateCost();
    }

    private void loadCourtDetails() {
        repository.getExecutor().execute(() -> {
            court = repository.getCourtDao().getCourtById(courtId);
            runOnUiThread(() -> {
                if (court != null) {
                    tvCourtName.setText(court.getName());
                    pricePerHour = court.getPricePerHour();
                    NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
                    tvCourtPrice.setText("Harga: " + format.format(pricePerHour) + " / Jam");
                    calculateCost();
                }
            });
        });
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, dayOfMonth);
            tvSelectedDate.setText(selectedDate);
            checkBookedSlots();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); // disable past dates
        dialog.show();
    }

    private void checkBookedSlots() {
        if (selectedDate.isEmpty()) return;

        repository.getExecutor().execute(() -> {
            List<Booking> activeBookings = repository.getBookingDao().getActiveBookingsForCourtOnDate(courtId, selectedDate);
            List<Integer> booked = new ArrayList<>();
            for (Booking b : activeBookings) {
                for (int h = b.getStartTime(); h < b.getEndTime(); h++) {
                    booked.add(h);
                }
            }

            runOnUiThread(() -> {
                bookedHours.clear();
                bookedHours.addAll(booked);
                
                // Remove selected hours that are now booked
                List<Integer> toRemove = new ArrayList<>();
                for (Integer sh : selectedHours) {
                    if (bookedHours.contains(sh)) {
                        toRemove.add(sh);
                    }
                }
                selectedHours.removeAll(toRemove);
                
                if (timeSlotAdapter != null) {
                    timeSlotAdapter.notifyDataSetChanged();
                }
                
                calculateCost();
            });
        });
    }

    private void calculateCost() {
        if (pricePerHour <= 0) {
            btnConfirm.setEnabled(false);
            btnConfirm.setAlpha(0.5f);
            return;
        }

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

        if (selectedHours.isEmpty()) {
            btnConfirm.setText("Pilih Jam Sewa");
            btnConfirm.setEnabled(false);
            btnConfirm.setAlpha(0.5f);
            return;
        }

        double total = selectedHours.size() * pricePerHour;
        btnConfirm.setText("Bayar - " + format.format(total));
        btnConfirm.setEnabled(true);
        btnConfirm.setAlpha(1.0f);
    }

    private void saveBooking() {
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Harap pilih tanggal booking!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedHours.isEmpty()) {
            Toast.makeText(this, "Harap pilih jam sewa!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirm.setEnabled(false);
        int customerId = session.getUserId();

        // Group selected hours into contiguous blocks
        java.util.Collections.sort(selectedHours);
        List<ContiguousBlock> blocks = new ArrayList<>();
        int start = selectedHours.get(0);
        int prev = start;
        for (int i = 1; i < selectedHours.size(); i++) {
            int current = selectedHours.get(i);
            if (current == prev + 1) {
                prev = current;
            } else {
                blocks.add(new ContiguousBlock(start, prev + 1));
                start = current;
                prev = current;
            }
        }
        blocks.add(new ContiguousBlock(start, prev + 1));

        repository.getExecutor().execute(() -> {
            // Check for overlap in active bookings for all blocks
            List<Booking> active = repository.getBookingDao().getActiveBookingsForCourtOnDate(courtId, selectedDate);
            boolean overlaps = false;
            for (ContiguousBlock block : blocks) {
                for (Booking b : active) {
                    if (block.startHour < b.getEndTime() && block.endHour > b.getStartTime()) {
                        overlaps = true;
                        break;
                    }
                }
                if (overlaps) break;
            }

            if (overlaps) {
                runOnUiThread(() -> {
                    Toast.makeText(BookingActivity.this, "Beberapa slot waktu pilihan Anda sudah dipesan! Silakan pilih jam lain.", Toast.LENGTH_LONG).show();
                    btnConfirm.setEnabled(true);
                    checkBookedSlots();
                });
            } else {
                // Insert a separate Booking document for each contiguous block
                for (ContiguousBlock block : blocks) {
                    double blockTotal = (block.endHour - block.startHour) * pricePerHour;
                    Booking newBooking = new Booking(
                            customerId,
                            courtId,
                            selectedDate,
                            block.startHour,
                            block.endHour,
                            blockTotal,
                            "PENDING",
                            "Belum diunggah",
                            System.currentTimeMillis()
                    );
                    repository.getBookingDao().insertBooking(newBooking);
                }
                runOnUiThread(() -> {
                    Toast.makeText(BookingActivity.this, "Pesanan berhasil dibuat! Selesaikan pembayaran dalam 10 menit.", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    // RecyclerView Adapter for Time Slots
    public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {
        private final List<Integer> hours;
        private final List<Integer> bookedHours;

        public TimeSlotAdapter(List<Integer> hours, List<Integer> bookedHours) {
            this.hours = hours;
            this.bookedHours = bookedHours;
        }

        public void setSelectedHour(Integer hour) {
            // Unused in multiple selection mode but kept for backward compatibility if any
        }

        @NonNull
        @Override
        public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
            return new TimeSlotViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
            int hour = hours.get(position);
            holder.bind(hour);
        }

        @Override
        public int getItemCount() {
            return hours.size();
        }

        public class TimeSlotViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvTimeSlot;

            public TimeSlotViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTimeSlot = itemView.findViewById(R.id.tv_time_slot);
            }

            public void bind(int hour) {
                String label = String.format(Locale.US, "%02d:00", hour);
                tvTimeSlot.setText(label);

                boolean isBooked = bookedHours.contains(hour);
                boolean isSelected = selectedHours.contains(hour);

                if (isBooked) {
                    tvTimeSlot.setBackgroundResource(R.drawable.bg_time_slot_booked);
                    tvTimeSlot.setTextColor(0xFF94A3B8); // Muted grey
                    tvTimeSlot.setOnClickListener(null);
                } else if (isSelected) {
                    tvTimeSlot.setBackgroundResource(R.drawable.bg_time_slot_selected);
                    tvTimeSlot.setTextColor(0xFFFFFFFF); // White
                    tvTimeSlot.setOnClickListener(v -> {
                        selectedHours.remove(Integer.valueOf(hour));
                        notifyDataSetChanged();
                        calculateCost();
                    });
                } else {
                    tvTimeSlot.setBackgroundResource(R.drawable.bg_time_slot_unselected);
                    tvTimeSlot.setTextColor(0xFF0F172A); // Dark slate
                    tvTimeSlot.setOnClickListener(v -> {
                        selectedHours.add(hour);
                        notifyDataSetChanged();
                        calculateCost();
                    });
                }
            }
        }
    }
}
