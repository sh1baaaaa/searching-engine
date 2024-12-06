package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.entity.SiteEntity;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    void deleteByName(String name);

    SiteEntity findByUrl(String url);

    Boolean existsByUrl(String url);
}
