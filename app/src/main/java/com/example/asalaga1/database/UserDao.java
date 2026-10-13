package com.example.asalaga1.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.asalaga1.model.User;
import java.util.List;

@Dao
public interface UserDao {
    @Insert
    long insertUser(User user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE id = :id")
    User getUserById(int id);

    @Query("SELECT * FROM users WHERE role != 'ADMIN'")
    List<User> getAllUsersExceptAdmin();

    @Query("DELETE FROM users WHERE id = :id")
    void deleteUser(int id);
}
