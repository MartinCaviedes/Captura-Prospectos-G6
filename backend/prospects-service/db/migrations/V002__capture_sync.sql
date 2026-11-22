BEGIN;
CREATE TABLE capture_sync (
    local_capture_id uuid PRIMARY KEY,
    version bigint NOT NULL,
    remote_id uuid NOT NULL UNIQUE REFERENCES prospects(id),
    device_id uuid NOT NULL,
    source_revision bigint NOT NULL CHECK (source_revision > 0),
    prospect_version bigint NOT NULL,
    request_hash varchar(64) NOT NULL,
    snapshot_json text NOT NULL
);
INSERT INTO schema_version(version, description) VALUES (2, 'Enlaces y snapshots de sincronizacion idempotente');
COMMIT;
