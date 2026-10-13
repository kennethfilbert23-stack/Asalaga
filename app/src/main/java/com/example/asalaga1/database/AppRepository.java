package com.example.asalaga1.database;

import android.content.Context;
import com.example.asalaga1.model.*;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppRepository {
    private final FirebaseFirestore db;
    private final ExecutorService executorService;

    public AppRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        executorService = Executors.newFixedThreadPool(4);
        
        // Pemicu inisialisasi data sampel online jika database kosong
        checkAndSeedFirebaseDatabase();
    }

    public ExecutorService getExecutor() {
        return executorService;
    }

    // --- USER DAO IMPLEMENTATION ---
    private final UserDao userDao = new UserDao() {
        @Override
        public long insertUser(User user) {
            try {
                int id = (int) (System.currentTimeMillis() % 100000000);
                user.setId(id);
                Tasks.await(db.collection("users").document(String.valueOf(id)).set(user));
                return id;
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }

        @Override
        public User getUserByEmail(String email) {
            try {
                QuerySnapshot snapshot = Tasks.await(db.collection("users").whereEqualTo("email", email).limit(1).get());
                if (!snapshot.isEmpty()) {
                    return snapshot.getDocuments().get(0).toObject(User.class);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public User getUserById(int id) {
            try {
                DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(String.valueOf(id)).get());
                if (snapshot.exists()) {
                    return snapshot.toObject(User.class);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public List<User> getAllUsersExceptAdmin() {
            List<User> list = new ArrayList<>();
            try {
                QuerySnapshot snapshot = Tasks.await(db.collection("users").get());
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    User u = doc.toObject(User.class);
                    if (u != null && !"ADMIN".equals(u.getRole())) {
                        list.add(u);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        }

        @Override
        public void deleteUser(int id) {
            try {
                Tasks.await(db.collection("users").document(String.valueOf(id)).delete());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    // --- VENUE DAO IMPLEMENTATION ---
    private final VenueDao venueDao = new VenueDao() {
        @Override
        public long insertVenue(Venue venue) {
            try {
                int id = (int) (System.currentTimeMillis() % 100000000);
                venue.setId(id);
                Tasks.await(db.collection("venues").document(String.valueOf(id)).set(venue));
                return id;
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }

        @Override
        public List<Venue> getAllApprovedVenues() {
            List<Venue> list = new ArrayList<>();
            try {
                QuerySnapshot snapshot = Tasks.await(db.collection("venues").whereEqualTo("status", "APPROVED").get());
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Venue v = doc.toObject(Venue.class);
                    if (v != null) list.add(v);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        }

        @Override
        public List<Venue> getApprovedVenuesBySportType(String sportType) {
            List<Venue> list = new ArrayList<>();
            try {
                // Fetch courts of this sportType
                QuerySnapshot courtSnapshot = Tasks.await(db.collection("courts").whereEqualTo("sportType", sportType).get());
                List<Integer> venueIds = new ArrayList<>();
                for (DocumentSnapshot doc : courtSnapshot.getDocuments()) {
                    Court c = doc.toObject(Court.class);
                    if (c != null && !venueIds.contains(c.getVenueId())) {
                        venueIds.add(c.getVenueId());
                    }
                }
                if (venueIds.isEmpty()) return list;
                
                // Now fetch approved venues that match these IDs
                QuerySnapshot venueSnapshot = Tasks.await(db.collection("venues").whereEqualTo("status", "APPROVED").get());
                for (DocumentSnapshot doc : venueSnapshot.getDocuments()) {
                    Venue v = doc.toObject(Venue.class);
                    if (v != null && venueIds.contains(v.getId())) {
                        list.add(v);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        }

        @Override
        public List<Venue> getVenuesByOwner(int ownerId) {
            List<Venue> list = new ArrayList<>();
            try {
                QuerySnapshot snapshot = Tasks.await(db.collection("venues").whereEqualTo("ownerId", ownerId).get());
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Venue v = doc.toObject(Venue.class);
                    if (v != null) list.add(v);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        }

        @Override
        public List<Venue> getPendingVenues() {
            List<Venue> list = new ArrayList<>();
            try {
                QuerySnapshot snapshot = Tasks.await(db.collection("venues").whereEqualTo("status", "PENDING").get());
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Venue v = doc.toObject(Venue.class);
                    if (v != null) list.add(v);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        }

        @Override
        public List<Venue> getAllVenues() {
            List<Venue> list = new ArrayList<>();
            try {
                QuerySnapshot snapshot = Tasks.await(db.collection("venues").get());
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Venue v = doc.toObject(Venue.class);
                    if (v != null) list.add(v);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        }

        @Override
        public Venue getVenueById(int id) {
            try {
                DocumentSnapshot snapshot = Tasks.await(db.collection("venues").document(String.valueOf(id)).get());
                if (snapshot.exists()) {
                    return snapshot.toObject(Venue.class);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void updateVenueStatus(int id, String status) {
            try {
                Tasks.await(db.collection("venues").document(String.valueOf(id)).update("status", status));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void updateVenueDetails(int id, String address, String description) {
            try {
                Tasks.await(db.collection("venues").document(String.valueOf(id)).update(
                    "address", address,
                    "description", description
                ));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void updateVenueImagePath(int id, String imagePath) {
            try {
                Tasks.await(db.collection("venues").document(String.valueOf(id)).update("imagePath", imagePath));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void deleteVenue(int id) {
            try {
                Tasks.await(db.collection("venues").document(String.valueOf(id)).delete());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    // --- COURT DAO IMPLEMENTATION ---
    private final CourtDao courtDao = new CourtDao() {
        @Override
        public long insertCourt(Court court) {
            try {
                int id = (int) (System.currentTimeMillis() % 100000000);
                court.setId(id);
                Tasks.await(db.collection("courts").document(String.valueOf(id)).set(court));
                return id;
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }

        @Override
        public List<Court> getCourtsByVenue(int venueId) {
            List<Court> list = new ArrayList<>();
            try {
                QuerySnapshot snapshot = Tasks.await(db.collection("courts").whereEqualTo("venueId", venueId).get());
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Court c = doc.toObject(Court.class);
                    if (c != null) list.add(c);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        }

        @Override
        public Court getCourtById(int id) {
            try {
                DocumentSnapshot snapshot = Tasks.await(db.collection("courts").document(String.valueOf(id)).get());
                if (snapshot.exists()) {
                    return snapshot.toObject(Court.class);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void updateCourtStatus(int id, String status) {
            try {
                Tasks.await(db.collection("courts").document(String.valueOf(id)).update("status", status));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void updateCourtImagePath(int id, String imagePath) {
            try {
                Tasks.await(db.collection("courts").document(String.valueOf(id)).update("imagePath", imagePath));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void deleteCourt(int id) {
            try {
                Tasks.await(db.collection("courts").document(String.valueOf(id)).delete());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    // --- BOOKING DAO IMPLEMENTATION ---
    private final BookingDao bookingDao = new BookingDao() {
        @Override
        public long insertBooking(Booking booking) {
            try {
                int id = (int) (System.currentTimeMillis() % 100000000);
                booking.setId(id);
                Tasks.await(db.collection("bookings").document(String.valueOf(id)).set(booking));
                return id;
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }

        @Override
        public List<Booking> getBookingsByCustomer(int customerId) {
            cleanExpiredBookings();
            List<Booking> list = new ArrayList<>();
            try {
                QuerySnapshot snapshot = Tasks.await(db.collection("bookings").whereEqualTo("customerId", customerId).get());
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Booking b = doc.toObject(Booking.class);
                    if (b != null) list.add(b);
                }
                list.sort((b1, b2) -> Integer.compare(b2.getId(), b1.getId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        }

        @Override
        public List<Booking> getBookingsByOwner(int ownerId) {
            cleanExpiredBookings();
            List<Booking> list = new ArrayList<>();
            try {
                // 1. Fetch all venues for ownerId
                QuerySnapshot venueSnapshot = Tasks.await(db.collection("venues").whereEqualTo("ownerId", ownerId).get());
                List<Integer> venueIds = new ArrayList<>();
                for (DocumentSnapshot doc : venueSnapshot.getDocuments()) {
                    Venue v = doc.toObject(Venue.class);
                    if (v != null) venueIds.add(v.getId());
                }
                if (venueIds.isEmpty()) return list;

                // 2. Fetch all courts for these venueIds
                QuerySnapshot courtSnapshot = Tasks.await(db.collection("courts").get());
                List<Integer> courtIds = new ArrayList<>();
                for (DocumentSnapshot doc : courtSnapshot.getDocuments()) {
                    Court c = doc.toObject(Court.class);
                    if (c != null && venueIds.contains(c.getVenueId())) {
                        courtIds.add(c.getId());
                    }
                }
                if (courtIds.isEmpty()) return list;

                // 3. Fetch all bookings and filter locally
                QuerySnapshot bookingSnapshot = Tasks.await(db.collection("bookings").get());
                for (DocumentSnapshot doc : bookingSnapshot.getDocuments()) {
                    Booking b = doc.toObject(Booking.class);
                    if (b != null && courtIds.contains(b.getCourtId())) {
                        list.add(b);
                    }
                }
                
                // 4. Sort locally by ID descending
                list.sort((b1, b2) -> Integer.compare(b2.getId(), b1.getId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        }

        @Override
        public List<Booking> getActiveBookingsForCourtOnDate(int courtId, String date) {
            cleanExpiredBookings();
            List<Booking> list = new ArrayList<>();
            try {
                QuerySnapshot snapshot = Tasks.await(
                    db.collection("bookings")
                      .whereEqualTo("courtId", courtId)
                      .whereEqualTo("bookingDate", date)
                      .get()
                );
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Booking b = doc.toObject(Booking.class);
                    if (b != null && !"CANCELLED".equals(b.getStatus())) {
                        list.add(b);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        }

        @Override
        public void updateBookingStatus(int id, String status) {
            try {
                Tasks.await(db.collection("bookings").document(String.valueOf(id)).update("status", status));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void updatePaymentProof(int id, String paymentProof) {
            try {
                Tasks.await(db.collection("bookings").document(String.valueOf(id)).update("paymentProof", paymentProof));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public Booking getBookingById(int id) {
            try {
                DocumentSnapshot snapshot = Tasks.await(db.collection("bookings").document(String.valueOf(id)).get());
                if (snapshot.exists()) {
                    return snapshot.toObject(Booking.class);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public List<Booking> getAllBookings() {
            cleanExpiredBookings();
            List<Booking> list = new ArrayList<>();
            try {
                QuerySnapshot snapshot = Tasks.await(db.collection("bookings").get());
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Booking b = doc.toObject(Booking.class);
                    if (b != null) list.add(b);
                }
                list.sort((b1, b2) -> Integer.compare(b2.getId(), b1.getId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return list;
        }
    };

    public UserDao getUserDao() {
        return userDao;
    }

    public VenueDao getVenueDao() {
        return venueDao;
    }

    public CourtDao getCourtDao() {
        return courtDao;
    }

    public BookingDao getBookingDao() {
        return bookingDao;
    }

    private void cleanExpiredBookings() {
        try {
            long now = System.currentTimeMillis();
            long tenMinutes = 10 * 60 * 1000;
            QuerySnapshot snapshot = Tasks.await(db.collection("bookings").whereEqualTo("status", "PENDING").get());
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Booking b = doc.toObject(Booking.class);
                if (b != null 
                        && ("Belum diunggah".equals(b.getPaymentProof()) || b.getPaymentProof() == null || b.getPaymentProof().isEmpty())
                        && (now - b.getCreatedAt() > tenMinutes)) {
                    Tasks.await(db.collection("bookings").document(String.valueOf(b.getId())).update("status", "CANCELLED"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- SEEDING FIREBASE DATABASE ---
    private void checkAndSeedFirebaseDatabase() {
        executorService.execute(() -> {
            try {
                // Check if user table is empty
                QuerySnapshot userSnapshot = Tasks.await(db.collection("users").limit(1).get());
                if (userSnapshot.isEmpty()) {
                    // Seed default users
                    User admin = new User("Admin Asalaga", "admin@asalaga.com", "admin123", "ADMIN", "081122334455", System.currentTimeMillis());
                    int adminId = (int) (System.currentTimeMillis() % 100000000);
                    admin.setId(adminId);
                    Tasks.await(db.collection("users").document(String.valueOf(adminId)).set(admin));

                    Thread.sleep(20); // Sleep briefly to prevent duplicate IDs on rapid timestamp generation
                    
                    User owner = new User("Pak Budi (Owner)", "owner@asalaga.com", "owner123", "OWNER", "081299998888", System.currentTimeMillis());
                    int ownerId = (int) (System.currentTimeMillis() % 100000000);
                    owner.setId(ownerId);
                    Tasks.await(db.collection("users").document(String.valueOf(ownerId)).set(owner));

                    Thread.sleep(20);

                    User customer = new User("Sewa Lapangan (Customer)", "customer@asalaga.com", "customer123", "CUSTOMER", "085711112222", System.currentTimeMillis());
                    int customerId = (int) (System.currentTimeMillis() % 100000000);
                    customer.setId(customerId);
                    Tasks.await(db.collection("users").document(String.valueOf(customerId)).set(customer));

                    Thread.sleep(20);

                    // Seed approved venue
                    Venue venueApproved = new Venue(ownerId, "Asalaga Arena", "Jl. Pemuda No. 10, Jakarta", "Pusat olahraga terlengkap dengan lantai vinyl standar internasional, parkir luas, kantin, dan kamar mandi bersih.", "venue_asalaga_arena", "APPROVED");
                    int approvedVenueId = (int) (System.currentTimeMillis() % 100000000);
                    venueApproved.setId(approvedVenueId);
                    Tasks.await(db.collection("venues").document(String.valueOf(approvedVenueId)).set(venueApproved));

                    Thread.sleep(20);

                    // Seed pending venue
                    Venue venuePending = new Venue(ownerId, "Budi Futsal Stadium", "Jl. Merdeka No. 88, Bogor", "Lapangan futsal dengan rumput sintetis premium, sejuk dengan kipas angin raksasa.", "venue_budi_futsal", "PENDING");
                    int pendingVenueId = (int) (System.currentTimeMillis() % 100000000);
                    venuePending.setId(pendingVenueId);
                    Tasks.await(db.collection("venues").document(String.valueOf(pendingVenueId)).set(venuePending));

                    Thread.sleep(20);

                    // Seed courts for Asalaga Arena
                    Court court1 = new Court(approvedVenueId, "Lapangan Futsal A (Sintetis)", "Futsal", 150000.0, "ACTIVE");
                    int court1Id = (int) (System.currentTimeMillis() % 100000000);
                    court1.setId(court1Id);
                    Tasks.await(db.collection("courts").document(String.valueOf(court1Id)).set(court1));
                    
                    Thread.sleep(20);

                    Court court2 = new Court(approvedVenueId, "Lapangan Badminton 1 (Vinyl)", "Badminton", 60000.0, "ACTIVE");
                    int court2Id = (int) (System.currentTimeMillis() % 100000000);
                    court2.setId(court2Id);
                    Tasks.await(db.collection("courts").document(String.valueOf(court2Id)).set(court2));

                    Thread.sleep(20);

                    Court court3 = new Court(approvedVenueId, "Lapangan Badminton 2 (Vinyl)", "Badminton", 60000.0, "ACTIVE");
                    int court3Id = (int) (System.currentTimeMillis() % 100000000);
                    court3.setId(court3Id);
                    Tasks.await(db.collection("courts").document(String.valueOf(court3Id)).set(court3));

                    Thread.sleep(20);

                    Court court4 = new Court(approvedVenueId, "Lapangan Basket Utama (Wood)", "Basketball", 200000.0, "ACTIVE");
                    int court4Id = (int) (System.currentTimeMillis() % 100000000);
                    court4.setId(court4Id);
                    Tasks.await(db.collection("courts").document(String.valueOf(court4Id)).set(court4));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
