package com.leang.authservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponseWithPagination<T> {
    private List<T> items;
    private PaginationResponse paginationResponse;

    public static <T> ApiResponseWithPagination<T> itemsAndPaginationResponse(List<T> items, int page, int size, int total) {
        long totalPages = (total < 0) ? -1 : (long) Math.ceil((double) total / size);
        return ApiResponseWithPagination.<T>builder()
                .items(items)
                .paginationResponse(PaginationResponse.paginationToResponse(page, size, (int) totalPages, total))
                .build();
    }


}
