package com.leang.authservice.controller;

import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.dto.response.OrderViewResponse;
import com.leang.authservice.service.CartMergeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart")
@SecurityRequirement(name = "bearerAuth")
public class CartController extends BaseResponse {

    private final CartMergeService cartMergeService;

    @Operation(summary = "Merge guest cookie cart into the authenticated user cart")
    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<OrderViewResponse>> merge(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OrderViewResponse merged = cartMergeService.mergeGuestCartIntoUser(request, response);
        return responseEntity(true, "Guest cart merged successfully.", HttpStatus.OK, merged);
    }
}
