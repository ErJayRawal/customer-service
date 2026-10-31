package com.acme.cloud.customers.service;

import com.acme.cloud.customers.dto.CustomerProfileRequest;
import com.acme.cloud.customers.dto.CustomerProfileResponse;
import com.acme.cloud.customers.entity.Customer;
import com.acme.cloud.customers.entity.CustomerIdentity;
import com.acme.cloud.customers.repository.CustomerIdentityRepository;
import com.acme.cloud.customers.repository.CustomerRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class CustomerProfileService {

    private final CustomerIdentityRepository customerIdentityRepository;
    private final CustomerRepository customerRepository;

    public CustomerProfileService(
            CustomerIdentityRepository customerIdentityRepository,
            CustomerRepository customerRepository
    ) {
        this.customerIdentityRepository = customerIdentityRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerProfileResponse getProfile(String issuer, String subject) {
        CustomerIdentity identity = customerIdentityRepository.findByIssuerAndSubject(issuer, subject)
                .orElseThrow(() -> new NotFoundException("Customer profile not found"));
        return toResponse(identity.getCustomer());
    }

    @Transactional
    public ProfileUpdateResult updateProfile(
            String issuer,
            String subject,
            CustomerProfileRequest request
    ) {
        var existingIdentity = customerIdentityRepository.findByIssuerAndSubject(issuer, subject);
        if (existingIdentity.isPresent()) {
            Customer customer = existingIdentity.get().getCustomer();
            customer.updateProfile(request.fullName(), request.email());
            return new ProfileUpdateResult(toResponse(customer), false);
        }

        Customer customer = new Customer(request.fullName(), request.email());
        customerRepository.persist(customer);
        customerIdentityRepository.persist(new CustomerIdentity(customer, issuer, subject));
        return new ProfileUpdateResult(toResponse(customer), true);
    }

    private CustomerProfileResponse toResponse(Customer customer) {
        return new CustomerProfileResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    public record ProfileUpdateResult(CustomerProfileResponse profile, boolean created) {
    }
}
