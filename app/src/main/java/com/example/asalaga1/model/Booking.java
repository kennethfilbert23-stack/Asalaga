package com.example.asalaga1.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "bookings",
    foreignKeys = {
        @ForeignKey(
            entity = User.class,
            parentColumns = "id",
            childColumns = "customer_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Court.class,
            parentColumns = "id",
            childColumns = "court_id",
            onDelete = ForeignKey.CASCADE
        )
    }
)
public class Booking {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "customer_id", index = true)
    private int customerId;

    @ColumnInfo(name = "court_id", index = true)
    private int courtId;

    @ColumnInfo(name = "booking_date")
    private String bookingDate; // YYYY-MM-DD

    @ColumnInfo(name = "start_time")
    private int startTime; // Jam mulai (misal: 14)

    @ColumnInfo(name = "end_time")
    private int endTime; // Jam selesai (misal: 16)

    @ColumnInfo(name = "total_price")
    private double totalPrice;

    @ColumnInfo(name = "status")
    private String status; // 'PENDING', 'CONFIRMED', 'CANCELLED'

    @ColumnInfo(name = "payment_proof")
    private String paymentProof; // String simulasi bukti pembayaran (bisa path gambar atau teks)

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // Constructor untuk Firestore
    public Booking() {
    }

    // Constructor
    public Booking(int customerId, int courtId, String bookingDate, int startTime, int endTime, double totalPrice, String status, String paymentProof, long createdAt) {
        this.customerId = customerId;
        this.courtId = courtId;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalPrice = totalPrice;
        this.status = status;
        this.paymentProof = paymentProof;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public int getCourtId() { return courtId; }
    public void setCourtId(int courtId) { this.courtId = courtId; }

    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public int getStartTime() { return startTime; }
    public void setStartTime(int startTime) { this.startTime = startTime; }

    public int getEndTime() { return endTime; }
    public void setEndTime(int endTime) { this.endTime = endTime; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentProof() { return paymentProof; }
    public void setPaymentProof(String paymentProof) { this.paymentProof = paymentProof; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
