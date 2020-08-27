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
    LongConnect longConnect;


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
        longConnect = new LongConnect(url,getApplicationContext());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button1:
                AuthRequest authRequest1 =new AuthRequest(1,"9c8c8bdf64322122dd5436ae66fe77ec0db099b17fbc8ecdb28352e44d1120fc","1194940411577839616");
//                longConnect.send("Auth",authRequest);
                longConnect.send("Auth",authRequest1);
//                longConnect.selectDatabase();
                break;
            case R.id.button2:
                AuthRequest authRequest =new AuthRequest(1,"9c8c8bdf64322122dd5436ae66fe77ec0db099b17fbc8ecdb28352e44d1120fc","1194940411577839616");
//                longConnect.send("Auth",authRequest);
                longConnect.LogIn(authRequest);
                break;
            case R.id.button3:
                boolean x = longConnect.connectStatus;
                Toast.makeText(this,"连接状态："+x,Toast.LENGTH_LONG).show();
                break;
            case R.id.button4:
                longConnect.send("qwe","qwewqeqweqwe");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + view.getId());
        }
    }

}