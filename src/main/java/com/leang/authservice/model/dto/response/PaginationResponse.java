package com.leang.authservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaginationResponse {
    private Long totalElements;
    private Integer currentPage;
    private Integer pageSize;
    private Integer totalPages;


    public static <T> PaginationResponse paginationToResponse(int page, int size, int totalPages, int total) {
        return PaginationResponse.builder()
                .currentPage(page)
                .totalPages(totalPages)
                .totalElements((long) total)
                .pageSize(size)
                .build();
    }
}
