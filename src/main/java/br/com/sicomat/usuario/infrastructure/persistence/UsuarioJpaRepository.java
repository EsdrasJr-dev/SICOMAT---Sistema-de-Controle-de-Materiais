package br.com.sicomat.usuario.infrastructure.persistence;

import br.com.sicomat.usuario.PerfilUsuario;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, Long> {

    Optional<UsuarioJpaEntity> findByMatriculaIgnoreCase(String matricula);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select usuario from UsuarioJpaEntity usuario where lower(usuario.matricula) = lower(:matricula)")
    Optional<UsuarioJpaEntity> buscarParaAutenticacao(@Param("matricula") String matricula);

    boolean existsByMatriculaIgnoreCase(String matricula);

    long countByPerfil(PerfilUsuario perfil);

    List<UsuarioJpaEntity> findAllByOrderByNomeAsc();
}