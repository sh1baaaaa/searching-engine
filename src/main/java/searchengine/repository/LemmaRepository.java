package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.LemmaEntity;

import java.util.List;
import java.util.Set;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {


    Boolean existsByLemma(String lemma);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO search_engine.lemma (site_id, lemma, frequency) VALUES (:siteId, :lemma, 1) ON DUPLICATE KEY UPDATE frequency = (frequency + 1)", nativeQuery = true)
    void updateOrInsertLemma(@Param("siteId") Integer siteId, @Param("lemma") String lemma);

    @Query
    LemmaEntity findByLemma(String lemma);

    @Query(value = "select * from search_engine.lemma l where l.lemma in :lemmas order by frequency", nativeQuery = true)
    List<LemmaEntity> findLemmasInSet(@Param("lemmas") Set<String> lemmas);

}
