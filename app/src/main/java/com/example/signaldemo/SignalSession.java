package com.example.signaldemo;

import android.os.Message;
import android.util.Log;

import com.microsoft.signalr.Action;
import com.microsoft.signalr.Action1;
import com.microsoft.signalr.Action2;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.OnClosedCallback;
import com.microsoft.signalr.Subscription;

import android.os.Handler;

import java.util.Date;
import java.util.TimerTask;


public class SignalSession {
    String TAG = getClass().getSimpleName();
    private String url;
    HubConnection hubConnection;

    private long heartDelay = 3*1000;
    private long connectDelay = 5;
    private long lastEchoTime = 0;

    private boolean isRunning;
    private boolean connectStatus;

    public boolean isConnectStatus() {
        return connectStatus;
    }

    Action1<Long> EchoCallback = (msg)->{
        lastEchoTime = msg;
    };
    private Action2<Long,String> receiveCallback;

    public SignalSession(String url1, Action2<Long,String> receiveCallbac) {
        this.url = url1;
        this.receiveCallback = receiveCallbac;
    }

    private void setOn(Action2<Long,String> receiveCallbac){
        //hubConnection关闭回调
        hubConnection.onClosed(new OnClosedCallback() {
            @Override
            public void invoke(Exception exception) {
                connectStatus = false;
            }
        });

        //在longconnect中注册callback，返回的数据直接在longconnect中处理。
        hubConnection.on("Echo",EchoCallback,Long.class);
        hubConnection.on("Receive",receiveCallbac,Long.class,String.class);

        hubConnection.on("Auth",(message)->{
            Log.i(TAG,"Auth:"+message.token+"\n"+message.info+"\n"+message.ok+"\n"+message.onlineClients);
        },AuthResponse.class);
        //被抛弃使用的方法
//        hubConnection.on("Echo",(message)->{
//            Log.i(TAG,"Echo time:"+lastEchoTime);
//            },String.class);

//        hubConnection.on("Receive", (seq,data)->{
////            Ack(seq);
//            Log.i(TAG,"receive:"+seq+"\n"+data);
//        },Long.class,String.class);

        hubConnection.on("Kickout", (mes)->{
            Log.i(TAG,"kickout"+mes);
        },Object.class);
        hubConnection.on("CheckAuth", (mes)->{
            Log.i(TAG,"CheckAuth"+mes);
        },String.class);
        hubConnection.on("OtherConnectionChanged",(mes)->{
            Log.i(TAG,"Other"+mes);
        },String.class);
    }

//    private void Ack(Long seq) {
//        hubConnection.send("Ack",seq);
//    }

    public void send(String method,Object... message){
        Log.i(TAG,"message:\n"+message+"   method"+method);
        hubConnection.send(method,message);
    }

    private void heartCheck(){
        Log.i(TAG,"wow this is heartCheck");
        new Thread(runnableHeart).start();
    }

    Runnable runnableHeart = new Runnable() {
        @Override
        public void run() {
            while(isRunning){
                long time = System.currentTimeMillis()/1000;
                hubConnection.send("Echo",String.valueOf(time));
                Log.i(TAG,"time\n"+time+"\nlast: "+lastEchoTime);
                if(time-lastEchoTime>connectDelay){
                    Log.i(TAG,"断线啦，要重连了");
                    connectStatus = false;
                }else {
                    connectStatus = true;
                }

//                Log.i(TAG,"qweeqweqqweqweqweqweqweqweqweqweqweqweqweqwertye!!@@@##"+connectStatus);
                try {
                    Thread.sleep(heartDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public void stopConnect(){
        isRunning = false;
        connectStatus = false;
        hubConnection.stop();
    }

    public void startConnect(){
        Log.i(TAG,"start connect this message from SignalRSession");
        hubConnection = HubConnectionBuilder.create(url)
                .build();
        setOn(receiveCallback);
        hubConnection.start().blockingAwait();
        heartCheck();
        isRunning = true;
    }

}
