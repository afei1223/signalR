package com.example.signaldemo;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import com.example.signaldemo.database.AppDatabase;
import com.example.signaldemo.database.MessageData;
import com.microsoft.signalr.Action1;
import com.microsoft.signalr.Action2;
import com.microsoft.signalr.OnClosedCallback;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TimerTask;

public class LongConnect {
    Context context;
    String url;
    SignalSession signalSession;
    //登录信息，用于重连时自动登录。
    private Object authMessage = null;
    String TAG = getClass().getSimpleName();
    //创建消息队列
    private Queue<SendMessage> sendMessageQueue = new LinkedList<SendMessage>();
    private Queue<ReceiveMessage> receiveMessageQueue = new LinkedList<ReceiveMessage>();
    //连接状态属性
    boolean connectStatus;
    volatile boolean isRunning = true;

    //用于操作数据库
    final AppDatabase db;

    Action2<Long,String> receiveCallBack = (seq, message)->{
        Log.i(TAG,"database");

        ReceiveMessage receiveMessage = new ReceiveMessage("receive",message);
        receiveMessageQueue.add(receiveMessage);

        /**
         * 存数据库，改换形式
         * */
        MessageData message1 = new MessageData();
        message1.data = message;
        message1.seq = seq;
        message1.hadRead = false;
//        insertToDatabase(message1);
    };

    private void insertToDatabase(MessageData message1) {
        db.userDao().insertAll(message1);
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    db.userDao().insertAll(message1);
//                    signalSession.send("Ack",message1.seq);
//                }catch (Exception e){
//                    Log.i(TAG,"insert error: "+e);
//                }
//            }
//        };
//        new Thread(runnable).start();
    }

    public void selectDatabase(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    List<MessageData> messageDataList = db.userDao().getAll();
                    for(MessageData messageData : messageDataList){
                        Log.i(TAG,"database: \nseq: "+messageData.seq+"\ndata: "+messageData.data+"\nhadRead"+messageData.hadRead);
                    }
                }catch (Exception e){
                    Log.i(TAG,"insert error: "+e);
                }
            }
        };
        new Thread(runnable).start();
    }

    public LongConnect(String url1, Context context) {
        this.url = url1;
        this.context = context;
        //创建数据库,如果存在不会重复创建
        db = Room.databaseBuilder(context,
                AppDatabase.class, "database-name").build();
        //signalSession = new SignalSession(url,receiveCallBack);
//        try{
//            signalSession = new SignalSession(url,receiveCallBack);
//            startConnect();
//            Log.i(TAG,"1connectStatus status: "+connectStatus);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
        Thread t = new Thread(runnableSend);
        t.start();
    }

    /***/
    Runnable runnableSend = new Runnable() {
        @Override
        public void run() {
            while(isRunning){
                try {
//                    connectStatus = signalSession.isConnectStatus();
                    //刷新连接状态
                    if(signalSession == null || !signalSession.isConnectStatus()){
                        try{
                            reConnect();
                        }catch (Exception e){
                            e.printStackTrace();
                            continue;
                        }
                        Log.i(TAG,"2connectStatus status: ");
                    }
                    while(!sendMessageQueue.isEmpty()){
                        //发送消息
                        SendMessage sendMessage = sendMessageQueue.poll();
                        signalSession.send(sendMessage.method, sendMessage.message);
                        Log.i(TAG,"sendMessageQueue is not null  !!!"+sendMessage.method+"\n"+sendMessage.message);
                    }
                    connectStatus = signalSession.isConnectStatus();
//                    if(!receiveMessageQueue.isEmpty()){
//                        //向flutter发送消息。
//                        receiveMessageQueue.poll();
//                        Log.i(TAG,"receiveMessageQueue is not null  !!!");
//                    }
                    /**
                     * 1. get连接状态 (finish)
                     * 2. 清空队列，（send）
                     * 3. 重连
                     *
                     * 1. 塞心跳(session) finish
                     * 2. 检查连接状态(session) finish
                     * */
//                    long now = System.currentTimeMillis();
//                    if(now > nextPingTime){
//                        //send ping
//                        nextPingTime = now + 5000;
//                    }
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    /**
     * 重连机制
     * 1. 调用登出，断开连接
     * 2. 重新建立连接
     * 3. 如果是在登录状态下，重新登录
     * */
    private void reConnect() {
        if(signalSession != null){
            signalSession.stopConnect();
        }
        signalSession = new SignalSession(url,receiveCallBack);
        signalSession.startConnect();
        if(authMessage!=null){
            signalSession.send("Auth",authMessage);
        }
    }

    /**
     * 开放给外部的四个方法
     * 1. 发送消息 send
     * 2. 登录 LogIn
     * 3. 登出 LogOut
     * */

    //发送消息
    public void send(String method,Object... messages){
        /**
         * queue
         * */
        if(method.equals("Echo")){
            long time = System.currentTimeMillis()/1000;
            signalSession.send(method,String.valueOf(time/1000));
        }else {
            SendMessage sendMessage = new SendMessage(method,messages);
            sendMessageQueue.offer(sendMessage);
        }
    }

    //登录
    public void LogIn(AuthRequest authRequest){
        this.authMessage = authRequest;
        //todo: write file
        signalSession.send("Auth",authMessage);

    }

    //登出
    public void LogOut(){
        authMessage = null;
        if(signalSession != null){
            signalSession.stopConnect();
        }
    }

}
