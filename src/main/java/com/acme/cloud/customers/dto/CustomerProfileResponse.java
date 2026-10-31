package com.acme.cloud.customers.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerProfileResponse(
        UUID customerId,
        String fullName,
        String email,
        Instant createdAt,
        Instant updatedAt
) {
}
