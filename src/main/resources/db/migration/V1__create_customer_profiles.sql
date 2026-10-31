CREATE TABLE customers
(
    id         UUID PRIMARY KEY,
    full_name  VARCHAR(200)              NOT NULL,
    email      VARCHAR(320)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE  NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE  NOT NULL
);

CREATE TABLE customer_identities
(
    id          UUID PRIMARY KEY,
    customer_id UUID         NOT NULL,
    issuer      VARCHAR(512) NOT NULL,
    subject     VARCHAR(255) NOT NULL,
    CONSTRAINT uq_customer_identity_issuer_subject UNIQUE (issuer, subject),
    CONSTRAINT fk_customer_identity_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE INDEX idx_customer_identity_customer_id ON customer_identities (customer_id);
