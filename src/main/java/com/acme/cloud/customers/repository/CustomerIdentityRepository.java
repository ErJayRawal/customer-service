package com.acme.cloud.customers.repository;

import com.acme.cloud.customers.entity.CustomerIdentity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class CustomerIdentityRepository implements PanacheRepository<CustomerIdentity> {

    public Optional<CustomerIdentity> findByIssuerAndSubject(String issuer, String subject) {
        return find("issuer = ?1 and subject = ?2", issuer, subject).firstResultOptional();
    }
}
