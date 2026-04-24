package horse_reserved.repository;

import horse_reserved.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su email
     * @param email Email del usuario
     * @return Optional con el usuario si existe
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Verifica si existe un usuario con el email especificado
     * @param email Email a verificar
     * @return true si existe, false si no
     */
    boolean existsByEmail(String email);

    /**
     * Busca usuarios por rol
     * @param role Rol a buscar (cliente, operador, administrador)
     * @return Lista de usuarios con ese rol
     */
    java.util.List<Usuario> findByRole(String role);

    /**
     * Busca usuarios activos
     * @param isActive Estado del usuario
     * @return Lista de usuarios con ese estado
     */
    java.util.List<Usuario> findByIsActive(Boolean isActive);

    Optional<Usuario> findByEmailAndIsActive(String email, Boolean isActive);

    boolean existsByEmailAndIsActive(String email, Boolean isActive);
}