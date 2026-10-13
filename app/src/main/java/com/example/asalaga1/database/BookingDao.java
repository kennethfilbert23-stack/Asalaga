package com.example.asalaga1.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.asalaga1.model.Booking;
import java.util.List;

@Dao
public interface BookingDao {
    @Insert
    long insertBooking(Booking booking);

    @Query("SELECT * FROM bookings WHERE customer_id = :customerId ORDER BY id DESC")
    List<Booking> getBookingsByCustomer(int customerId);

    @Query("SELECT b.* FROM bookings b JOIN courts c ON b.court_id = c.id JOIN venues v ON c.venue_id = v.id WHERE v.owner_id = :ownerId ORDER BY b.id DESC")
    List<Booking> getBookingsByOwner(int ownerId);

    @Query("SELECT * FROM bookings WHERE court_id = :courtId AND booking_date = :date AND status != 'CANCELLED'")
    List<Booking> getActiveBookingsForCourtOnDate(int courtId, String date);

    @Query("UPDATE bookings SET status = :status WHERE id = :id")
    void updateBookingStatus(int id, String status);

    @Query("UPDATE bookings SET payment_proof = :paymentProof WHERE id = :id")
    void updatePaymentProof(int id, String paymentProof);

    @Query("SELECT * FROM bookings WHERE id = :id")
    Booking getBookingById(int id);

    @Query("SELECT * FROM bookings ORDER BY id DESC")
    List<Booking> getAllBookings();
}
