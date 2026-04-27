package com.leang.authservice.service;

public interface NotificationService {

    void sendMessageToAllUsers(String message);

    void sendMessageToUser(String userId, String message);
}
