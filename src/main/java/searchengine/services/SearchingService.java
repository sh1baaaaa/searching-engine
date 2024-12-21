package searchengine.services;



import searchengine.entity.IndexEntity;
import searchengine.entity.PageEntity;

import java.util.List;

public interface SearchingService {

    List<IndexEntity> searchLemmas(String query, String offset, String limit);

    List<IndexEntity> searchLemmasByPage(String query, String offset, String limit, String site);


}
