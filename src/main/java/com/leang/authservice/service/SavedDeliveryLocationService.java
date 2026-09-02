package com.leang.authservice.service;

import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.exception.ForbiddenException;
import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.request.SavedDeliveryLocationRequest;
import com.leang.authservice.model.dto.response.SavedDeliveryLocationResponse;
import com.leang.authservice.model.entity.SavedDeliveryLocation;
import com.leang.authservice.repository.SavedDeliveryLocationRepository;
import com.leang.authservice.util.FulfillmentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedDeliveryLocationService {

    public static final int MAX_LOCATIONS_PER_USER = 3;

    private final SavedDeliveryLocationRepository repository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<SavedDeliveryLocationResponse> listMine() {
        return repository.findAllByUserIdOrderByDefaultLocationDescCreatedAtDesc(currentUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SavedDeliveryLocationResponse create(SavedDeliveryLocationRequest request) {
        UUID userId = currentUserId();
        validateLocation(request);
        if (repository.countByUserId(userId) >= MAX_LOCATIONS_PER_USER) {
            throw new BadRequestException(
                    "You can save up to 3 locations. Delete one first.",
                    Map.of("limit", String.valueOf(MAX_LOCATIONS_PER_USER))
            );
        }

        boolean makeDefault = Boolean.TRUE.equals(request.isDefault()) || repository.countByUserId(userId) == 0;
        if (makeDefault) {
            repository.clearDefaultsForUser(userId);
        }

        SavedDeliveryLocation saved = repository.save(SavedDeliveryLocation.builder()
                .userId(userId)
                .label(trim(request.label()))
                .deliveryAddress(trimOrNull(request.deliveryAddress()))
                .formattedAddress(trimOrNull(request.formattedAddress()))
                .latitude(request.latitude())
                .longitude(request.longitude())
                .province(trimOrNull(request.province()))
                .district(trimOrNull(request.district()))
                .commune(trimOrNull(request.commune()))
                .placeId(trimOrNull(request.placeId()))
                .deliveryInstructions(trimOrNull(request.deliveryInstructions()))
                .defaultLocation(makeDefault)
                .build());
        return toResponse(saved);
    }

    @Transactional
    public SavedDeliveryLocationResponse update(UUID id, SavedDeliveryLocationRequest request) {
        validateLocation(request);
        SavedDeliveryLocation existing = requireOwned(id);
        existing.setLabel(trim(request.label()));
        existing.setDeliveryAddress(trimOrNull(request.deliveryAddress()));
        existing.setFormattedAddress(trimOrNull(request.formattedAddress()));
        existing.setLatitude(request.latitude());
        existing.setLongitude(request.longitude());
        existing.setProvince(trimOrNull(request.province()));
        existing.setDistrict(trimOrNull(request.district()));
        existing.setCommune(trimOrNull(request.commune()));
        existing.setPlaceId(trimOrNull(request.placeId()));
        existing.setDeliveryInstructions(trimOrNull(request.deliveryInstructions()));

        if (Boolean.TRUE.equals(request.isDefault())) {
            repository.clearDefaultsForUser(existing.getUserId());
            existing.setDefaultLocation(true);
        } else if (Boolean.FALSE.equals(request.isDefault())) {
            existing.setDefaultLocation(false);
        }

        return toResponse(repository.save(existing));
    }

    @Transactional
    public void delete(UUID id) {
        SavedDeliveryLocation existing = requireOwned(id);
        boolean wasDefault = existing.isDefaultLocation();
        UUID userId = existing.getUserId();
        repository.delete(existing);
        if (wasDefault) {
            List<SavedDeliveryLocation> remaining =
                    repository.findAllByUserIdOrderByDefaultLocationDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                SavedDeliveryLocation next = remaining.get(0);
                next.setDefaultLocation(true);
                repository.save(next);
            }
        }
    }

    @Transactional
    public SavedDeliveryLocationResponse setDefault(UUID id) {
        SavedDeliveryLocation existing = requireOwned(id);
        repository.clearDefaultsForUser(existing.getUserId());
        existing.setDefaultLocation(true);
        return toResponse(repository.save(existing));
    }

    @Transactional(readOnly = true)
    public SavedDeliveryLocation requireOwned(UUID id) {
        UUID userId = currentUserId();
        return repository.findByIdAndUserId(id, userId)
                .orElseGet(() -> {
                    if (repository.existsById(id)) {
                        throw new ForbiddenException("You can only manage your own delivery locations");
                    }
                    throw new NotFoundException("Delivery location not found");
                });
    }

    private void validateLocation(SavedDeliveryLocationRequest request) {
        String label = request.label() == null ? "" : request.label().trim();
        if (label.isEmpty() || label.length() > 40) {
            throw new BadRequestException("label must be between 1 and 40 characters", Map.of("label", "invalid"));
        }
        if (request.latitude() == null || request.longitude() == null) {
            throw new BadRequestException("latitude and longitude are required", Map.of("latitude", "required"));
        }
        if (!FulfillmentValidator.inCambodia(request.latitude(), request.longitude())) {
            throw new BadRequestException(
                    "Delivery location must be within Cambodia",
                    Map.of("latitude", "outside Cambodia", "longitude", "outside Cambodia")
            );
        }
    }

    private UUID currentUserId() {
        return UUID.fromString(currentUserService.keycloakSub());
    }

    private SavedDeliveryLocationResponse toResponse(SavedDeliveryLocation location) {
        return new SavedDeliveryLocationResponse(
                location.getId(),
                location.getLabel(),
                location.getDeliveryAddress(),
                location.getFormattedAddress(),
                location.getLatitude(),
                location.getLongitude(),
                location.getProvince(),
                location.getDistrict(),
                location.getCommune(),
                location.getPlaceId(),
                location.getDeliveryInstructions(),
                location.isDefaultLocation(),
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
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
