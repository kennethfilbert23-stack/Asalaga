package com.example.asalaga1.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.asalaga1.model.Venue;
import java.util.List;

@Dao
public interface VenueDao {
    @Insert
    long insertVenue(Venue venue);

    @Query("SELECT * FROM venues WHERE status = 'APPROVED'")
    List<Venue> getAllApprovedVenues();

    @Query("SELECT DISTINCT v.* FROM venues v JOIN courts c ON v.id = c.venue_id WHERE v.status = 'APPROVED' AND c.sport_type = :sportType")
    List<Venue> getApprovedVenuesBySportType(String sportType);

    @Query("SELECT * FROM venues WHERE owner_id = :ownerId")
    List<Venue> getVenuesByOwner(int ownerId);

    @Query("SELECT * FROM venues WHERE status = 'PENDING'")
    List<Venue> getPendingVenues();

    @Query("SELECT * FROM venues")
    List<Venue> getAllVenues();

    @Query("SELECT * FROM venues WHERE id = :id")
    Venue getVenueById(int id);

    @Query("UPDATE venues SET status = :status WHERE id = :id")
    void updateVenueStatus(int id, String status);

    @Query("UPDATE venues SET address = :address, description = :description WHERE id = :id")
    void updateVenueDetails(int id, String address, String description);

    @Query("UPDATE venues SET image_path = :imagePath WHERE id = :id")
    void updateVenueImagePath(int id, String imagePath);

    @Query("DELETE FROM venues WHERE id = :id")
    void deleteVenue(int id);
}
