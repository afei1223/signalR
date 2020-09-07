package com.example.signaldemo.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class MessageData {
    @PrimaryKey()
    public long seq;

    @ColumnInfo(name = "data")
    public String data;

    @ColumnInfo(name = "ReceiveTime")
    public long ReceiveTime;

}
