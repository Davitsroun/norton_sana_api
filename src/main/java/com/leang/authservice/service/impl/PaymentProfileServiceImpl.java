package com.leang.authservice.service.impl;

import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.request.PaymentProfileRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.PaymentProfileResponse;
import com.leang.authservice.model.entity.PaymentProfile;
import com.leang.authservice.repository.PaymentProfileRepository;
import com.leang.authservice.service.CurrentUserService;
import com.leang.authservice.service.FulfillmentApplier;
import com.leang.authservice.service.OrderFulfillmentService;
import com.leang.authservice.service.PaymentProfileService;
import com.leang.authservice.service.SavedLocationFulfillmentResolver;
import com.leang.authservice.util.FulfillmentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentProfileServiceImpl implements PaymentProfileService {

    private final PaymentProfileRepository paymentProfileRepository;
    private final CurrentUserService currentUserService;
    private final FulfillmentApplier fulfillmentApplier;
    private final OrderFulfillmentService orderFulfillmentService;
    private final SavedLocationFulfillmentResolver savedLocationFulfillmentResolver;

    @Override
    @Transactional
    public PaymentProfileResponse create(PaymentProfileRequest request) {
        FulfillmentValidator.FulfillmentInput input = resolve(request);
        FulfillmentValidator.validateAndThrow(input);
        PaymentProfile profile = PaymentProfile.builder()
                .userId(currentUserId())
                .build();
        fulfillmentApplier.applyToProfile(profile, input);
        PaymentProfile saved = paymentProfileRepository.save(profile);
        orderFulfillmentService.syncOpenOrderFromProfile(currentUserId(), input);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentProfileResponse update(UUID id, PaymentProfileRequest request) {
        FulfillmentValidator.FulfillmentInput input = resolve(request);
        FulfillmentValidator.validateAndThrow(input);
        PaymentProfile existing = findOwned(id);
        fulfillmentApplier.applyToProfile(existing, input);
        PaymentProfile saved = paymentProfileRepository.save(existing);
        orderFulfillmentService.syncOpenOrderFromProfile(currentUserId(), input);
        return toResponse(saved);
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

    private FulfillmentValidator.FulfillmentInput resolve(PaymentProfileRequest request) {
        return savedLocationFulfillmentResolver.resolve(
                request.deliveryOption(),
                request.fullName(),
                request.contactNumber(),
                request.deliveryAddress(),
                request.latitude(),
                request.longitude(),
                request.province(),
                request.district(),
                request.commune(),
                request.placeId(),
                request.formattedAddress(),
                request.deliveryInstructions(),
                request.pickupNotes(),
                request.savedLocationId()
        );
    }

    private UUID currentUserId() {
        return UUID.fromString(currentUserService.keycloakSub());
    }

    private PaymentProfile findOwned(UUID id) {
        return paymentProfileRepository.findByPaymentProfileIdAndUserId(id, currentUserId())
                .orElseThrow(() -> new NotFoundException("Payment profile not found"));
    }

    private PaymentProfileResponse toResponse(PaymentProfile profile) {
        return new PaymentProfileResponse(
                profile.getPaymentProfileId(),
                profile.getDeliveryOption(),
                profile.getFullName(),
                profile.getContactNumber(),
                profile.getDeliveryAddress(),
                profile.getLatitude(),
                profile.getLongitude(),
                profile.getProvince(),
                profile.getDistrict(),
                profile.getCommune(),
                profile.getPlaceId(),
                profile.getFormattedAddress(),
                profile.getDeliveryInstructions(),
                profile.getPickupNotes(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
