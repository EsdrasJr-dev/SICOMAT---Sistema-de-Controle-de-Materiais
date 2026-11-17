package br.com.sicomat.movimentacao.infrastructure.persistence;

import br.com.sicomat.movimentacao.StatusEmpenho;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmpenhoJpaRepository extends JpaRepository<EmpenhoJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select empenho from EmpenhoJpaEntity empenho where empenho.id = :id and empenho.setor = :setor")
    Optional<EmpenhoJpaEntity> buscarPorIdDoSetorParaAtualizacao(
            @Param("id") long id, @Param("setor") String setor);

    Optional<EmpenhoJpaEntity> findByIdAndSetor(long id, String setor);

    Page<EmpenhoJpaEntity> findAllBySetorAndStatusInOrderByDataHoraDesc(
            String setor, Collection<StatusEmpenho> status, Pageable pageable);
}
