package io.github.grupo6.users.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;

/**
 * Entidad JPA que representa una cuenta de usuario persistida y sus sedes asignadas.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @Column(length = 64, nullable = false, updatable = false)
    private String id;

    @Column(length = 120, nullable = false, unique = true)
    private String username;

    @Column(length = 160, nullable = false, unique = true)
    private String email;

    @Column(name = "display_name", length = 120, nullable = false)
    private String displayName;

    @JsonIgnore
    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private RolUsuario rol;

    @Column(name = "sedes_asignadas", length = 255, nullable = false)
    private String sedesAsignadas;

    @Column(nullable = false)
    private boolean activo = true;

    /**
     * Constructor vacío requerido por JPA para instanciar la entidad Usuario.
     */
    public Usuario() {
    }

    /**
     * Construye una cuenta de usuario completa con rol, credenciales cifradas y sedes asignadas.
     */
    public Usuario(String id, String username, String email, String displayName,
                   String passwordHash, RolUsuario rol, String sedesAsignadas, boolean activo) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.sedesAsignadas = sedesAsignadas;
        this.activo = activo;
    }

    /**
     * Convierte la cadena de sedes separadas por coma en una lista inmutable de identificadores.
     */
    public List<String> obtenerSedeIds() {
        if (sedesAsignadas == null || sedesAsignadas.isBlank()) {
            return List.of();
        }
        return Arrays.stream(sedesAsignadas.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    /**
     * Obtiene el identificador único UUID de la cuenta de usuario.
     */
    public String getId() {
        return id;
    }

    /**
     * Asigna el identificador único UUID de la cuenta de usuario.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre de usuario único utilizado para iniciar sesión.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Define el nombre de usuario único para autenticación en el sistema.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Obtiene el correo electrónico institucional normalizado del usuario.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Define el correo electrónico institucional asociado a la cuenta del usuario.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Obtiene el nombre visible del usuario para mostrar en la aplicación móvil.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Define el nombre visible del usuario para despliegue en interfaz.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Obtiene el hash BCrypt de la contraseña almacenada del usuario.
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Actualiza el hash BCrypt de la contraseña del usuario.
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Obtiene el rol operativo asignado al usuario dentro de la plataforma.
     */
    public RolUsuario getRol() {
        return rol;
    }

    /**
     * Asigna el rol operativo del usuario (ADMIN, CAPTADOR o SUPERVISOR).
     */
    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    /**
     * Obtiene los identificadores de sedes asignadas almacenados en formato texto.
     */
    public String getSedesAsignadas() {
        return sedesAsignadas;
    }

    /**
     * Define los identificadores de sedes asignadas separados por comas.
     */
    public void setSedesAsignadas(String sedesAsignadas) {
        this.sedesAsignadas = sedesAsignadas;
    }

    /**
     * Indica si la cuenta de usuario se encuentra habilitada para iniciar sesión.
     */
    public boolean isActivo() {
        return activo;
    }

    /**
     * Cambia el estado de habilitación de la cuenta de usuario.
     */
    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
