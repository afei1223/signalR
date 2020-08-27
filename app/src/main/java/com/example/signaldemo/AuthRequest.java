package com.example.signaldemo;

class AuthRequest{
    String UserId;
    int ClientType;
    String Token;
    public AuthRequest(int clientType,String token,String userId){
        this.UserId = userId;
        this.ClientType = clientType;
        this.Token = token;
    }
}

class SendMessage{
    String method;
    Object[] message;
    public SendMessage(String method, Object[] message){
        this.method = method;
        this.message = message;
    }
}

class ReceiveMessage{
    String whereFrom;
    String message;

    public ReceiveMessage(String whereFrom, String message) {
        this.whereFrom = whereFrom;
        this.message = message;
    }

}