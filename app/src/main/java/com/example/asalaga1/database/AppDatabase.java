package com.example.asalaga1.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.asalaga1.model.Booking;
import com.example.asalaga1.model.Court;
import com.example.asalaga1.model.User;
import com.example.asalaga1.model.Venue;
import java.util.concurrent.Executors;

@Database(entities = {User.class, Venue.class, Court.class, Booking.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract UserDao userDao();
    public abstract VenueDao venueDao();
    public abstract CourtDao courtDao();
    public abstract BookingDao bookingDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "asalaga_database"
            )
            .fallbackToDestructiveMigration()
            .addCallback(roomCallback)
            .build();
        }
        return instance;
    }

    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            // Pre-populate database
            Executors.newSingleThreadExecutor().execute(() -> {
                UserDao userDao = instance.userDao();
                VenueDao venueDao = instance.venueDao();
                CourtDao courtDao = instance.courtDao();

                // 1. Seed Users
                User admin = new User("Admin Asalaga", "admin@asalaga.com", "admin123", "ADMIN", "081122334455", System.currentTimeMillis());
                userDao.insertUser(admin);

                User owner = new User("Pak Budi (Owner)", "owner@asalaga.com", "owner123", "OWNER", "081299998888", System.currentTimeMillis());
                long ownerId = userDao.insertUser(owner);

                User customer = new User("Sewa Lapangan (Customer)", "customer@asalaga.com", "customer123", "CUSTOMER", "085711112222", System.currentTimeMillis());
                userDao.insertUser(customer);

                // 2. Seed Venues (APPROVED & PENDING for testing)
                Venue venueApproved = new Venue((int) ownerId, "Asalaga Arena", "Jl. Pemuda No. 10, Jakarta", "Pusat olahraga terlengkap dengan lantai vinyl standar internasional, parkir luas, kantin, dan kamar mandi bersih.", "venue_asalaga_arena", "APPROVED");
                long approvedVenueId = venueDao.insertVenue(venueApproved);

                Venue venuePending = new Venue((int) ownerId, "Budi Futsal Stadium", "Jl. Merdeka No. 88, Bogor", "Lapangan futsal dengan rumput sintetis premium, sejuk dengan kipas angin raksasa.", "venue_budi_futsal", "PENDING");
                venueDao.insertVenue(venuePending);

                // 3. Seed Courts for Approved Venue
                Court court1 = new Court((int) approvedVenueId, "Lapangan Futsal A (Sintetis)", "Futsal", 150000.0, "ACTIVE");
                Court court2 = new Court((int) approvedVenueId, "Lapangan Badminton 1 (Vinyl)", "Badminton", 60000.0, "ACTIVE");
                Court court3 = new Court((int) approvedVenueId, "Lapangan Badminton 2 (Vinyl)", "Badminton", 60000.0, "ACTIVE");
                Court court4 = new Court((int) approvedVenueId, "Lapangan Basket Utama (Wood)", "Basketball", 200000.0, "ACTIVE");

                courtDao.insertCourt(court1);
                courtDao.insertCourt(court2);
                courtDao.insertCourt(court3);
                courtDao.insertCourt(court4);
            });
        }
    };
}
