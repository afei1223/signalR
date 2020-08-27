package com.example.signaldemo.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface messageDao {
    @Query("SELECT * FROM MessageData")
    List<MessageData> getAll();

    @Insert
    void insertAll(MessageData... message);

    @Delete
    void delete(MessageData... message);
}
