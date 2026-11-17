package br.com.sicomat.equipe.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecebedorJpaRepository extends JpaRepository<RecebedorJpaEntity, Long> {

    List<RecebedorJpaEntity> findAllBySetorAndAtivoTrueOrderByNomeAsc(String setor);

    Optional<RecebedorJpaEntity> findByIdAndSetor(long id, String setor);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select recebedor from RecebedorJpaEntity recebedor where recebedor.id = :id and recebedor.setor = :setor")
    Optional<RecebedorJpaEntity> buscarPorIdDoSetorParaAtualizacao(
            @Param("id") long id, @Param("setor") String setor);

    boolean existsBySetorAndMatriculaIgnoreCase(String setor, String matricula);
}
