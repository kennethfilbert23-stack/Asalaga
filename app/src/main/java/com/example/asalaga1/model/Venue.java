package com.example.asalaga1.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "venues",
    foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "owner_id",
        onDelete = ForeignKey.CASCADE
    )
)
public class Venue {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "owner_id", index = true)
    private int ownerId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "address")
    private String address;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "image_path")
    private String imagePath;

    @ColumnInfo(name = "status")
    private String status; // 'PENDING', 'APPROVED', 'REJECTED'

    // Constructor untuk Firestore
    public Venue() {
    }

    // Constructor
    public Venue(int ownerId, String name, String address, String description, String imagePath, String status) {
        this.ownerId = ownerId;
        this.name = name;
        this.address = address;
        this.description = description;
        this.imagePath = imagePath;
        this.status = status;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
