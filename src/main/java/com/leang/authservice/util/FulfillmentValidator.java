package com.leang.authservice.util;

import com.leang.authservice.enums.DeliveryOption;
import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.exception.ConflictException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates pickup vs delivery fulfillment before payment / QR.
 */
public final class FulfillmentValidator {

    private static final double CAMBODIA_LAT_MIN = 10.0;
    private static final double CAMBODIA_LAT_MAX = 15.0;
    private static final double CAMBODIA_LNG_MIN = 102.0;
    private static final double CAMBODIA_LNG_MAX = 108.0;

    /** Cambodia mobile: +855 / 855 / 0 + 8–9 digits. */
    private static final Pattern KH_PHONE = Pattern.compile("^(\\+855|855|0)?[1-9]\\d{7,8}$");

    private FulfillmentValidator() {
    }

    public static void validateAndThrow(FulfillmentInput input) {
        Map<String, String> errors = validate(input);
        if (!errors.isEmpty()) {
            throw new BadRequestException("Fulfillment validation failed", errors);
        }
    }

    public static void assertActiveCartStatus(String status) {
        if (status == null) {
            throw new ConflictException("Order is not open for checkout", Map.of());
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("pending") && !normalized.equals("processing")) {
            throw new ConflictException("Order is not in pending or processing state", Map.of("status", status));
        }
    }

    public static Map<String, String> validate(FulfillmentInput input) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (input.deliveryOption() == null) {
            errors.put("deliveryOption", "deliveryOption is required");
            return errors;
        }

        String fullName = input.fullName() == null ? "" : input.fullName().trim();
        if (fullName.length() < 2 || fullName.length() > 100) {
            errors.put("fullName", "fullName must be between 2 and 100 characters");
        }

        String phone = normalizePhone(input.contactNumber());
        if (phone.isEmpty()) {
            errors.put("contactNumber", "contactNumber is required");
        } else if (!KH_PHONE.matcher(phone).matches()) {
            errors.put("contactNumber", "Invalid Cambodia phone number");
        }

        if (input.deliveryOption() == DeliveryOption.PICKUP) {
            if (input.latitude() != null || input.longitude() != null) {
                errors.put("latitude", "latitude and longitude must be null for PICKUP");
            }
        } else if (input.deliveryOption() == DeliveryOption.DELIVERY) {
            if (input.latitude() == null || input.longitude() == null) {
                errors.put("latitude", "latitude and longitude are required for DELIVERY");
            } else {
                if (!inCambodia(input.latitude(), input.longitude())) {
                    errors.put("latitude", "Delivery location must be within Cambodia");
                    errors.put("longitude", "Delivery location must be within Cambodia");
                }
            }
            String notes = input.deliveryAddress();
            if ((notes == null || notes.isBlank())
                    && (input.formattedAddress() == null || input.formattedAddress().isBlank())) {
                errors.put("deliveryAddress", "deliveryAddress or formattedAddress is required for DELIVERY");
            }
        }

        return errors;
    }

    public static String normalizePhone(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[\\s\\-()]", "");
    }

    public static boolean inCambodia(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return latitude >= CAMBODIA_LAT_MIN && latitude <= CAMBODIA_LAT_MAX
                && longitude >= CAMBODIA_LNG_MIN && longitude <= CAMBODIA_LNG_MAX;
    }

    public static String fulfillmentMethod(DeliveryOption option) {
        return option == null ? null : option.name().toLowerCase(Locale.ROOT);
    }

    public record FulfillmentInput(
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
            String pickupNotes
    ) {
    }
}
