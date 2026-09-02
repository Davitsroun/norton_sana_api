package com.leang.authservice.controller;

import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.dto.response.StockAlertItemResponse;
import com.leang.authservice.service.StockAlertService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/stock")
@RequiredArgsConstructor
@Tag(name = "AdminStock")
@SecurityRequirement(name = "bearerAuth")
public class AdminStockController extends BaseResponse {

    private final StockAlertService stockAlertService;

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<StockAlertItemResponse>>> listAlerts() {
        return responseEntity(true, "Stock alerts retrieved successfully.", HttpStatus.OK, stockAlertService.listAlerts());
    }
}
