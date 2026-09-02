package com.leang.authservice.service;

import com.leang.authservice.enums.DeliveryOption;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.PaymentProfile;
import com.leang.authservice.util.FulfillmentValidator;
import org.springframework.stereotype.Component;

@Component
public class FulfillmentApplier {

    public void applyToOrder(Order order, FulfillmentValidator.FulfillmentInput input) {
        order.setFulfillment(FulfillmentValidator.fulfillmentMethod(input.deliveryOption()));
        order.setCustomerName(trim(input.fullName()));
        order.setContactNumber(FulfillmentValidator.normalizePhone(input.contactNumber()));
        applyLocationFields(order, input);
    }

    public void applyToProfile(PaymentProfile profile, FulfillmentValidator.FulfillmentInput input) {
        profile.setDeliveryOption(input.deliveryOption());
        profile.setFullName(trim(input.fullName()));
        profile.setContactNumber(FulfillmentValidator.normalizePhone(input.contactNumber()));
        applyLocationFields(profile, input);
    }

    private void applyLocationFields(Order order, FulfillmentValidator.FulfillmentInput input) {
        if (input.deliveryOption() == DeliveryOption.PICKUP) {
            order.setDeliveryAddress(null);
            order.setLatitude(null);
            order.setLongitude(null);
            order.setProvince(null);
            order.setDistrict(null);
            order.setCommune(null);
            order.setPlaceId(null);
            order.setFormattedAddress(null);
            order.setDeliveryInstructions(null);
            order.setPickupNotes(trimOrNull(input.pickupNotes()));
            return;
        }
        order.setDeliveryAddress(primaryDeliveryLabel(input));
        order.setLatitude(input.latitude());
        order.setLongitude(input.longitude());
        order.setProvince(trimOrNull(input.province()));
        order.setDistrict(trimOrNull(input.district()));
        order.setCommune(trimOrNull(input.commune()));
        order.setPlaceId(trimOrNull(input.placeId()));
        order.setFormattedAddress(trimOrNull(input.formattedAddress()));
        order.setDeliveryInstructions(trimOrNull(input.deliveryInstructions()));
        order.setPickupNotes(null);
    }

    private void applyLocationFields(PaymentProfile profile, FulfillmentValidator.FulfillmentInput input) {
        if (input.deliveryOption() == DeliveryOption.PICKUP) {
            profile.setDeliveryAddress(null);
            profile.setLatitude(null);
            profile.setLongitude(null);
            profile.setProvince(null);
            profile.setDistrict(null);
            profile.setCommune(null);
            profile.setPlaceId(null);
            profile.setFormattedAddress(null);
            profile.setDeliveryInstructions(null);
            profile.setPickupNotes(trimOrNull(input.pickupNotes()));
            return;
        }
        profile.setDeliveryAddress(primaryDeliveryLabel(input));
        profile.setLatitude(input.latitude());
        profile.setLongitude(input.longitude());
        profile.setProvince(trimOrNull(input.province()));
        profile.setDistrict(trimOrNull(input.district()));
        profile.setCommune(trimOrNull(input.commune()));
        profile.setPlaceId(trimOrNull(input.placeId()));
        profile.setFormattedAddress(trimOrNull(input.formattedAddress()));
        profile.setDeliveryInstructions(trimOrNull(input.deliveryInstructions()));
        profile.setPickupNotes(null);
    }

    private static String primaryDeliveryLabel(FulfillmentValidator.FulfillmentInput input) {
        String notes = trimOrNull(input.deliveryAddress());
        if (notes != null) {
            return notes;
        }
        return trimOrNull(input.formattedAddress());
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
