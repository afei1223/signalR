package com.example.signaldemo.recordDatabase;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.signaldemo.database.MessageData;
import com.example.signaldemo.database.messageDao;

@Database(entities = {recordData.class}, version = 1 ,exportSchema = false )
public abstract class recordDatabase extends RoomDatabase {
    public abstract recordDao recordDao();
}