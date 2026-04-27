package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.NotificationMessageRequest;
import com.leang.authservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/sendMessageToAllUsers")
    public ResponseEntity<Map<String, String>> sendMessageToAllUsers(@Valid @RequestBody NotificationMessageRequest request) {
        notificationService.sendMessageToAllUsers(request.message());
        return ResponseEntity.ok(Map.of("message", "Notification sent to all users"));
    }

    @PostMapping("/sendMessageToUser/{userId}")
    public ResponseEntity<Map<String, String>> sendMessageToUser(
            @PathVariable String userId,
            @Valid @RequestBody NotificationMessageRequest request) {
        notificationService.sendMessageToUser(userId, request.message());
        return ResponseEntity.ok(Map.of("message", "Notification sent to user"));
    }
}
