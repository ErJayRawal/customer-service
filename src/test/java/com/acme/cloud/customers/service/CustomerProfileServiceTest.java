package com.acme.cloud.customers.service;

import com.acme.cloud.customers.dto.CustomerProfileRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CustomerProfileServiceTest {

    @Inject
    CustomerProfileService customerProfileService;

    @Test
    void createsAndUpdatesAProfileForTheSameExternalIdentity() {
        String issuer = "https://issuer.example/" + UUID.randomUUID();
        String subject = UUID.randomUUID().toString();

        var created = customerProfileService.updateProfile(
                issuer,
                subject,
                new CustomerProfileRequest(" Ada Lovelace ", " ada@example.com ")
        );

        assertTrue(created.created());
        assertNotNull(created.profile().customerId());
        assertEquals("Ada Lovelace", created.profile().fullName());
        assertEquals("ada@example.com", created.profile().email());
        assertNotNull(created.profile().createdAt());

        var updated = customerProfileService.updateProfile(
                issuer,
                subject,
                new CustomerProfileRequest("Ada Byron", "ada.byron@example.com")
        );
        var loaded = customerProfileService.getProfile(issuer, subject);

        assertFalse(updated.created());
        assertEquals(created.profile().customerId(), updated.profile().customerId());
        assertEquals(created.profile().customerId(), loaded.customerId());
        assertEquals("Ada Byron", loaded.fullName());
        assertEquals("ada.byron@example.com", loaded.email());
        assertEquals(created.profile().createdAt().toEpochMilli(), loaded.createdAt().toEpochMilli());
    }

    @Test
    void returnsNotFoundForAnUnmappedIdentity() {
        assertThrows(
                NotFoundException.class,
                () -> customerProfileService.getProfile(
                        "https://issuer.example/" + UUID.randomUUID(),
                        UUID.randomUUID().toString()
                )
        );
    }
}
