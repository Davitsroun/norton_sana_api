package com.leang.authservice.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public record MinioProps(
        String url, String accessName, String accessSecret, String bucketName) {
}

