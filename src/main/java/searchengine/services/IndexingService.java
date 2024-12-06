package searchengine.services;

import searchengine.entity.SiteEntity;

public interface IndexingService {

    void startIndexing();

    void stopIndexing();

    void deleteByName(String name);

    SiteEntity findByUrl(String url);

    Boolean existByUrl(String url);

}
