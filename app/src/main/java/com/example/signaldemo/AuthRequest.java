package com.example.signaldemo;

class AuthRequest{
    String UserId;
    int ClientType;
    String Token;
    int version;
    public AuthRequest(int clientType, String token, String userId, int version){
        this.UserId = userId;
        this.ClientType = clientType;
        this.Token = token;
        this.version = version;
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
