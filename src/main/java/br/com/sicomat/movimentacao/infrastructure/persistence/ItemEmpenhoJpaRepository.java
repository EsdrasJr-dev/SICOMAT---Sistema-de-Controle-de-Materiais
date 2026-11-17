package br.com.sicomat.movimentacao.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemEmpenhoJpaRepository extends JpaRepository<ItemEmpenhoJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from ItemEmpenhoJpaEntity item join fetch item.itemMaterial " +
            "where item.empenho.id = :empenhoId order by item.id")
    List<ItemEmpenhoJpaEntity> buscarPorEmpenhoParaAtualizacao(@Param("empenhoId") long empenhoId);

    List<ItemEmpenhoJpaEntity> findAllByEmpenho_IdOrderById(long empenhoId);

    java.util.Optional<ItemEmpenhoJpaEntity> findByIdAndEmpenho_Id(long id, long empenhoId);
}
