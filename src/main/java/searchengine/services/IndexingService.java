package searchengine.services;


import searchengine.dto.IndexingResponse;

public interface IndexingService {

    void deleteAllSitesData();
    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();
    IndexingResponse indexPage(String url);


}
