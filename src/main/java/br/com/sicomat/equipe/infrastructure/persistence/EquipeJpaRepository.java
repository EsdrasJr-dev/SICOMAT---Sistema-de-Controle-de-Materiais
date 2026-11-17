package br.com.sicomat.equipe.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipeJpaRepository extends JpaRepository<EquipeJpaEntity, Long> {

    List<EquipeJpaEntity> findAllBySetorAndAtivoTrueOrderByNomeAsc(String setor);

    Optional<EquipeJpaEntity> findByIdAndSetor(long id, String setor);

    boolean existsBySetorAndNomeIgnoreCase(String setor, String nome);
}
