package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.PageEntity;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    @Transactional()
    void deleteByPath(String path);

    Boolean existsByPath(String path);

    PageEntity findByPath(String path);

    PageEntity findByPathAndSiteUrl(String path, String siteUrl);

}
