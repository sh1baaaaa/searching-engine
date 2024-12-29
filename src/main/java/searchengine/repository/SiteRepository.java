package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.SiteEntity;



@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "UPDATE site SET status = :status")
    void updateAllSitesStatus(String status);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "UPDATE site SET status = :status WHERE url = :url")
    void updateSiteStatusByUrl(String url, String status);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "UPDATE site SET status = :status, last_error = :errorMessage WHERE url = :url")
    void updateSiteStatusByUrl(String url, String status, String errorMessage);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "DELETE FROM site")
    void deleteAll();

    SiteEntity findByUrl(String url);

    Boolean existsByUrl(String url);


}
