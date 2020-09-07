package com.example.signaldemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Objects;

import io.reactivex.internal.functions.ObjectHelper;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
//    SignalSession signalSession;
    ReliableClient reliableClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button1 = findViewById(R.id.button1);
        button1.setOnClickListener(this);
        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(this);
        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(this);
        Button button4 = findViewById(R.id.button4);
        button4.setOnClickListener(this);
//        String url = "http://staging-im.effio.cn/im";
        String url = "http://192.168.200.7:30032/im";
        reliableClient = new ReliableClient(url,getApplicationContext());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button1:
//                AuthRequest authRequest1 =new AuthRequest(1,"7d1a907d581c054844df351116db3b635324a303b8bc6ef8f260bfe2cc090048","41rtJPs6hcB",1);
//                reliableClient.send("Auth",authRequest1);
                reliableClient.selectDatabase();
                break;
            case R.id.button2:
                AuthRequest authRequest =new AuthRequest(1,"0b508bf5015cddcc8666f1954540292cb988825996d5ad9e619c7e8f64ec7973","1a17d923-815e-45d9-8b8c-0a9ab9603684",1);
//                longConnect.send("Auth",authRequest);
                reliableClient.send("Auth",authRequest);
                reliableClient.LogIn(authRequest);
                break;
            case R.id.button3:
                boolean x = reliableClient.isConected();
                Toast.makeText(this,"连接状态："+x,Toast.LENGTH_LONG).show();
                break;
            case R.id.button4:
                reliableClient.selectDatabase1();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + view.getId());
        }
    }

}