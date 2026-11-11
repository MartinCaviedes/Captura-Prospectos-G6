package io.github.grupo6.users.controller;

import io.github.grupo6.users.model.RolUsuario;
import io.github.grupo6.users.model.Usuario;
import io.github.grupo6.users.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para consulta de identidad actual, estado administrativo y gestión de usuarios.
 */
@RestController
public class StatusController {

    private final UsuarioService usuarioService;

    /**
     * Construye el controlador inyectando el servicio de usuarios persistidos.
     */
    public StatusController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public record CrearUsuarioRequest(
        @NotBlank @Size(max = 120) String username,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Size(min = 6, max = 128) String password,
        @NotNull RolUsuario rol,
        String sedesAsignadas
    ) {}

    public record EstadoUsuarioRequest(boolean activo) {}

    /**
     * Retorna el perfil e identidad del usuario autenticado a partir del token JWT.
     */
    @GetMapping("/api/v1/me")
    public Map<String, Object> identity(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("service", "users-service");
        respuesta.put("subject", jwt.getSubject());
        respuesta.put("roles", jwt.getClaimAsStringList("roles"));

        usuarioService.buscarPorIdentificador(jwt.getSubject()).ifPresent(usuario -> {
            respuesta.put("userId", usuario.getId());
            respuesta.put("username", usuario.getUsername());
            respuesta.put("email", usuario.getEmail());
            respuesta.put("displayName", usuario.getDisplayName());
            respuesta.put("rol", usuario.getRol().name());
            respuesta.put("siteIds", usuario.obtenerSedeIds());
            respuesta.put("activo", usuario.isActivo());
        });
        return respuesta;
    }

    /**
     * Verifica el acceso administrativo exclusivo para usuarios con el rol ADMIN.
     */
    @GetMapping("/api/v1/admin/status")
    public Map<String, String> admin() {
        return Map.of("service", "users-service", "status", "ok");
    }

    /**
     * Lista todas las cuentas de usuario persistidas para supervisión y administración.
     */
    @GetMapping("/api/v1/users")
    public List<Usuario> listarUsuarios() {
        return usuarioService.listarUsuarios();
    }

    /**
     * Obtiene el detalle de un usuario específico buscando por su identificador único.
     */
    @GetMapping("/api/v1/users/{id}")
    public ResponseEntity<Usuario> obtenerUsuarioPorId(@PathVariable String id) {
        return usuarioService.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Crea una nueva cuenta de usuario persistida con rol y sedes asignadas.
     */
    @PostMapping("/api/v1/users")
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario crearUsuario(@Valid @RequestBody CrearUsuarioRequest request) {
        return usuarioService.crearUsuario(
            request.username(),
            request.email(),
            request.displayName(),
            request.password(),
            request.rol(),
            request.sedesAsignadas()
        );
    }

    /**
     * Activa o desactiva el acceso de una cuenta de usuario existente.
     */
    @PatchMapping("/api/v1/users/{id}/estado")
    public Usuario cambiarEstado(@PathVariable String id,
                                 @RequestBody EstadoUsuarioRequest request) {
        return usuarioService.cambiarEstadoActivo(id, request.activo());
    }

    /**
     * Maneja errores de negocio como duplicidad o usuario inexistente devolviendo HTTP 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of("code", "BAD_REQUEST", "message", ex.getMessage());
    }
}
