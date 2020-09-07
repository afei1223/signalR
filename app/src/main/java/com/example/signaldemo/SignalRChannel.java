package com.example.signaldemo;

import android.os.Message;
import android.util.Log;

import com.microsoft.signalr.Action2;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.OnClosedCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Handler;

/**
 * 封装hubConnection
 * 1. 心跳机制
 * 2. 连接状态
 * 3. 监听收到的消息发送出去
 * 4. 开放开始连接，断开连接，发送消息三个方法
 * */
public class SignalRChannel {

    private final int ECHO_NUM      =  1001;
    private final int RECEIVE_NUM   =  1002;
    private final int KICKOUT_NUM   =  1003;
    private final int AUTH_NUM      =  1004;
    private final int CHECKAUTH_NUM =  1005;
    private final int OTHERCONNECTIONCHANGED_NUM = 1006;

    private String TAG = getClass().getSimpleName();
    private String url;
    private HubConnection hubConnection;

    private long heartDelay = 3*1000;
    private long KeepAliveTimeOutSecond = 5;
    private long lastRecvTime = 0;

    private boolean isRunning;
    private boolean connectStatus;

    public boolean isConnected() {
        return connectStatus;
    }

    private android.os.Handler receiveHandler;

    public SignalRChannel(String url1, android.os.Handler handler) {
        this.url = url1;
        this.receiveHandler = handler;
    }

    private void setOn(){
        //hubConnection关闭回调
        hubConnection.onClosed(new OnClosedCallback() {
            @Override
            public void invoke(Exception exception) {
                connectStatus = false;
                isRunning = false;
            }
        });

        //消息封装，多余一个参数的封装城json，方便后面提取
        //一个参数的，直接放到obj中，在接收的地方根据类型来处理
        hubConnection.on("Auth",(message)->{
            Message msg = new Message();
            msg.what = AUTH_NUM;
            msg.obj = message;
            MessageReceived(msg);
            Log.i(TAG,"Auth:"+message.token+"\n"+message.info+"\n"+message.ok+"\n"+message.onlineClients);
        },AuthResponse.class);
        //被抛弃使用的方法
        hubConnection.on("Echo",(message)->{
            Message message1 = new Message();
            message1.what = ECHO_NUM;
            message1.obj = message;
            MessageReceived(message1);
            },String.class);

        hubConnection.on("Receive", (seq,data)->{
            Message Msg = new Message();
            Msg.what = RECEIVE_NUM;
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("seq",seq);
                jsonObject.put("data",data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Msg.obj = jsonObject;
            MessageReceived(Msg);
        },Long.class,String.class);

        hubConnection.on("Kickout", (reason)->{
            Message msg = new Message();
            msg.what = KICKOUT_NUM;
            msg.obj = reason;
            MessageReceived(msg);
//            Log.i(TAG,"kickout"+mes);
        },Object.class);
        hubConnection.on("CheckAuth", (mes)->{
            Message msg = new Message();
            msg.what = CHECKAUTH_NUM;
            msg.obj = mes;
            MessageReceived(msg);
            Log.i(TAG,"CheckAuth"+mes);
        },String.class);
        hubConnection.on("OtherConnectionChanged",(mes)->{
            Log.i(TAG,"Other"+mes);
            Message msg = new Message();
            msg.what = OTHERCONNECTIONCHANGED_NUM;
            msg.obj = mes;
            MessageReceived(msg);
        },String.class);
    }

    private void MessageReceived(Message msg){
        lastRecvTime = System.currentTimeMillis()/1000;
        //todo 传递给ReliableClient
        //使用handler
        Runnable receiverRun = new Runnable() {
            @Override
            public void run() {
                receiveHandler.sendMessage(msg);
            }
        };
        new Thread(receiverRun).start();
    }

//    private void Ack(Long seq) {
//        hubConnection.send("Ack",seq);
//    }

    private void heartCheck(){
        Log.i(TAG,"wow this is heartCheck");
        new Thread(runnableHeart).start();
    }

    private Runnable runnableHeart = new Runnable() {
        @Override
        public void run() {
            while(isRunning){
                long ping = System.currentTimeMillis()/1000;
                //发送心跳包
                try{
                    hubConnection.send("Echo",String.valueOf(ping));
                }catch (Exception e){
                    connectStatus = false;
                }
                //心跳延时
                try {
                    Thread.sleep(heartDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //最后一次接收消息时间小于发送心跳时间，
                //起码在心跳时间内，没有收到包。
                if(lastRecvTime < ping){
                    long delay = System.currentTimeMillis()/1000 - ping;
                    //时间差大于重连时间的时候，判定为超时，连接状态置为false
                    if(delay > KeepAliveTimeOutSecond){
                        connectStatus = false;
                    }else {
                        connectStatus = true;
                    }
                }else {
                    connectStatus = true;
                }
            }
        }
    };

    /**
     * 开放的三个方法
     * */
    public void send(String method,Object... message){
        Log.i(TAG,"message:\n"+message+"   method"+method);
        try{
            hubConnection.send(method,message);
        }catch (Exception e){
            connectStatus = false;
        }

    }

    public void stopConnect(){
        isRunning = false;
        connectStatus = false;
        hubConnection.stop();
    }

    public void startConnect(){
        Log.i(TAG,"start connect this message from SignalRSession");
        hubConnection = HubConnectionBuilder.create(url)
                .build();
        setOn();
        hubConnection.start().blockingAwait();
        heartCheck();
        isRunning = true;
    }

}
