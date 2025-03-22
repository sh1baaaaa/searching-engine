package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.IndexEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    Boolean existsByPageIdPathAndLemmaLemma(String pagePath, String lemma);

    List<IndexEntity> findByLemmaLemma(String lemma);

    @Query(nativeQuery = true
            , value = "SELECT COUNT(*) FROM `index` i JOIN lemma l ON i.lemma_id = l.id WHERE l.lemma = :lemma")
    Integer lemmaCount(String lemma);


    @Query(nativeQuery = true,
            value = "SELECT * FROM `index` i " +
                    "JOIN page p ON i.page_id = p.id " +
                    "JOIN site s ON p.site_id = s.id " +
                    "JOIN lemma l ON i.lemma_id = l.id " +
                    "WHERE s.url = :site AND l.lemma = :lemma ")
    List<IndexEntity> findByLemmaAndSite(String lemma, String site);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "DELETE FROM `index`")
    void deleteAll();

    @Query(value = "select * from `index` i where i.page_id = :pageId and i.lemma_id in :lemmas"
            , nativeQuery = true)
    List<IndexEntity> findIndexByPageIdAndLemmas(@Param("pageId")Integer pageId, @Param("lemmas") List<Integer> lemmas);
}
