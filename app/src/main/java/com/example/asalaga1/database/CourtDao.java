package com.example.asalaga1.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.asalaga1.model.Court;
import java.util.List;

@Dao
public interface CourtDao {
    @Insert
    long insertCourt(Court court);

    @Query("SELECT * FROM courts WHERE venue_id = :venueId")
    List<Court> getCourtsByVenue(int venueId);

    @Query("SELECT * FROM courts WHERE id = :id")
    Court getCourtById(int id);

    @Query("UPDATE courts SET status = :status WHERE id = :id")
    void updateCourtStatus(int id, String status);

    @Query("UPDATE courts SET image_path = :imagePath WHERE id = :id")
    void updateCourtImagePath(int id, String imagePath);

    @Query("DELETE FROM courts WHERE id = :id")
    void deleteCourt(int id);
}
