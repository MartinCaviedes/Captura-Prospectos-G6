BEGIN;
CREATE TABLE schema_version (
    version integer PRIMARY KEY,
    description varchar(200) NOT NULL,
    installed_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE usuarios (
    id varchar(64) PRIMARY KEY,
    username varchar(120) NOT NULL UNIQUE,
    email varchar(160) NOT NULL UNIQUE,
    display_name varchar(120) NOT NULL,
    password_hash varchar(255) NOT NULL,
    rol varchar(32) NOT NULL CHECK (rol IN ('ADMIN', 'CAPTADOR', 'SUPERVISOR')),
    sedes_asignadas varchar(255) NOT NULL,
    activo boolean NOT NULL
);
INSERT INTO schema_version(version, description) VALUES (1, 'Esquema inicial de usuarios');
COMMIT;
