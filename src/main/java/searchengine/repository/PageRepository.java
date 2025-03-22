package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.entity.PageEntity;

import java.util.List;
import java.util.Set;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    @Query(value = """
            SELECT *
            FROM search_engine.page
            WHERE id IN (
                SELECT page_id
                FROM `index`
                WHERE lemma_id IN (
                    SELECT l.id
                    FROM search_engine.lemma l
                    JOIN search_engine.page p ON l.site_id = p.site_id
                    WHERE l.lemma IN :lemmas
                    GROUP BY l.id, l.frequency, l.site_id
                    HAVING (l.frequency / COUNT(p.site_id)) < 0.95
                    ORDER BY  l.frequency DESC
                )
                GROUP BY page_id
                HAVING COUNT(DISTINCT lemma_id) = :lemmaCount
            )
            AND site_id = CASE
            WHEN :siteId = 0 THEN site_id
            ELSE :siteId END""", nativeQuery = true)
    List<PageEntity> findPagesWithLemmasAndSite(@Param("lemmas") Set<String> lemmas, @Param("siteId") Integer siteModelId, @Param("lemmaCount")Integer lemmaCount);

}
