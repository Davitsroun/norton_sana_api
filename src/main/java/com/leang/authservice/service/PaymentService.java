package com.leang.authservice.service;

import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Payment;

import java.util.UUID;

public interface PaymentService {

    Payment create(Payment payment);

    Payment update(UUID id, Payment payment);

    void delete(UUID id);

    Payment getById(UUID id);

    ApiResponseWithPagination<Payment> getAll(int page, int size);
}

