package com.leang.authservice.service;

import com.leang.authservice.model.dto.request.PaymentProfileRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.PaymentProfileResponse;

import java.util.UUID;

public interface PaymentProfileService {
    PaymentProfileResponse create(PaymentProfileRequest request);
    PaymentProfileResponse update(UUID id, PaymentProfileRequest request);
    PaymentProfileResponse getById(UUID id);
    ApiResponseWithPagination<PaymentProfileResponse> getAll(int page, int size);
    void delete(UUID id);
}
