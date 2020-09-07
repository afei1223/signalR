package com.example.signaldemo;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class LogFile {
    private File file;
    private Context context;
    private String fileName;
    private FileWriter fw;
    private String path;
    private int i = 0;

    public boolean fileStatus;

    public LogFile(Context context) {
        this.context = context;
        fileName = String.valueOf((System.currentTimeMillis()))+"log.txt";
        file = context.getExternalFilesDir("YiyunLogFile");
//        file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        openLog();
    }

    public void saveLog(String log) {
        try {
            fw.write(log+"\n");
            Log.i("TAG","write:\n"+log);
            i++;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(i >= 2000){
            closeLog();
            fileName  = (System.currentTimeMillis())+"log.txt";
            i = 0;
            openLog();
        }
    }

    public void closeLog(){
        try {
            fw.close();
            fileStatus = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openLog(){
        try {
            fw = new FileWriter(file + File.separator + fileName);
            fileStatus = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
