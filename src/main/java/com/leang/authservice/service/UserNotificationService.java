package com.leang.authservice.service;

import com.leang.authservice.model.dto.request.UserNotificationCreateRequest;
import com.leang.authservice.model.dto.request.UserNotificationUpdateRequest;
import com.leang.authservice.model.dto.response.UserNotificationResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.Payment;

import java.util.List;
import java.util.UUID;

public interface UserNotificationService {

    void notifyPaymentSuccessOnce(Order order, Payment payment);

    List<UserNotificationResponse> listMine();

    UserNotificationResponse getMine(UUID id);

    UserNotificationResponse createMine(UserNotificationCreateRequest request);

    UserNotificationResponse updateMine(UUID id, UserNotificationUpdateRequest request);

    void deleteMine(UUID id);
}
