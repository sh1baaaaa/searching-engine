package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.entity.IndexEntity;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    Boolean existsByPagePathAndLemmaLemma(String pagePath, String lemma);

    void deleteByPagePathAndLemmaLemma(String pagePath, String lemma);
}
