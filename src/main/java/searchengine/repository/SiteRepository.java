package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.SiteEntity;

import java.time.LocalDateTime;


@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Query
    SiteEntity findSiteByUrl(String url);

    Boolean existsByUrl(String url);

    @Modifying
    @Transactional
    @Query(value = "Update search_engine.site s set s.status_time = :time Where s.url = :url ", nativeQuery = true)
    void updateIndexingTimeByUrl(@Param("time") LocalDateTime time, @Param("url") String url);

    @Modifying
    @Transactional
    @Query(value = "Update search_engine.site s set s.status = :status Where s.url = :url ", nativeQuery = true)
    void updateStatusByUrl(@Param("status") String status, @Param("url") String url);


}
