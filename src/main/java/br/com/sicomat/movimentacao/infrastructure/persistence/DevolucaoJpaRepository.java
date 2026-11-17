package br.com.sicomat.movimentacao.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevolucaoJpaRepository extends JpaRepository<DevolucaoJpaEntity, Long> {

    List<DevolucaoJpaEntity> findAllByEmpenho_IdOrderByDataHoraAsc(long empenhoId);
}
