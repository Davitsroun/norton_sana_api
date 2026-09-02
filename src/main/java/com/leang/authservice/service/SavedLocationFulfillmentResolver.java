package com.leang.authservice.service;

import com.leang.authservice.enums.DeliveryOption;
import com.leang.authservice.model.entity.SavedDeliveryLocation;
import com.leang.authservice.util.FulfillmentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Merges optional {@code savedLocationId} into fulfillment input for DELIVERY.
 * Explicit request fields override saved location values. Name/phone always come from the request.
 */
@Component
@RequiredArgsConstructor
public class SavedLocationFulfillmentResolver {

    private final SavedDeliveryLocationService savedDeliveryLocationService;

    public FulfillmentValidator.FulfillmentInput resolve(
            DeliveryOption deliveryOption,
            String fullName,
            String contactNumber,
            String deliveryAddress,
            Double latitude,
            Double longitude,
            String province,
            String district,
            String commune,
            String placeId,
            String formattedAddress,
            String deliveryInstructions,
            String pickupNotes,
            UUID savedLocationId
    ) {
        if (deliveryOption != DeliveryOption.DELIVERY || savedLocationId == null) {
            return new FulfillmentValidator.FulfillmentInput(
                    deliveryOption,
                    fullName,
                    contactNumber,
                    deliveryAddress,
                    latitude,
                    longitude,
                    province,
                    district,
                    commune,
                    placeId,
                    formattedAddress,
                    deliveryInstructions,
                    pickupNotes
            );
        }

        SavedDeliveryLocation saved = savedDeliveryLocationService.requireOwned(savedLocationId);
        return new FulfillmentValidator.FulfillmentInput(
                deliveryOption,
                fullName,
                contactNumber,
                firstNonBlank(deliveryAddress, saved.getDeliveryAddress()),
                latitude != null ? latitude : saved.getLatitude(),
                longitude != null ? longitude : saved.getLongitude(),
                firstNonBlank(province, saved.getProvince()),
                firstNonBlank(district, saved.getDistrict()),
                firstNonBlank(commune, saved.getCommune()),
                firstNonBlank(placeId, saved.getPlaceId()),
                firstNonBlank(formattedAddress, saved.getFormattedAddress()),
                firstNonBlank(deliveryInstructions, saved.getDeliveryInstructions()),
                pickupNotes
        );
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }
}
