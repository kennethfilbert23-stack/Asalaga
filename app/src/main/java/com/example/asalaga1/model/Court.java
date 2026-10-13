package com.example.asalaga1.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "courts",
    foreignKeys = @ForeignKey(
        entity = Venue.class,
        parentColumns = "id",
        childColumns = "venue_id",
        onDelete = ForeignKey.CASCADE
    )
)
public class Court {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "venue_id", index = true)
    private int venueId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "sport_type")
    private String sportType; // 'Futsal', 'Badminton', 'Basketball', 'Tennis'

    @ColumnInfo(name = "price_per_hour")
    private double pricePerHour;

    @ColumnInfo(name = "status")
    private String status; // 'ACTIVE', 'MAINTENANCE'

    @ColumnInfo(name = "image_path")
    private String imagePath;

    // Constructor untuk Firestore
    public Court() {
    }

    // Constructor
    public Court(int venueId, String name, String sportType, double pricePerHour, String status) {
        this(venueId, name, sportType, pricePerHour, status, null);
    }

    public Court(int venueId, String name, String sportType, double pricePerHour, String status, String imagePath) {
        this.venueId = venueId;
        this.name = name;
        this.sportType = sportType;
        this.pricePerHour = pricePerHour;
        this.status = status;
        this.imagePath = imagePath;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVenueId() { return venueId; }
    public void setVenueId(int venueId) { this.venueId = venueId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSportType() { return sportType; }
    public void setSportType(String sportType) { this.sportType = sportType; }

    public double getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(double pricePerHour) { this.pricePerHour = pricePerHour; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}
