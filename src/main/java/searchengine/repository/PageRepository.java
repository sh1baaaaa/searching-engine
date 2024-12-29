package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.PageEntity;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "DELETE FROM page")
    void deleteAll();

    Boolean existsByPath(String path);

    PageEntity findByPathAndSiteUrl(String path, String siteUrl);

}
