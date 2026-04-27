package com.leang.authservice.service.impl;

import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.exception.ForbiddenException;
import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.request.FavoriteBrandCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Brand;
import com.leang.authservice.model.entity.FavoriteBrand;
import com.leang.authservice.repository.BrandRepository;
import com.leang.authservice.repository.FavoriteBrandRepository;
import com.leang.authservice.service.FavoriteBrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteBrandServiceImpl implements FavoriteBrandService {

    private final FavoriteBrandRepository favoriteBrandRepository;
    private final BrandRepository brandRepository;

    private UUID currentUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getClaimAsString("sub"));
    }

    @Override
    public FavoriteBrand create(FavoriteBrandCreateRequest dto) {
        UUID userId = currentUserId();

        if (dto.getBrandId() == null) {
            throw new BadRequestException("brandId is required");
        }

        if (favoriteBrandRepository.existsByUserIdAndBrand_BrandId(userId, dto.getBrandId())) {
            throw new BadRequestException("Brand already in favorites");
        }

        Brand brand = brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new NotFoundException("Brand not found"));

        FavoriteBrand favoriteBrand = FavoriteBrand.builder()
                .userId(userId)
                .brand(brand)
                .build();

        return favoriteBrandRepository.save(favoriteBrand);
    }

    @Override
    public FavoriteBrand getById(UUID id) {
        FavoriteBrand favoriteBrand = favoriteBrandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Favorite brand not found"));

        if (!favoriteBrand.getUserId().equals(currentUserId())) {
            throw new ForbiddenException("You do not have permission to access this resource");
        }

        return favoriteBrand;
    }

    @Override
    public ApiResponseWithPagination<FavoriteBrand> getAll(int page, int size) {
        UUID userId = currentUserId();
        PageRequest pageable = PageRequest.of(page, size);
        Page<FavoriteBrand> favoriteBrandPage = favoriteBrandRepository.findByUserId(userId, pageable);
        return ApiResponseWithPagination.itemsAndPaginationResponse(
                favoriteBrandPage.getContent(),
                page,
                size,
                (int) favoriteBrandPage.getTotalElements()
        );
    }

    @Override
    public void delete(UUID id) {
        FavoriteBrand favoriteBrand = favoriteBrandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Favorite brand not found"));

        if (!favoriteBrand.getUserId().equals(currentUserId())) {
            throw new ForbiddenException("You do not have permission to delete this resource");
        }

        favoriteBrandRepository.delete(favoriteBrand);
    }
}

