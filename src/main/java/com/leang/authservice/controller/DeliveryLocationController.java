package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.SavedDeliveryLocationRequest;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.dto.response.SavedDeliveryLocationResponse;
import com.leang.authservice.service.SavedDeliveryLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery-locations")
@RequiredArgsConstructor
@Tag(name = "DeliveryLocation")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryLocationController extends BaseResponse {

    private final SavedDeliveryLocationService savedDeliveryLocationService;

    @Operation(summary = "List my saved delivery locations (max 3, default first)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedDeliveryLocationResponse>>> list() {
        return responseEntity(true, "Delivery locations retrieved successfully.", HttpStatus.OK,
                savedDeliveryLocationService.listMine());
    }

    @Operation(summary = "Save a delivery location (max 3 per user)")
    @PostMapping
    public ResponseEntity<ApiResponse<SavedDeliveryLocationResponse>> create(
            @Valid @RequestBody SavedDeliveryLocationRequest request
    ) {
        return responseEntity(true, "Delivery location saved successfully.", HttpStatus.CREATED,
                savedDeliveryLocationService.create(request));
    }

    @Operation(summary = "Update a saved delivery location")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavedDeliveryLocationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SavedDeliveryLocationRequest request
    ) {
        return responseEntity(true, "Delivery location updated successfully.", HttpStatus.OK,
                savedDeliveryLocationService.update(id, request));
    }

    @Operation(summary = "Delete a saved delivery location")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        savedDeliveryLocationService.delete(id);
        return responseEntity(true, "Delivery location deleted successfully.", HttpStatus.OK);
    }

    @Operation(summary = "Set a saved delivery location as default")
    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<SavedDeliveryLocationResponse>> setDefault(@PathVariable UUID id) {
        return responseEntity(true, "Default delivery location updated.", HttpStatus.OK,
                savedDeliveryLocationService.setDefault(id));
    }
}
