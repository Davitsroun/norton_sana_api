package com.leang.authservice.model.dto.request;

import lombok.Data;

@Data
public class BrandCreateRequest {
    private String brandName;
    private String country;
}

