package br.com.sicomat.estoque.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialJpaRepository extends JpaRepository<MaterialJpaEntity, Long> {

    List<MaterialJpaEntity> findAllBySetorOrderByNomeAsc(String setor);

    Optional<MaterialJpaEntity> findByIdAndSetor(long id, String setor);

    boolean existsBySetorAndCodigoIgnoreCase(String setor, String codigo);
}