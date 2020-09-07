package com.example.signaldemo;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.example.signaldemo.database.AppDatabase;
import com.example.signaldemo.database.MessageData;
import com.example.signaldemo.recordDatabase.recordData;
import com.example.signaldemo.recordDatabase.recordDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ReliableClient {
    private final int ECHO_NUM      =  1001;
    private final int RECEIVE_NUM   =  1002;
    private final int KICKOUT_NUM   =  1003;
    private final int AUTH_NUM      =  1004;
    private final int CHECKAUTH_NUM =  1005;
    private final int OTHERCONNECTIONCHANGED_NUM = 1006;

    private Hashtable<Long,MessageData> hTable=new Hashtable<Long,MessageData>();
    private long curRecvSeq = -1;
    private long processTime = System.currentTimeMillis();

    private Context context;
    private String url;
    private SignalRChannel signalRChannel;
    //登录信息，用于重连时自动登录。
    private AuthRequest authMessage = null;
    private String TAG = getClass().getSimpleName();
    //创建消息队列
    private Queue<SendMessage> sendMessageQueue = new LinkedList<SendMessage>();
    //连接状态属性
   private volatile boolean isRunning = true;

    //用于操作数据库
    private final AppDatabase db;
    private final recordDatabase recordDb;

    //日志
    private LogFile logFile;

    public ReliableClient(String url1, Context context) {
        this.url = url1;
        this.context = context;
        //创建数据库,如果存在不会重复创建
        db = Room.databaseBuilder(context,
                AppDatabase.class, "database-name").build();
        recordDb = Room.databaseBuilder(context,
                recordDatabase.class,"database-name1").build();
        loadData();
        logFile = new LogFile(context);
        Thread t = new Thread(runnableSend);
        t.start();
    }

    private void loadData() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(recordDb.recordDao().databaseCount()<1){
                    //数据库没有数据，设置为默认值
                    curRecvSeq = -1;
                    authMessage = null;
                    Log.i(TAG,"load <1 ");
                }else if(recordDb.recordDao().databaseCount() == 1){
                    //数据库一条数据，取这条数据
                    recordData messageData = recordDb.recordDao().getRecord();
                    curRecvSeq = messageData.curRecvSeq;
                    authMessage = new AuthRequest(messageData.ClientType,messageData.Token,messageData.UserId,messageData.Version);
                    if(authMessage.ClientType == -1){
                        authMessage = null;
                    }
                    Log.i(TAG,"load = 1 "+curRecvSeq);
                }else {
                    Log.i(TAG,"qweq: "+recordDb.recordDao().databaseCount());
                    //数据库很多数据，取最后一条的数据
                    recordData messageData = recordDb.recordDao().getRecord();
                    curRecvSeq = messageData.curRecvSeq;
                    authMessage = new AuthRequest(messageData.ClientType,messageData.Token,messageData.UserId,messageData.Version);

                    recordDb.recordDao().deleteAll();
                    recordData record1 = new recordData();
                    record1.Token = authMessage.Token;
                    record1.curRecvSeq = messageData.curRecvSeq;
                    record1.Version = authMessage.version;
                    record1.ClientType = authMessage.ClientType;
                    record1.UserId = authMessage.UserId;
                    recordDb.recordDao().insertAll(record1);
                    if(authMessage.ClientType == -1){
                        authMessage = null;
                    }
                    Log.i(TAG,"load > 1 "+curRecvSeq);
                }
            }
        };
        new Thread(runnable).start();
        if(curRecvSeq != -1){
        //如果有操作记录，那么查询数据库，取出未处理的数据，发给flutter。
            List<MessageData> messageDataList = db.userDao().getAll();
            for(MessageData messageData : messageDataList){
                //未操作数据压入哈希表
                hTable.put(messageData.seq,messageData);
                curRecvSeq ++;
            }
        }
        Log.i(TAG,"load msg :"+curRecvSeq);
    }

    private Handler receiveHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case ECHO_NUM:
                    Log.i(TAG,"echo handler");
//                    time = nowTime();
//                    log = time + "    收到一条echo消息 \n" + msg.obj;
//                    logFile.saveLog(log);
                    break;
                case RECEIVE_NUM:
                    receiveMessage(msg);
                    Log.i(TAG,"receive handler");
                    break;
                case KICKOUT_NUM:
//                    time = nowTime();
//                    log = time + "    收到一条kickout消息 \n" + msg.obj;
//                    logFile.saveLog(log);
                    Log.i(TAG,"kickout handler");
                    break;
                case  AUTH_NUM:
//                    time = nowTime();
//                    log = time + "    收到一条auth消息 \n" + msg.obj;
//                    logFile.saveLog(log);
                    Log.i(TAG,"auth handler");
                    break;
                case CHECKAUTH_NUM:
//                    time = nowTime();
//                    log = time + "    收到一条kickout消息 \n" + msg.obj;
//                    logFile.saveLog(log);
                    Log.i(TAG,"checkAuth handler");
                    break;
                case OTHERCONNECTIONCHANGED_NUM:
//                    time = nowTime();
//                    log = time + "    收到一条kickout消息 \n" + msg.obj;
//                    logFile.saveLog(log);
                    Log.i(TAG,"OtherConnectionChanged handler");
                    break;
            }
        }
    };

    private String nowTime() {
        String timeStr = null;
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年-MM月dd日-HH时mm分ss秒 ");
        timeStr = dateFormat.format(date);
        return timeStr;
    }

    // 收到receive的消息后的处理
    // 无脑回ack，
    // 然后消息去重，
    // 存入数据库
    // 顺序化
    // 等待时间过长，取最小的处理。
    private void receiveMessage(Message msg) {
        try {
            JSONObject jsonObject = new JSONObject(String.valueOf(msg.obj));
            long seq = Long.valueOf(String.valueOf(jsonObject.get("seq")));
            String data = jsonObject.getString("data");
//            signalRChannel.send("Ack",seq);
            signalRChannel.send("Ack",seq);
            if(curRecvSeq == -1){
                curRecvSeq = seq - 1;
            }
            if(hTable.containsKey(seq)){
                return;
            }else {
                long time = System.currentTimeMillis();
                MessageData msgData = new MessageData();
                msgData.data = data;
                msgData.seq = seq;
                msgData.ReceiveTime = time;
                insertToDatabase(msgData);
                hTable.put(seq,msgData);

                if(seq != (curRecvSeq + 1)){
                    Log.i(TAG,"丢包");
                    boolean isTimeOut = (System.currentTimeMillis() - processTime)/1000 > 5;
                    if(isTimeOut){
                        Log.i(TAG,"超时检测");
                        Log.i(TAG,"curRecvSeq: "+curRecvSeq+"    seq: "+seq);
                        long min = curRecvSeq+10000;
                        for(Long key:hTable.keySet()){
                            if(min>key){
                                min = key;
                            }
                        }
//
                        curRecvSeq = min - 1;
                    }
                }

//                if(seq == (curRecvSeq+1)){
                while (true){
                    MessageData messageData = hTable.get(curRecvSeq+1);
                    if(messageData==null){
                        break;
                    }
                    //发送给flutter
                    sendToFlutter(msgData);
//                        receiveMessageQueue.add(msgData);
                    //保存记录
                    processTime = System.currentTimeMillis();
                    saveInDatabase(curRecvSeq,authMessage);
                    String time1 = nowTime();
                    String log = time1 + "    收到一条receive消息 \n seq: " + msgData.seq + " \nmsg: "+ msgData.data ;
                    logFile.saveLog(log);
                    Log.i(TAG,"gogogogogo :"+curRecvSeq);
                    curRecvSeq ++;
                }
//                }
            }

            Log.i(TAG,"seqqqqqqqqqqqqqqqqqqqqqq:\n"+seq);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //todo 发送消息给flutter
    /**
     * 1. 发送消息给flutter    （handler发送eventchannel）
     * 2. 处理完之后删除这条数据 （下次不处理这条数据）
     * 3. 写入log文件
     * */
    private void sendToFlutter(MessageData msgData) {

    }

    private void saveInDatabase(long curRecvSeq, AuthRequest authMessage) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                recordData record = new recordData();
                record.curRecvSeq = curRecvSeq;
                record.ClientType = authMessage.ClientType;
                record.Token = authMessage.Token;
                record.UserId = authMessage.UserId;
                record.Version = authMessage.version;
                if(recordDb.recordDao().databaseCount()<1){
                    recordDb.recordDao().insertAll(record);
                }else if(recordDb.recordDao().databaseCount() == 1){
                    recordDb.recordDao().Update(record);
                }else {
                    recordDb.recordDao().deleteAll();
                    recordDb.recordDao().insertAll(record);
                }
            }
        };
        new Thread(runnable).start();
    }

    private void insertToDatabase(MessageData message1) {
//        db.userDao().insertAll(message1);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    db.userDao().insertAll(message1);
                }catch (Exception e){
                    Log.i(TAG,"insert error: "+e);
                }
            }
        };
        new Thread(runnable).start();
    }

    //test 用于查询是否成功存入数据库
    public void selectDatabase(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    List<MessageData> messageDataList = db.userDao().getAll();
                    for(MessageData messageData : messageDataList){
                        Log.i(TAG,"database: \nseq: "+messageData.seq+"\ndata: "+messageData.data+"\nhadRead: "+messageData.ReceiveTime+"\ntime: "+messageData.ReceiveTime);
                    }
                }catch (Exception e){
                    Log.i(TAG,"select error: "+e);
                }
            }
        };
        new Thread(runnable).start();
    }
    public void selectDatabase1(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    recordData records = recordDb.recordDao().getRecord();
                    Log.i(TAG,"Records userId: " +records.UserId
                        + "\n curRecvSeq: " +records.curRecvSeq + "\n token: "+records.Token);
                }catch (Exception e){
                    Log.i(TAG,"select error: "+e);
                }
            }
        };
        new Thread(runnable).start();
    }


    public boolean isConected(){
        return signalRChannel.isConnected();
    }
    /***/
    private Runnable runnableSend = new Runnable() {
        @Override
        public void run() {
            while(isRunning){
                try {
//                    connectStatus = signalSession.isConnectStatus();
                    //刷新连接状态
                    if(signalRChannel == null || !signalRChannel.isConnected()){
                        try{
                            reConnect();
                        }catch (Exception e){
                            e.printStackTrace();
                            continue;
                        }
                    }
                    while(!sendMessageQueue.isEmpty()){
                        //发送消息
                        SendMessage sendMessage = sendMessageQueue.poll();
                        signalRChannel.send(sendMessage.method, sendMessage.message);
                    }
                    if(!logFile.fileStatus){
                        logFile.openLog();
                    }
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
        if(signalRChannel != null){
            signalRChannel.stopConnect();
        }
        signalRChannel = new SignalRChannel(url,receiveHandler);
        signalRChannel.startConnect();
        if(authMessage!=null){
            signalRChannel.send("Auth",authMessage);
        }
    }

    /**
     * 开放给外部的四个方法
     * 1. 发送消息 send
     * 2. 登录 LogIn
     * 3. 登出 LogOut
     * 4. 发送接收的消息走handler
     * */

    //发送消息
    public void send(String method,Object... messages){
        /**
         * queue
         * */
        if(method.equals("Echo")){
            long time = System.currentTimeMillis()/1000;
            signalRChannel.send(method,String.valueOf(time/1000));
        }else {
            SendMessage sendMessage = new SendMessage(method,messages);
            sendMessageQueue.offer(sendMessage);
        }
    }

    //登录
    public void LogIn(AuthRequest authRequest){
        this.authMessage = authRequest;
        //todo: write file
//        signalRChannel.send("Auth",authMessage);

    }

    //登出
    public void LogOut(){
        authMessage = null;
        if(signalRChannel != null){
            signalRChannel.stopConnect();
        }
        if(logFile.fileStatus){
            logFile.closeLog();
        }
    }

}
