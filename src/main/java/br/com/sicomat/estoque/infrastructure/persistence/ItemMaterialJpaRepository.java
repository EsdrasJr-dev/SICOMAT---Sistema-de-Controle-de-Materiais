package br.com.sicomat.estoque.infrastructure.persistence;

import br.com.sicomat.estoque.StatusItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemMaterialJpaRepository extends JpaRepository<ItemMaterialJpaEntity, Long> {

    boolean existsBySetorAndNumeroPatrimonioIgnoreCase(String setor, String numeroPatrimonio);

    long countByMaterial_Id(long materialId);

    long countByMaterial_IdAndStatus(long materialId, StatusItem status);

    List<ItemMaterialJpaEntity> findAllByMaterial_Id(long materialId);

    List<ItemMaterialJpaEntity> findAllByMaterial_SetorAndStatusOrderByNumeroPatrimonio(
            String setor, StatusItem status);
}