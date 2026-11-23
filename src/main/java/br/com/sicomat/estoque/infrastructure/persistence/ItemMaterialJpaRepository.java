package br.com.sicomat.estoque.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sicomat.estoque.StatusItem;
import jakarta.persistence.LockModeType;

public interface ItemMaterialJpaRepository extends JpaRepository<ItemMaterialJpaEntity, Long> {

    boolean existsBySetorAndNumeroPatrimonioIgnoreCase(String setor, String numeroPatrimonio);

    long countByMaterial_Id(long materialId);

    long countByMaterial_IdAndStatus(long materialId, StatusItem status);

    long countByMaterial_IdAndStatusAndEmpenhoPendenteIsNull(long materialId, StatusItem status);

    java.util.Optional<ItemMaterialJpaEntity> findByIdAndSetor(long id, String setor);

    List<ItemMaterialJpaEntity> findAllBySetorOrderByNumeroPatrimonio(String setor);

    List<ItemMaterialJpaEntity> findAllByMaterial_SetorAndStatusOrderByNumeroPatrimonio(
        String setor, StatusItem status);

        List<ItemMaterialJpaEntity> findAllByMaterial_SetorAndStatusAndEmpenhoPendenteIsNullOrderByNumeroPatrimonio(
        String setor, StatusItem status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from ItemMaterialJpaEntity item join fetch item.material " +
            "where item.id in :ids and item.setor = :setor order by item.id")
    List<ItemMaterialJpaEntity> buscarPorIdsDoSetorParaAtualizacao(
            @Param("ids") List<Long> ids, @Param("setor") String setor);
}
