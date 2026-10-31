package com.acme.cloud.customers.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "customer_identities", uniqueConstraints = {
        @UniqueConstraint(name = "uq_customer_identity_issuer_subject", columnNames = {"issuer", "subject"})
})
public class CustomerIdentity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_customer_identity_customer")
    )
    private Customer customer;

    @Column(name = "issuer", nullable = false, updatable = false, length = 512)
    private String issuer;

    @Column(name = "subject", nullable = false, updatable = false, length = 255)
    private String subject;

    protected CustomerIdentity() {
    }

    public CustomerIdentity(Customer customer, String issuer, String subject) {
        this.id = UUID.randomUUID();
        this.customer = customer;
        this.issuer = issuer;
        this.subject = subject;
    }

    public Customer getCustomer() {
        return customer;
    }
}
