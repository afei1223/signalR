package com.example.signaldemo.recordDatabase;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class recordData {
    @ColumnInfo(name = "curRecvSeq")
    @PrimaryKey
    public long curRecvSeq;

    @ColumnInfo(name = "UserId")
    public String UserId;

    @ColumnInfo(name = "ClientType")
    public int ClientType;

    @ColumnInfo(name = "Token")
    public String Token;

    @ColumnInfo(name = "Version")
    public int Version;
}
