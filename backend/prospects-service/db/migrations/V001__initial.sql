BEGIN;
CREATE TABLE schema_version (
    version integer PRIMARY KEY,
    description varchar(200) NOT NULL,
    installed_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE prospects (
    id uuid PRIMARY KEY,
    version bigint NOT NULL,
    rut varchar(12) NOT NULL,
    event_id uuid NOT NULL,
    site_id varchar(120) NOT NULL,
    captor_id uuid NOT NULL,
    given_names varchar(120) NOT NULL,
    family_names varchar(120) NOT NULL,
    email varchar(160) NOT NULL,
    phone varchar(20) NOT NULL,
    consent_version varchar(80) NOT NULL,
    consent_recorded_at timestamp(6) with time zone NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    cancelled_at timestamp(6) with time zone,
    cancelled_by uuid,
    cancellation_reason varchar(500),
    active_key varchar(64),
    CONSTRAINT uk_prospect_active UNIQUE (active_key)
);
INSERT INTO schema_version(version, description) VALUES (1, 'Esquema inicial de prospectos');
COMMIT;
