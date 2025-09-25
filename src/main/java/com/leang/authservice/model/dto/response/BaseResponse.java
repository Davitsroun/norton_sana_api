package com.leang.authservice.model.dto.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;

public abstract class BaseResponse {

    // Don't Edit It
    public <T> ResponseEntity<ApiResponse<T>> responseEntity(Boolean success, String message, HttpStatus httpStatus, T payload) {

        ApiResponse<T> apiResponse = ApiResponse.<T>builder()
                .success(success)
                .message(message)
                .status(httpStatus)
                .payload(payload)
                .build();
        return new ResponseEntity<>(apiResponse,
                apiResponse.getStatus()
        );
    }

    //no payload
    public <T> ResponseEntity<ApiResponse<T>> responseEntity(Boolean success, String message, HttpStatus httpStatus) {

        ApiResponse<T> apiResponse = ApiResponse.<T>builder()
                .success(success)
                .message(message)
                .status(httpStatus)
                .build();
        return new ResponseEntity<>(apiResponse,
                apiResponse.getStatus()
        );
    }

    // no payload and status code
    public <T> ResponseEntity<ApiResponse<T>> responseEntity(Boolean success, String message) {

        ApiResponse<T> apiResponse = ApiResponse.<T>builder()
                .success(success)
                .message(message)
                .build();
        return new ResponseEntity<>(apiResponse,
                apiResponse.getStatus()
        );
    }


    // Don't Edit It
    public ResponseEntity<ProblemDetail> problemDetailResponseEntity(Map<?, ?> errors) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("error", errors);
        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<ProblemDetail> problemDetailResponseEntityCustom(String error, HttpStatus status) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setDetail(error);
        return new ResponseEntity<>(problemDetail, status);
    }
}
