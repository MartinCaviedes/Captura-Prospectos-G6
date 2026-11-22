BEGIN;
-- Ejecutar sobre V001 con rol migrador. No borrar/recrear usuarios ni cambiar sus asignaciones.
ALTER TABLE usuarios ALTER COLUMN username TYPE varchar(160);
ALTER TABLE usuarios ADD COLUMN status varchar(16) NOT NULL DEFAULT 'ACTIVE';
UPDATE usuarios SET status = CASE WHEN activo THEN 'ACTIVE' ELSE 'DISABLED' END;
ALTER TABLE usuarios ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE usuarios ADD CONSTRAINT usuarios_status_check CHECK (status IN ('PENDING','ACTIVE','DISABLED'));
ALTER TABLE usuarios ADD CONSTRAINT usuarios_active_status CHECK (activo = (status = 'ACTIVE'));
ALTER TABLE usuarios ADD CONSTRAINT usuarios_version_check CHECK (version >= 0);
-- Si hay duplicados previos por mayúsculas, falla/rollback sin fusionar ni eliminar cuentas.
CREATE UNIQUE INDEX usuarios_username_lower ON usuarios(lower(username));
CREATE UNIQUE INDEX usuarios_email_lower ON usuarios(lower(email));
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM usuarios a JOIN usuarios b ON lower(a.username)=lower(b.email) AND a.id<>b.id) THEN
        RAISE EXCEPTION 'Identificadores de acceso ambiguos: revisar datos anteriores sin fusionarlos automáticamente';
    END IF;
    IF EXISTS (
        SELECT 1 FROM usuarios u WHERE u.activo AND u.rol <> 'ADMIN' AND (
            btrim(u.sedes_asignadas) = '' OR EXISTS (
                SELECT 1 FROM unnest(string_to_array(u.sedes_asignadas, ',')) AS s(value)
                WHERE btrim(s.value) NOT IN ('site-1','site-2','site-3','site-4','site-5','site-6','site-7','site-8')
            ) OR cardinality(string_to_array(u.sedes_asignadas, ',')) <> (
                SELECT count(DISTINCT btrim(s.value)) FROM unnest(string_to_array(u.sedes_asignadas, ',')) AS s(value)
            )
        )
    ) THEN
        RAISE EXCEPTION 'Cuentas activas con asignaciones inválidas: revisar datos anteriores sin inventar permisos';
    END IF;
END $$;
CREATE TABLE account_access_audit (
    id varchar(64) PRIMARY KEY,
    actor_id varchar(64) NOT NULL REFERENCES usuarios(id),
    target_id varchar(64) NOT NULL REFERENCES usuarios(id),
    before_status varchar(16) NOT NULL,
    after_status varchar(16) NOT NULL,
    before_role varchar(32) NOT NULL,
    after_role varchar(32) NOT NULL,
    before_sites varchar(255) NOT NULL,
    after_sites varchar(255) NOT NULL,
    changed_at timestamp with time zone NOT NULL,
    target_version bigint NOT NULL
);
CREATE INDEX account_audit_target_time ON account_access_audit(target_id, changed_at);
CREATE INDEX usuarios_status_created ON usuarios(status, created_at, id);
INSERT INTO schema_version(version, description) VALUES (2, 'Registro pendiente y permisos administrativos');
COMMIT;
