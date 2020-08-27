package com.example.signaldemo.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {MessageData.class}, version = 1 ,exportSchema = false )
public abstract class AppDatabase extends RoomDatabase {
    public abstract messageDao userDao();
}
