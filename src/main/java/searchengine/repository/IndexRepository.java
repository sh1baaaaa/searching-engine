package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.entity.IndexEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    Boolean existsByPagePathAndLemmaLemma(String pagePath, String lemma);

    void deleteByPagePathAndLemmaLemma(String pagePath, String lemma);

    List<IndexEntity> findByLemmaLemma(String lemma);

    @Query(nativeQuery = true
            , value = "SELECT COUNT(*) FROM `index` i JOIN lemma l ON i.lemma_id = l.id WHERE l.lemma = :lemma")
    Integer lemmaCount(String lemma);

    @Query(nativeQuery = true
            , value = "SELECT COUNT(*) FROM `index` i " +
            "JOIN lemma l ON i.lemma_id = l.id " +
            "JOIN site s ON l.site_id = s.id " +
            "WHERE l.lemma = :lemma AND s.name = :site")
    Integer lemmaCount(String lemma, String site);

    @Query(nativeQuery = true,
            value = "SELECT * FROM 'index' i " +
                    "JOIN page p ON i.page_id = p.id " +
                    "JOIN site s ON p.site_id = s.id " +
                    "JOIN lemma l ON i.lemma_id = l.id " +
                    "WHERE s.site = :site AND l.lemma = :lemma ")
    List<IndexEntity> findByLemmaAndSite(String lemma, String site);

}
