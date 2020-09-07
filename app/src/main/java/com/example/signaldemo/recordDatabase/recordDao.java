package com.example.signaldemo.recordDatabase;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.signaldemo.database.MessageData;

import java.util.List;

@Dao
public interface recordDao {
    @Query("SELECT * FROM recordData ORDER BY curRecvSeq DESC LIMIT 1")
    recordData getRecord();

    @Insert
    void insertAll(recordData recordData);

    @Update
    void Update(recordData recordData);

    @Query("SELECT COUNT(curRecvSeq) FROM recordData")
    int databaseCount();

    @Query("DELETE FROM recordData")
    void deleteAll();

}
