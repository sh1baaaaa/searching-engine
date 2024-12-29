package searchengine.services;

import searchengine.entity.IndexEntity;

import java.util.List;

public interface IndexService {

    Boolean isExistByLemmaLemmaAndPagePath(String lemma, String pagePath);

    List<IndexEntity> findByLemma(String lemma);

    List<IndexEntity> findByLemmaAndSite(String lemma, String  site);

    void save(IndexEntity indexEntity);

    Integer findLemmaCount(String lemma);

}
