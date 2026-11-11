package io.github.grupo6.users.service;

import io.github.grupo6.users.model.RolUsuario;
import io.github.grupo6.users.model.Usuario;
import io.github.grupo6.users.repository.UsuarioRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de negocio para autenticación y administración de usuarios persistidos.
 */
@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Inicializa el servicio inyectando el repositorio de usuarios y el codificador BCrypt.
     */
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Carga un usuario desde la base de datos por username o email para Spring Security.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        String limpio = identificador == null ? "" : identificador.trim();
        Usuario usuario = usuarioRepository
            .findByUsernameIgnoreCaseOrEmailIgnoreCase(limpio, limpio)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + limpio));

        return User.withUsername(usuario.getUsername())
            .password(usuario.getPasswordHash())
            .disabled(!usuario.isActivo())
            .roles(usuario.getRol().name())
            .build();
    }

    /**
     * Busca una cuenta de usuario persistida por su username o correo electrónico.
     */
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return Optional.empty();
        }
        String limpio = identificador.trim();
        return usuarioRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(limpio, limpio);
    }

    /**
     * Obtiene la lista completa de cuentas de usuario registradas en la base de datos.
     */
    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    /**
     * Busca una cuenta de usuario específica mediante su identificador único UUID.
     */
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    /**
     * Registra un nuevo usuario en base de datos cifrando su contraseña con BCrypt.
     */
    @Transactional
    public Usuario crearUsuario(String username, String email, String displayName,
                                String rawPassword, RolUsuario rol, String sedesAsignadas) {
        String userNormalizado = username.trim();
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByUsernameIgnoreCase(userNormalizado)
                || usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new IllegalArgumentException("El username o correo electrónico ya está registrado");
        }
        Usuario nuevo = new Usuario(
            UUID.randomUUID().toString(),
            userNormalizado,
            emailNormalizado,
            displayName.trim(),
            passwordEncoder.encode(rawPassword),
            rol,
            sedesAsignadas == null || sedesAsignadas.isBlank() ? "site-1" : sedesAsignadas.trim(),
            true
        );
        return usuarioRepository.save(nuevo);
    }

    /**
     * Actualiza el estado activo o inactivo de un usuario existente por su identificador.
     */
    @Transactional
    public Usuario cambiarEstadoActivo(String id, boolean activo) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));
        usuario.setActivo(activo);
        return usuarioRepository.save(usuario);
    }
}
