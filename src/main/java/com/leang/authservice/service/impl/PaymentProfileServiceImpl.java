package com.leang.authservice.service.impl;

import com.leang.authservice.enums.DeliveryOption;
import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.request.PaymentProfileRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.PaymentProfileResponse;
import com.leang.authservice.model.entity.PaymentProfile;
import com.leang.authservice.repository.PaymentProfileRepository;
import com.leang.authservice.service.CurrentUserService;
import com.leang.authservice.service.PaymentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentProfileServiceImpl implements PaymentProfileService {

    private final PaymentProfileRepository paymentProfileRepository;
    private final CurrentUserService currentUserService;

    @Override
    public PaymentProfileResponse create(PaymentProfileRequest request) {
        validateRequest(request);
        PaymentProfile profile = PaymentProfile.builder()
                .userId(currentUserId())
                .deliveryOption(request.deliveryOption())
                .fullName(request.fullName().trim())
                .contactNumber(request.contactNumber().trim())
                .deliveryAddress(normalizeAddress(request.deliveryAddress()))
                .build();
        return toResponse(paymentProfileRepository.save(profile));
    }

    @Override
    public PaymentProfileResponse update(UUID id, PaymentProfileRequest request) {
        validateRequest(request);
        PaymentProfile existing = findOwned(id);
        existing.setDeliveryOption(request.deliveryOption());
        existing.setFullName(request.fullName().trim());
        existing.setContactNumber(request.contactNumber().trim());
        existing.setDeliveryAddress(normalizeAddress(request.deliveryAddress()));
        return toResponse(paymentProfileRepository.save(existing));
    }

    @Override
    public PaymentProfileResponse getById(UUID id) {
        return toResponse(findOwned(id));
    }

    @Override
    public ApiResponseWithPagination<PaymentProfileResponse> getAll(int page, int size) {
        Page<PaymentProfileResponse> data = paymentProfileRepository
                .findByUserId(currentUserId(), PageRequest.of(page, size))
                .map(this::toResponse);
        return ApiResponseWithPagination.itemsAndPaginationResponse(
                data.getContent(), page, size, (int) data.getTotalElements()
        );
    }

    @Override
    public void delete(UUID id) {
        paymentProfileRepository.delete(findOwned(id));
    }

    private UUID currentUserId() {
        return UUID.fromString(currentUserService.keycloakSub());
    }

    private PaymentProfile findOwned(UUID id) {
        return paymentProfileRepository.findByPaymentProfileIdAndUserId(id, currentUserId())
                .orElseThrow(() -> new NotFoundException("Payment profile not found"));
    }

    private void validateRequest(PaymentProfileRequest request) {
        if (request.deliveryOption() == DeliveryOption.DELIVERY &&
                (request.deliveryAddress() == null || request.deliveryAddress().isBlank())) {
            throw new BadRequestException("deliveryAddress is required for DELIVERY option");
        }
    }

    private String normalizeAddress(String address) {
        return address == null || address.isBlank() ? null : address.trim();
    }

    private PaymentProfileResponse toResponse(PaymentProfile profile) {
        return new PaymentProfileResponse(
                profile.getPaymentProfileId(),
                profile.getDeliveryOption(),
                profile.getFullName(),
                profile.getContactNumber(),
                profile.getDeliveryAddress(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
