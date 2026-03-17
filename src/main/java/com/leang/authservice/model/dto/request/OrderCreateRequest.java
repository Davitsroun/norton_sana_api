package com.leang.authservice.model.dto.request;

import com.leang.authservice.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateRequest {

    /**
     * Order status to initialize the order with.
     * Example: Status.PENDING
     */
    private Status status;
}

