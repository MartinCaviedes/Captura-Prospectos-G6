package io.github.grupo6.users.repository;

import io.github.grupo6.users.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la gestión y consulta de cuentas de usuario persistidas.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    /**
     * Busca un usuario por su nombre de usuario o correo electrónico ignorando mayúsculas.
     */
    Optional<Usuario> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    /**
     * Busca un usuario coincidente con el nombre de usuario ignorando mayúsculas.
     */
    Optional<Usuario> findByUsernameIgnoreCase(String username);

    /**
     * Busca un usuario coincidente con el correo electrónico ignorando mayúsculas.
     */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    /**
     * Verifica si ya existe una cuenta registrada con el nombre de usuario indicado.
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Verifica si ya existe una cuenta registrada con el correo electrónico indicado.
     */
    boolean existsByEmailIgnoreCase(String email);
}
