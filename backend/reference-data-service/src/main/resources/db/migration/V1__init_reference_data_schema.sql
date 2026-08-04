CREATE TABLE reference_types (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_reference_types_code UNIQUE (code)
);

CREATE TABLE reference_values (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_id    UUID         NOT NULL REFERENCES reference_types(id) ON DELETE CASCADE,
    value      VARCHAR(255) NOT NULL,
    code       VARCHAR(50)  NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_reference_values_type_code UNIQUE (type_id, code)
);

CREATE INDEX idx_reference_values_type_id ON reference_values(type_id);

INSERT INTO reference_types (id, code, description, active) VALUES
    ('3f9c3c4c-0001-4a1e-8c8e-000000000001', 'INTERNET', 'Internet Service Providers', TRUE),
    ('3f9c3c4c-0001-4a1e-8c8e-000000000002', 'AIRTIME', 'Mobile Network Operators', TRUE),
    ('3f9c3c4c-0001-4a1e-8c8e-000000000003', 'BILLER', 'Bill Payment Providers', TRUE);

INSERT INTO reference_values (type_id, value, code, sort_order, active) VALUES
    ('3f9c3c4c-0001-4a1e-8c8e-000000000001', 'MPT', 'MPT01', 1, TRUE),
    ('3f9c3c4c-0001-4a1e-8c8e-000000000001', 'GTV', 'GTV01', 2, TRUE),
    ('3f9c3c4c-0001-4a1e-8c8e-000000000001', 'Ooredoo', 'OOR01', 3, TRUE),
    ('3f9c3c4c-0001-4a1e-8c8e-000000000002', 'MPT', 'MPT', 1, TRUE),
    ('3f9c3c4c-0001-4a1e-8c8e-000000000002', 'Telenor', 'TLN', 2, TRUE),
    ('3f9c3c4c-0001-4a1e-8c8e-000000000002', 'Ooredoo', 'OOR', 3, TRUE),
    ('3f9c3c4c-0001-4a1e-8c8e-000000000002', 'Mytel', 'MTL', 4, TRUE),
    ('3f9c3c4c-0001-4a1e-8c8e-000000000003', 'Electricity', 'ELEC', 1, TRUE),
    ('3f9c3c4c-0001-4a1e-8c8e-000000000003', 'Water', 'WATER', 2, TRUE),
    ('3f9c3c4c-0001-4a1e-8c8e-000000000003', 'Internet', 'INTERNET', 3, TRUE);
